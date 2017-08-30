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
		this.controller = controller;
		this.configuration = configuration;
		receiver = new NamespaceReceiver(this, configuration.getAddress(), configuration.getPort());
	}
	
	public void tearDown() {
		receiver.stopReception();
	}
	
	private void initialize() throws InterruptedException {
		logger.info("Initializing NamingService...");
		
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
		
		for(String s : initialNodePaths) {
			createSystemNodeIfDoesNotExist(s);
		}
		
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
		} catch (IOException e) {
			logger.warn("Error processing " + configuration.getInitNodeFile(), e);
		}
		
		NodeConfig initNode = JSONable.fromJSON(initialNodeJSON, NodeConfig.class);
		if(Node.getInstance().exists(controller, initNode.getID())) {
			logger.info("Creating initial node...");
			Node.getInstance().createNode(controller, initNode);
		} else {
			logger.debug("Initial node already exists.");
		}
	}
	
	public void start() {
		try {
			initialize();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			logger.fatal("Cannot initialize NamingService. Quitting program.", e);
			System.exit(1);
		}
		
		receiver.startReceiving();
	}
	
	private void createSystemNodeIfDoesNotExist(String path) throws IllegalArgumentException, InterruptedException {
		if(controller.exists(path) == false) {
			controller.addNode(path, "");
		}
	}
}
