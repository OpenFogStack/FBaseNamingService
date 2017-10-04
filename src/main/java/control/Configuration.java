package control;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;

public class Configuration {
	
	private static Logger logger = Logger.getLogger(Configuration.class.getName());
	
	private Properties properties;
	private static final String defaultFileName = "local.properties";
	
	// General
	private String address;
	private int port;
	private boolean debugMode;
	
	// Security
	private String publicKey;
	private String privateKey;
	
	// System
	private String system;
	private String localOS;
	private String folderSeparator;
	private String root;
	
	// Initialization
	private String initNodeFile;
	private String initClientFile;
	
	public Configuration() {
		this(defaultFileName);
	}
	
	public Configuration(String configName) {
		logger.info("Starting configuration...");
		
		this.properties = new Properties();
		
		InputStream is = Configuration.class.getClassLoader().getResourceAsStream(configName);
		
		try {
			if (is == null) {
				is = new FileInputStream(defaultFileName);
			}
			
			properties.load(is);
			
			// General
			address = properties.getProperty("address");
			port = Integer.parseInt(properties.getProperty("port"));
			debugMode = Boolean.parseBoolean(properties.getProperty("debugMode", "false"));
			
			// Security 
			publicKey = properties.getProperty("publicKey");
			privateKey = properties.getProperty("privateKey");
			
			// System
			system = properties.getProperty("system");
			localOS = properties.getProperty("localOS").toLowerCase();
			root = properties.getProperty("root");
			
			// Initialization
			initNodeFile = properties.getProperty("initNodeFile");
			initClientFile = properties.getProperty("initClientFile");
			
			// Set
			switch(localOS) {
			case "unix":
				folderSeparator = "/";
				break;
			case "windows":
				folderSeparator = "\\";
				break;
			default:
				folderSeparator = "/";
				break;
			}	
			
			// TODO Check all fields are properly set
		} catch (IOException | NumberFormatException e) {
			logger.fatal("Error processing configuration file. Quitting program", e);
			System.exit(1);
		}
	}

	public static String getDefaultfilename() {
		return defaultFileName;
	}

	public String getAddress() {
		return address;
	}

	public int getPort() {
		return port;
	}
	
	public boolean isDebugMode() {
		return debugMode;
	}

	public String getPublicKey() {
		return publicKey;
	}

	public String getPrivateKey() {
		return privateKey;
	}
	
	public String getSystem() {
		return system;
	}
	
	public String getLocalOS() {
		return localOS;
	}
	
	public String getFolderSeparator() {
		return folderSeparator;
	}
	
	public String getRoot() {
		return root;
	}
	
	public String getInitNodeFile() {
		return initNodeFile;
	}
	
	public String getInitClientFile() {
		return initClientFile;
	}
}
