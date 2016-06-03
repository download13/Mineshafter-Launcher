package info.mineshafter.crypto;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Hash {
	public static String md5(InputStream input) {
		return hash("MD5", input);
	}

	public static String md5(byte[] input) {
		return hash("MD5", input);
	}

	public static String md5(String input) {
		return hash("MD5", input.getBytes());
	}

	public static String sha1(InputStream input) {
		return hash("SHA-1", input);
	}

	private static String hash(String algorithm, InputStream input) {
		DigestInputStream stream;
		try {
			stream = new DigestInputStream(input, MessageDigest.getInstance(algorithm));
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return "";
		}

		byte[] buffer = new byte[8192];
		try {
			while (stream.read(buffer) != -1) {}
			stream.close();
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		}

		return String.format("%1$032x", new Object[] { new BigInteger(1, stream.getMessageDigest().digest()) });
	}

	private static String hash(String algorithm, byte[] input) {
		byte[] digest;
		try {
			digest = MessageDigest.getInstance(algorithm).digest();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return "";
		}

		return String.format("%1$032x", new Object[] { new BigInteger(1, digest) });
	}
}
