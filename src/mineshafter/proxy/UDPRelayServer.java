package mineshafter.proxy;

import java.net.*;
import java.io.*;

class UDPRelayServer {
	DatagramSocket client_sock;
	DatagramSocket remote_sock;

	int relayPort;
	InetAddress relayIP;

	Thread pipe_thread1, pipe_thread2;
	Thread master_thread;

	static int datagramSize = 0xFFFF; // 64K, a bit more than max udp size
	static int idleTimeout = 30 * 1000;

	public UDPRelayServer(InetAddress clientIP, int clientPort) throws IOException {
		client_sock = new DatagramSocket(0, InetAddress.getLoopbackAddress());

		// client_sock = new Socks5DatagramSocket(clientIP, clientPort); XXX
		relayPort = client_sock.getLocalPort();
		relayIP = client_sock.getLocalAddress();

		remote_sock = new DatagramSocket();
	}

	// Public methods
	// ///////////////

	/**
	 * Sets the timeout for UDPRelay server.<br>
	 * Zero timeout implies infinity.<br>
	 * Default timeout is 3 minutes.
	 */

	static public void setTimeout(int timeout) {
		idleTimeout = timeout;
	}

	/**
	 * Sets the size of the datagrams used in the UDPRelayServer.<br>
	 * Default size is 64K, a bit more than maximum possible size of the
	 * datagram.
	 */
	static public void setDatagramSize(int size) {
		datagramSize = size;
	}

	/**
	 * Port to which client should send datagram for association.
	 */
	public int getRelayPort() {
		return relayPort;
	}

	/**
	 * IP address to which client should send datagrams for association.
	 */
	public InetAddress getRelayIP() {
		return relayIP;
	}

	public void start() throws IOException {
		pipeActive(client_sock, remote_sock);
		pipeActive(remote_sock, client_sock);
	}

	public synchronized void stop() {
		remote_sock.close();
		client_sock.close();
	}

	private void pipe(final DatagramSocket from, final DatagramSocket to) {
		byte[] data = new byte[datagramSize];
		DatagramPacket dp = new DatagramPacket(data, data.length);// XXX
		try {
			from.setSoTimeout(idleTimeout);
			while (true) {
				from.receive(dp);
				to.send(dp);
				dp.setLength(data.length);
			}
		} catch (IOException e) {}
	}

	private void pipeActive(final DatagramSocket from, final DatagramSocket to) {
		Thread t = new Thread("Datagram pipe thread") {
			public void run() {
				pipe(from, to);
			}
		};
		t.start();
	}
}
