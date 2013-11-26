package info.mineshafter.proxy;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class SocksProxyConnection implements ProxyConnection {
	static final int TIMEOUT = 30 * 1000;
	static final int INPUT_BUFFER_SIZE = 0xffff;

	private Socket clientSock;
	// UDPRelayServer relayServer = null;
	private BufferedInputStream in;
	private OutputStream out;
	private SocksProxyHandler handler;

	public SocksProxyConnection(Socket s, SocksProxyHandler h) {
		clientSock = s;
		handler = h;
	}

	public void start() {
		new Thread(this).start();
	}

	public void run() {
		try {
			startSession();
			SocksMessage msg = readMsg();
			handleRequest(msg);
			abort();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void startSession() {
		try {
			clientSock.setSoTimeout(TIMEOUT);

			BufferedInputStream in = new BufferedInputStream(clientSock.getInputStream(), INPUT_BUFFER_SIZE);
			OutputStream out = clientSock.getOutputStream();
			in.mark(INPUT_BUFFER_SIZE);
			int version = in.read();
			if (version == 5) {
				int num_methods = in.read();
				for (int bread = 0; bread < num_methods; bread++) {
					if (in.read() == 0) {
						out.write(new byte[] { 5, 0 });
					}
				}
			} else if (version == 4) {
				in.reset();
			}

			this.in = in;
			this.out = out;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void handleRequest(SocksMessage msg) throws Exception {
		if (msg.ip == null) {
			if (msg instanceof Socks5Message) msg.ip = InetAddress.getByName(msg.host);
			else return;
		}

		switch (msg.command) {
		case Socks5Message.CMD_CONNECT:
			onConnect(msg);
			break;
		case Socks5Message.CMD_BIND:
			onBind(msg);
			break;
		case Socks5Message.CMD_UDP_ASSOCIATE:
			// onUDP(msg);
			// break;
			throw new Exception("UDP Assoc!!!!!");
		}
	}

	private void onConnect(SocksMessage msg) throws IOException {
		SocksMessage response = null;
		if (msg instanceof Socks5Message) {
			response = new Socks5Message(Socks5Message.REPLY_SUCCESS, InetAddress.getLocalHost(), 0);
		} else {
			response = new Socks4Message(Socks4Message.REPLY_OK, InetAddress.getLocalHost(), 0);
		}
		response.write(out);

		if (!handler.onConnect(in, out, msg)) {
			Socket s = new Socket(msg.ip, msg.port);
			pipeActive(in, s.getOutputStream());
			pipe(s.getInputStream(), out);
		}
	}

	private void onBind(SocksMessage msg) throws IOException {
		ServerSocket bound = new ServerSocket(0, 5, InetAddress.getLoopbackAddress());
		bound.setSoTimeout(TIMEOUT);

		SocksMessage response;
		if (msg.version == 5) response = new Socks5Message(Socks5Message.REPLY_SUCCESS, bound.getInetAddress(), bound.getLocalPort());
		else response = new Socks4Message(Socks4Message.REPLY_OK, bound.getInetAddress(), bound.getLocalPort());
		response.write(out);

		Socket s = null;
		while (true) {
			try {
				s = bound.accept();
			} catch (SocketTimeoutException e) {
				s = null;
				bound.close();
				break;
			}
			if (s.getInetAddress().equals(msg.ip)) {
				bound.close();
				break;
			} else s.close();
		}

		if (msg.version == 5) response = new Socks5Message(s == null ? Socks5Message.REPLY_FAILURE : Socks5Message.REPLY_SUCCESS, s.getInetAddress(), s.getPort());
		else response = new Socks4Message(s == null ? Socks4Message.REPLY_REJECTED : Socks4Message.REPLY_OK, s.getInetAddress(), s.getPort());
		response.write(out);

		if (s != null) {
			pipeActive(in, s.getOutputStream());
			pipe(s.getInputStream(), out);
		}
	}

	/*
	 * private void onUDP(ProxyMessage msg) throws IOException { if
	 * (msg.ip.getHostAddress().equals("0.0.0.0")) msg.ip =
	 * clientSock.getInetAddress(); // XXX Expect incoming UDP packets from
	 * msg.ip:msg.port // Reply with the local address and port we bound to to
	 * listen for UDP // packets relayServer = new UDPRelayServer(msg.ip,
	 * msg.port, clientSock); // XXX relayServer.start(); ProxyMessage response
	 * = new Socks5Message(Socks5Message.REPLY_SUCCESS, relayServer.relayIP,
	 * relayServer.relayPort); response.write(out); clientSock.setSoTimeout(0);
	 * 
	 * try { while (in.read() >= 0) {} } catch (EOFException e) {}
	 * relayServer.stop(); }
	 */

	private SocksMessage readMsg() throws IOException {
		in.mark(5);
		int version = in.read();
		in.reset();
		SocksMessage msg = null;
		if (version == 5) {
			msg = new Socks5Message(in);
		} else if (version == 4) {
			msg = new Socks4Message(in);
		} else {
			throw new IOException("Invalid SOCKS version: " + version);
		}
		return msg;
	}

	private synchronized void abort() {
		try {
			out.flush();
			out.close();
			in.close();
			clientSock.close();
		} catch (Exception e) {}
	}

	private void pipe(final InputStream in, final OutputStream out) {
		try {
			byte[] buf = new byte[8192];
			while (true) {
				int len = in.read(buf);
				//System.out.println("Piped " + len + " bytes");
				if (len < 0) break;
				out.write(buf, 0, len);
				out.flush();
			}
		} catch (EOFException e) {
			return;
		} catch (SocketException e) {
			return;
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			in.close();
			out.close();
		} catch (IOException e) {}
	}

	private void pipeActive(final InputStream in, final OutputStream out) {
		Thread t = new Thread("Active piping thread") {
			public void run() {
				pipe(in, out);
			}
		};
		t.start();
	}
}
