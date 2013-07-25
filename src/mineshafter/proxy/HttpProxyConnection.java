package mineshafter.proxy;

import java.net.Socket;

public class HttpProxyConnection implements ProxyConnection {
	private Socket clientSock;
	private HttpProxyHandler handler;

	public HttpProxyConnection(Socket s, HttpProxyHandler h) {
		clientSock = s;
		handler = h;
	}

	public void start() {
		DataInputStream fromClient = new DataInputStream(clientSock.getInputStream());
		HashMap<String, String> headers = new HashMap<String, String>();
		System.out.println("Proxy Request Started");
		// Read the incoming request
		String[] requestLine = readUntil(fromClient, '\n').split(" ");
		String method = requestLine[0].trim().toUpperCase();
		String url = requestLine[1].trim();

		System.out.println("Proxy Request: " + method + " " + url);

		HttpProxyHandler handler = new YggdrasilProxyHandler(this, clientSock);
		handler.start();
	}

	public void run() {

	}
}
