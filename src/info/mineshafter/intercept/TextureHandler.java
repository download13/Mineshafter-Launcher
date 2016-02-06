package info.mineshafter.intercept;

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

import info.mineshafter.util.Streams;

public class TextureHandler implements Handler {
	private Map<String, URL> skinLookup = new ConcurrentHashMap<String, URL>();
	private Map<String, URL> capeLookup = new ConcurrentHashMap<String, URL>();

	private static String textureHost = "textures.minecraft.net";
	private static Pattern textureUrl = Pattern.compile("/texture/([0-9a-fA-F]+)");
	private static TextureHandler instance;

	public static synchronized TextureHandler getInstance() {
		if (instance == null) {
			instance = new TextureHandler();
		}

		return instance;
	}

	private TextureHandler() {}

	public boolean handle(URL req) {
		if(!textureHost.equalsIgnoreCase(req.getHost())) {
			return false;
		}

		Matcher m = textureUrl.matcher(req.getPath());
		if(!m.matches()) {
			return false;
		}

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
			Matcher m = textureUrl.matcher(req.path);
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

			HttpURLConnection conn = (HttpURLConnection) skinUrl.openConnection();
			byte[] data = Streams.toByteArray(conn.getInputStream());

			Response r = new Response(data);

			return r;
		} catch (IOException e) {
			e.printStackTrace();

			return null;
		}
	}

	// Use the URL in the padding to make sure that when someone changes their skin, the cache doesn't keep serving the same one
	public String addSkin(String id, String skinUrl) {
		try {
			URL url = new URL(skinUrl);
			url = new URL("raw" + url.getProtocol(), url.getHost(), url.getPort(), url.getFile()); // Make sure we don't loop getting a handled url
			skinLookup.put(id, url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		// id: 32 chars, idPadding: 14 chars, urlPadding: 14 chars, type: 1 char
		String r = "http://textures.minecraft.net/texture/" + id + createPadding(id) + createPadding(skinUrl) + "0";
		//System.out.println(r + " = " + id + ":" + skinUrl);
		return r;
	}

	public String addCape(String id, String capeUrl) {
		try {
			URL url = new URL(capeUrl);
			url = new URL("raw" + url.getProtocol(), url.getHost(), url.getPort(), url.getFile());
			capeLookup.put(id, url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		String r = "http://textures.minecraft.net/texture/" + id + createPadding(id) + createPadding(capeUrl) + "1";
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
