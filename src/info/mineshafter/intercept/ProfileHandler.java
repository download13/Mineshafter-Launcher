package info.mineshafter.intercept;

import info.mineshafter.datasources.AutomaticProfileAuthority;
import info.mineshafter.models.Profile;

import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import biz.source_code.base64Coder.Base64Coder;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

public class ProfileHandler implements Handler {
	private static String SEARCH_HOST = "api.mojang.com";
	private static String GET_HOST = "sessionserver.mojang.com";

	private static Pattern SEARCH_PATH = Pattern.compile("/profiles/page/(.+?)");
	private static Pattern GET_PATH = Pattern.compile("/session/minecraft/profile/([0-9a-fA-F]+?)");

	private static AutomaticProfileAuthority profiles = AutomaticProfileAuthority.getInstance();

	private static ProfileHandler instance;

	private ProfileHandler() {}

	public static synchronized ProfileHandler getInstance() {
		if (instance == null) {
			instance = new ProfileHandler();
		}

		return instance;
	}

	public boolean canHandle(URL url) {
		return (url.getHost().equalsIgnoreCase(SEARCH_HOST) || url.getHost().equalsIgnoreCase(GET_HOST));
	}

	public Response handle(Request req) {
		String r = "";

		if (req.getHost().equalsIgnoreCase(GET_HOST)) {
			Matcher m = GET_PATH.matcher(req.getPath());
			// TODO: Console output
			if (m.matches()) {
				String uuid = m.group(1);

				r = getProfile(uuid);
			}
		} else if (req.getHost().equalsIgnoreCase(SEARCH_HOST)) {
			String body = new String(req.body);

			Matcher m = SEARCH_PATH.matcher(req.getPath());

			if (m.matches()) {
				int page = Integer.parseInt(m.group(1));

				r = searchProfile(page, body);
			}
		}

		Response res = new Response(r.getBytes());
		res.setHeader("content-type", "application/json");
		res.setHeader("content-length", Integer.toString(res.body.length));

		return res;
	}

	private String getProfile(String id) {
		System.out.println("ProfileHandler.getProfile " + id);

		Profile p = profiles.getProfile(id);

		JsonObject textures = new JsonObject();
                if (p.getSkin() != null){
                    textures.set("SKIN", new JsonObject().add("url", p.getSkin()));
                }
		if (p.getCape() != null){
                    textures.set("CAPE", new JsonObject().add("url", p.getCape()));
                }
		JsonObject textureProperty = new JsonObject();
		textureProperty.set("timestamp", System.currentTimeMillis());
		textureProperty.set("profileId", p.getId());
		textureProperty.set("profileName", p.getName());
		textureProperty.set("isPublic", true);
		textureProperty.set("textures", textures);

		String textureJSON = textureProperty.toString(); // Build the texture property info
		System.out.println(textureJSON);

		textureJSON = Base64Coder.encodeString(textureJSON); // Must be base64 encoded

		JsonObject profileProperty = new JsonObject();
		profileProperty.set("name", "textures");
		profileProperty.set("value", textureJSON);
		profileProperty.set("signature", "");

		JsonObject profileResponse = new JsonObject();
		profileResponse.set("id", p.getId());
		profileResponse.set("name", p.getName());
		profileResponse.set("properties", new JsonArray().add(profileProperty));

		String response = profileResponse.toString();
		System.out.println(response);

		return response;
	}

	private String searchProfile(int page, String body) {
		System.out.println("Proxy: ProfileName");

		// Request comes as an array of objects {name: <username>, agent: 'Minecraft'}
		JsonArray requests = JsonObject.readFrom(body).asArray();
		JsonArray profilesJson = new JsonArray();

		for (JsonValue r : requests) {
			Profile p = profiles.searchProfile(r.asObject().get("name").asString());

			JsonObject profileResult = new JsonObject();
			profileResult.set("id", p.getId());
			profileResult.set("name", p.getName());

			profilesJson.add(profileResult);
		}

		JsonObject searchResponse = new JsonObject();
		searchResponse.set("profiles", profilesJson);
		searchResponse.set("size", profilesJson.size());

		String response = searchResponse.toString();

		return response;
	}
}
