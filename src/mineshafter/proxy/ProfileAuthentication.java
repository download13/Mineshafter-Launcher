package mineshafter.proxy;

public class ProfileAuthentication {
	public String username;
	public String uuid;
	public String displayName;
	public String accessToken;
	
	public ProfileAuthentication(String id, String name) {
		this.uuid = id;
		this.username = name;
		this.displayName = name;
	}
}
