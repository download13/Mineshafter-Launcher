This file is part of MineshafterMC, licensed under the Apache License 2.0 (Apache 2.0).

Copyright (c) MineshafterMC <http://mineshafter.info>
Copyright (c) Contributers

package info.mineshafter.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;

public class SimpleRequest {
	public static byte[] get(String url) {
		try {
			return get(new URL(url));
		} catch (MalformedURLException e) {
			return null;
		}
	}
	public static byte[] get(URL url) {
		try {
			HttpURLConnection c = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
			c.setRequestMethod("GET");
			c.setRequestProperty("Host", url.getHost());
			BufferedInputStream in = new BufferedInputStream(c.getInputStream());
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			Streams.pipeStreams(in, out);
			return out.toByteArray();
		} catch(IOException e) {
			return null;
		}
	}
	public static byte[] post(String url,byte[] data) {
		try {
			return post(new URL(url),data);
		} catch (MalformedURLException e) {
			return null;
		}
	}
	public static byte[] post(URL url,byte[] data)
	{
		try {
			HttpURLConnection c=(HttpURLConnection)url.openConnection(Proxy.NO_PROXY);
			c.setRequestMethod("POST");
			c.setDoOutput(true);
			c.setRequestProperty("Host", url.getHost());
			c.setRequestProperty("Content-length", Integer.toString(data.length));
			c.getOutputStream().write(data);
			BufferedInputStream in = new BufferedInputStream(c.getInputStream());
			ByteArrayOutputStream returnStream = new ByteArrayOutputStream();
			Streams.pipeStreams(in, returnStream);
			return returnStream.toByteArray();
		} catch(IOException e) {
			return null;
		}
	}
}
