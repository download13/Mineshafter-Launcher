package info.mineshafter.intercept;

import java.net.URL;

public interface Handler {
	public boolean handle(URL url);
	
	public Response handle(Request req);
}
