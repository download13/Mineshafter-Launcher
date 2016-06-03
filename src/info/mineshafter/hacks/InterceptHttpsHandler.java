package info.mineshafter.hacks;

import info.mineshafter.intercept.Handler;
import info.mineshafter.intercept.MetadataHandler;
import info.mineshafter.intercept.ProfileHandler;
import info.mineshafter.intercept.SkinHandler;
import info.mineshafter.intercept.TextureHandler;
import info.mineshafter.intercept.YggdrasilImpersonator;

import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;

public class InterceptHttpsHandler extends sun.net.www.protocol.https.Handler {
	private static Handler[] handlers = new Handler[] {
		SkinHandler.getInstance(),
		YggdrasilImpersonator.getInstance(),
		ProfileHandler.getInstance(),
		TextureHandler.getInstance(),
		MetadataHandler.getInstance(),
	};

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
		
		if(handler != null) {
			return new URLConnectionAdapter(url, handler);
		} else {
			return super.openConnection(url, p);
		}
	}
}
