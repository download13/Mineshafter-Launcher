package info.mineshafter.hacks;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

public class URLHandlerFactory implements URLStreamHandlerFactory {
	public URLStreamHandler createURLStreamHandler(String protocol) {
		// If protocol is HTTP or HTTPS check if we want to intercept
		if (protocol.equalsIgnoreCase("http")) {
			return new InterceptHttpHandler();
		} else if (protocol.equalsIgnoreCase("rawhttp")){
                    // Nothing we need to worry about is going over HTTP
                    return new sun.net.www.protocol.http.Handler();
                } else if (protocol.equalsIgnoreCase("https")) {
			return new InterceptHttpsHandler();
		} else if (protocol.equalsIgnoreCase("rawhttps")) {
			// Just in case we need to call the real destination
			return new sun.net.www.protocol.https.Handler();
		}

		return null;
	}
}
