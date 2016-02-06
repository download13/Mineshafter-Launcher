package info.mineshafter.intercept;

import java.net.URL;

public class Request extends Message {
	public String host;
	public String path;
	
	public Request(URL u) {
		host = u.getHost();
		path = u.getPath();
	}
}
