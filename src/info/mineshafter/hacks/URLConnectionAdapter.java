package info.mineshafter.hacks;

import info.mineshafter.intercept.Handler;
import info.mineshafter.intercept.Request;
import info.mineshafter.intercept.Response;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class URLConnectionAdapter extends HttpURLConnection {
	private Request req;
	private Response res;
	private boolean didRequest = false;
	private ByteArrayOutputStream bodyStream;
	private ByteArrayInputStream replyStream;
	private Handler handler = null;

	protected URLConnectionAdapter(URL url, Handler h) {
		super(url);
		//System.out.println("H1");

		req = new Request(url);

		bodyStream = new ByteArrayOutputStream();

		handler = h;
	}

	private void doRequest() {
		if (didRequest) return;

		if (doOutput && bodyStream.size() > 0) {
			req.setBody(bodyStream.toByteArray());
		}

		res = handler.handle(req);

		responseCode = res.getStatus();

		replyStream = new ByteArrayInputStream(res.getBody());
	}

	public int getResponseCode() {
		if (!didRequest) doRequest();
		//System.out.println(".getResponseCode()");

		return responseCode;
	}

	@Override
	public InputStream getInputStream() {
		if (!didRequest) doRequest();
		//System.out.println(".getInputStream()");

		return replyStream;
	}

	@Override
	public int getContentLength() {
		String len = getHeaderField("content-length");
		if (len == null) return -1;

		return Integer.parseInt(len);
	}

	@Override
	public String getContentType() {
		return getHeaderField("content-type");
	}

	@Override
	public Map<String, List<String>> getHeaderFields() {
		getInputStream();

		return res.getHeaders();
	}

	@Override
	public String getHeaderField(String name) {
		getInputStream();

		List<String> items = res.getHeaders().get(name);
		if (items == null) return null;
		return items.get(0); // Probably won't break right away
	}

	@Override
	public OutputStream getOutputStream() {
		doOutput = true;

		return bodyStream;
	}

	public void setDoInput(boolean doinput) {
		//System.out.println(".setDoInput()");
		doInput = doinput;
	}

	public void setDoOutput(boolean dooutput) {
		//System.out.println(".setDoOutput()");
		doOutput = dooutput;
	}

	@Override
	public void setRequestProperty(String key, String value) {
		req.setHeader(key, value);
	}

	@Override
	public void addRequestProperty(String key, String value) {
		req.addHeader(key, value);
	}

	@Override
	public void connect() throws IOException {
		//System.out.println(".connect()");
	} // Don't really need this...

	@Override
	public void disconnect() {
		//System.out.println(".disconnect()");
	}

	@Override
	public boolean usingProxy() {
		return false;
	}
}
