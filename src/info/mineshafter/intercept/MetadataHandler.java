package info.mineshafter.intercept;

import info.mineshafter.crypto.Hash;
import info.mineshafter.http.client.HttpClient;
import info.mineshafter.util.JarPatcher;
import info.mineshafter.util.Resources;

import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

public class MetadataHandler implements Handler {
	private static MetadataHandler instance;
	private static String METADATA_HOST = "launchermeta.mojang.com";
	private static String LIBRARY_HOST = "libraries.minecraft.net";
	private static Pattern manifestMatcher = Pattern.compile("/mc/game/");
	private static Map<String, byte[]> artifactCache = new ConcurrentHashMap<String, byte[]>();

	public static synchronized MetadataHandler getInstance() {
		if (instance == null) {
			instance = new MetadataHandler();
		}

		return instance;
	}

	private MetadataHandler() {}

	public boolean canHandle(URL url) {
		String host = url.getHost();
		if (host.equalsIgnoreCase(METADATA_HOST)) {
			Matcher m = manifestMatcher.matcher(url.getPath());
			return m.matches();
		} else if (host.equalsIgnoreCase(LIBRARY_HOST)) { return artifactCache.containsKey(url.toString()); }
		return false;
	}

	public Response handle(Request req) {
		Matcher m = manifestMatcher.matcher(req.getPath());
		if (m.matches()) return handleManifest(req);

		String cacheKey = req.getUrl().toString();
		if (artifactCache.containsKey(cacheKey)) { return new Response(artifactCache.get(cacheKey)); }

		return null;
	}

	private Response handleManifest(Request req) {
		String manifest = new String(HttpClient.get(req.getUrl()));
		JsonObject manifestJson = JsonObject.readFrom(manifest);
		JsonArray libraries = manifestJson.get("libraries").asArray();
		for (JsonValue v : libraries) {
			JsonObject lib = v.asObject();
			String name = lib.get("name").asString();
			if (name.contains("authlib")) {
				JsonObject artifact = lib.get("downloads").asObject().get("artifact").asObject();
				String auhtlibUrl = artifact.get("url").asString();
				byte[] authlibData = HttpClient.getRaw(auhtlibUrl);
				byte[] patchedAuthlibData = patchAuthlib(authlibData);
				String patchedAuthlibHash = Hash.sha1(patchedAuthlibData);
				artifact.set("sha1", patchedAuthlibHash);
				artifact.set("size", patchedAuthlibData.length);
			}
		}

		return new Response(manifestJson.toString());
	}

	private byte[] patchAuthlib(byte[] authlibData) {
		JarPatcher patcher = new JarPatcher(authlibData);
		patcher.setEntry("com/mojang/authlib/properties/Property.class", Resources.loadByteArray("resources/Property.class"));
		return patcher.write();
	}
}
