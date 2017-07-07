package namespace;

import java.util.function.BiConsumer;

import org.apache.zookeeper.KeeperException;

import ZkSystem.ZkController;
import crypto.CryptoProvider.EncryptionAlgorithm;
import model.config.KeygroupConfig;
import model.config.KeygroupMember;
import model.config.ReplicaNodeConfig;
import model.config.TriggerNodeConfig;
import model.data.KeygroupID;

/**
 * The KeyGroup class performs all operations on the KeyGroups section of
 * the system. This is primarily designed to keep track of information
 * about how nodes are logically structured together in the system
 * to form a fog network.
 * 
 * @author Wm. Keith van der Meulen
 */
abstract class KeyGroup extends SystemEntity {
	
	/**
	 * Name of the entity type
	 */
	static final String type = "keygroup";
	
	/**
	 * Creates KeyGroup within the FBase system
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param entity KeygroupConfig object to add to system
	 * @return Response object with Boolean containing the success or failure of operation
	 */
	static Response<Boolean> createKeyGroup(ZkController controller, KeygroupConfig entity) {
		try {
			if(isActive(controller, entity.getKeygroupID().toString())) {
				return new Response<Boolean>(false, ResponseCode.ERROR_IS_ACTIVE);
			} else if (isTombstoned(controller, entity.getKeygroupID().toString())) {
				return new Response<Boolean>(false, ResponseCode.ERROR_TOMBSTONED);
			} else {
				// Parse key group to JSON and into byte[]
				byte[] data = entity.toJSON().getBytes();
				
				if(data == null) {
					return new Response<Boolean>(false, ResponseCode.ERROR_INVALID_CONTENT);
				}
				
				// Build App Node if necessary
				if(!isActive(controller, entity.getKeygroupID().getAppPath())) {
					controller.addNode(activePath(entity.getKeygroupID().getAppPath()), "".getBytes());
				}
				
				// Build Originator Node if necessary
				if(!isActive(controller, entity.getKeygroupID().getOriginatorPath())) {
					controller.addNode(activePath(entity.getKeygroupID().getOriginatorPath()), "".getBytes());
				}
				
				// Build KeyGroup Node
				controller.addNode(activePath(entity.getKeygroupID().toString()), data);
				return new Response<Boolean>(true, ResponseCode.SUCCESS);
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
	 * Adds a replica node to an existing KeyGroup
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param rnode The ReplicaNodeConfig to be added to the KeyGroup
	 * @param keygroupID The ID for the config to add the replica node to
	 * @return Response object with Boolean containing the success or failure of operation
	 */
	static Response<Boolean> addReplicaNodeToKeyGroup(ZkController controller, ReplicaNodeConfig rnode, KeygroupID keygroupID) {
		return addNodeToKeyGroup(controller, rnode, keygroupID, (keygroupConfig, node)->keygroupConfig.addReplicaNode((ReplicaNodeConfig) node));
	}
	
	/**
	 * Adds a trigger node to an existing KeyGroup
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param tNode The TriggerNodeConfig to be added to the KeyGroup
	 * @param keygroupID The ID for the config to add the replica node to
	 * @return Response object with Boolean containing the success or failure of operation
	 */
	static Response<Boolean> addTriggerNodeToKeyGroup(ZkController controller, TriggerNodeConfig tNode, KeygroupID keygroupID) {
		return addNodeToKeyGroup(controller, tNode, keygroupID, (keygroupConfig, node)->keygroupConfig.addTriggerNode((TriggerNodeConfig) node));
	}
	
	/**
	 * Adds a node to an existing KeyGroup
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param node The node to be added to the KeyGroup
	 * @param keygroupID The ID for the config to add the replica node to
	 * @param addToList Expression to add node to proper list
	 * @return Response object with Boolean containing the success or failure of operation
	 */
	private static Response<Boolean> addNodeToKeyGroup(ZkController controller, KeygroupMember node, KeygroupID keygroupID, BiConsumer<KeygroupConfig, KeygroupMember> addToList ) {
		try {
			if(isActive(controller, keygroupID.toString())) {
				// Get current data from key group
				byte[] data = controller.readNode(keygroupID.toString());
				
				// Parse to object, add nodeID to list, parse back to byte[]
				KeygroupConfig keygroup = KeygroupConfig.fromJSON(data.toString(), KeygroupConfig.class);
				addToList.accept(keygroup, node);
				data = keygroup.toJSON().getBytes();
				
				// Update key group
				controller.updateNode(activePath(keygroupID.toString()), data);
				return new Response<Boolean>(true, ResponseCode.SUCCESS);
			} else if (isTombstoned(controller, keygroupID.toString())) {
				return new Response<Boolean>(false, ResponseCode.ERROR_TOMBSTONED);
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
	 * Removes a node from an existing KeyGroup
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param nodeID The node ID to be deleted from the KeyGroup
	 * @param keygroupID The ID to the KeyGroup to delete the node from
	 * @return Response object with Boolean containing the success or failure of operation
	 */
	static Response<Boolean> removeNodeFromKeyGroup(ZkController controller, String nodeID, KeygroupID keygroupID) {
		try {
			if(isActive(controller, keygroupID.toString())) {
				return removeNodeFromActiveKeyGroup(controller, nodeID, keygroupID);
			} else if (isTombstoned(controller, keygroupID.toString())) {
				return removeNodeFromTombstonedKeyGroup(controller, nodeID, keygroupID);
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
	 * Responds with all information about the KeyGroup. This includes the
	 * encryption key and algorithm, so can only be called by nodes in the KeyGroup.
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param keygroupID The ID to the KeyGroup to get information from
	 * @return Response object with String containing the KeyGroup information
	 */
	static Response<String> getKeyGroupInfoAuthorized(ZkController controller, KeygroupID keygroupID) {
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
		} catch (KeeperException e) {
			e.printStackTrace();
			return new Response<String>(null, ResponseCode.ERROR_OTHER);
		} catch (InterruptedException e) {
			e.printStackTrace();
			return new Response<String>(null, ResponseCode.ERROR_INTERNAL);
		}
	}
	
	/**
	 * Responds with all information about the KeyGroup. This does not includes the
	 * encryption key and algorithm, so can be used by any node in the system.
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param keygroupID The ID to the KeyGroup to get information from
	 * @return Response object with String containing the KeyGroup information
	 */
	static Response<String> getKeyGroupInfoUnauthorized(ZkController controller, KeygroupID keygroupID) {
		try {
			byte[] data = null;
			if(isActive(controller, keygroupID.toString())) {
				data = controller.readNode(activePath(keygroupID.toString()));
			} else if (isTombstoned(controller, keygroupID.toString())) {
				data = controller.readNode(tombstonedPath(keygroupID.toString()));
			} else {
				return new Response<String>(null, ResponseCode.ERROR_DOESNT_EXIST);
			}
			
			// Remove encryption algorithm and secret
			KeygroupConfig keygroup = KeygroupConfig.fromJSON(data.toString(), KeygroupConfig.class);
			keygroup.setEncryptionAlgorithm(null);
			keygroup.setEncryptionSecret(null);
			data = keygroup.toJSON().getBytes();
			
			return new Response<String>(data.toString(), ResponseCode.SUCCESS);
		} catch (KeeperException e) {
			e.printStackTrace();
			return new Response<String>(null, ResponseCode.ERROR_OTHER);
		} catch (InterruptedException e) {
			e.printStackTrace();
			return new Response<String>(null, ResponseCode.ERROR_INTERNAL);
		}
	}
	
	/**
	 * Updates the encryption key and algorithm to be used for communication
	 * within the KeyGroup.
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param keygroupID The ID to the KeyGroup to get update encryption information
	 * @param encryptionSecret Encryption key for communication within the KeyGroup
	 * @param encryptionAlgorithm Encryption algorithm (symmetric) used for communication within the KeyGroup
	 * @return Response object with Boolean containing the success or failure of operation
	 */
	static Response<Boolean> updateKeyGroupCrypto(ZkController controller, KeygroupID keygroupID, String encryptionSecret, EncryptionAlgorithm encryptionAlgorithm) {
		try {
			if(isActive(controller, keygroupID.toString())) {
				byte[] data = controller.readNode(activePath(keygroupID.toString()));
				
				KeygroupConfig keygroup = KeygroupConfig.fromJSON(data.toString(), KeygroupConfig.class);
				keygroup.setEncryptionSecret(encryptionSecret);
				keygroup.setEncryptionAlgorithm(encryptionAlgorithm);
				data = keygroup.toJSON().getBytes();
				
				controller.updateNode(activePath(keygroupID.toString()), data);
				return new Response<Boolean>(true, ResponseCode.SUCCESS);
			} else if (isTombstoned(controller, keygroupID.toString())) {
				return new Response<Boolean>(false, ResponseCode.ERROR_TOMBSTONED);
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
	 * Irreversibly tombstones KeyGroup for eventual deletion upon all nodes in the KeyGroup
	 * successfully removing themselves.
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param keygroupID The ID to the KeyGroup to remove
	 * @return Response object with Boolean containing the success or failure of operation
	 */
	static Response<Boolean> removeKeyGroup(ZkController controller, KeygroupID keygroupID) {
		return removeEntity(controller, keygroupID.toString());
	}
	
	/**
	 * Removes a node from an active KeyGroup
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param nodeID The node ID to be deleted from the KeyGroup
	 * @param keygroupID The ID to the KeyGroup to delete the node from
	 * @return Response object with Boolean containing the success or failure of operation
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	private static Response<Boolean> removeNodeFromActiveKeyGroup(ZkController controller, String nodeID, KeygroupID keygroupID) throws KeeperException, InterruptedException {
		byte[] data = controller.readNode(activePath(keygroupID.toString()));
		
		// Parse to object
		KeygroupConfig keygroup = KeygroupConfig.fromJSON(data.toString(), KeygroupConfig.class);
		
		// Find correct list
		if(keygroup.containsReplicaNode(nodeID)) {
			// Check Node is not the last replica node
			if(keygroup.getReplicaNodes().size() == 1) {
				return new Response<Boolean>(false, ResponseCode.ERRROR_ILLEGAL_COMMAND);
			}
			
			keygroup.removeReplicaNode(nodeID);
		} else if (keygroup.containsTriggerNode(nodeID)) {
			keygroup.removeTriggerNode(nodeID);
		} else {
			return new Response<Boolean>(false, ResponseCode.ERROR_DOESNT_EXIST);
		}
		
		data = keygroup.toJSON().getBytes();
		
		// Update the ZkNode
		controller.updateNode(activePath(keygroupID.toString()), data);
		return new Response<Boolean>(true, ResponseCode.SUCCESS);
	}
	
	/**
	 * Removes a node from an tombstoned KeyGroup
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param nodeID The node ID to be deleted from the KeyGroup
	 * @param keygroupID The ID to the KeyGroup to delete the node from
	 * @return Response object with Boolean containing the success or failure of operation
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	private static Response<Boolean> removeNodeFromTombstonedKeyGroup(ZkController controller, String nodeID, KeygroupID keygroupID) throws KeeperException, InterruptedException {
		byte[] data = controller.readNode(tombstonedPath(keygroupID.toString()));
		
		// Parse to object
		KeygroupConfig keygroup = KeygroupConfig.fromJSON(data.toString(), KeygroupConfig.class);
		
		// Find correct list
		if(keygroup.containsReplicaNode(nodeID)) {
			// Check Node if node is the last replica node
			if(keygroup.getReplicaNodes().size() == 1) {
				// If no remaining trigger nodes, destroy group, otherwise, send error until trigger node list is empty
				if(keygroup.getTriggerNodes().size() == 0) {
					return destroyKeyGroup(controller, keygroupID);
				} else {
					return new Response<Boolean>(false, ResponseCode.ERRROR_ILLEGAL_COMMAND);
				}
			}
			
			keygroup.removeReplicaNode(nodeID);
		} else if (keygroup.containsTriggerNode(nodeID)) {
			keygroup.removeTriggerNode(nodeID);
		} else {
			return new Response<Boolean>(false, ResponseCode.ERROR_DOESNT_EXIST);
		}
		
		data = keygroup.toJSON().getBytes();
		
		// Update the ZkNode
		controller.updateNode(tombstonedPath(keygroupID.toString()), data);
		return new Response<Boolean>(true, ResponseCode.SUCCESS);
	}
	
	/**
	 * Permanently removes KeyGroup and all empty parents from system
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param keygroupID The ID to the KeyGroup to delete the node from
	 * @return Response object with Boolean containing the success or failure of operation
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	private static Response<Boolean> destroyKeyGroup(ZkController controller, KeygroupID keygroupID) throws KeeperException, InterruptedException {
		// Remove KeyGroup ZkNode
		controller.deleteNode(tombstonedPath(keygroupID.toString()));
		
		// Remove higher level nodes if necessary
		if(controller.getChildren(tombstonedPath(keygroupID.getOriginatorPath())).isEmpty()) {
			controller.deleteNode(tombstonedPath(keygroupID.getOriginatorPath()));
			
			if(controller.getChildren(tombstonedPath(keygroupID.getAppPath())).isEmpty()) {
				controller.deleteNode(tombstonedPath(keygroupID.getAppPath()));
			}
		}
		
		return new Response<Boolean>(true, ResponseCode.SUCCESS);
	}
}
