package control;

import java.io.File;

import database.IControllable;
import database.localfiles.LocalFileController;

public class Starter {

	public static void main(String[] args) {
		Configuration configuration = new Configuration();
		IControllable controller = new LocalFileController(new File(configuration.getRoot()));
		new NamingService(controller, configuration);
	}

}
