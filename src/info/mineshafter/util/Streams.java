package info.mineshafter.util;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Streams {
	public static int pipeStreams(InputStream in, OutputStream out) {
		try {
			byte[] b = new byte[8192];
			int read;
			int total = 0;
			while (true) {
				try {
					read = in.read(b);
					if (read == -1) break;
				} catch (IOException e) {
					break;
				}
				out.write(b, 0, read);
				total += read;
			}

			out.flush();

			return total;
		} catch (IOException e) {
			e.printStackTrace();
			return -1;
		}
	}

	public static void pipeStreamsActive(final InputStream in, final OutputStream out) {
		Thread thread = new Thread("Active Pipe Thread") {
			public void run() {
				byte[] b = new byte[8192];
				int count;
				while (true) {
					try {
						System.out.println("Proxy pipeStreamsActive reading");
						count = in.read(b);
						System.out.println("Proxy pipeStreamsActive donereading");
						if (count == -1) break;
						System.out.println("Proxy pipeStreamsActive writing");
						out.write(b, 0, count);
						System.out.println("Proxy pipeStreamsActive donewriting");
						out.flush();
					} catch (IOException e) {
						break;
					}
				}
				try {
					out.close();
					in.close();
				} catch (IOException e) {
					System.out.println("Proxy pipeStreamsActive:");
					e.printStackTrace();
				}
			}
		};
		thread.start();
	}
	
	public static byte[] toByteArray(InputStream s) {
		ByteArrayOutputStream os = new ByteArrayOutputStream();

		pipeStreams(s, os);

		return os.toByteArray();
	}

	public static String toString(InputStream s) {
		return new String(toByteArray(s));
	}

	public static void close(Closeable s) {
		try {
			s.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
