package namespace;

import java.util.List;

import namespace.KeyGroupResponse.KeyGroupResponseCode;

/**
 * The KeyGroup class performs all operations on the KeyGroups section of
 * the system. This is primarily designed to keep track of information
 * about how nodes are logically structured together in the system
 * to form a fog network.
 * 
 * @author Wm. Keith van der Meulen
 */
class KeyGroup {
	
	/**
	 * The KeyGroup path consisting of &lt;App&gt;/&lt;User&gt;/&lt;KeyGroup&gt;
	 */
	String path;
	
	/**
	 * List of fog nodes that receive and store data
	 */
	List<String> replicaNodes;
	
	/**
	 * List of fog nodes that process data streams from replicaNodes
	 */
	List<String> triggerNodes;
	
	/**
	 * Encryption key for communication within the KeyGroup
	 */
	String encryptionKey;
	
	/**
	 * Encryption algorithm (symmetric) used for communication within the KeyGroup
	 */
	String encryptionAlgorithm;
	
	/**
	 * Constructor for KeyGroup containing all KeyGroup fields used
	 * within FBase.
	 * 
	 * @param path The KeyGroup path consisting of &lt;App&gt;/&lt;User&gt;/&lt;KeyGroup&gt;
	 * @param replicaNodes List of fog nodes that receive and store data
	 * @param triggerNodes List of fog nodes that process data streams from replicaNodes
	 * @param encryptionKey Encryption key for communication within the KeyGroup
	 * @param encryptionAlgorithm Encryption algorithm (symmetric) used for communication within the KeyGroup
	 */
	KeyGroup(String path, List<String> replicaNodes, List<String> triggerNodes, String encryptionKey, String encryptionAlgorithm) {
		this.path = path;
		this.replicaNodes = replicaNodes;
		this.triggerNodes = triggerNodes;
		this.encryptionKey = encryptionKey;
		this.encryptionAlgorithm = encryptionAlgorithm;
	}
	
	/**
	 * Creates KeyGroup within the FBase system
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param nodeID Node ID for the calling node (must be a replica node in the new KeyGroup)
	 * @param path The KeyGroup path consisting of &lt;App&gt;/&lt;User&gt;/&lt;KeyGroup&gt;
	 * @param replicaNodes List of fog nodes that receive and store data
	 * @param triggerNodes List of fog nodes that process data streams from replicaNodes
	 * @param encryptionKey Encryption key for communication within the KeyGroup
	 * @param encryptionAlgorithm Encryption algorithm (symmetric) used for communication within the KeyGroup
	 * @return Response object with Boolean containing the success or failure of operation
	 */
	static KeyGroupResponse<Boolean> createKeyGroup(ZkController controller, String nodeID, String path, List<String> replicaNodes, List<String> triggerNodes, String encryptionKey, String encryptionAlgorithm) {
		// TODO Fill out stub function
		
		return new KeyGroupResponse<Boolean>(null, KeyGroupResponseCode.NULL); // XXX Placeholder return... must be changed
	}
	
	/**
	 * Adds a replica node to an existing KeyGroup
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param nodeID The replica node ID to be added to the KeyGroup
	 * @param path The path to the KeyGroup to add a replica node to
	 * @return Response object with Boolean containing the success or failure of operation
	 */
	static KeyGroupResponse<Boolean> addReplicaNodeToKeyGroup(ZkController controller, String nodeID, String path) {
		// TODO Fill out stub function
		
		return new KeyGroupResponse<Boolean>(null, KeyGroupResponseCode.NULL); // XXX Placeholder return... must be changed
	}
	
	/**
	 * Adds a trigger node to an existing KeyGroup
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param nodeID The trigger node ID to be added to the KeyGroup
	 * @param path The path to the KeyGroup to add a trigger node to
	 * @return Response object with Boolean containing the success or failure of operation
	 */
	static KeyGroupResponse<Boolean> addTriggerNodeToKeyGroup(ZkController controller, String nodeID, String path) {
		// TODO Fill out stub function
		
		return new KeyGroupResponse<Boolean>(null, KeyGroupResponseCode.NULL); // XXX Placeholder return... must be changed
	}
	
	/**
	 * Removes a node from an existing KeyGroup
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param nodeID The node ID to be deleted from the KeyGroup
	 * @param path The path to the KeyGroup to delete the node from
	 * @return Response object with Boolean containing the success or failure of operation
	 */
	static KeyGroupResponse<Boolean> removeNodeFromKeyGroup(ZkController controller, String nodeID, String path) {
		// TODO Fill out stub function
		
		return new KeyGroupResponse<Boolean>(null, KeyGroupResponseCode.NULL); // XXX Placeholder return... must be changed
	}
	
	/**
	 * Responds with all information about the KeyGroup. This includes the
	 * encryption key and algorithm, so can only be called by nodes in the KeyGroup.
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param path The path to the KeyGroup to get information from
	 * @return Response object with String containing the KeyGroup information
	 */
	static KeyGroupResponse<String> getKeyGroupInfoAuthorized(ZkController controller, String path) {
		// TODO Fill out stub function
		
		return new KeyGroupResponse<String>(null, KeyGroupResponseCode.NULL); // XXX Placeholder return... must be changed
	}
	
	/**
	 * Responds with all information about the KeyGroup. This does not includes the
	 * encryption key and algorithm, so can be used by any node in the system.
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param path The path to the KeyGroup to get information from
	 * @return Response object with String containing the KeyGroup information
	 */
	static KeyGroupResponse<String> getKeyGroupInfoUnauthorized(ZkController controller, String path) {
		// TODO Fill out stub function
		
		return new KeyGroupResponse<String>(null, KeyGroupResponseCode.NULL); // XXX Placeholder return... must be changed
	}
	
	/**
	 * Updates the encryption key and algorithm to be used for communication
	 * within the KeyGroup.
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param path The path to the KeyGroup to get update encryption information
	 * @param encryptionKey Encryption key for communication within the KeyGroup
	 * @param encryptionAlgorithm Encryption algorithm (symmetric) used for communication within the KeyGroup
	 * @return Response object with Boolean containing the success or failure of operation
	 */
	static KeyGroupResponse<Boolean> updateKeyGroupCrypto(ZkController controller, String path, String encryptionKey, String encryptionAlgorithm) {
		// TODO Fill out stub function
		
		return new KeyGroupResponse<Boolean>(null, KeyGroupResponseCode.NULL); // XXX Placeholder return... must be changed
	}
	
	/**
	 * Permanently tombstones KeyGroup for eventual deletion upon all nodes in the KeyGroup
	 * successfully removing themselves.
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param path The path to the KeyGroup to remove
	 * @return Response object with Boolean containing the success or failure of operation
	 */
	static KeyGroupResponse<Boolean> removeKeyGroup(ZkController controller, String path) {
		// TODO Fill out stub function
		
		return new KeyGroupResponse<Boolean>(null, KeyGroupResponseCode.NULL); // XXX Placeholder return... must be changed
	}
}
