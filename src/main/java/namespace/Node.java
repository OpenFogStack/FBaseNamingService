package namespace;

import java.util.List;

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
	 * Node ID string
	 */
	String nodeID;
	
	/**
	 * Public encryption key of the node
	 */
	String publicKey;
	
	/**
	 * List of addresses (e.g. IP address) of all machines associated with the node
	 */
	List<String> machines;
	
	/**
	 * Plain text description of node location (e.g. "Berlin, Germany")
	 */
	String location;
	
	/**
	 * Plain text description of node (e.g. "TU Berlin ISE Raspberry Pi Cluster #4")
	 */
	String description;
	
	/**
	 * Constructor for Node containing all node fields used
	 * within FBase.
	 * 
	 * @param nodeID Node ID string
	 * @param publicKey Public encryption key of the node
	 * @param machines List of addresses (e.g. IP address) of all machines associated with the node
	 * @param location Plain text description of node location (e.g. "Berlin, Germany")
	 * @param description Plain text description of node (e.g. "TU Berlin ISE Raspberry Pi Cluster #4")
	 */
	Node(String nodeID, String publicKey, List<String> machines, String location, String description) {
		this.nodeID = nodeID;
		this.publicKey = publicKey;
		this.machines = machines;
		this.location = location;
		this.description = description;
	}
	
	/**
	 * Registers a node with the FBase system
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param nodeID Requested ID of new node
	 * @param publicKey Public encryption key of the node
	 * @param machines List of addresses (e.g. IP address) of all machines associated with the node
	 * @param location Plain text description of node location (e.g. "Berlin, Germany")
	 * @param description Plain text description of node (e.g. "TU Berlin ISE Raspberry Pi Cluster #4")
	 * @return Response object with Boolean containing the success or failure of operation
	 */
	static Response<Boolean> registerNode(ZkController controller, String nodeID, String publicKey, List<String> machines, String location, String description) {
		// Create Node to register
		Node entity = new Node(nodeID, publicKey, machines, location, description);
		
		return registerEntity(controller, nodeID, entity);
	}
	
	/**
	 * Responds with all information about the node
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param nodeID ID of node to get information from
	 * @return Response object with String containing the Node information
	 */
	static Response<String> getNodeInfo(ZkController controller, String nodeID) {
		return getEntityInfo(controller, nodeID);
	}
	
	/**
	 * Updates information kept on the node with the matching node ID
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param nodeID ID of node to update
	 * @param publicKey Public encryption key of the node
	 * @param machines List of addresses (e.g. IP address) of all machines associated with the node
	 * @param location Plain text description of node location (e.g. "Berlin, Germany")
	 * @param description Plain text description of node (e.g. "TU Berlin ISE Raspberry Pi Cluster #4")
	 * @return Response object with Boolean containing the success or failure of operation
	 */
	static Response<Boolean> updateNodeInfo(ZkController controller, String nodeID, String publicKey, List<String> machines, String location, String description) {
		// Create Node to update
		Node entity = new Node(nodeID, publicKey, machines, location, description);
		
		return updateEntityInfo(controller, nodeID, entity);
	}
	
	/**
	 * Permanently tombstones a node in the system. Tombstoned nodes wishing to
	 * enter the system again must register as a new node with a new ID
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param nodeID Node to tombstone
	 * @return Response object with Boolean containing the success or failure of operation
	 */
	static Response<Boolean> removeNode(ZkController controller, String nodeID) {
		return removeEntity(controller, nodeID);
	}
}
