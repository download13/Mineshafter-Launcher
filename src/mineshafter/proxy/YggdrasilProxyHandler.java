package mineshafter.proxy;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.Socket;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;

import com.google.gson.Gson;

import mineshafter.util.Streams;

public class YggdrasilProxyHandler extends Thread {
	private DataInputStream fromClient;
	private DataOutputStream toClient;
	private Socket connection;

	private YggdrasilProxy proxy;

	private static String[] BLACKLISTED_HEADERS = new String[] { "Connection", "Proxy-Connection", "Transfer-Encoding" };

	public YggdrasilProxyHandler(YggdrasilProxy proxy, Socket conn) throws IOException {
		setName("YggdrasilProxyHandler Thread");

		this.proxy = proxy;

		connection = conn;
		fromClient = new DataInputStream(conn.getInputStream());
		toClient = new DataOutputStream(conn.getOutputStream());
	}

	public void run() {
		HashMap<String, String> headers = new HashMap<String, String>();
		System.out.println("Proxy Request Started");
		// Read the incoming request
		String[] requestLine = readUntil(fromClient, '\n').split(" ");
		String method = requestLine[0].trim().toUpperCase();
		String url = requestLine[1].trim();

		System.out.println("Proxy Request: " + method + " " + url);

		// Read the incoming headers
		// System.out.println("Headers:");
		String header;
		do {
			header = readUntil(fromClient, '\n').trim();
			// System.out.println("H: " + header + ", " + header.length());
			int splitPoint = header.indexOf(':');
			if (splitPoint != -1) {
				headers.put(header.substring(0, splitPoint).toLowerCase().trim(), header.substring(splitPoint + 1).trim());
			}
		} while (header.length() > 0);

		Matcher skinMatcher = YggdrasilProxy.SKIN_URL.matcher(url);
		Matcher cloakMatcher = YggdrasilProxy.CLOAK_URL.matcher(url);
		Matcher authServerMatcher = YggdrasilProxy.AUTHSERVER_URL.matcher(url);

		byte[] data = null;

		if (skinMatcher.matches()) {
			System.out.println("Proxy: Skin");

			String username = skinMatcher.group(1);
			if (proxy.skinCache.containsKey(username)) { // Is the skin in the
															// cache?
				System.out.println("Skin from cache");

				data = proxy.skinCache.get(username); // Then get it from there
			} else {
				url = "http://" + YggdrasilProxy.authServer + "/mcapi/skin/" + username + ".png";
				System.out.println("To: " + url);

				data = getRequest(url); // Then get it...
				System.out.println("Response length: " + data.length);

				proxy.skinCache.put(username, data); // And put it in there
			}

		} else if (cloakMatcher.matches()) {
			System.out.println("Proxy: Cloak");

			String username = cloakMatcher.group(1);
			if (proxy.cloakCache.containsKey(username)) {
				System.out.println("Cloak from cache");
				data = proxy.cloakCache.get(username);
			} else {
				url = "http://" + YggdrasilProxy.authServer + "/mcapi/cloak/" + username + ".png";
				System.out.println("To: " + url);

				data = getRequest(url);
				System.out.println("Response length: " + data.length);

				proxy.cloakCache.put(username, data);
			}

		} else if (authServerMatcher.matches()) {
			System.out.println("Proxy: authserver");

			String endpoint = authServerMatcher.group(1);
			try {
				int postlen = Integer.parseInt(headers.get("content-length"));
				char[] postdata = new char[postlen];
				InputStreamReader reader = new InputStreamReader(fromClient);
				reader.read(postdata);
				String postedJSON = new String(postdata);
				data = authServerEndpoint(endpoint, postedJSON).getBytes();

			} catch (IOException e) {
				System.out.println("Unable to read POST data from getversion request");
				e.printStackTrace();
			}

		} else {
			System.out.println("Proxy: No handler. Piping.");

			try {
				if (!url.startsWith("http://") && !url.startsWith("https://")) {
					url = "http://" + url;
				}
				URL u = new URL(url);
				if (method.equals("CONNECT")) {
					int port = u.getPort();
					if (port == -1) port = 80;
					System.out.println("proxy osemthing url: " + u.getHost() + " , " + u.getPort());
					Socket sock = new Socket(u.getHost(), port);

					Streams.pipeStreamsActive(fromClient, new BufferedOutputStream(sock.getOutputStream()));
					Streams.pipeStreamsActive(new BufferedInputStream(sock.getInputStream()), toClient);
					return;

				} else if (method.equals("GET") || method.equals("POST")) {
					HttpURLConnection c = (HttpURLConnection) u.openConnection(Proxy.NO_PROXY);
					c.setRequestMethod(method);
					boolean post = method.equals("POST");

					for (String k : headers.keySet()) {
						c.setRequestProperty(k, headers.get(k)); // TODO Might
																	// need to
																	// blacklist
																	// these as
																	// well
																	// later
					}

					if (post) {
						c.setDoInput(true);
						c.setDoOutput(true);
						c.setUseCaches(false);
						c.connect();
						int postlen = Integer.parseInt(headers.get("content-length"));
						byte[] postdata = new byte[postlen];
						fromClient.read(postdata);

						DataOutputStream os = new DataOutputStream(c.getOutputStream());
						os.write(postdata);
					}
					// Collect the headers from the server and retransmit them
					int responseCode = c.getResponseCode();
					String res = "HTTP/1.0 " + responseCode + " " + c.getResponseMessage() + "\r\n";
					res += "Connection: close\r\nProxy-Connection: close\r\n";

					Map<String, List<String>> h = c.getHeaderFields();
					headerloop: for (String k : h.keySet()) {
						if (k == null) continue;
						k = k.trim();
						for (String forbiddenHeader : BLACKLISTED_HEADERS) {
							if (k.equalsIgnoreCase(forbiddenHeader)) continue headerloop;
						}

						List<String> vals = h.get(k);
						for (String v : vals) {
							res += k + ": " + v + "\r\n";
						}
					}
					res += "\r\n";

					// System.out.println(res);
					int size = -1;
					if (responseCode / 100 != 5) {
						toClient.writeBytes(res);
						size = Streams.pipeStreams(c.getInputStream(), toClient);
					}

					toClient.close();
					connection.close();

					System.out.println("Piping finished, data size: " + size);

				} else if (method.equals("HEAD")) {
					HttpURLConnection c = (HttpURLConnection) u.openConnection(Proxy.NO_PROXY);
					c.setRequestMethod("HEAD");

					for (String k : headers.keySet()) {
						c.setRequestProperty(k, headers.get(k));
					}

					String res = "HTTP/1.0 " + c.getResponseCode() + " " + c.getResponseMessage() + "\r\n";
					res += "Proxy-Connection: close\r\n";

					java.util.Map<String, java.util.List<String>> h = c.getHeaderFields();
					for (String k : h.keySet()) {
						if (k == null) continue;
						java.util.List<String> vals = h.get(k);
						for (String v : vals) {
							res += k + ": " + v + "\r\n";
						}
					}
					res += "\r\n";

					// System.out.println(res);

					toClient.writeBytes(res); // TODO Occasional exception
												// socket write error
					toClient.close();
					connection.close();

				} else {
					System.out.println("UNEXPECTED REQUEST TYPE: " + method);
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			return;
		}

		try {
			if (data != null) {
				toClient.writeBytes("HTTP/1.0 200 OK\r\nConnection: close\r\nProxy-Connection: close\r\nContent-Length: " + data.length + "\r\n");
				toClient.writeBytes("\r\n");
				toClient.write(data);
				toClient.flush();
			}
			fromClient.close();
			toClient.close();
			connection.close();
			// System.out.println(data.length);
			// System.out.println(new String(data));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String authServerEndpoint(String endpoint, String postedJSON) {
		Map<String, ProfileAuthentication> profiles = this.proxy.profiles;
		Gson gson = new Gson();
		YggdrasilRequestData data = gson.fromJson(postedJSON, YggdrasilRequestData.class);

		System.out.println("Proxy postedJSON: " + postedJSON);
		try {
			if (endpoint.equalsIgnoreCase("authenticate")) {
				MessageDigest accessTokenHash = MessageDigest.getInstance("MD5");
				accessTokenHash.update(data.username.getBytes());
				accessTokenHash.update(data.password.getBytes());
				accessTokenHash.update(Long.toString(System.currentTimeMillis()).getBytes());
				String accessToken = String.format("%1$032x", new Object[] { new BigInteger(1, accessTokenHash.digest()) });
				MessageDigest idHash = MessageDigest.getInstance("MD5");
				idHash.update(data.username.getBytes());
				String id = String.format("%1$032x", new Object[] { new BigInteger(1, idHash.digest()) });

				profiles.put(accessToken, new ProfileAuthentication(id, data.username));
				ProfileResponse pr = new ProfileResponse(id, data.username);
				YggdrasilAuthResponse r = new YggdrasilAuthResponse(data.clientToken, accessToken, pr, pr);
				System.out.println("Proxy authenticate response: " + gson.toJson(r));
				return gson.toJson(r);

			} else if (endpoint.equalsIgnoreCase("refresh")) {
				MessageDigest accessTokenHash = MessageDigest.getInstance("MD5");
				accessTokenHash.update(data.accessToken.getBytes());
				accessTokenHash.update(Long.toString(System.currentTimeMillis()).getBytes());
				String accessToken = String.format("%1$032x", new Object[] { new BigInteger(1, accessTokenHash.digest()) });

				ProfileAuthentication user = profiles.get(data.accessToken);
				if (user == null) { return "{\"error\":\"ForbiddenOperationException\",\"errorMessage\":\"Invalid token.\"}"; }
				profiles.remove(data.accessToken);
				profiles.put(accessToken, user);
				String displayName = user.displayName == null ? user.username : user.displayName;
				YggdrasilAuthResponse r = new YggdrasilAuthResponse(data.clientToken, accessToken, new ProfileResponse(user.uuid, displayName), null);
				System.out.println("Proxy refresh response: " + gson.toJson(r));
				return gson.toJson(r);

			} else if (endpoint.equalsIgnoreCase("invalidate")) {
				profiles.remove(data.accessToken);
				return "";
			} else {
				return "{\"error\":\"ForbiddenOperationException\",\"errorMessage\":\"Invalid credentials. Invalid username or password.\"}";
			}
		} catch (NoSuchAlgorithmException e) {
			return null;
		}
	}

	public static byte[] getRequest(String url) {
		try {
			HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection(Proxy.NO_PROXY);
			conn.setUseCaches(false);
			conn.setInstanceFollowRedirects(false);
			Map<String, List<String>> requestHeaders = conn.getRequestProperties();
			int code = conn.getResponseCode();

			if (code == 301 || code == 302 || code == 303) {
				System.out.println("Java didn't redirect automatically, going manual: " + Integer.toString(code));
				String l = conn.getHeaderField("location").trim();
				System.out.println("Manual redirection to: " + l);
				return getRequest(l);
			}

			System.out.println("Response: " + code);
			if (code == 403) {
				String s = "403 from req to " + url + "\nRequest headers:\n";

				for (String k : requestHeaders.keySet()) {
					if (k == null) continue;
					java.util.List<String> vals = requestHeaders.get(k);
					for (String v : vals) {
						s += k + ": " + v + "\n";
					}
				}

				s += "Response headers:\n";
				Map<String, List<String>> responseHeaders = conn.getHeaderFields();
				for (String k : responseHeaders.keySet()) {
					if (k == null) continue;
					java.util.List<String> vals = responseHeaders.get(k);
					for (String v : vals) {
						s += k + ": " + v + "\n";
					}
				}

				System.out.println(s);
				System.out.println("Contents:\n" + new String(grabData(conn.getErrorStream())));
			}

			if (code / 100 == 4) { return new byte[0]; }
			BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
			return grabData(in);

		} catch (MalformedURLException e) {
			System.out.println("Bad URL in getRequest:");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("IO error during a getRequest:");
			e.printStackTrace();
		}

		return new byte[0];
	}

	public static byte[] postRequest(String url, String postdata, String contentType) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		OutputStreamWriter writer = new OutputStreamWriter(out);
		// System.out.println("Postdata: " + postdata);

		try {
			writer.write(postdata);
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}

		byte[] rd = postRequest(url, out.toByteArray(), contentType);

		return rd;
	}

	public static byte[] postRequest(String url, byte[] postdata, String contentType) {
		try {
			URL u = new URL(url);

			HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection(Proxy.NO_PROXY);
			c.setDoOutput(true);
			c.setRequestMethod("POST");

			// System.out.println("Postdata_bytes: " + new String(postdata));

			c.setRequestProperty("Host", u.getHost());
			c.setRequestProperty("Content-Length", Integer.toString(postdata.length));
			c.setRequestProperty("Content-Type", contentType);

			BufferedOutputStream out = new BufferedOutputStream(c.getOutputStream());
			out.write(postdata);
			out.flush();
			out.close();

			byte[] data = grabData(new BufferedInputStream(c.getInputStream()));
			return data;

		} catch (java.net.UnknownHostException e) {
			System.out.println("Unable to resolve remote host, returning null");
			// e.printStackTrace();
		} catch (MalformedURLException e) {
			System.out.println("Bad URL when doing postRequest:");
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	public static byte[] grabData(InputStream in) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];

		while (true) {
			int len;
			try {
				len = in.read(buffer);
				if (len == -1) break;
			} catch (IOException e) {
				break;
			}
			out.write(buffer, 0, len);
		}

		return out.toByteArray();
	}

	public static String readUntil(DataInputStream is, String endSequence) {
		return readUntil(is, endSequence.getBytes());
	}

	public static String readUntil(DataInputStream is, char endSequence) {
		return readUntil(is, new byte[] { (byte) endSequence });
	}

	public static String readUntil(DataInputStream is, byte endSequence) {
		return readUntil(is, new byte[] { endSequence });
	}

	public static String readUntil(DataInputStream is, byte[] endSequence) { // If
																				// there
																				// is
																				// an
																				// edge
																				// case,
																				// make
																				// sure
																				// we
																				// can
																				// see
																				// it
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		String r = null;

		try {
			int i = 0;
			while (true) {
				boolean end = false;
				byte b;
				try {
					b = is.readByte(); // Read a byte
				} catch (EOFException e) {
					break;
				}
				if (b == endSequence[i]) { // If equal to current byte of
											// endSequence
					if (i == endSequence.length - 1) {
						end = true; // If we hit the end of endSequence, we're
									// done
					}
					i++; // Increment for next round
				} else {
					i = 0; // Reset
				}

				out.write(b);

				if (end) break;
			}
		} catch (IOException e) {
			System.out.println("readUntil unable to read from InputStream, endSeq: " + new String(endSequence));
			e.printStackTrace();
		}

		try {
			r = out.toString("UTF-8");
		} catch (java.io.UnsupportedEncodingException ex) {
			System.out.println("readUntil unable to encode data: " + out.toString());
			ex.printStackTrace();
		}

		return r;
	}
}
