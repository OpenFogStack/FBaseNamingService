package control;

import java.io.File;

import org.apache.log4j.Logger;

import database.IControllable;
import database.localfiles.LocalFileController;

public class Starter {
	
	private static Logger logger = Logger.getLogger(Starter.class.getName());
	
	public static void main(String[] args) {
		logger.info("And SysAdmin said \"Let there be FBase Naming Service,\" and there was FBase Naming Service.");
		
		Configuration configuration = new Configuration();
		IControllable controller = new LocalFileController(new File(configuration.getRoot()), configuration.getFolderSeparator());
		NamingService ns = new NamingService(controller, configuration);
		ns.start();
	}
	
}
