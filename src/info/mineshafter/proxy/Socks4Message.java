package info.mineshafter.proxy;

import java.io.*;
import java.net.*;

class Socks4Message extends SocksMessage {

	private byte[] msgBytes;
	private int msgLength;

	/**
	 * Server failed reply, cmd command for failed request
	 */
	public Socks4Message(int cmd) {
		super(cmd, null, 0);
		this.user = null;

		msgLength = 2;
		msgBytes = new byte[2];

		msgBytes[0] = (byte) 0;
		msgBytes[1] = (byte) command;
	}

	// Server successfull reply
	public Socks4Message(int cmd, InetAddress ip, int port) {
		this(0, cmd, ip, port, null);
	}

	public Socks4Message(int version, int cmd, InetAddress ip, int port, String user) {
		super(cmd, ip, port);
		this.user = user;
		this.version = version;

		msgLength = user == null ? 8 : 9 + user.length();
		msgBytes = new byte[msgLength];

		msgBytes[0] = (byte) version;
		msgBytes[1] = (byte) command;
		msgBytes[2] = (byte) (port >> 8);
		msgBytes[3] = (byte) port;

		byte[] addr;

		if (ip != null) addr = ip.getAddress();
		else {
			addr = new byte[4];
			addr[0] = addr[1] = addr[2] = addr[3] = 0;
		}
		System.arraycopy(addr, 0, msgBytes, 4, 4);

		if (user != null) {
			byte[] buf = user.getBytes();
			System.arraycopy(buf, 0, msgBytes, 8, buf.length);
			msgBytes[msgBytes.length - 1] = 0;
		}
	}

	public Socks4Message(InputStream in) throws IOException {
		msgBytes = null;
		read(in);
	}

	public void read(InputStream in) throws IOException {
		DataInputStream d_in = new DataInputStream(in);
		version = d_in.readUnsignedByte();
		command = d_in.readUnsignedByte();
		port = d_in.readUnsignedShort();
		byte[] addr = new byte[4];
		d_in.readFully(addr);
		ip = bytes2IP(addr);
		host = ip.getHostName();
		int b = in.read();
		// Hope there are no idiots with user name bigger than this
		byte[] userBytes = new byte[256];
		int i = 0;
		for (i = 0; i < userBytes.length && b > 0; ++i) {
			userBytes[i] = (byte) b;
			b = in.read();
		}
		user = new String(userBytes, 0, i);
	}

	public void write(OutputStream out) throws IOException {
		if (msgBytes == null) {
			Socks4Message msg = new Socks4Message(version, command, ip, port, user);
			msgBytes = msg.msgBytes;
			msgLength = msg.msgLength;
		}
		out.write(msgBytes);
	}

	static InetAddress bytes2IP(byte[] addr) {
		String s = bytes2IPV4(addr, 0);
		try {
			return InetAddress.getByName(s);
		} catch (UnknownHostException uh_ex) {
			return null;
		}
	}

	static final int SOCKS_VERSION = 4;

	public final static int REQUEST_CONNECT = 1;
	public final static int REQUEST_BIND = 2;

	public final static int REPLY_OK = 90;
	public final static int REPLY_REJECTED = 91;
	public final static int REPLY_NO_CONNECT = 92;
	public final static int REPLY_BAD_IDENTD = 93;

}
