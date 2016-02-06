package info.mineshafter.datasources;

import info.mineshafter.models.Profile;


public interface ProfileAuthority {
	public Profile getProfile(String id);

	public Profile searchProfile(String name);
}
