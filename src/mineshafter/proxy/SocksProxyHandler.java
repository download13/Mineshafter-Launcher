package mineshafter.proxy;

import java.io.InputStream;
import java.io.OutputStream;

public interface SocksProxyHandler {
	public boolean onConnect(InputStream clientIn, OutputStream clientOut, SocksMessage msg);

	public boolean onBind();
}
