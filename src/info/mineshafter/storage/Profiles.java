package info.mineshafter.storage;

import info.mineshafter.Util;
import info.mineshafter.datasources.MineshafterProfileAuthority;
import info.mineshafter.datasources.ProfileAuthority;
import info.mineshafter.datasources.YggdrasilProfileAuthority;
import info.mineshafter.models.Profile;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/*
 * Loads local profiles from disk
 * Loads official profiles from official authority
 * Loads other profiles from other authorities
 */
public final class Profiles {
	private static Profiles instance;

	private static ProfileAuthority yggdrasilAuthority = YggdrasilProfileAuthority.getInstance();
	private static ProfileAuthority mineshafterAuthority = MineshafterProfileAuthority.getInstance();

	private Map<String, Profile> profiles = new ConcurrentHashMap<String, Profile>();

	private Profiles() {
		/*
		File profilesFile = new File(Util.getWorkingDirectory(), "launcher_profiles.json");

		try {
			JsonObject profs = JsonObject.readFrom(new FileReader(profilesFile));
			JsonObject auths = profs.get("authenticationDatabase").asObject();

			for (String id : auths.names()) {
				JsonObject profileData = auths.get(id).asObject();
				Profile p = getProfileById(id);

				if (p.getName() == null) {
					p.setName(profileData.get("displayName").asString());
				}

				p.setAccessToken(profileData.get("accessToken").asString());

				//System.out.println(p.getId() + ", " + p.getName() + ", " + p.getAccessToken());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		*/
	}

	public static synchronized Profiles getInstance() {
		if (instance == null) {
			instance = new Profiles();
		}

		return instance;
	}

	public boolean addProfile(Profile p) {
		synchronized (profiles) {
			if (profiles.containsKey(p.getId())) { return false; }

			profiles.put(p.getId(), p);

			return true;
		}
	}

	public Profile byId(String id) {
		synchronized (profiles) {
			Profile p = profiles.get(id);

			if (p == null) {
				p = mineshafterAuthority.getProfile(id);
				if (p == null) {
					p = yggdrasilAuthority.getProfile(id);
					if (p == null) {
						p = new Profile(id);
					}
				}

				addProfile(p);
			}
			System.out.println("getProfileById(" + id + ") = " + p.getSkin());

			return p;
		}
	}

	public Profile getProfileByName(String name) {
		synchronized (profiles) {
			for (Profile p : profiles.values()) {
				if (p.getName().equals(name)) { return p; }
			}

			Profile p = yggdrasilAuthority.searchProfile(name);
			if (p == null) {
				p = mineshafterAuthority.searchProfile(name);
				if (p == null) {
					p = new Profile(Util.getMd5(name));
					p.setName(name);
				}
			}

			addProfile(p);

			return p;
		}
	}

	public Profile getProfileByAccessToken(String token) {
		synchronized (profiles) {
			for (Profile p : profiles.values()) {
				if (p.getAccessToken().equals(token)) { return p; }
			}

			return null;
		}
	}
}
