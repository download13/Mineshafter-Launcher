package mineshafter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;

import mineshafter.proxy.ModularProxy;
import mineshafter.proxy.SocksProxyConnection;
import mineshafter.proxy.YggdrasilProxyHandler;
import mineshafter.util.Resources;
import mineshafter.util.Streams;
import SevenZip.Compression.LZMA.*;

public class Bootstrap extends JFrame {
	public static Thread mainThread;

	private static final long serialVersionUID = 1;
	private static int bootstrapVersion = 4;
	private static int mineshafterBootstrapVersion = 4;

	private final File workDir;
	private final File launcherJar;
	private final File packedLauncherJar;
	private final File packedLauncherJarNew;
	private final File patchedLauncherJar;

	public Bootstrap() {
		super("Minecraft Launcher");
		this.workDir = Util.getWorkingDirectory();
		this.launcherJar = new File(workDir, "launcher.jar");
		this.packedLauncherJar = new File(workDir, "launcher.pack.lzma");
		this.packedLauncherJarNew = new File(workDir, "launcher.pack.lzma.new");
		this.patchedLauncherJar = new File(workDir, "launcher_mcpatched.jar");
	}

	public void run() {
		if (!workDir.isDirectory()) workDir.mkdir();
		if (packedLauncherJarNew.isFile()) renameNew();

		String md5 = null;
		if (this.packedLauncherJar.exists()) md5 = Util.getMd5(this.packedLauncherJar);
		if (!Util.grabLauncher(md5, this.packedLauncherJarNew)) System.out.println("New launcher not downloaded");
		renameNew();
		unpack();
		patchLauncher();
		startLauncher();
	}

	public void renameNew() {
		if (this.packedLauncherJarNew.isFile()) {
			this.packedLauncherJar.delete();
			this.packedLauncherJarNew.renameTo(this.packedLauncherJar);
		}
	}

	public void unpack() {
		if (!this.packedLauncherJar.exists()) return;

		String path = this.packedLauncherJar.getAbsolutePath();
		File unpacked = new File(path.substring(0, path.lastIndexOf('.')));

		try {
			BufferedInputStream inStream = new BufferedInputStream(new FileInputStream(this.packedLauncherJar));
			BufferedOutputStream outStream = new BufferedOutputStream(new FileOutputStream(unpacked));

			byte[] properties = new byte[5];
			inStream.read(properties, 0, 5);
			Decoder decoder = new Decoder();
			decoder.SetDecoderProperties(properties);
			long outSize = 0;
			for (int i = 0; i < 8; i++) {
				int v = inStream.read();
				outSize |= ((long) v) << (8 * i);
			}

			decoder.Code(inStream, outStream, outSize);

			inStream.close();
			outStream.flush();
			outStream.close();

			JarOutputStream jarOut = new JarOutputStream(new FileOutputStream(this.launcherJar));
			Pack200.newUnpacker().unpack(unpacked, jarOut);
			jarOut.close();
			unpacked.delete();
		} catch (IOException e) {
			System.out.println("Exception while unpacking:");
			e.printStackTrace();
		}
	}

	public void patchLauncher() {
		if (!this.launcherJar.exists()) return;
		if (this.patchedLauncherJar.exists()) this.patchedLauncherJar.delete();

		try {
			ZipInputStream inStream = new ZipInputStream(new FileInputStream(this.launcherJar));
			ZipOutputStream outStream = new ZipOutputStream(new FileOutputStream(this.patchedLauncherJar));
			ZipEntry entry;
			String n;
			InputStream dataSource;
			while ((entry = inStream.getNextEntry()) != null) {
				n = entry.getName();
				if (n.startsWith("META-INF/") && (n.endsWith(".DSA") || n.endsWith(".RSA") || n.endsWith(".SF"))) continue;

				outStream.putNextEntry(entry);
				if (n.equals("META-INF/MANIFEST.MF")) dataSource = new ByteArrayInputStream("Manifest-Version: 1.0\n".getBytes());
				else if (n.equals("net/minecraft/launcher/Http.class")) dataSource = Resources.load("resources/Http.class");
				else if (n.equals("net/minecraft/launcher/updater/download/Downloadable.class")) dataSource = Resources.load("resources/Downloadable.class");
				else if (n.equals("net/minecraft/hopper/Util.class")) dataSource = Resources.load("resources/Util.class");
				else dataSource = inStream;
				Streams.pipeStreams(dataSource, outStream);
				outStream.flush();
			}
			inStream.close();
			outStream.close();
		} catch (Exception e) {
			System.out.println("Error while patching launcher:");
			e.printStackTrace();
		}
	}

	public void startLauncher() {
		ModularProxy proxy = new ModularProxy(SocksProxyConnection.class, (Object) new YggdrasilProxyHandler());
		proxy.start();
		int proxyPort = proxy.getListeningPort();

		System.setErr(System.out);
		System.setProperty("java.net.preferIPv4Stack", "true");
		Proxy proxyInfo = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("127.0.0.1", proxyPort));

		try {
			Class<?> launcher = new URLClassLoader(new URL[] { this.patchedLauncherJar.toURI().toURL() }).loadClass("net.minecraft.launcher.Launcher");
			Constructor<?> ctor = launcher.getConstructor(new Class[] { JFrame.class, File.class, Proxy.class, PasswordAuthentication.class, java.lang.String[].class, Integer.class });
			ctor.newInstance(new Object[] { this, this.workDir, proxyInfo, null, new String[] {}, bootstrapVersion });
		} catch (Exception e) {
			System.out.println("Error while starting launcher:");
			e.printStackTrace();
		}

		setSize(854, 480);
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setLocationRelativeTo(null);
		setVisible(true);
	}

	public static void main(String[] args) {
		mainThread = Thread.currentThread();

		float v = Util.getCurrentBootstrapVersion();
		System.out.println("Current proxy version: " + mineshafterBootstrapVersion);
		System.out.println("Gotten proxy version: " + v);
		if (mineshafterBootstrapVersion < v) {
			JOptionPane.showMessageDialog(null, "A new version of Mineshafter is available at http://mineshafter.info/\nGo get it.", "Update Available", JOptionPane.PLAIN_MESSAGE);
			System.exit(0);
		}

		Bootstrap frame = new Bootstrap();
		frame.run();
	}
}
