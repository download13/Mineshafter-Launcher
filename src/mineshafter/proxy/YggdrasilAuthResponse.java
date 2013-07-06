package mineshafter.proxy;

import java.util.ArrayList;

public class YggdrasilAuthResponse {
	public String accessToken;
	public String clientToken;
	public ProfileResponse selectedProfile;
	public ArrayList<ProfileResponse> availableProfiles;
	
	public YggdrasilAuthResponse(String clientToken, String accessToken, ProfileResponse selected, ProfileResponse available) {
		this.clientToken = clientToken;
		this.accessToken = accessToken;
		this.selectedProfile = selected;
		if(available != null) {
			this.availableProfiles = new ArrayList<ProfileResponse>();
			this.availableProfiles.add(available);
		} else this.availableProfiles = null;
	}
}
