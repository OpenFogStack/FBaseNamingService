package namespace;

import ZkSystem.ZkController;
import model.config.NodeConfig;
import model.data.NodeID;

/**
 * The Node class performs all operations on the Nodes section of
 * the system. This is primarily designed to keep track of information
 * about all fog nodes similar to a phone book so their details
 * are available.
 * 
 * @author Wm. Keith van der Meulen
 */
class Node extends SystemEntity {
	
	/**
	 * Name of the entity type
	 */
	static final String type = "node";
	
	/**
	 * Registers a node with the FBase system
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param entity The NodeConfig object be registered to the system
	 * @return Response object with Boolean containing the success or failure of operation
	 */
	static Response<Boolean> registerNode(ZkController controller, NodeConfig entity) {
		return registerEntity(controller, entity.getNodeID(), entity);
	}
	
	/**
	 * Responds with all information about the node
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param nodeID ID of node to get information from
	 * @return Response object with String containing the Node information
	 */
	static Response<String> getNodeInfo(ZkController controller, NodeID nodeID) {
		return getEntityInfo(controller, nodeID);
	}
	
	/**
	 * Updates information kept on the node with the matching node ID
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param entity The new NodeConfig object to store
	 * @return Response object with Boolean containing the success or failure of operation
	 */
	static Response<Boolean> updateNodeInfo(ZkController controller, NodeConfig entity) {
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
	static Response<Boolean> removeNode(ZkController controller, NodeID nodeID) {
		return removeEntity(controller, nodeID);
	}
}
