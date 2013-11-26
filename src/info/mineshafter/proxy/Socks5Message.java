package info.mineshafter.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.DataInputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;

class Socks5Message extends SocksMessage {
	public int addrType;

	byte[] data;

	public Socks5Message(int cmd, InetAddress ip, int port) {
		super(cmd, ip, port);
		this.host = ip == null ? "0.0.0.0" : ip.getHostName();
		this.version = SOCKS_VERSION;

		byte[] addr;

		if (ip == null) {
			addr = new byte[4];
			addr[0] = addr[1] = addr[2] = addr[3] = 0;
		} else addr = ip.getAddress();

		addrType = addr.length == 4 ? ATYP_IPV4 : ATYP_IPV6;

		data = new byte[6 + addr.length];
		data[0] = (byte) SOCKS_VERSION; // Version
		data[1] = (byte) command; // Command
		data[2] = (byte) 0; // Reserved byte
		data[3] = (byte) addrType; // Address type

		// Put Address
		System.arraycopy(addr, 0, data, 4, addr.length);
		// Put port
		data[data.length - 2] = (byte) (port >> 8);
		data[data.length - 1] = (byte) (port);
	}

	public Socks5Message(int cmd, String hostName, int port) {
		super(cmd, null, port);
		this.host = hostName;
		this.version = SOCKS_VERSION;

		// System.out.println("Doing ATYP_DOMAINNAME");

		addrType = ATYP_DOMAINNAME;
		byte addr[] = hostName.getBytes();

		data = new byte[7 + addr.length];
		data[0] = (byte) SOCKS_VERSION; // Version
		data[1] = (byte) command; // Command
		data[2] = (byte) 0; // Reserved byte
		data[3] = (byte) ATYP_DOMAINNAME; // Address type
		data[4] = (byte) addr.length; // Length of the address

		// Put Address
		System.arraycopy(addr, 0, data, 5, addr.length);
		// Put port
		data[data.length - 2] = (byte) (port >> 8);
		data[data.length - 1] = (byte) (port);
	}

	public Socks5Message(InputStream in) throws IOException {
		read(in);
	}

	public void read(InputStream in) throws IOException {
		data = null;
		ip = null;

		DataInputStream di = new DataInputStream(in);

		version = di.readUnsignedByte();
		command = di.readUnsignedByte();
		di.readUnsignedByte(); // Reserved
		addrType = di.readUnsignedByte();

		byte addr[];

		switch (addrType) {
		case ATYP_IPV4:
			addr = new byte[4];
			di.readFully(addr);
			host = bytes2IPV4(addr, 0);
			break;
		case ATYP_IPV6:
			addr = new byte[SOCKS_IPV6_LENGTH];
			di.readFully(addr);
			host = bytes2IPV6(addr, 0);
			break;
		case ATYP_DOMAINNAME:
			addr = new byte[di.readUnsignedByte()];
			di.readFully(addr);
			host = new String(addr);
			break;
		default:
			return;
		}

		port = di.readUnsignedShort();

		if (addrType != ATYP_DOMAINNAME && doResolveIP) {
			try {
				ip = InetAddress.getByName(host);
			} catch (UnknownHostException uh_ex) {}
		}
	}

	public void write(OutputStream out) throws IOException {
		if (data == null) {
			Socks5Message msg;

			if (addrType == ATYP_DOMAINNAME) msg = new Socks5Message(command, host, port);
			else {
				if (ip == null) {
					try {
						ip = InetAddress.getByName(host);
					} catch (UnknownHostException e) {
						return; // XXX What do here?
					}
				}
				msg = new Socks5Message(command, ip, port);
			}
			data = msg.data;
		}
		out.write(data);
	}

	public InetAddress getInetAddress() throws UnknownHostException {
		if (ip != null) return ip;
		return (ip = InetAddress.getByName(host));
	}

	public static final int SOCKS_VERSION = 5;

	public static final int REPLY_SUCCESS = 0;
	public static final int REPLY_FAILURE = 1;
	public static final int REPLY_BADCONNECT = 2;
	public static final int REPLY_BADNETWORK = 3;
	public static final int REPLY_HOST_UNREACHABLE = 4;
	public static final int REPLY_CONNECTION_REFUSED = 5;
	public static final int REPLY_TTL_EXPIRE = 6;
	public static final int REPLY_CMD_NOT_SUPPORTED = 7;
	public static final int REPLY_ADDR_NOT_SUPPORTED = 8;

	public static final int CMD_CONNECT = 0x1;
	public static final int CMD_BIND = 0x2;
	public static final int CMD_UDP_ASSOCIATE = 0x3;

	public static final int ATYP_IPV4 = 0x1; // Where is 2??
	public static final int ATYP_DOMAINNAME = 0x3; // !!!!rfc1928
	public static final int ATYP_IPV6 = 0x4;

	public static final int SOCKS_IPV6_LENGTH = 16;

	static boolean doResolveIP = true;

}
