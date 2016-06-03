package info.mineshafter;

import info.mineshafter.hacks.URLHandlerFactory;

import java.lang.reflect.Method;
import java.net.URL;

public class GameStarter {
	public static void main(String[] args) {
		String className = args[0];
		System.out.println("GameStarter running! " + className);

		// Activate the hooks
		URL.setURLStreamHandlerFactory(new URLHandlerFactory());

		String[] passedArgs = new String[args.length - 1];
		for (int i = 1; i < args.length; i++) {
			passedArgs[i - 1] = args[i];
		}

		try {
			Class<?> game = Class.forName(className);
			Method main = game.getMethod("main", String[].class);
			main.invoke(null, (Object) passedArgs);
		} catch (Exception e) {
			System.out.println("Error while starting game:");
			e.printStackTrace();
		}
	}
}
