package info.mineshafter.proxy;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.io.*;

public class ModularProxy implements Runnable {
	static int timeout = 30 * 1000;

	private ServerSocket ss;
	private Class<? extends ProxyConnection> connectionClass;
	private Object handler;

	public ModularProxy(Class<? extends ProxyConnection> cc, Object h) {
		connectionClass = cc;
		handler = h;
	}

	public int getListeningPort() {
		return ss.getLocalPort();
	}

	public void start() {
		try {
			ss = new ServerSocket(0, 16, InetAddress.getLoopbackAddress());
			System.out.println("p: "+ss.getLocalSocketAddress().toString());
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		new Thread(this).start();
	}

	public void run() {
		try {
			while (true) {
				Socket s = ss.accept();
				Constructor<?> ctor = connectionClass.getConstructors()[0];
				ProxyConnection pc = (ProxyConnection) ctor.newInstance(new Object[] { s, handler });
				pc.start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
