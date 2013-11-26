package info.mineshafter.proxy;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public interface HttpProxyHandler {
	public boolean onGET(String url, Map<String, String> headers, InputStream in, OutputStream out);

	public boolean onPOST(String url, Map<String, String> headers, InputStream in, OutputStream out);

	public boolean onHEAD(String url, Map<String, String> headers, InputStream in, OutputStream out);

	public boolean onCONNECT(String url, Map<String, String> headers, InputStream in, OutputStream out);
}
