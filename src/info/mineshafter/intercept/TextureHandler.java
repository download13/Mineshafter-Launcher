package info.mineshafter.intercept;

import info.mineshafter.util.Streams;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

public class TextureHandler implements Handler {
	private Map<String, URL> skinLookup = new ConcurrentHashMap<String, URL>();
	private Map<String, URL> capeLookup = new ConcurrentHashMap<String, URL>();

	private static String textureHost = "textures.minecraft.net";
	private static Pattern textureUrl = Pattern.compile("/([0-9a-fA-F]+)");
	private static TextureHandler instance;

	public static synchronized TextureHandler getInstance() {
		if (instance == null) {
			instance = new TextureHandler();
		}

		return instance;
	}

	private TextureHandler() {}

	public boolean canHandle(URL req) {
		if (!textureHost.equalsIgnoreCase(req.getHost())) { return false; }

		Matcher m = textureUrl.matcher(req.getPath());
		if (!m.matches()) { return false; }

		String hash = m.group(1);
		char type = hash.charAt(60);

		if (type != '0' && type != '1') { return false; }

		String id = hash.substring(0, 32);
		String idPadding = hash.substring(32, 46);
		String padding = createPadding(id);

		return padding.equals(idPadding); // If equal, it's one of ours
	}

	public Response handle(Request req) {
		try {
			System.out.println("TextureHandler.handle: " + req.getPath());

			Matcher m = textureUrl.matcher(req.getPath());
			m.matches();
			String hash = m.group(1);
			String id = hash.substring(0, 32);
			char type = hash.charAt(60);

			URL skinUrl = null;
			if (type == '0') { // Skin
				skinUrl = skinLookup.get(id);
			} else if (type == '1') { // Cape
				skinUrl = capeLookup.get(id);
			}

			//System.out.println("TextureHandler.handle type: " + type);
			byte[] data = new byte[0];
			int responseCode = 404;
			if (skinUrl != null) {
				System.out.println("TextureHandler skinurl: " + skinUrl);

				String protocol = skinUrl.getProtocol();
				if (protocol.equalsIgnoreCase("https")) {
					HttpsURLConnection conn = (HttpsURLConnection) skinUrl.openConnection();
					conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.79 Safari/537.36 Edge/14.14393");
					conn.setInstanceFollowRedirects(true);
					responseCode = conn.getResponseCode();
					data = Streams.toByteArray(conn.getInputStream());

				} else if (protocol.equalsIgnoreCase("http")) {
					HttpURLConnection conn = (HttpURLConnection) skinUrl.openConnection();
					conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.79 Safari/537.36 Edge/14.14393");
					conn.setInstanceFollowRedirects(true);
					responseCode = conn.getResponseCode();
					data = Streams.toByteArray(conn.getInputStream());
				}

				System.out.println("Got " + Integer.toString(data.length) + " bytes of skin data");

				if (data.length == 0) { return new Response(responseCode, data); }
			} else {
				System.out.println("TextureHandler 404");
				return new Response(404, data);
			}
			System.out.println("Texture response code: " + responseCode);
			if (data.length == 0) { return new Response(responseCode, data); }
			return new Response(data);
		} catch (IOException e) {
			e.printStackTrace();

			return null;
		}
	}

	// Use the URL in the padding to make sure that when someone changes their skin, the cache doesn't keep serving the same one
	public String addSkin(String id, String skinUrl) {
		try {
			URL url = new URL(skinUrl);
			url = new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getFile()); // Make sure we don't loop getting a handled url
			skinLookup.put(id, url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return skinUrl;
		}

		// id: 32 chars, idPadding: 14 chars, urlPadding: 14 chars, type: 1 char
		String r = "http://textures.minecraft.net/" + id + createPadding(id) + createPadding(skinUrl) + "0";
		//System.out.println(r + " = " + id + ":" + skinUrl);
		return r;
	}

	public String addCape(String id, String capeUrl) {
		try {
			URL url = new URL(capeUrl);
			url = new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getFile());
			capeLookup.put(id, url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return capeUrl;
		}

		String r = "http://textures.minecraft.net/" + id + createPadding(id) + createPadding(capeUrl) + "1";
		//System.out.println(r + " = " + id + ":" + capeUrl);
		return r;
	}

	private static String createPadding(String str) {
		return createPadding(str, 14);
	}

	private static String createPadding(String str, int chars) {
		try {
			// id length is 16 bytes, 32 characters
			MessageDigest d = MessageDigest.getInstance("SHA-1");
			String hash = bytesToHex(d.digest(str.getBytes()));

			String padding = hash.substring(0, chars);

			return padding;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		return null;
	}

	private static char[] hexList = "0123456789abcdef".toCharArray();

	private static String bytesToHex(byte[] buf) {
		StringBuilder sb = new StringBuilder(buf.length * 2);
		for (byte b : buf) {
			sb.append(hexList[(b >>> 4) & 0x0f]);
			sb.append(hexList[b & 0x0f]);
		}
		return sb.toString();
	}
}
