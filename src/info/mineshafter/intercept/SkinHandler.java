package info.mineshafter.intercept;

import info.mineshafter.util.Http;

import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SkinHandler implements Handler {
	public static String SKIN_SERVER = "mineshafter.info";

	public static String HOST = "skins.minecraft.net";
	public static Pattern SKIN_PATH = Pattern.compile("/MinecraftSkins/(.+?)\\.png");
	public static Pattern CLOAK_PATH = Pattern.compile("/MinecraftCloaks/(.+?)\\.png");

	private Map<String, byte[]> skinCache = new ConcurrentHashMap<String, byte[]>();
	private Map<String, byte[]> cloakCache = new ConcurrentHashMap<String, byte[]>();
	
	private static SkinHandler instance;

	private SkinHandler() {}
	
	public static synchronized SkinHandler getInstance() {
		if (instance == null) {
			instance = new SkinHandler();
		}

		return instance;
	}

	public boolean handle(URL url) {
		return url.getHost().equalsIgnoreCase(HOST);
	}

	public Response handle(Request req) {
		Matcher skinMatcher = SKIN_PATH.matcher(req.path);
		Matcher cloakMatcher = CLOAK_PATH.matcher(req.path);

		Response res;

		if (skinMatcher.matches()) {
			res = new Response(handleSkin(skinMatcher.group(1)));
			return res;
		} else if (cloakMatcher.matches()) {
			res = new Response(handleCloak(cloakMatcher.group(1)));
			return res;
		}

		return null;
	}

	public byte[] handleSkin(String username) {
		System.out.println("Proxy: Skin");

		if (!skinCache.containsKey(username)) {
			String url = "http://" + SKIN_SERVER + "/mcapi/skin/" + username + ".png";
			System.out.println("To: " + url);

			byte[] skindata = Http.getRequest(url);
			skinCache.put(username, skindata);
		}

		return skinCache.get(username);
	}

	public byte[] handleCloak(String username) {
		System.out.println("Proxy: Cloak");

		if (!cloakCache.containsKey(username)) {
			String url = "http://" + SKIN_SERVER + "/mcapi/cloak/" + username + ".png";
			System.out.println("To: " + url);

			byte[] cloakdata = Http.getRequest(url);
			cloakCache.put(username, cloakdata);
		}

		return cloakCache.get(username);
	}
}
