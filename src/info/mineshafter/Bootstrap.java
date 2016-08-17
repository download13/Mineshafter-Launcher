package info.mineshafter;

import info.mineshafter.hacks.URLHandlerFactory;
import info.mineshafter.util.JarPatcher;
import info.mineshafter.util.Resources;
import info.mineshafter.util.Streams;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URL;
import java.net.URLClassLoader;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;

public class Bootstrap extends JFrame {
	public static Thread mainThread;

	private static final long serialVersionUID = 1;
	private static int bootstrapVersion = 4;
	private static int mineshafterBootstrapVersion = 9;

	public Bootstrap() {
		super("Minecraft Launcher");

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
		File workDir = Util.getWorkingDirectory();
		File launcherJar = new File(workDir, "launcher.jar");
		File packedLauncherJar = new File(workDir, "launcher.pack.lzma");
		File patchedLauncherJar = new File(workDir, "launcher_mcpatched.jar");
		File starterJar = new File(workDir, "ms-starter.jar");

		if (!workDir.isDirectory()) workDir.mkdir();

		ensureLatestLauncherExists(packedLauncherJar, launcherJar);
		ensurePatchedLauncherExists(launcherJar, patchedLauncherJar);
		ensureGameStarterExists(starterJar);
		startLauncher(workDir, frame, patchedLauncherJar);
	}

	private static void ensureLatestLauncherExists(File packedLauncherJar, File launcherJar) {
		File newerPackedLauncherJar = new File(packedLauncherJar.getParentFile(), "launcher.pack.lzma.new");
		File packed200LauncherJar = new File(packedLauncherJar.getParentFile(), "launcher.pack");

		String md5 = null;
		if (packedLauncherJar.exists()) md5 = Util.getMd5(packedLauncherJar);
		Util.grabLauncher(md5, newerPackedLauncherJar);

		if (newerPackedLauncherJar.isFile()) {
			packedLauncherJar.delete();
			newerPackedLauncherJar.renameTo(packedLauncherJar);
		}

		Util.unpackLZMA(packedLauncherJar, packed200LauncherJar);
		Util.unpack200(packed200LauncherJar, launcherJar);
		packed200LauncherJar.delete();
	}

	private static void ensurePatchedLauncherExists(File launcherJar, File patchedLauncherJar) {
		if (launcherJar.exists() && patchedLauncherJar.exists()) patchedLauncherJar.delete();

		JarPatcher patcher = new JarPatcher(launcherJar);
		for (String name : patcher.getEntries()) { // Get rid of all that metadata
                    if (name.startsWith("META-INF/")) {
                        if (name.contains("MANIFEST.MF")){
                            //Remove MANIFEST.MF signatures
                            //Also this file contains Implementation-Version parameter which is loaded by the launcher as the launcher version
                            byte[] manifestData = patcher.getEntry(name);
                            String manifestString = new String(manifestData);
                            String rippedData = manifestString.substring(0, manifestString.indexOf("Name"));
                            patcher.setEntry(name, rippedData.getBytes());
                        } else if (!name.contains("log4j-provider.properties")){
                            //Ignore this file. It is needed for Console Tab and logging related.
                            patcher.removeEntry(name);
                        }   
                    }
		}
		patcher.setEntry("net/minecraft/launcher/game/MinecraftGameRunner.class", Resources.loadByteArray("resources/MinecraftGameRunner.class"));
		patcher.setEntry("net/minecraft/launcher/game/MinecraftGameRunner$1.class", Resources.loadByteArray("resources/MinecraftGameRunner$1.class"));

		//patcher.setEntry("info/mineshafter/mod/JarPatcher.class", Resources.loadByteArray("info/mineshafter/mod/JarPatcher.class"));
		//patcher.setEntry("info/mineshafter/util/Streams.class", Resources.loadByteArray("info/mineshafter/util/Streams.class"));
		//patcher.setEntry("info/mineshafter/util/Resources.class", Resources.loadByteArray("info/mineshafter/util/Resources.class"));

		patcher.write(patchedLauncherJar);
	}

	private static void ensureGameStarterExists(File starterJar) {
		// Basically just copy this launcher into another file
		File thisJarFile = new File(Bootstrap.class.getProtectionDomain().getCodeSource().getLocation().getPath());
		InputStream in = null;
		OutputStream out = null;
		try {
			in = new FileInputStream(thisJarFile);

			if (starterJar.exists()) { // Only delete once we know we can make a new one
				starterJar.delete();
			}

			out = new FileOutputStream(starterJar);

			Streams.pipeStreams(in, out);

			Streams.close(in);
			Streams.close(out);
		} catch (FileNotFoundException e) {
			System.out.println("This jar file not found: Unable to create ms-starter.jar");
			return;
		}
	}

	private static void startLauncher(File workDir, JFrame frame, File patchedLauncherJar) {
		System.setErr(System.out);
		System.setProperty("java.net.preferIPv4Stack", "true");

		URL.setURLStreamHandlerFactory(new URLHandlerFactory());

		try {
			@SuppressWarnings("resource")
			Class<?> launcher = new URLClassLoader(new URL[] { patchedLauncherJar.toURI().toURL() }).loadClass("net.minecraft.launcher.Launcher");
			Constructor<?> ctor = launcher.getConstructor(new Class[] { JFrame.class, File.class, Proxy.class, PasswordAuthentication.class, java.lang.String[].class, Integer.class });
			ctor.newInstance(new Object[] { frame, workDir, Proxy.NO_PROXY, null, new String[] {}, bootstrapVersion });
		} catch (Exception e) {
			System.out.println("Error while starting launcher:");
			e.printStackTrace();
		}
	}
}
