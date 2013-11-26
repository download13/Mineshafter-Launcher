package info.mineshafter.proxy;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;

import info.mineshafter.Util;

public class YggdrasilProxyHandler implements HttpProxyHandler, SocksProxyHandler {
	public static String authServer = "mineshafter.info";

	public static Pattern SKIN_URL = Pattern.compile("http://skins\\.minecraft\\.net/MinecraftSkins/(.+?)\\.png");
	public static Pattern CLOAK_URL = Pattern.compile("http://skins\\.minecraft\\.net/MinecraftCloaks/(.+?)\\.png");
	public static Pattern AUTHSERVER_URL = Pattern.compile("http://authserver\\.mojang\\.com/(.*)");

	private Map<String, byte[]> skinCache = new Hashtable<String, byte[]>();
	private Map<String, byte[]> cloakCache = new Hashtable<String, byte[]>();

	private YggdrasilImpersonator imp = new YggdrasilImpersonator(new File(Util.getWorkingDirectory(), "launcher_profiles.json"));

	public YggdrasilProxyHandler() {}

	public boolean onConnect(InputStream in, OutputStream out, SocksMessage msg) {
		in.mark(0xffff);
		String firstLine = readUntil(in, '\n');
		String[] request = firstLine.split(" ");
		if(request.length != 3) {
			System.out.println("Not an HTTP request: " + firstLine);
			try {
				in.reset();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return false;
		}
		String method = request[0].toUpperCase();
		String path = request[1];

		Map<String, String> headers = new HashMap<String, String>();
		String header;
		do {
			header = readUntil(in, '\n');
			header = header.trim();
			int splitPoint = header.indexOf(':');
			if (splitPoint != -1) {
				headers.put(header.substring(0, splitPoint).toLowerCase().trim(), header.substring(splitPoint + 1).trim());
			}
		} while (header.length() > 0);

		String url = "http://" + headers.get("host") + path;
		System.out.println("Proxy - onConnect - url: " + url);

		if (method.equals("GET")) {
			if (onGET(url, headers, in, out)) return true;
		} else if (method.equals("POST")) {
			if (onPOST(url, headers, in, out)) return true;
		} else if (method.equals("HEAD")) {
			if (onHEAD(url, headers, in, out)) return true;
		}
		try {
			in.reset();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean onBind() {
		// XXX Figure out what to do here later
		return false;
	}

	public boolean onGET(String url, Map<String, String> headers, InputStream in, OutputStream out) {
		System.out.println("Proxy - onGET: " + url);
		Matcher skinMatcher = SKIN_URL.matcher(url);
		Matcher cloakMatcher = CLOAK_URL.matcher(url);

		byte[] data = null;

		if (skinMatcher.matches()) {
			System.out.println("Proxy: Skin");

			String username = skinMatcher.group(1);
			if (skinCache.containsKey(username)) {
				data = skinCache.get(username);
			} else {
				url = "http://" + authServer + "/mcapi/skin/" + username + ".png";
				System.out.println("To: " + url);
				data = getRequest(url);
				System.out.println("Response length: " + data.length);
				skinCache.put(username, data);
				sendResponse(out, "image/png", data);
				return true;
			}

		} else if (cloakMatcher.matches()) {
			System.out.println("Proxy: Cloak");

			String username = cloakMatcher.group(1);
			if (cloakCache.containsKey(username)) {
				data = cloakCache.get(username);
			} else {
				url = "http://" + authServer + "/mcapi/cloak/" + username + ".png";
				System.out.println("To: " + url);
				data = getRequest(url);
				System.out.println("Response length: " + data.length);
				cloakCache.put(username, data);
				sendResponse(out, "image/png", data);
				return true;
			}
		}

		return false;
	}

	public boolean onPOST(String url, Map<String, String> headers, InputStream in, OutputStream out) {
		int contentLength = Integer.parseInt(headers.get("content-length"));
		Matcher authServerMatcher = AUTHSERVER_URL.matcher(url);

		if (authServerMatcher.matches()) {
			System.out.println("Proxy: Authserver");

			String endpoint = authServerMatcher.group(1);
			try {
				char[] body = new char[contentLength];
				InputStreamReader reader = new InputStreamReader(in);
				reader.read(body);
				String postedJSON = new String(body);
				String response = authServerEndpoint(endpoint, postedJSON);
				sendResponse(out, "application/json", response);

			} catch (IOException e) {
				System.out.println("Unable to read POST data from getversion request");
				e.printStackTrace();
			}
			return true;
		}

		return false;
	}

	public boolean onHEAD(String url, Map<String, String> headers, InputStream in, OutputStream out) {
		return false;
	}

	public boolean onCONNECT(String url, Map<String, String> headers, InputStream in, OutputStream out) {
		return false;
	}

	private void sendResponse(OutputStream out, String contentType, String data) {
		sendResponse(out, contentType, data.getBytes());
	}

	private void sendResponse(OutputStream out, String contentType, byte[] data) {
		OutputStreamWriter writer = new OutputStreamWriter(out);
		try {
			writer.append("HTTP/1.0 200 OK\r\nConnection: close\r\nProxy-Connection: close\r\n");
			writer.append("Content-Length: " + Integer.toString(data.length) + "\r\n");
			if (contentType != null) {
				writer.append("Content-Type: " + contentType + "\r\n\r\n");
			}
			writer.flush();
			out.write(data);
			out.flush();
			out.close();
		} catch (IOException e) {
			System.out.println("Proxy - sendResponse error:");
			e.printStackTrace();
		}
	}

	private String authServerEndpoint(String endpoint, String postedJSON) {
		Gson gson = new Gson();
		YggdrasilRequest data = gson.fromJson(postedJSON, YggdrasilRequest.class);

		System.out.println("Proxy postedJSON: " + postedJSON);
		if (endpoint.equalsIgnoreCase("authenticate")) {
			String reply = imp.authenticate(data);
			System.out.println("Proxy authenticate response: " + reply);
			return reply;

		} else if (endpoint.equalsIgnoreCase("refresh")) {
			String reply = imp.refresh(data);
			System.out.println("Proxy refresh response: " + reply);
			return reply;

		} else if (endpoint.equalsIgnoreCase("invalidate")) {
			String reply = imp.invalidate(data);
			return reply;

		} else {
			return "";
		}
	}

	public static byte[] getRequest(String url) {
		try {
			HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection(Proxy.NO_PROXY);
			conn.setUseCaches(false);
			conn.setInstanceFollowRedirects(true);
			Map<String, List<String>> requestHeaders = conn.getRequestProperties();
			int code = conn.getResponseCode();

			if (code / 100 == 3) {
				System.out.println("Java didn't redirect automatically, going manual: " + Integer.toString(code));
				String loc = conn.getHeaderField("location").trim();
				System.out.println("Manual redirection to: " + loc);
				return getRequest(loc);
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

	public static String readUntil(InputStream is, String endSequence) {
		return readUntil(is, endSequence.getBytes());
	}

	public static String readUntil(InputStream is, char endSequence) {
		return readUntil(is, new byte[] { (byte) endSequence });
	}

	public static String readUntil(InputStream is, byte endSequence) {
		return readUntil(is, new byte[] { endSequence });
	}

	public static String readUntil(InputStream is, byte[] endSequence) { // If there is an edge case, make sure we can see it
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		try {
			int i = 0;
			while (true) {
				boolean end = false;
				byte b;
				try {
					b = (byte) is.read();
				} catch (EOFException e) {
					break;
				}
				if (b == endSequence[i]) { // If equal to current byte of endSequence
					if (i == endSequence.length - 1) end = true; // If we hit the end of endSequence, we're done
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
			return out.toString("UTF-8");
		} catch (java.io.UnsupportedEncodingException ex) {}

		return null;
	}
}
