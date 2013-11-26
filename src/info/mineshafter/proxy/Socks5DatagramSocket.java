package info.mineshafter.proxy;

import java.net.*;
import java.io.*;

public class Socks5DatagramSocket extends DatagramSocket {
	InetAddress relayIP;
	int relayPort;

	Socks5DatagramSocket(InetAddress relayIP, int relayPort) throws IOException {
		super();
		this.relayIP = relayIP;
		this.relayPort = relayPort;
	}

	public void send(DatagramPacket dp) throws IOException {
		byte[] head = formHeader(dp.getAddress(), dp.getPort());
		byte[] buf = new byte[head.length + dp.getLength()];
		byte[] data = dp.getData();
		// Merge head and data
		System.arraycopy(head, 0, buf, 0, head.length);
		// System.arraycopy(data,dp.getOffset(),buf,head.length,dp.getLength());
		System.arraycopy(data, 0, buf, head.length, dp.getLength());

		super.send(new DatagramPacket(buf, buf.length, relayIP, relayPort));
	}

	public void receive(DatagramPacket dp) throws IOException {
		super.receive(dp);
		int init_length = dp.getLength();
		while (!relayIP.equals(dp.getAddress()) || relayPort != dp.getPort()) {
			dp.setLength(init_length);
			super.receive(dp);
		}

		byte[] data = dp.getData();
		ByteArrayInputStream bIn = new ByteArrayInputStream(data, 0, dp.getLength());
		SocksMessage msg = new Socks5Message(bIn);
		dp.setPort(msg.port);
		dp.setAddress(msg.getInetAddress());

		int data_length = bIn.available();
		System.arraycopy(data, dp.getLength() - data_length, data, 0, data_length);
		dp.setLength(data_length);
	}

	private byte[] formHeader(InetAddress ip, int port) {
		Socks5Message request = new Socks5Message(0, ip, port);
		request.data[0] = 0;
		return request.data;
	}
}
