package info.mineshafter;

import info.mineshafter.hacks.HandlerFactory;
import info.mineshafter.mod.JarPatcher;
import info.mineshafter.util.Resources;
import info.mineshafter.util.Streams;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;

import SevenZip.Compression.LZMA.*;

public class Bootstrap extends JFrame {
	public static Thread mainThread;

	private static final long serialVersionUID = 1;
	private static int bootstrapVersion = 4;
	private static int mineshafterBootstrapVersion = 8;

	private final File workDir;
	private final File launcherJar;
	private final File packedLauncherJar;
	private final File packedLauncherJarNew;
	private final File patchedLauncherJar;
	private final File starterJar;

	public Bootstrap() {
		super("Minecraft Launcher");
		workDir = Util.getWorkingDirectory();
		launcherJar = new File(workDir, "launcher.jar");
		packedLauncherJar = new File(workDir, "launcher.pack.lzma");
		packedLauncherJarNew = new File(workDir, "launcher.pack.lzma.new");
		patchedLauncherJar = new File(workDir, "launcher_mcpatched.jar");
		starterJar = new File(workDir, "ms-starter.jar");
	}

	public void run() {
		if (!workDir.isDirectory()) workDir.mkdir();
		if (packedLauncherJarNew.isFile()) renameNew();

		String md5 = null;
		if (packedLauncherJar.exists()) md5 = Util.getMd5(packedLauncherJar);
		if (!Util.grabLauncher(md5, packedLauncherJarNew)) System.out.println("New launcher not downloaded");
		renameNew();
		unpack();
		patchLauncher();
		buildGameStarter();
		startLauncher();
	}

	public void renameNew() {
		if (packedLauncherJarNew.isFile()) {
			packedLauncherJar.delete();
			packedLauncherJarNew.renameTo(packedLauncherJar);
		}
	}

	public void unpack() {
		if (!this.packedLauncherJar.exists()) return;

		String path = packedLauncherJar.getAbsolutePath();
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

			JarOutputStream jarOut = new JarOutputStream(new FileOutputStream(launcherJar));
			Pack200.newUnpacker().unpack(unpacked, jarOut);
			jarOut.close();
			unpacked.delete();
		} catch (IOException e) {
			System.out.println("Exception while unpacking:");
			e.printStackTrace();
		}
	}

	public void patchLauncher() {
		if (!launcherJar.exists()) return;
		if (patchedLauncherJar.exists()) patchedLauncherJar.delete();

		JarPatcher patcher = new JarPatcher(launcherJar);
		for(String name : patcher.getEntries()) { // Get rid of all that metadata
			if(name.startsWith("META-INF/")) {
				patcher.removeEntry(name);
			}
		}
		patcher.setEntry("net/minecraft/launcher/game/MinecraftGameRunner.class", Resources.loadByteArray("resources/MinecraftGameRunner.class"));
		patcher.setEntry("net/minecraft/launcher/game/MinecraftGameRunner$1.class", Resources.loadByteArray("resources/MinecraftGameRunner$1.class"));
		patcher.setEntry("yggdrasil_session_pubkey.der", Resources.loadByteArray("resources/ms_pubkey.der"));

		patcher.setEntry("info/mineshafter/mod/JarPatcher.class", Resources.loadByteArray("info/mineshafter/mod/JarPatcher.class"));
		patcher.setEntry("info/mineshafter/util/Streams.class", Resources.loadByteArray("info/mineshafter/util/Streams.class"));
		patcher.setEntry("info/mineshafter/util/Resources.class", Resources.loadByteArray("info/mineshafter/util/Resources.class"));

		patcher.setEntry("resources/Property.class", Resources.loadByteArray("resources/Property.class"));

		patcher.write(patchedLauncherJar);
	}

	public void buildGameStarter() {
		File thisFile = new File(Bootstrap.class.getProtectionDomain().getCodeSource().getLocation().getPath());
		InputStream in = null;
		OutputStream out = null;
		try {
			in = new FileInputStream(thisFile);

			if (starterJar.exists()) { // Only delete once we know we can make a new one
				starterJar.delete();
			}

			out = new FileOutputStream(starterJar);

			Streams.pipeStreams(in, out);

			Streams.close(in);
			Streams.close(out);
		} catch (FileNotFoundException e) {
			return;
		}
	}

	public void startLauncher() {
		System.setErr(System.out);
		System.setProperty("java.net.preferIPv4Stack", "true");

		URL.setURLStreamHandlerFactory(new HandlerFactory());

		setSize(854, 480);
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setLocationRelativeTo(null);
		setVisible(true);

		try {
			@SuppressWarnings("resource")
			Class<?> launcher = new URLClassLoader(new URL[] { patchedLauncherJar.toURI().toURL() }).loadClass("net.minecraft.launcher.Launcher");
			Constructor<?> ctor = launcher.getConstructor(new Class[] { JFrame.class, File.class, Proxy.class, PasswordAuthentication.class, java.lang.String[].class, Integer.class });
			ctor.newInstance(new Object[] { this, this.workDir, Proxy.NO_PROXY, null, new String[] {}, bootstrapVersion });
		} catch (Exception e) {
			System.out.println("Error while starting launcher:");
			e.printStackTrace();
		}
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
