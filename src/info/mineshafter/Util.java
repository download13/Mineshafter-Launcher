package info.mineshafter;

import info.mineshafter.util.SimpleRequest;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;

import SevenZip.Compression.LZMA.Decoder;

public class Util {
	public static final String APPLICATION_NAME = "minecraft";

	public static enum OS {
		WINDOWS, MACOS, SOLARIS, LINUX, UNKNOWN;
	}

	public static OS getPlatform() {
		String osName = System.getProperty("os.name").toLowerCase();
		if (osName.contains("win")) return OS.WINDOWS;
		if (osName.contains("mac")) return OS.MACOS;
		if (osName.contains("linux")) return OS.LINUX;
		if (osName.contains("unix")) return OS.LINUX;
		return OS.UNKNOWN;
	}

	public static File getWorkingDirectory() {
		String userHome = System.getProperty("user.home", ".");
		File workingDirectory;
		switch (getPlatform()) {
		case LINUX:
		case SOLARIS:
			workingDirectory = new File(userHome, ".minecraft/");
			break;
		case WINDOWS:
			String applicationData = System.getenv("APPDATA");
			String folder = applicationData != null ? applicationData : userHome;
			workingDirectory = new File(folder, ".minecraft/");
			break;
		case MACOS:
			workingDirectory = new File(userHome, "Library/Application Support/minecraft");
			break;
		default:
			workingDirectory = new File(userHome, "minecraft/");
		}

		return workingDirectory;
	}

	public static boolean grabLauncher(String md5, File file) {
		return Util.grabLauncher(md5, file, 4);
	}

	public static boolean grabLauncher(String md5, File file, int tries) {
		try {
			URL url = new URL("http://s3.amazonaws.com/Minecraft.Download/launcher/launcher.pack.lzma");
			HttpURLConnection connection = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);

			connection.setUseCaches(false);
			connection.setDefaultUseCaches(false);
			connection.setRequestProperty("Cache-Control", "no-store,max-age=0,no-cache");
			connection.setRequestProperty("Expires", "0");
			connection.setRequestProperty("Pragma", "no-cache");
			if (md5 != null) {
				connection.setRequestProperty("If-None-Match", md5.toLowerCase());
			}
			connection.setConnectTimeout(15000);
			connection.setReadTimeout(10000);

			int code = connection.getResponseCode();
			if (code == 304) {
				System.out.println("Launcher already exists");
				return false;
			} else if (code / 100 == 2) {
				InputStream inputStream = connection.getInputStream();
				FileOutputStream outputStream = new FileOutputStream(file);

				byte[] buffer = new byte[8192];
				try {
					int read = inputStream.read(buffer);
					while (read >= 1) {
						outputStream.write(buffer, 0, read);
						read = inputStream.read(buffer);
					}
				} finally {
					inputStream.close();
					outputStream.close();
				}
				return true;
			} else if (tries == 0) {
				System.out.println("Failed to download launcher with error: " + code);
				return false;
			} else {
				return Util.grabLauncher(md5, file, tries - 1);
			}
		} catch (Exception e) {
			return false;
		}
	}

	public static String getMd5(File file) {
		DigestInputStream stream = null;
		try {
			stream = new DigestInputStream(new FileInputStream(file), MessageDigest.getInstance("MD5"));
			byte[] buffer = new byte[8192];
			while (stream.read(buffer) != -1) {}
			stream.close();
		} catch (Exception ignored) {
			return null;
		}

		return String.format("%1$032x", new Object[] { new BigInteger(1, stream.getMessageDigest().digest()) });
	}

	public static String getMd5(String v) {
		try {
			MessageDigest hash = MessageDigest.getInstance("MD5");
			hash.update(v.getBytes());
			String r = String.format("%1$032x", new Object[] { new BigInteger(1, hash.digest()) });
			return r;
		} catch (NoSuchAlgorithmException e) {}
		return null;
	}

	public static float getCurrentBootstrapVersion() {
		try {
			byte[] verdata = SimpleRequest.get(new URL("http://mineshafter.info/bootver"));
			String verstring;
			if (verdata == null) verstring = "0";
			else verstring = new String(verdata);
			if (verstring.isEmpty()) verstring = "0";

			float version;
			try {
				version = Float.parseFloat(verstring);
			} catch (Exception e) {
				version = 0;
			}
			return version;
		} catch (Exception e) {
			System.out.println("Error while checking version:");
			e.printStackTrace();
			return 0;
		}
	}

	public static void unpackLZMA(File packedFile, File unpackedFile) {
		try {
			BufferedInputStream inStream = new BufferedInputStream(new FileInputStream(packedFile));
			BufferedOutputStream outStream = new BufferedOutputStream(new FileOutputStream(unpackedFile));

			byte[] properties = new byte[5];
			inStream.read(properties, 0, 5);
			Decoder decoder = new Decoder();
			decoder.SetDecoderProperties(properties);
			long outSize = 0;
			for (int i = 0; i < 8; i++) {
				int v = inStream.read();
				outSize |= ((long) v) << (8 * i);
			}

			decoder.Code(inStream, outStream, outSize);

			inStream.close();
			outStream.flush();
			outStream.close();
		} catch (IOException e) {
			System.out.println("Exception while unpacking:");
			e.printStackTrace();
		}
	}

	public static void unpack200(File packedFile, File unpackedFile) {
		try {
			BufferedInputStream inStream = new BufferedInputStream(new FileInputStream(packedFile));
			JarOutputStream outStream = new JarOutputStream(new FileOutputStream(unpackedFile));

			Pack200.newUnpacker().unpack(inStream, outStream);
			outStream.close();
		} catch (IOException e) {
			System.out.println("Exception while unpacking:");
			e.printStackTrace();
		}
	}
}
