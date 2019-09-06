package info.mineshafter;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLStreamHandlerFactory;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;

import info.mineshafter.hacks.URLHandlerFactory;

public class GameStarter {
	public static void main(String[] args) {
		String className = args[0];

		String[] passedArgs = new String[args.length - 1];
		for (int i = 1; i < args.length; i++) {
			passedArgs[i - 1] = args[i];
		}

		// LogManager.getLogger().info("GameStarter running: args {}", args);

		Class<?> game;
		try {
			game = Class.forName(className);
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
			return;
		}

		Method main;
		try {
			main = game.getMethod("main", String[].class);
		} catch (NoSuchMethodException e1) {
			e1.printStackTrace();
			return;
		} catch (SecurityException e1) {
			e1.printStackTrace();
			return;
		}

		Thread setupProtoclHandlersThread = new Thread() {
			public void run() {
				try {
					TimeUnit.MILLISECONDS.sleep(1000);
				} catch (InterruptedException e) {
					LogManager.getLogger().info("didnt sleep");
					e.printStackTrace();
				}

				LogManager.getLogger().info("gonna patch");

				setupProtocolHandlers();
			}
		};
		setupProtoclHandlersThread.start();

		try {
			main.invoke(null, (Object) passedArgs);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
			return;
		}
	}

	public static void setupProtocolHandlers() {
		LogManager.getLogger().info("Patching protocol factory");
		try {
			final Field factoryField = URL.class.getDeclaredField("factory");
			factoryField.setAccessible(true);
			URLStreamHandlerFactory previousFactory = (URLStreamHandlerFactory) factoryField.get(null);
			LogManager.getLogger().info("Previous factory: {}", previousFactory);
			URLHandlerFactory factory = new URLHandlerFactory();
			factory.setPreviousFactory(previousFactory);
			factoryField.set(null, null);
			URL.setURLStreamHandlerFactory(factory);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}
}
