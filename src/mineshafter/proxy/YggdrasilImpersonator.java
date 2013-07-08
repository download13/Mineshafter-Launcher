package mineshafter.proxy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import mineshafter.Util;
import com.google.gson.Gson;

public class YggdrasilImpersonator {
	private List<Profile> profiles;
	private Gson gson;

	public YggdrasilImpersonator(File profilesFile) {
		this.profiles = Collections.synchronizedList(new ArrayList<Profile>());
		this.gson = new Gson();

		try {
			ProfilesJSON profiles = gson.fromJson(new FileReader(profilesFile), ProfilesJSON.class);
			for (String name : profiles.profiles.keySet()) {
				Profile p = profiles.profiles.get(name).authentication;
				if (p.displayName == null || p.displayName.length() == 0) p.displayName = p.username;
				this.profiles.add(p);
			}
		} catch (FileNotFoundException e) {
			return;
		}

	}

	public Profile getProfileByUsername(String name) {
		synchronized (profiles) {
			for (Profile p : profiles) {
				if (p.username.equals(name)) return p;
			}
		}
		return null;
	}

	public Profile getProfileByAccessToken(String token) {
		synchronized (profiles) {
			for (Profile p : profiles) {
				if (p.accessToken.equals(token)) return p;
			}
		}
		return null;
	}

	public String authenticate(YggdrasilRequest req) {
		String accessToken = Util.getMd5(req.username + req.password + Long.toString(System.currentTimeMillis()));
		Profile p = getProfileByUsername(req.username);
		if (p == null) { // Create new profile entry for this user
			String id = Util.getMd5(req.username);
			p = new Profile(req.username, accessToken, id, req.username);
			profiles.add(p);
		}
		p.accessToken = accessToken;

		ProfileResponse pr = new ProfileResponse(p.uuid, p.displayName);
		YggdrasilAuthResponse r = new YggdrasilAuthResponse(req.clientToken, p.accessToken, pr, pr);
		return gson.toJson(r);
	}

	public String refresh(YggdrasilRequest req) {
		Profile p = getProfileByAccessToken(req.accessToken);
		if (p == null) { return "{\"error\":\"ForbiddenOperationException\",\"errorMessage\":\"Invalid token.\"}"; }

		String newAccessToken = Util.getMd5(req.accessToken + Long.toString(System.currentTimeMillis()));
		p.accessToken = newAccessToken;
		YggdrasilAuthResponse r = new YggdrasilAuthResponse(req.clientToken, newAccessToken, new ProfileResponse(p.uuid, p.displayName), null);
		return gson.toJson(r);
	}

	public String invalidate(YggdrasilRequest req) {
		Profile p = getProfileByAccessToken(req.accessToken);
		p.accessToken = null;
		return "";
	}

	public class ProfilesJSON {
		public Map<String, OuterProfile> profiles;
		public String selectedProfile;
		public String clientToken;

		public class OuterProfile {
			public String name;
			public Profile authentication;
		}
	}

	public class Profile {
		public String username;
		public String accessToken;
		public String uuid;
		public String displayName;

		public Profile(String u, String t, String id, String d) {
			username = u;
			accessToken = t;
			uuid = id;
			displayName = d;
		}
	}

	public class ProfileResponse {
		public String id;
		public String name;

		public ProfileResponse(String id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	public class YggdrasilAuthResponse {
		public String accessToken;
		public String clientToken;
		public ProfileResponse selectedProfile;
		public ArrayList<ProfileResponse> availableProfiles;

		public YggdrasilAuthResponse(String clientToken, String accessToken, ProfileResponse selected, ProfileResponse available) {
			this.clientToken = clientToken;
			this.accessToken = accessToken;
			this.selectedProfile = selected;
			if (available != null) {
				this.availableProfiles = new ArrayList<ProfileResponse>();
				this.availableProfiles.add(available);
			} else this.availableProfiles = null;
		}
	}
}
