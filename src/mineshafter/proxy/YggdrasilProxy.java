package mineshafter.proxy;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.regex.Pattern;

import mineshafter.Util;

import com.google.gson.Gson;


public class YggdrasilProxy extends Thread {
	public static String authServer = "mineshafter.info";
	
	public static Pattern SKIN_URL = Pattern.compile("http://skins\\.minecraft\\.net/MinecraftSkins/(.+?)\\.png");
	public static Pattern CLOAK_URL = Pattern.compile("http://skins\\.minecraft\\.net/MinecraftCloaks/(.+?)\\.png");
	public static Pattern AUTHSERVER_URL = Pattern.compile("http://authserver\\.mojang\\.com/(.*)");
	
	private int port = -1;
	
	public Hashtable<String, byte[]> skinCache;
	public Hashtable<String, byte[]> cloakCache;
	public Map<String,ProfileAuthentication> profiles = new HashMap<String,ProfileAuthentication>();
	
	public YggdrasilProxy() {
		setName("MineProxy Thread");
		
		try {
			skinCache = new Hashtable<String, byte[]>();
			cloakCache = new Hashtable<String, byte[]>();
		} catch(Exception e) {
			System.out.println("Proxy starting error:");
			e.printStackTrace();
		}
		
		Gson gson = new Gson();
		try {
			Profiles profiles = gson.fromJson(new FileReader(new File(Util.getWorkingDirectory(), "launcher_profiles.json")), Profiles.class);
			for(String name : profiles.profiles.keySet()) {
				ProfileAuthentication p = profiles.profiles.get(name).authentication;
				//System.out.println(p.accessToken + '=' + p.uuid + "," + p.username);
				this.profiles.put(p.accessToken, p);
				p.accessToken = null;
			}
		} catch (Exception e) {
			System.out.println("Proxy: FAILED TO LOAD PROFILES!");
			e.printStackTrace();
		}
	}
	
	public void run() {
		try {
			ServerSocket server = null;
			int port = 9010; // A lot of other applications use the 80xx range, let's try for some less crowded real-estate
			while (port < 12000) { // That should be enough
				try {
					System.out.println("Trying to proxy on port " + port);
					byte[] loopback = {127, 0, 0, 1};
					server = new ServerSocket(port, 16, InetAddress.getByAddress(loopback));
					this.port = port;
					System.out.println("Proxying successful");
					break;
				} catch(BindException ex) {
					port++;
				}
			}
			
			while (true) {
				Socket connection = server.accept();
				
				YggdrasilProxyHandler handler = new YggdrasilProxyHandler(this, connection);
				handler.start();
			}
		} catch(IOException e) {
			System.out.println("Error in server accept loop:");
			e.printStackTrace();
		}
	}
	
	public int getPort() {
		while (port < 0) {
			try {
				sleep(50);
			} catch (InterruptedException e) {
				System.out.println("Interrupted while waiting for port");
				e.printStackTrace();
			}
		}
		return port;
	}
}
