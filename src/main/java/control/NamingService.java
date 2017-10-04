package control;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import communication.NamespaceReceiver;
import database.IControllable;
import model.JSONable;
import model.config.ClientConfig;
import model.config.NodeConfig;
import namespace.Client;
import namespace.Node;

public class NamingService {

	private static Logger logger = Logger.getLogger(NamingService.class.getName());

	public IControllable controller;
	public Configuration configuration;
	public NamespaceReceiver receiver;

	public NamingService(IControllable controller, Configuration configuration) {
		this.controller = controller;
		this.configuration = configuration;
		receiver = new NamespaceReceiver(this, configuration.getAddress(), configuration.getPort());
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
	 * @throws FileNotFoundException
	 */
	public boolean initializeDataStorage(boolean wipeExistent)
			throws InterruptedException, FileNotFoundException {
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

			for (int j = initialNodePaths.size() - 1; j >= 0; j--) {
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

		// we consider it to be the first startup, if any of the initialNodePath nodes did not
		// exist
		boolean firstStartup = false;
		for (String s : initialNodePaths) {
			if (createSystemNodeIfDoesNotExist(s)) {
				firstStartup = true;
			}
		}

		if (firstStartup) {
			FileInputStream isNode = new FileInputStream(configuration.getInitNodeFile());
			NodeConfig initNode = JSONable.fromJSON(isNode, NodeConfig.class);
			if (!Node.getInstance().exists(controller, initNode.getID())) {
				logger.info("Creating initial node...");
				Node.getInstance().createNode(controller, initNode);
			} else {
				logger.debug("Initial node already exists.");
			}
			
			FileInputStream isClient = new FileInputStream(configuration.getInitClientFile());
			ClientConfig initClient = JSONable.fromJSON(isClient, ClientConfig.class);
			if (!Client.getInstance().exists(controller, initClient.getID())) {
				logger.info("Creating initial client...");
				Client.getInstance().createClient(controller, initClient);
			} else {
				logger.debug("Initial client already exists.");
			}
		}

		return success;
	}

	public void start() {
		try {
			initializeDataStorage(false);
		} catch (InterruptedException | FileNotFoundException e) {
			Thread.currentThread().interrupt();
			logger.fatal("Cannot initialize NamingService. Quitting program.", e);
			System.exit(1);
		}

		receiver.startReceiving();
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
			controller.addNode(path, "");
			firstStartup = true;
		}
		return firstStartup;
	}
}
