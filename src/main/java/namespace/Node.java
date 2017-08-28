package namespace;

import database.IControllable;
import model.config.NodeConfig;
import model.data.NodeID;
import model.messages.Response;

/**
 * The Node class performs all operations on the Nodes section of
 * the system. This is primarily designed to keep track of information
 * about all fog nodes similar to a phone book so their details
 * are available.
 * 
 * @author Wm. Keith van der Meulen
 */
public class Node extends SystemEntity {
	
	private static Node instance;
	
	public static Node getInstance() {
		if(instance == null) {
			instance = new Node();
		}
		
		return instance;
	}
	
	private Node() {
		super("node");
	}
	
	/**
	 * Registers a node with the FBase system
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param entity The NodeConfig object be registered to the system
	 * @return Response object with Boolean containing the success or failure of operation
	 */
	public Response<Boolean> registerNode(IControllable controller, NodeConfig entity) {
		return registerEntity(controller, entity.getNodeID(), entity);
	}
	
	/**
	 * Responds with all information about the node
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param nodeID ID of node to get information from
	 * @return Response object with String containing the Node information
	 */
	public Response<String> getNodeInfo(IControllable controller, NodeID nodeID) {
		return getEntityInfo(controller, nodeID);
	}
	
	/**
	 * Updates information kept on the node with the matching node ID
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param entity The new NodeConfig object to store
	 * @return Response object with Boolean containing the success or failure of operation
	 */
	Response<Boolean> updateNodeInfo(IControllable controller, NodeConfig entity) {
		return updateEntityInfo(controller, entity.getNodeID(), entity);
	}
	
	/**
	 * Permanently tombstones a node in the system. Tombstoned nodes wishing to
	 * enter the system again must register as a new node with a new ID
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param nodeID Node to tombstone
	 * @return Response object with Boolean containing the success or failure of operation
	 */
	Response<Boolean> removeNode(IControllable controller, NodeID nodeID) {
		return removeEntity(controller, nodeID);
	}
}
