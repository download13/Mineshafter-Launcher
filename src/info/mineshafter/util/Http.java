package info.mineshafter.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class Http {

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

	public static void sendResponse(OutputStream out, String contentType, String data) {
		sendResponse(out, contentType, data.getBytes());
	}

	public static void sendResponse(OutputStream out, String contentType, byte[] data) {
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

	public static String readUntil(InputStream is, int length) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		try {
			for (int i = 0; i < length; i++) {
				try {
					byte b = (byte) is.read();
					out.write(b);
				} catch (EOFException e) {
					break;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			return out.toString("UTF-8");
		} catch (java.io.UnsupportedEncodingException ex) {}

		return null;
	}
}
