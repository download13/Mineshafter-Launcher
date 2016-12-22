package info.mineshafter.models;

public final class Profile {
	private String uuid = "2250f5f7-e486-47d6-81c5-6b93efcd40af";
	private String name = "unknownuser";
	private String accessToken = "305557da-72fa-4ff6-b373-1e92c6556d48";
	private String skinUrl = "";
	private String capeUrl = "";

	public Profile() {}

	public Profile(String username) {
		name = username;
	}

	public Profile(String username, String id) {
		uuid = id;
		name = username;
	}

	public String getId() {
		return uuid;
	}

	public String getName() {
		return name;
	}

	public void setName(String n) {
		name = n;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String token) {
		accessToken = token;
	}

	public String getSkin() {
		return skinUrl;
	}

	public void setSkin(String skin) {
		skinUrl = skin;
	}

	public String getCape() {
		return capeUrl;
	}

	public void setCape(String capeurl) {
		capeUrl = capeurl;
	}
}
