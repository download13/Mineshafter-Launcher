package info.mineshafter.intercept;

import info.mineshafter.Util;
import info.mineshafter.datasources.MineshafterProfileAuthority;
import info.mineshafter.models.Profile;

import java.net.URL;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

public class YggdrasilImpersonator implements Handler {
	private static String HOST = "authserver.mojang.com";

	private static MineshafterProfileAuthority profiles = MineshafterProfileAuthority.getInstance();

	private static YggdrasilImpersonator instance;

	private YggdrasilImpersonator() {}

	public static synchronized YggdrasilImpersonator getInstance() {
		if (instance == null) {
			instance = new YggdrasilImpersonator();
		}

		return instance;
	}

	public boolean canHandle(URL url) {
		return url.getHost().equalsIgnoreCase(HOST);
	}

	public Response handle(Request req) {
		String r = "";

		JsonObject yggreq = JsonObject.readFrom(new String(req.body));

		String path = req.getPath().toLowerCase();

		if (path.equals("/authenticate")) {
			r = authenticate(yggreq);
		} else if (path.equals("/refresh")) {
			r = refresh(yggreq);
		} else {
			r = ""; // Send a blank response
			// This will work for /validate, /invalidate, and /signout
		}

		Response res = new Response(r.getBytes());
		res.setHeader("content-type", "application/json");
		res.setHeader("content-length", Integer.toString(res.body.length));

		return res;
	}

	public String authenticate(JsonObject req) {
		String username = req.get("username").asString();
		String password = req.get("password").asString();
		String clientToken = req.get("clientToken").asString();

		String accessToken = Util.getMd5AsUUID(username + password + Long.toString(System.currentTimeMillis()));

		Profile profile = profiles.searchProfile(username);
		JsonObject user = new JsonObject();

		user.set("id", profile.getId());
		user.set("name", profile.getName());

		JsonObject response = new JsonObject();
		response.set("clientToken", clientToken);
		response.set("accessToken", accessToken);
		response.set("selectedProfile", user);
		response.set("availableProfiles", new JsonArray().add(user));
		System.out.println(response.toString());

		return response.toString();
	}

	public String refresh(JsonObject req) {
		return "{\"error\":\"ForbiddenOperationException\",\"errorMessage\":\"Invalid token.\"}";
	}
}
