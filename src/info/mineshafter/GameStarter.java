package info.mineshafter;

import info.mineshafter.hacks.URLHandlerFactory;

import java.lang.reflect.Method;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class GameStarter {
	public static void main(String[] args) {
		String className = args[0];
		System.out.println("GameStarter running! " + className);

		// Activate the hooks
		URL.setURLStreamHandlerFactory(new URLHandlerFactory());

		bypassTls();

		String[] passedArgs = new String[args.length - 1];
		for (int i = 1; i < args.length; i++) {
			passedArgs[i - 1] = args[i];
		}

		try {
			Class<?> game = Class.forName(className);
			Method main = game.getMethod("main", String[].class);
			main.invoke(null, (Object) passedArgs);
		} catch (Exception e) {
			System.out.println("Error while starting game:");
			e.printStackTrace();
		}
	}

	private static void bypassTls() {
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}

			public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
		} };

		SSLContext sc;
		try {
			sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (KeyManagementException e) {
			e.printStackTrace();
		}
	}
}
