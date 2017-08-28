package control;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import communication.NamespaceReceiver;
import database.IControllable;
import model.JSONable;
import model.config.NodeConfig;
import namespace.Node;

public class NamingService {
	
	public IControllable controller;
	public Configuration configuration;
	public NamespaceReceiver receiver;
	
	public NamingService(IControllable controller, Configuration configuration) {
		this.controller = controller;
		this.configuration = configuration;
		receiver = new NamespaceReceiver(this, configuration.getAddress(), configuration.getPort());
		receiver.startReceiving();
		
		try {
			initialize();
		} catch (IllegalArgumentException | InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void tearDown() {
		receiver.stopReception();
	}
	
	private void initialize() throws IllegalArgumentException, InterruptedException {
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
			throw new IllegalArgumentException("Path does not exist");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		NodeConfig initNode = JSONable.fromJSON(initialNodeJSON, NodeConfig.class);
		Node.getInstance().registerNode(controller, initNode);
	}
	
	private void createSystemNodeIfDoesNotExist(String path) throws IllegalArgumentException, InterruptedException {
		if(controller.exists(path) == false) {
			controller.addNode(path, "");
		}
	}
}
