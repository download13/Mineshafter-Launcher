package info.mineshafter.hacks;

import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;

import info.mineshafter.intercept.Handler;
import info.mineshafter.intercept.TextureHandler;

public class InterceptHttpHandler extends sun.net.www.protocol.http.Handler {
	private static Handler[] handlers = new Handler[] { TextureHandler.getInstance() };

	@Override
	protected URLConnection openConnection(URL u) throws IOException {
		return openConnection(u, null);
	}

	@Override
	protected URLConnection openConnection(URL url, Proxy p) throws IOException {
		Handler handler = null;

		for (Handler h : handlers) {
			if (h.canHandle(url)) {
				handler = h;
				break;
			}
		}

		System.out.println("Should handle? " + url.toString() + " " + (handler != null));

		if (handler != null) {
			return new URLConnectionAdapter(url, handler);
		} else {
			return super.openConnection(url, p);
		}
	}
}
