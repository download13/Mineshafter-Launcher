package info.mineshafter.datasources;

import info.mineshafter.models.Profile;
import info.mineshafter.util.Streams;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import biz.source_code.base64Coder.Base64Coder;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

public final class YggdrasilProfileAuthority implements ProfileAuthority {
	private static final String SEARCH_URL = "rawhttps://api.mojang.com/profiles/page/1";

	private static final String GET_URL = "rawhttps://sessionserver.mojang.com/session/minecraft/profile/";

	private static ProfileAuthority instance;

	private YggdrasilProfileAuthority() {}

	public static synchronized ProfileAuthority getInstance() {
		if (instance == null) {
			instance = new YggdrasilProfileAuthority();
		}

		return instance;
	}

	/*
	 * id is a user uuid without the - separators returns a Profile object or
	 * null if user doesn't exist
	 */

	public Profile getProfile(String id) {
		System.out.println("YggdrasilProfileClient.getProfile(" + id + ")");
		URL u = null;
		try {
			u = new URL(GET_URL + id + "?unsigned=true");
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		}

		try {
			HttpURLConnection conn = (HttpURLConnection) u.openConnection();

			try {
				if (conn.getResponseCode() != 200) { System.out.println("return " + conn.getResponseCode()); return null; } // Nothing there
			} catch (IOException ex) {
				ex.printStackTrace();
				System.out.println("return null");
				return null;
			}

			InputStream in = conn.getInputStream();

			String responseJSON = Streams.toString(in);
			Streams.close(in);

			JsonObject responseProfile = JsonObject.readFrom(responseJSON);

			Profile p = new Profile(responseProfile.get("id").asString());
			p.setName(responseProfile.get("name").asString());

			JsonArray properties = responseProfile.get("properties").asArray();

			for (JsonValue prop : properties) {
				JsonObject property = prop.asObject();

				if (property.get("name").asString().equalsIgnoreCase("textures")) {
					String texturesString = Base64Coder.decodeString(property.get("value").asString());
					JsonObject textures = JsonObject.readFrom(texturesString).get("textures").asObject();

					//System.out.println("ygd:");
					JsonValue skin = textures.get("SKIN");
					JsonValue cape = textures.get("CAPE");
					if (skin != null && !skin.isNull()) {
						p.setSkin(skin.asObject().get("url").asString());
						System.out.println(p.getSkin());
					}
					if (cape != null && !cape.isNull()) {
						p.setCape(cape.asObject().get("url").asString());
						System.out.println(p.getCape());
					}

					break;
				}
			}

			return p;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public Profile searchProfile(String name) {
		URL u = null;
		try {
			u = new URL(SEARCH_URL);
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		}

		JsonObject lookup = new JsonObject();
		lookup.set("name", name);
		lookup.set("agent", "Minecraft");

		JsonArray request = new JsonArray();
		request.add(lookup);

		byte[] requestBody = request.toString().getBytes();

		try {
			HttpURLConnection conn = (HttpURLConnection) u.openConnection();

			conn.setRequestProperty("Content-Type", "application/json");
			conn.setRequestProperty("Content-Length", Integer.toString(requestBody.length));

			conn.setDoOutput(true);

			OutputStream out = conn.getOutputStream();
			out.write(requestBody);
			Streams.close(out);

			InputStream in = conn.getInputStream();
			String responseJSON = Streams.toString(in);
			Streams.close(in);

			System.out.println("YggdrasilProfileClient.searchProfile(" + name + ") = " + responseJSON);

			JsonObject response = JsonObject.readFrom(responseJSON);
			JsonArray profiles = response.get("profiles").asArray();
			if (profiles.size() == 0) { return null; }

			JsonObject profile = profiles.get(0).asObject();
			Profile p = getProfile(profile.get("id").asString());

			return p;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
}
