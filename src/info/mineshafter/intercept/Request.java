package info.mineshafter.intercept;

import java.net.URL;

public class Request extends Message {
	private URL url;

	public Request(URL u) {
		url = u;
	}

	public URL getUrl() {
		return url;
	}

	public String getHost() {
		return url.getHost();
	}

	public String getPath() {
		return url.getPath();
	}
        public String getQuery(){
            return url.getQuery();
        }
}
