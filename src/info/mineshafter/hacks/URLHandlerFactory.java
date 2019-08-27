package info.mineshafter.hacks;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

public class URLHandlerFactory implements URLStreamHandlerFactory {
	public URLStreamHandler createURLStreamHandler(String protocol) {
		// If protocol is HTTP or HTTPS check if we want to intercept
		if (protocol.equalsIgnoreCase("http")) {
			return new InterceptHttpHandler();
		} else if (protocol.equalsIgnoreCase("rawhttp")) {
			// Nothing we need to worry about is going over HTTP
			//return this.getDefaultHandler("http");
			return new sun.net.www.protocol.http.Handler();
		} else if (protocol.equalsIgnoreCase("https")) {
			return new InterceptHttpsHandler();
		} else if (protocol.equalsIgnoreCase("rawhttps")) {
			// Just in case we need to call the real destination
			return new sun.net.www.protocol.https.Handler();
			//return this.getDefaultHandler("https");
		}

		return this.getDefaultHandler(protocol);

	}

	private URLStreamHandler getDefaultHandler(String protocol) {
		System.out.println("getDefaultHandler " + protocol);

		ClassLoader cl = ClassLoader.getSystemClassLoader();

		Class<?> cls;
		try {
			cls = cl.loadClass("sun.net.www.protocol." + protocol + ".Handler");

			return (URLStreamHandler) cls.newInstance();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		} catch (InstantiationException e) {
			e.printStackTrace();
			return null;
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			return null;
		}
	}

}
