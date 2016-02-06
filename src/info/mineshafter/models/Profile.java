package info.mineshafter.models;

public final class Profile {
	private String uuid = null;
	private String name = null;
	private String accessToken = null;
	private String skinUrl = null;
	private String capeUrl = null;
	private boolean officialOrigin;

	public Profile(String id) {
		this(id, true);
	}

	public Profile(String id, boolean official) {
		uuid = id;
		officialOrigin = official;
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

	public boolean isOfficial() {
		return officialOrigin;
	}
}
