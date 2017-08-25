package control;

import java.util.ArrayList;
import java.util.List;

import communication.NamespaceReceiver;
import database.IControllable;

public class NamingService {
	
	public IControllable controller;
	public Configuration configuration;
	
	public NamingService(IControllable controller, Configuration configuration) {
		this.controller = controller;
		this.configuration = configuration;
		NamespaceReceiver receiver = new NamespaceReceiver(this, configuration.getAddress(), configuration.getPort());
		receiver.startReceiving();
		
		try {
			initialize();
		} catch (IllegalArgumentException | InterruptedException e) {
			e.printStackTrace();
		}
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
	}
	
	private void createSystemNodeIfDoesNotExist(String path) throws IllegalArgumentException, InterruptedException {
		if(controller.exists(path) == false) {
			controller.addNode(path, "");
		}
	}
}
