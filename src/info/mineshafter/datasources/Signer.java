package info.mineshafter.datasources;

import info.mineshafter.util.Resources;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

public class Signer {
	static private Signer instance;
	
	private PrivateKey key;
	
	private Signer() {
		byte[] keydata = Resources.loadByteArray("resources/ms_privkey.der");

		try {
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			key = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(keydata));
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	public static synchronized Signer getInstance() {
		if(instance == null) {
			instance = new Signer();
		}

		return instance;
	}
	
	public byte[] sign(byte[] data) {
		Signature signature;
		try {
			signature = Signature.getInstance("SHA1withRSA");
			signature.initSign(key);
			signature.update(data);
			return signature.sign();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (SignatureException e) {
			e.printStackTrace();
		}
		
		return null;
	}
}
