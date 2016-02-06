package info.mineshafter.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Resources {
	public static InputStream load(String filename) throws FileNotFoundException {
		InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename);
		if(in == null) in = new FileInputStream(filename);
		return in;
	}
	
	public static String loadString(String filename) {
		try {
			char[] b = new char[4096];
			int read = 0;
			StringBuilder builder = new StringBuilder();
			BufferedReader reader = new BufferedReader(new InputStreamReader(load(filename)));
			while((read = reader.read(b)) != -1) {
				builder.append(String.valueOf(b, 0, read));
			}
			reader.close();
			return builder.toString();
		} catch(Exception e) {
			System.out.println("load resources:");
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static byte[] loadByteArray(String filename) {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			InputStream in = load(filename);
			
			Streams.pipeStreams(in, out);
			
			return out.toByteArray();
		} catch(Exception e) {
			System.out.println("load resources:");
			e.printStackTrace();
		}
		
		return null;
	}
}
