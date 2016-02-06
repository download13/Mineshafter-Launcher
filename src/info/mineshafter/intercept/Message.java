package info.mineshafter.intercept;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Message {
	private Map<String, List<String>> headers = new ConcurrentHashMap<String, List<String>>();
	public byte[] body;

	public Map<String, List<String>> getHeaders() {
		return Collections.unmodifiableMap(headers);
	}
	
	public void setHeader(String key, String value) {
		List<String> items = Collections.synchronizedList(new ArrayList<String>());
		items.add(value);

		headers.put(key, items);
	}
	
	public void addHeader(String key, String value) {
		List<String> items = headers.get(key);

		if (items == null) {
			setHeader(key, value);
			return;
		}

		items.add(value);
	}
}
