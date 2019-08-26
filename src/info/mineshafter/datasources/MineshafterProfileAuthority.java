package info.mineshafter.datasources;

import info.mineshafter.Util;
import info.mineshafter.intercept.TextureHandler;
import info.mineshafter.models.Profile;
import info.mineshafter.util.Streams;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import javax.net.ssl.HttpsURLConnection;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.ParseException;

public class MineshafterProfileAuthority {
	private static final String API_URL = "https://mineshafter.info/mcapi/profile";
	private static TextureHandler textureHandler = TextureHandler.getInstance();

	private static MineshafterProfileAuthority instance;

	private MineshafterProfileAuthority() {}

	public static synchronized MineshafterProfileAuthority getInstance() {
		if (instance == null) {
			instance = new MineshafterProfileAuthority();
		}

		return instance;
	}

	// id is the uuid of the user, returns a Profile object
	public Profile getProfile(String id) {
		String uuid = id;
		System.out.println("MineshafterProfileClient.getProfile(" + uuid + ")");
		URL u;
		try {
			u = new URL(API_URL + "?uuid=" + id);

			HttpsURLConnection conn = (HttpsURLConnection) u.openConnection();

			InputStream in = conn.getInputStream();
			String profileJSON = Streams.toString(in);
			Streams.close(in);

			System.out.println("MS API Response: " + profileJSON);

			if (profileJSON == null || profileJSON.length() == 0) { return new Profile(); }

			JsonObject pj = JsonObject.readFrom(profileJSON);

			Profile p = new Profile(pj.get("username").asString(), uuid);
			JsonValue skinVal = pj.get("skin");
			JsonValue capeVal = pj.get("cape");

			String url;
			if (skinVal != null && !skinVal.isNull() && !skinVal.asString().isEmpty()) {
				url = textureHandler.addSkin(uuid, skinVal.asString());
				p.setSkin(url);
			}

			if (capeVal != null && !capeVal.isNull() && !capeVal.asString().isEmpty()) {
				url = textureHandler.addCape(uuid, capeVal.asString());
				p.setCape(url);
			}

			return p;
		} catch (ParseException e) {
			System.out.println("Unable to parse getProfile response, using blank profile");
			return new Profile();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return new Profile();
	}

	// name is the username, returns a Profile object
	public Profile searchProfile(String name) {
		System.out.println("MineshafterProfileClient.searchProfile(" + name + ")");

		try {
			name = URLEncoder.encode(name, "UTF-8");
			URL u = new URL(API_URL + "?username=" + name);
			HttpsURLConnection conn = (HttpsURLConnection) u.openConnection();

			InputStream in = conn.getInputStream();
			String uuid = Util.ensureUUIDFormatted(Streams.toString(in));
			Streams.close(in);

			if (uuid == null || uuid.length() == 0) { return new Profile(name); }

			return new Profile(name, uuid);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return new Profile(name);
	}
}
