package info.mineshafter.intercept;

import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MetadataHandler implements Handler {
	private static MetadataHandler instance;
	private static String METADATA_HOST = "launchermeta.mojang.com";
	private static Pattern manifestMatcher = Pattern.compile("/mc/game/");

	public static synchronized MetadataHandler getInstance() {
		if (instance == null) {
			instance = new MetadataHandler();
		}

		return instance;
	}

	private MetadataHandler() {}

	public boolean canHandle(URL url) {
		if (!url.getHost().equalsIgnoreCase(METADATA_HOST)) { return false; }

		Matcher m = manifestMatcher.matcher(url.getPath());
		if (!m.matches()) { return false; }

		return true;
	}

	public Response handle(Request req) {
		// TODO Auto-generated method stub
		return null;
	}

}
