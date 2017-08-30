package namespace;

import java.util.HashSet;
import java.util.function.BiConsumer;

import org.apache.log4j.Logger;

import crypto.CryptoProvider.EncryptionAlgorithm;
import database.IControllable;
import model.JSONable;
import model.config.KeygroupConfig;
import model.config.KeygroupMember;
import model.config.ReplicaNodeConfig;
import model.config.TriggerNodeConfig;
import model.data.ClientID;
import model.data.KeygroupID;
import model.data.NodeID;
import model.messages.Response;
import model.messages.ResponseCode;

/**
 * The Keygroup class performs all operations on the Keygroups section of
 * the system. This is primarily designed to keep track of information
 * about how nodes are logically structured together in the system
 * to form a fog network.
 * 
 * @author Wm. Keith van der Meulen
 */
public class Keygroup extends SystemEntity {
	
	private static Logger logger = Logger.getLogger(Keygroup.class.getName());
	
	private static Keygroup instance;
	
	public static Keygroup getInstance() {
		if(instance == null) {
			instance = new Keygroup();
		}
		
		return instance;
	}
	
	private Keygroup() {
		super("keygroup");
	}
	
	/**
	 * Creates Keygroup within the FBase system
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param entity KeygroupConfig object to add to system
	 * @return Response object with Boolean containing the success or failure of operation
	 */
	Response<String> createKeygroup(IControllable controller, KeygroupConfig entity) {
		try {
			if(isActive(controller, entity.getKeygroupID())) {
				logger.warn("Keygroup " + entity.getID() + " is already is active");
				return new Response<String>(null, ResponseCode.ERROR_IS_ACTIVE);
			} else if (isTombstoned(controller, entity.getKeygroupID())) {
				logger.warn("Keygroup " + entity.getID() + " is already tombstoned");
				return new Response<String>(null, ResponseCode.ERROR_TOMBSTONED);
			} else {
				// Build App Node if necessary
				if(!isActive(controller, entity.getKeygroupID().getAppPath())) {
					controller.addNode(activePath(entity.getKeygroupID().getAppPath()), "");
				}
				
				// Build Tenant Node if necessary
				if(!isActive(controller, entity.getKeygroupID().getTenantPath())) {
					controller.addNode(activePath(entity.getKeygroupID().getTenantPath()), "");
				}
				
				// Build Keygroup Node
				return createEntity(controller, entity.getID(), entity);
			} 
		} catch (InterruptedException e) {
			logger.error("Error creating keygroup " + entity.getID());
			e.printStackTrace();
			return new Response<String>(null, ResponseCode.ERROR_INTERNAL);
		}
	}

	/**
	 * Adds a client to an existing Keygroup
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param clientID The ID of the client to add
	 * @param keygroupID The ID of the Keygroup the client should be added to
	 * @return
	 */
	Response<String> addClient(IControllable controller, ClientID clientID, KeygroupID keygroupID) {
		try {
			if(isActive(controller, keygroupID)) {
				logger.debug("Adding " + clientID + " to " + keygroupID);
				
				// Get current data from keygroup and update
				String data = readEntity(controller, keygroupID).getValue();
				KeygroupConfig keygroup = JSONable.fromJSON(data.toString(), KeygroupConfig.class);
				keygroup.addClient(clientID);
				
				// Update keygroup
				return updateEntity(controller, keygroupID, keygroup);
			} else if (isTombstoned(controller, keygroupID)) {
				logger.warn("Can't add client " + clientID + " since keygroup " + keygroupID + " is tombstoned");
				return new Response<String>(null, ResponseCode.ERROR_TOMBSTONED);
			} else {
				logger.warn("Keygroup " + keygroupID + " doesn't exist");
				return new Response<String>(null, ResponseCode.ERROR_DOESNT_EXIST);
			}
		} catch (InterruptedException e) {
			logger.error("Error adding client " + clientID + " to " + keygroupID);
			e.printStackTrace();
			return new Response<String>(null, ResponseCode.ERROR_INTERNAL);
		}
	}
	
	/**
	 * Adds a replica node to an existing Keygroup
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param rnode The ReplicaNodeConfig to be added to the Keygroup
	 * @param keygroupID The ID for the config to add the replica node to
	 * @return Response object with Boolean containing the success or failure of operation
	 */
	Response<String> addReplicaNode(IControllable controller, ReplicaNodeConfig rnode, KeygroupID keygroupID) {
		logger.debug("Adding replica node " + rnode.getID() + " to " + keygroupID);
		return addNode(controller, rnode, keygroupID, (keygroupConfig, node)->keygroupConfig.addReplicaNode((ReplicaNodeConfig) node));
	}
	
	/**
	 * Adds a trigger node to an existing Keygroup
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param tNode The TriggerNodeConfig to be added to the Keygroup
	 * @param keygroupID The ID for the config to add the replica node to
	 * @return Response object with Boolean containing the success or failure of operation
	 */
	Response<String> addTriggerNode(IControllable controller, TriggerNodeConfig tNode, KeygroupID keygroupID) {
		logger.debug("Adding trigger node " + tNode.getID() + " to " + keygroupID);
		return addNode(controller, tNode, keygroupID, (keygroupConfig, node)->keygroupConfig.addTriggerNode((TriggerNodeConfig) node));
	}
	
	/**
	 * Adds a node to an existing Keygroup
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param node The node to be added to the Keygroup
	 * @param keygroupID The ID for the config to add the replica node to
	 * @param addToList Expression to add node to proper list
	 * @return Response object with Boolean containing the success or failure of operation
	 */
	private Response<String> addNode(IControllable controller, KeygroupMember node, KeygroupID keygroupID, BiConsumer<KeygroupConfig, KeygroupMember> addToList ) {
		try {
			if(isActive(controller, keygroupID)) {
				// Get current data from keygroup
				String data = readEntity(controller, keygroupID).getValue();
				
				// Parse to object, add nodeID to list and back to JSON
				KeygroupConfig keygroup = JSONable.fromJSON(data.toString(), KeygroupConfig.class);
				addToList.accept(keygroup, node);
				
				// Update keygroup
				logger.debug("Adding node " + node.getID() + " to " + keygroupID);
				return updateEntity(controller, keygroupID, keygroup);
			} else if (isTombstoned(controller, keygroupID)) {
				logger.warn("Cannot add " + node.getID() + " because keygroup " + keygroupID + "is tombstoned");
				return new Response<String>(null, ResponseCode.ERROR_TOMBSTONED);
			} else {
				logger.warn("Keygroup " + keygroupID + " doesn't exist");
				return new Response<String>(null, ResponseCode.ERROR_DOESNT_EXIST);
			}
		} catch (InterruptedException e) {
			logger.error("Error adding " + node.getID() + " to " + keygroupID);
			e.printStackTrace();
			return new Response<String>(null, ResponseCode.ERROR_INTERNAL);
		}
	}
	
	/**
	 * Removes a client from an existing Keygroup
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param clientID The ID of the client to remove from the Keygroup
	 * @param keygroupID The ID of the Keygroup to delete the client from
	 * @return
	 */
	Response<String> removeClient(IControllable controller, ClientID clientID, KeygroupID keygroupID) {
		// Get current data from keygroup
		String data = readEntity(controller, keygroupID).getValue();
		
		// Parse to object
		KeygroupConfig keygroup = JSONable.fromJSON(data.toString(), KeygroupConfig.class);
		
		// Remove client
		if(keygroup.containsClient(clientID)) {
			logger.debug("Removing " + clientID + " from " + keygroupID);
			keygroup.removeClient(clientID);
		} else {
			logger.warn("Client " + clientID + " doesn't exists in " + keygroupID);
			return new Response<String>(null, ResponseCode.ERROR_DOESNT_EXIST);
		}
		
		// Update the logical node
		return updateEntity(controller, keygroupID, keygroup);
	}
	
	/**
	 * Removes a node from an existing Keygroup
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param nodeID The node ID to be deleted from the Keygroup
	 * @param keygroupID The ID to the Keygroup to delete the node from
	 * @return Response object with Boolean containing the success or failure of operation
	 */
	Response<String> deleteNode(IControllable controller, NodeID nodeID, KeygroupID keygroupID) {
		try {
			if(isActive(controller, keygroupID)) {
				logger.debug("Removing node " + nodeID + " from active keygroup " + keygroupID);
				return removeNodeFromActiveKeygroup(controller, nodeID, keygroupID);
			} else if (isTombstoned(controller, keygroupID)) {
				logger.debug("Removing node " + nodeID + " from tombstoned keygroup " + keygroupID);
				return removeNodeFromTombstonedKeygroup(controller, nodeID, keygroupID);
			} else {
				logger.warn("Keygroup " + keygroupID + " doesn't exist");
				return new Response<String>(null, ResponseCode.ERROR_DOESNT_EXIST);
			}
		} catch (InterruptedException e) {
			logger.error("Error deleting node " + nodeID + " from keygroup " + keygroupID);
			e.printStackTrace();
			return new Response<String>(null, ResponseCode.ERROR_INTERNAL);
		}
	}
	
	/**
	 * Responds with all information about the Keygroup.
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param keygroupID The ID to the Keygroup to get information from
	 * @return Response object with String containing the Keygroup information
	 */
	Response<String> readKeygroup(IControllable controller, KeygroupID keygroupID, NodeID senderID) {
		logger.debug("Reading keygroup " + keygroupID);

		Response<String> r = readEntity(controller, keygroupID);
		
		if(r.getResponseCode().equals(ResponseCode.SUCCESS)) {
			KeygroupConfig keygroup = JSONable.fromJSON(r.getValue(), KeygroupConfig.class);
			
			if(keygroup.containsReplicaNode(senderID) || keygroup.containsTriggerNode(senderID)) {
				logger.debug("Sending node " + senderID + " reading all information from " + keygroupID);
				return r;
			} else {
				logger.debug("Sending node " + senderID + " cannot read encryption info from " + keygroupID);
				
				// Remove encryption algorithm and secret
				keygroup.setEncryptionAlgorithm(null);
				keygroup.setEncryptionSecret(null);
				
				String data = JSONable.toJSON(keygroup);
				
				return new Response<String>(data, ResponseCode.SUCCESS);
			}
		} else {
			logger.error("Reading " + keygroupID + " returns a " + r.getResponseCode() + " response");
			return r;
		}
	}
	
	/**
	 * Responds with all information about the Keygroup. This includes the
	 * encryption key and algorithm, so can only be called by nodes in the Keygroup.
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param keygroupID The ID to the Keygroup to get information from
	 * @return Response object with String containing the Keygroup information
	 */
	Response<String> readKeygroupAuthorized(IControllable controller, KeygroupID keygroupID) {
		logger.debug("Reading keygroup " + keygroupID + " with permissions of authorized node");
		String data = readEntity(controller, keygroupID).getValue();
		return new Response<String>(data, ResponseCode.SUCCESS);
	}
	
	/**
	 * Responds with all information about the Keygroup. This does not includes the
	 * encryption key and algorithm, so can be used by any node in the system.
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param keygroupID The ID to the Keygroup to get information from
	 * @return Response object with String containing the Keygroup information
	 */
	Response<String> readKeygroupUnauthorized(IControllable controller, KeygroupID keygroupID) {
		logger.debug("Reading keygroup " + keygroupID + " with permissions of unauthorized node");
		String data = readEntity(controller, keygroupID).getValue();
		
		logger.debug("Stripping encryption information from keygroup " + keygroupID + " for read by unauthorized node");
		
		// Remove encryption algorithm and secret
		KeygroupConfig keygroup = JSONable.fromJSON(data.toString(), KeygroupConfig.class);
		keygroup.setEncryptionAlgorithm(null);
		keygroup.setEncryptionSecret(null);
		data = JSONable.toJSON(keygroup);
		
		return new Response<String>(data, ResponseCode.SUCCESS);
	}
	
	/**
	 * Updates the encryption key and algorithm to be used for communication
	 * within the Keygroup.
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param keygroupID The ID to the Keygroup to get update encryption information
	 * @param encryptionSecret Encryption key for communication within the Keygroup
	 * @param encryptionAlgorithm Encryption algorithm (symmetric) used for communication within the Keygroup
	 * @return Response object with Boolean containing the success or failure of operation
	 */
	Response<String> updateKeygroupCrypto(IControllable controller, KeygroupID keygroupID, String encryptionSecret, EncryptionAlgorithm encryptionAlgorithm) {
		logger.debug("Updating cryptography information for keygroup " + keygroupID);
		try {
			if(isActive(controller, keygroupID)) {
				// Get current data from keygroup
				String data = readEntity(controller, keygroupID).getValue();
				
				KeygroupConfig keygroup = JSONable.fromJSON(data.toString(), KeygroupConfig.class);
				keygroup.setEncryptionSecret(encryptionSecret);
				keygroup.setEncryptionAlgorithm(encryptionAlgorithm);
				
				return updateEntity(controller, keygroupID, keygroup);
			} else if (isTombstoned(controller, keygroupID)) {
				logger.warn("Can't update cryptography information because keygroup " + keygroupID + " is tombstoned");
				return new Response<String>(null, ResponseCode.ERROR_TOMBSTONED);
			} else {
				logger.warn("Keygroup " + keygroupID + " doesn't exist");
				return new Response<String>(null, ResponseCode.ERROR_DOESNT_EXIST);
			}
		} catch (InterruptedException e) {
			logger.error("Error updating cryptography information for keygroup " + keygroupID);
			e.printStackTrace();
			return new Response<String>(null, ResponseCode.ERROR_INTERNAL);
		}
	}
	
	/**
	 * Irreversibly tombstones Keygroup for eventual deletion upon all nodes in the Keygroup
	 * successfully removing themselves.
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param keygroupID The ID to the Keygroup to remove
	 * @return Response object with Boolean containing the success or failure of operation
	 */
	Response<Boolean> removeKeygroup(IControllable controller, KeygroupID keygroupID) {
		logger.info("Tombstoning keygroup " + keygroupID);
		return deleteEntity(controller, keygroupID);
	}
	
	/**
	 * Removes a node from an active Keygroup
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param nodeID The node ID to be deleted from the Keygroup
	 * @param keygroupID The ID to the Keygroup to delete the node from
	 * @return Response object with Boolean containing the success or failure of operation
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	private Response<String> removeNodeFromActiveKeygroup(IControllable controller, NodeID nodeID, KeygroupID keygroupID) throws InterruptedException {
		// Get current data from keygroup
		String data = readEntity(controller, keygroupID).getValue();
		
		// Parse to object
		KeygroupConfig keygroup = JSONable.fromJSON(data.toString(), KeygroupConfig.class);
		
		// Find correct list
		if(keygroup.containsReplicaNode(nodeID)) {
			logger.debug("Attempting to remove replica node " + nodeID + " from keygroup " + keygroupID);
			// Check Node is not the last replica node
			if(keygroup.getReplicaNodes().size() == 1) {
				logger.warn("Can't remove node " + nodeID + " since it is the last replica node in the keygroup " + keygroupID);
				return new Response<String>(null, ResponseCode.ERROR_ILLEGAL_COMMAND);
			}
			
			keygroup.removeReplicaNode(nodeID);
		} else if (keygroup.containsTriggerNode(nodeID)) {
			logger.debug("Removing trigger node " + nodeID + " from keygroup " + keygroupID);
			keygroup.removeTriggerNode(nodeID);
		} else {
			logger.warn("Keygroup " + keygroupID + " doesn't exist");
			return new Response<String>(null, ResponseCode.ERROR_DOESNT_EXIST);
		}
		
		// Update the logical node
		logger.debug("Removing node " + nodeID + " from keygroup " + keygroupID);
		return updateEntity(controller, keygroupID, keygroup);
	}
	
	/**
	 * Removes a node from an tombstoned Keygroup
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param nodeID The node ID to be deleted from the Keygroup
	 * @param keygroupID The ID to the Keygroup to delete the node from
	 * @return Response object with Boolean containing the success or failure of operation
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	private Response<String> removeNodeFromTombstonedKeygroup(IControllable controller, NodeID nodeID, KeygroupID keygroupID) throws InterruptedException {
		// Get current data from keygroup
		String data = readEntity(controller, keygroupID).getValue();
		
		// Parse to object
		KeygroupConfig keygroup = JSONable.fromJSON(data.toString(), KeygroupConfig.class);
		
		// Find correct list
		if(keygroup.containsReplicaNode(nodeID)) {
			logger.debug("Attempting to remove replica node " + nodeID + " from keygroup " + keygroupID);
			// Check Node if node is the last replica node
			if(keygroup.getReplicaNodes().size() == 1) {
				logger.debug("Node " + nodeID + " is the last replica node in tombstoned keygroup " + keygroupID);
				// If no remaining trigger nodes, destroy group, otherwise, send error until trigger node list is empty
				if(keygroup.getTriggerNodes().size() == 0) {
					logger.debug("All trigger and replica nodes are gone, so permanently destroying keygroup " + keygroupID);
					return destroyKeygroup(controller, keygroupID);
				} else {
					logger.warn("Can't remove node " + nodeID + " from tombstoned keygroup " + keygroupID + " since trigger nodes still exist in the keygroup");
					return new Response<String>(null, ResponseCode.ERROR_ILLEGAL_COMMAND);
				}
			}
			
			logger.debug("Successfully removed replica node " + nodeID + " from keygroup " + keygroupID);
			keygroup.removeReplicaNode(nodeID);
		} else if (keygroup.containsTriggerNode(nodeID)) {
			logger.debug("Successfully removed trigger node " + nodeID + " from keygroup " + keygroupID);
			keygroup.removeTriggerNode(nodeID);
		} else {
			logger.warn("Keygroup " + keygroupID + " doesn't contain node " + nodeID);
			return new Response<String>(null, ResponseCode.ERROR_DOESNT_EXIST);
		}
		
		// Update the logical node
		return updateEntity(controller, keygroupID, keygroup);
	}
	
	/**
	 * Permanently removes Keygroup and all empty parents from system
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param keygroupID The ID to the Keygroup to delete the node from
	 * @return Response object with Boolean containing the success or failure of operation
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	private Response<String> destroyKeygroup(IControllable controller, KeygroupID keygroupID) throws InterruptedException {
		String data = readEntity(controller, keygroupID).getValue();
		KeygroupConfig keygroup = JSONable.fromJSON(data, KeygroupConfig.class);
		keygroup.setReplicaNodes(new HashSet<ReplicaNodeConfig>());
		data = JSONable.toJSON(keygroup);
		
		logger.debug("Permanently destroying keygroup " + keygroupID);
		// Remove Keygroup logical node
		controller.deleteNode(tombstonedPath(keygroupID));
		
		// Remove higher level nodes if necessary
		if(controller.getChildren(tombstonedPath(keygroupID.getTenantPath())).isEmpty()) {
			controller.deleteNode(tombstonedPath(keygroupID.getTenantPath()));
			
			if(controller.getChildren(tombstonedPath(keygroupID.getAppPath())).isEmpty()) {
				controller.deleteNode(tombstonedPath(keygroupID.getAppPath()));
			}
		}
		
		return new Response<String>(data, ResponseCode.SUCCESS);
	}
}
