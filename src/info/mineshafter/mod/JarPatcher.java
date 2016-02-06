package info.mineshafter.mod;

import info.mineshafter.util.Streams;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class JarPatcher {
	private Map<String, byte[]> entries;
	
	public JarPatcher(File jarFile) {
		entries = new HashMap<String, byte[]>();

		try {
			ZipEntry entry;
			ZipInputStream jarIn = new ZipInputStream(new FileInputStream(jarFile));

			while ((entry = jarIn.getNextEntry()) != null) {
				String name = entry.getName();

				entries.put(name, Streams.toByteArray(jarIn));
			}
			
			jarIn.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setEntry(String name, byte[] data) {
		entries.put(name, data);
	}

	public void removeEntry(String name) {
		entries.remove(name);
	}

	public Collection<String> getEntries() {
		return new HashSet<String>(entries.keySet());
	}

	public void write(File destination) {
		try {
			write(new FileOutputStream(destination));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void write(OutputStream destination) {
		ZipOutputStream out = new ZipOutputStream(destination);

		try {
			for(String name : entries.keySet()) {
				ZipEntry entry = new ZipEntry(name);
				byte[] blob = entries.get(name);

				out.putNextEntry(entry);

				out.write(blob);
				out.flush();
			}

			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
