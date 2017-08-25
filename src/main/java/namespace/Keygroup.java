package namespace;

import java.util.function.BiConsumer;

import org.apache.zookeeper.KeeperException;

import crypto.CryptoProvider.EncryptionAlgorithm;
import database.IControllable;
import model.JSONable;
import model.config.KeygroupConfig;
import model.config.KeygroupMember;
import model.config.ReplicaNodeConfig;
import model.config.TriggerNodeConfig;
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
	Response<Boolean> createKeygroup(IControllable controller, KeygroupConfig entity) {
		try {
			if(isActive(controller, entity.getKeygroupID().toString())) {
				return new Response<Boolean>(false, ResponseCode.ERROR_IS_ACTIVE);
			} else if (isTombstoned(controller, entity.getKeygroupID().toString())) {
				return new Response<Boolean>(false, ResponseCode.ERROR_TOMBSTONED);
			} else {
				// Parse key group to JSON and into byte[]
				String data = JSONable.toJSON(entity);
				
				if(data == null) {
					return new Response<Boolean>(false, ResponseCode.ERROR_INVALID_CONTENT);
				}
				
				// Build App Node if necessary
				if(!isActive(controller, entity.getKeygroupID().getAppPath())) {
					controller.addNode(activePath(entity.getKeygroupID().getAppPath()), "");
				}
				
				// Build Tenant Node if necessary
				if(!isActive(controller, entity.getKeygroupID().getTenantPath())) {
					controller.addNode(activePath(entity.getKeygroupID().getTenantPath()), "");
				}
				
				// Build Keygroup Node
				controller.addNode(activePath(entity.getKeygroupID().toString()), data);
				return new Response<Boolean>(true, ResponseCode.SUCCESS);
			} 
		} catch (InterruptedException e) {
			e.printStackTrace();
			return new Response<Boolean>(false, ResponseCode.ERROR_INTERNAL);
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
	Response<Boolean> addReplicaNodeToKeygroup(IControllable controller, ReplicaNodeConfig rnode, KeygroupID keygroupID) {
		return addNodeToKeygroup(controller, rnode, keygroupID, (keygroupConfig, node)->keygroupConfig.addReplicaNode((ReplicaNodeConfig) node));
	}
	
	/**
	 * Adds a trigger node to an existing Keygroup
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param tNode The TriggerNodeConfig to be added to the Keygroup
	 * @param keygroupID The ID for the config to add the replica node to
	 * @return Response object with Boolean containing the success or failure of operation
	 */
	Response<Boolean> addTriggerNodeToKeygroup(IControllable controller, TriggerNodeConfig tNode, KeygroupID keygroupID) {
		return addNodeToKeygroup(controller, tNode, keygroupID, (keygroupConfig, node)->keygroupConfig.addTriggerNode((TriggerNodeConfig) node));
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
	private Response<Boolean> addNodeToKeygroup(IControllable controller, KeygroupMember node, KeygroupID keygroupID, BiConsumer<KeygroupConfig, KeygroupMember> addToList ) {
		try {
			if(isActive(controller, keygroupID.toString())) {
				// Get current data from key group
				String data = controller.readNode(pathPrefixActive + keygroupID.toString());
				
				// Parse to object, add nodeID to list, parse back to byte[]
				KeygroupConfig keygroup = JSONable.fromJSON(data.toString(), KeygroupConfig.class);
				addToList.accept(keygroup, node);
				data = JSONable.toJSON(keygroup);
				
				// Update key group
				controller.updateNode(activePath(keygroupID.toString()), data);
				return new Response<Boolean>(true, ResponseCode.SUCCESS);
			} else if (isTombstoned(controller, keygroupID.toString())) {
				return new Response<Boolean>(false, ResponseCode.ERROR_TOMBSTONED);
			} else {
				return new Response<Boolean>(false, ResponseCode.ERROR_DOESNT_EXIST);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			return new Response<Boolean>(false, ResponseCode.ERROR_INTERNAL);
		}
	}
	
	/**
	 * Removes a node from an existing Keygroup
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param nodeID The node ID to be deleted from the Keygroup
	 * @param keygroupID The ID to the Keygroup to delete the node from
	 * @return Response object with Boolean containing the success or failure of operation
	 */
	Response<Boolean> removeNodeFromKeygroup(IControllable controller, NodeID nodeID, KeygroupID keygroupID) {
		try {
			if(isActive(controller, keygroupID.toString())) {
				return removeNodeFromActiveKeygroup(controller, nodeID, keygroupID);
			} else if (isTombstoned(controller, keygroupID.toString())) {
				return removeNodeFromTombstonedKeygroup(controller, nodeID, keygroupID);
			} else {
				return new Response<Boolean>(false, ResponseCode.ERROR_DOESNT_EXIST);
			}
		} catch (KeeperException e) {
			e.printStackTrace();
			return new Response<Boolean>(false, ResponseCode.ERROR_OTHER);
		} catch (InterruptedException e) {
			e.printStackTrace();
			return new Response<Boolean>(false, ResponseCode.ERROR_INTERNAL);
		}
	}
	
	/**
	 * Responds with all information about the Keygroup.
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param keygroupID The ID to the Keygroup to get information from
	 * @return Response object with String containing the Keygroup information
	 */
	Response<String> getKeygroupInfo(IControllable controller, KeygroupID keygroupID) {
		try {
			String data = null;
			if(isActive(controller, keygroupID.toString())) {
				data = controller.readNode(activePath(keygroupID.toString())).toString();
			} else if (isTombstoned(controller, keygroupID.toString())) {
				data = controller.readNode(tombstonedPath(keygroupID.toString())).toString();
			} else {
				return new Response<String>(null, ResponseCode.ERROR_DOESNT_EXIST);
			}
			
			return new Response<String>(data, ResponseCode.SUCCESS);
		} catch (InterruptedException e) {
			e.printStackTrace();
			return new Response<String>(null, ResponseCode.ERROR_INTERNAL);
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
	Response<String> getKeygroupInfoAuthorized(IControllable controller, KeygroupID keygroupID) {
		try {
			String data = null;
			if(isActive(controller, keygroupID.toString())) {
				data = controller.readNode(activePath(keygroupID.toString())).toString();
			} else if (isTombstoned(controller, keygroupID.toString())) {
				data = controller.readNode(tombstonedPath(keygroupID.toString())).toString();
			} else {
				return new Response<String>(null, ResponseCode.ERROR_DOESNT_EXIST);
			}
			
			return new Response<String>(data, ResponseCode.SUCCESS);
		} catch (InterruptedException e) {
			e.printStackTrace();
			return new Response<String>(null, ResponseCode.ERROR_INTERNAL);
		}
	}
	
	/**
	 * Responds with all information about the Keygroup. This does not includes the
	 * encryption key and algorithm, so can be used by any node in the system.
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param keygroupID The ID to the Keygroup to get information from
	 * @return Response object with String containing the Keygroup information
	 */
	Response<String> getKeygroupInfoUnauthorized(IControllable controller, KeygroupID keygroupID) {
		try {
			String data = null;
			if(isActive(controller, keygroupID.toString())) {
				data = controller.readNode(activePath(keygroupID.toString()));
			} else if (isTombstoned(controller, keygroupID.toString())) {
				data = controller.readNode(tombstonedPath(keygroupID.toString()));
			} else {
				return new Response<String>(null, ResponseCode.ERROR_DOESNT_EXIST);
			}
			
			// Remove encryption algorithm and secret
			KeygroupConfig keygroup = JSONable.fromJSON(data.toString(), KeygroupConfig.class);
			keygroup.setEncryptionAlgorithm(null);
			keygroup.setEncryptionSecret(null);
			data = JSONable.toJSON(keygroup);
			
			return new Response<String>(data.toString(), ResponseCode.SUCCESS);
		} catch (InterruptedException e) {
			e.printStackTrace();
			return new Response<String>(null, ResponseCode.ERROR_INTERNAL);
		}
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
	Response<Boolean> updateKeygroupCrypto(IControllable controller, KeygroupID keygroupID, String encryptionSecret, EncryptionAlgorithm encryptionAlgorithm) {
		try {
			if(isActive(controller, keygroupID.toString())) {
				String data = controller.readNode(activePath(keygroupID.toString()));
				
				KeygroupConfig keygroup = JSONable.fromJSON(data.toString(), KeygroupConfig.class);
				keygroup.setEncryptionSecret(encryptionSecret);
				keygroup.setEncryptionAlgorithm(encryptionAlgorithm);
				data = JSONable.toJSON(keygroup);
				
				controller.updateNode(activePath(keygroupID.toString()), data);
				return new Response<Boolean>(true, ResponseCode.SUCCESS);
			} else if (isTombstoned(controller, keygroupID.toString())) {
				return new Response<Boolean>(false, ResponseCode.ERROR_TOMBSTONED);
			} else {
				return new Response<Boolean>(false, ResponseCode.ERROR_DOESNT_EXIST);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			return new Response<Boolean>(false, ResponseCode.ERROR_INTERNAL);
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
		return removeEntity(controller, keygroupID);
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
	private Response<Boolean> removeNodeFromActiveKeygroup(IControllable controller, NodeID nodeID, KeygroupID keygroupID) throws KeeperException, InterruptedException {
		String data = controller.readNode(activePath(keygroupID.toString()));
		
		// Parse to object
		KeygroupConfig keygroup = JSONable.fromJSON(data.toString(), KeygroupConfig.class);
		
		// Find correct list
		if(keygroup.containsReplicaNode(nodeID)) {
			// Check Node is not the last replica node
			if(keygroup.getReplicaNodes().size() == 1) {
				return new Response<Boolean>(false, ResponseCode.ERROR_ILLEGAL_COMMAND);
			}
			
			keygroup.removeReplicaNode(nodeID);
		} else if (keygroup.containsTriggerNode(nodeID)) {
			keygroup.removeTriggerNode(nodeID);
		} else {
			return new Response<Boolean>(false, ResponseCode.ERROR_DOESNT_EXIST);
		}
		
		data = JSONable.toJSON(keygroup);
		
		// Update the ZkNode
		controller.updateNode(activePath(keygroupID.toString()), data);
		return new Response<Boolean>(true, ResponseCode.SUCCESS);
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
	private Response<Boolean> removeNodeFromTombstonedKeygroup(IControllable controller, NodeID nodeID, KeygroupID keygroupID) throws KeeperException, InterruptedException {
		String data = controller.readNode(tombstonedPath(keygroupID.toString()));
		
		// Parse to object
		KeygroupConfig keygroup = JSONable.fromJSON(data.toString(), KeygroupConfig.class);
		
		// Find correct list
		if(keygroup.containsReplicaNode(nodeID)) {
			// Check Node if node is the last replica node
			if(keygroup.getReplicaNodes().size() == 1) {
				// If no remaining trigger nodes, destroy group, otherwise, send error until trigger node list is empty
				if(keygroup.getTriggerNodes().size() == 0) {
					return destroyKeygroup(controller, keygroupID);
				} else {
					return new Response<Boolean>(false, ResponseCode.ERROR_ILLEGAL_COMMAND);
				}
			}
			
			keygroup.removeReplicaNode(nodeID);
		} else if (keygroup.containsTriggerNode(nodeID)) {
			keygroup.removeTriggerNode(nodeID);
		} else {
			return new Response<Boolean>(false, ResponseCode.ERROR_DOESNT_EXIST);
		}
		
		data = JSONable.toJSON(keygroup);
		
		// Update the ZkNode
		controller.updateNode(tombstonedPath(keygroupID.toString()), data);
		return new Response<Boolean>(true, ResponseCode.SUCCESS);
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
	private Response<Boolean> destroyKeygroup(IControllable controller, KeygroupID keygroupID) throws KeeperException, InterruptedException {
		// Remove Keygroup ZkNode
		controller.deleteNode(tombstonedPath(keygroupID.toString()));
		
		// Remove higher level nodes if necessary
		if(controller.getChildren(tombstonedPath(keygroupID.getTenantPath())).isEmpty()) {
			controller.deleteNode(tombstonedPath(keygroupID.getTenantPath()));
			
			if(controller.getChildren(tombstonedPath(keygroupID.getAppPath())).isEmpty()) {
				controller.deleteNode(tombstonedPath(keygroupID.getAppPath()));
			}
		}
		
		return new Response<Boolean>(true, ResponseCode.SUCCESS);
	}
}
