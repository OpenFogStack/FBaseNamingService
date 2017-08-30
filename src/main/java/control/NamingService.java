package control;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import communication.NamespaceReceiver;
import database.IControllable;
import model.JSONable;
import model.config.NodeConfig;
import namespace.Node;

public class NamingService {
	
	private static Logger logger = Logger.getLogger(NamingService.class.getName());
	
	public IControllable controller;
	public Configuration configuration;
	public NamespaceReceiver receiver;
	
	public NamingService(IControllable controller, Configuration configuration) {
		logger.info("Starting NamingService...");
		
		this.controller = controller;
		this.configuration = configuration;
		receiver = new NamespaceReceiver(this, configuration.getAddress(), configuration.getPort());
		receiver.startReceiving();
		
		try {
			initializeDataStorage(false);
		} catch (InterruptedException e) {
			logger.fatal("Cannot initialize NamingService. Quitting program.");
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public void tearDown() {
		receiver.stopReception();
	}
	
	/**
	 * Initializes the data storage and wipes existent data if wipeExistent == true
	 * 
	 * @param wipeExistent
	 * @throws InterruptedException
	 * @return true, if successful
	 */
	public boolean initializeDataStorage(boolean wipeExistent) throws InterruptedException {
		logger.info("Initializing NamingService...");
		boolean success = true;
		
		List<String> initialNodePaths = new ArrayList<String>();
		
		initialNodePaths.add("/client");
		initialNodePaths.add("/client/active");
		initialNodePaths.add("/client/tombstoned");
		
		initialNodePaths.add("/node");
		initialNodePaths.add("/node/active");
		initialNodePaths.add("/node/tombstoned");
		
		initialNodePaths.add("/keygroup");
		initialNodePaths.add("/keygroup/active");
		initialNodePaths.add("/keygroup/tombstoned");
		
		if (wipeExistent) {
			logger.info("Wiping existing data");
			
			for (int j = initialNodePaths.size() - 1; j >= 0; j--){
				if (controller.exists(initialNodePaths.get(j))) {
					try {
						controller.deleteNodeRecursive((initialNodePaths.get(j)));
					} catch (IOException e) {
						logger.error("Failed to delete node: " + e.getMessage());
						success = false;
						e.printStackTrace();
					}
				}
			}
			
			logger.debug("Deleted existing data");
		}
		
		// we consider it to be the first startup, if any of the initialNodePath nodes did not exist 
		boolean firstStartup = false;
		for (String s : initialNodePaths) {
			if (createSystemNodeIfDoesNotExist(s)) {
				firstStartup = true;
			}
		}
		
		if (firstStartup) {			
			File initialNodeFile = new File(configuration.getInitNodeFile());
			String initialNodeJSON = null;
			
			try {
				FileReader reader = new FileReader(initialNodeFile);
		        char[] chars = new char[(int) initialNodeFile.length()];
		        reader.read(chars);
		        initialNodeJSON = new String(chars);
		        reader.close();
			} catch (FileNotFoundException e) {
				logger.warn("Path " + configuration.getInitNodeFile() + " does not exist");
				success = false;
			} catch (IOException e) {
				logger.warn("Error processing " + configuration.getInitNodeFile());
				e.printStackTrace();
				success = false;
			}
			
			NodeConfig initNode = JSONable.fromJSON(initialNodeJSON, NodeConfig.class);
			if(!Node.getInstance().exists(controller, initNode.getID())) {
				logger.info("Creating initial node...");
				Node.getInstance().createNode(controller, initNode);
			} else {
				logger.debug("Initial node already exists.");
			}
		}
		
		return success;
	}
	
	/**
	 * Create a system node if it does not exist yet.
	 * 
	 * @param path for the to be created node
	 * @return true, if not existed before
	 * @throws IllegalArgumentException
	 * @throws InterruptedException
	 */
	private boolean createSystemNodeIfDoesNotExist(String path)
			throws IllegalArgumentException, InterruptedException {
		boolean firstStartup = false;
		if (controller.exists(path) == false) {
			firstStartup = true;
			controller.addNode(path, "");
		}
		return firstStartup;
	}
}
