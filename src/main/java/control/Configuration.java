package control;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Configuration {
	
	private Properties properties;
	private static final String defaultFileName = "local.properties";
	
	// General
	private String address;
	private int port;
	
	// Security
	private String publicKey;
	private String privateKey;
	
	// System
	private String system;
	private String root;
	
	public Configuration() {
		this(defaultFileName);
	}
	
	public Configuration(String configName) {
		this.properties = new Properties();
		
		InputStream is = Configuration.class.getClassLoader().getResourceAsStream(configName);
		
		try {
			properties.load(is);
			
			// General
			address = properties.getProperty("address");
			port = Integer.parseInt(properties.getProperty("port"));
			
			// Security 
			publicKey = properties.getProperty("publicKey");
			privateKey = properties.getProperty("privateKey");
			
			// System
			system = properties.getProperty("system");
			root = properties.getProperty("root", "");
			
			// TODO Check all fields are properly set
		} catch (IOException | NumberFormatException e) {
			e.printStackTrace();
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

	public String getPublicKey() {
		return publicKey;
	}

	public String getPrivateKey() {
		return privateKey;
	}
	
	public String getSystem() {
		return system;
	}
	
	public String getRoot() {
		return root;
	}
}
