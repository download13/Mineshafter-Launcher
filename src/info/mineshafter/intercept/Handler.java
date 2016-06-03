package info.mineshafter.intercept;

import java.net.URL;

public interface Handler {
	public boolean canHandle(URL url);
	
	public Response handle(Request req);
}
