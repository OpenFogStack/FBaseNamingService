package namespace;

import java.io.IOException;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.zookeeper.KeeperException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The KeyGroup class performs all operations on the KeyGroups section of
 * the system. This is primarily designed to keep track of information
 * about how nodes are logically structured together in the system
 * to form a fog network.
 * 
 * @author Wm. Keith van der Meulen
 */
class KeyGroup extends SystemEntity {
	
	/**
	 * Name of the entity type
	 */
	static final String type = "keygroup";
	
	/**
	 * The KeyGroup path consisting of &lt;App&gt;/&lt;User&gt;/&lt;Group&gt;
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
	 * @param path The KeyGroup path consisting of &lt;App&gt;/&lt;User&gt;/&lt;Group&gt;
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
	 * @param path The KeyGroup path consisting of &lt;App&gt;/&lt;User&gt;/&lt;Group&gt; with only alphanumeric characters, plus _-()&.
	 * @param replicaNodes List of fog nodes that receive and store data
	 * @param triggerNodes List of fog nodes that process data streams from replicaNodes
	 * @param encryptionKey Encryption key for communication within the KeyGroup
	 * @param encryptionAlgorithm Encryption algorithm (symmetric) used for communication within the KeyGroup
	 * @return Response object with Boolean containing the success or failure of operation
	 */
	static Response<Boolean> createKeyGroup(ZkController controller, String nodeID, String path, List<String> replicaNodes, List<String> triggerNodes, String encryptionKey, String encryptionAlgorithm) {
		try {
			if(isActive(controller, path)) {
				return new Response<Boolean>(false, ResponseCode.ERROR_IS_ACTIVE);
			} else if (isTombstoned(controller, path)) {
				return new Response<Boolean>(false, ResponseCode.ERROR_TOMBSTONED);
			} else {
				// Create key group to register
				KeyGroup keygroup = new KeyGroup(path, replicaNodes, triggerNodes, encryptionKey, encryptionAlgorithm);
				
				Pattern pattern = Pattern.compile("([/]([A-Za-z0-9][A-Za-z0-9|_|-|(|)|&|/|.]*)*)");
				Matcher matcher = pattern.matcher(path);
				
				// Ensure path uses only accepted characters
				if(!matcher.matches()) {
					return new Response<Boolean>(false, ResponseCode.ERROR_INVALID_CONTENT);
				}
				
				// Ensure calling node is one of the replica nodes
				if(!keygroup.replicaNodes.contains(nodeID)) {
					return new Response<Boolean>(false, ResponseCode.ERROR_INVALID_CONTENT);
				}
				
				// Ensure calling node is not one of the trigger nodes
				if(keygroup.triggerNodes.contains(nodeID)) {
					return new Response<Boolean>(false, ResponseCode.ERROR_INVALID_CONTENT);
				}
				
				String[] pathNodes = pathToNodes(path);
				
				// Parse key group to JSON and into byte[]
				byte[] data;
				try {
					data = toJson(keygroup);
				} catch (JsonProcessingException e) {
					e.printStackTrace();
					return new Response<Boolean>(false, ResponseCode.ERROR_INVALID_CONTENT);
				}
				
				// Build App Node if necessary
				if(!isActive(controller, pathNodes[0])) {
					controller.addNode(activePath(pathNodes[0]), "".getBytes());
				}
				
				// Build User Node if necessary
				if(!isActive(controller, pathNodes[0] + "/" + pathNodes[1])) {
					controller.addNode(activePath(pathNodes[0] + "/" + pathNodes[1]), "".getBytes());
				}
				
				// Build KeyGroup Node
				controller.addNode(activePath(path), data);
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
	 * @param nodeID The replica node ID to be added to the KeyGroup
	 * @param path The path to the KeyGroup to add a replica node to
	 * @return Response object with Boolean containing the success or failure of operation
	 */
	static Response<Boolean> addReplicaNodeToKeyGroup(ZkController controller, String nodeID, String path) {
		return addNodeToKeyGroup(controller, nodeID, path, (keygroup, id)->keygroup.replicaNodes.add(id));
	}
	
	/**
	 * Adds a trigger node to an existing KeyGroup
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param nodeID The trigger node ID to be added to the KeyGroup
	 * @param path The path to the KeyGroup to add a trigger node to
	 * @return Response object with Boolean containing the success or failure of operation
	 */
	static Response<Boolean> addTriggerNodeToKeyGroup(ZkController controller, String nodeID, String path) {
		return addNodeToKeyGroup(controller, nodeID, path, (keygroup, id)->keygroup.triggerNodes.add(id));
	}
	
	/**
	 * Adds a node to an existing KeyGroup
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param nodeID The trigger node ID to be added to the KeyGroup
	 * @param path The path to the KeyGroup to add a trigger node to
	 * @param addToList Expression to add nodeID to proper list
	 * @return Response object with Boolean containing the success or failure of operation
	 */
	private static Response<Boolean> addNodeToKeyGroup(ZkController controller, String nodeID, String path, BiConsumer<KeyGroup, String> addToList ) {
		try {
			if(isActive(controller, path)) {
				// Get current data from key group
				byte[] data = controller.readNode(path);
				
				// Parse to object, add nodeID to list, parse back to byte[]
				KeyGroup keygroup = null;
				try {
					keygroup = jsonToKeyGroup(data);
					addToList.accept(keygroup, nodeID);
					data = toJson(keygroup);
				} catch (JsonProcessingException e) {
					e.printStackTrace();
					return new Response<Boolean>(false, ResponseCode.ERROR_INVALID_CONTENT);
				} catch (IOException e) {
					e.printStackTrace();
					return new Response<Boolean>(false, ResponseCode.ERROR_INTERNAL);
				}
				
				// Update key group
				controller.updateNode(activePath(path), data);
				return new Response<Boolean>(true, ResponseCode.SUCCESS);
			} else if (isTombstoned(controller, path)) {
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
	 * @param path The path to the KeyGroup to delete the node from
	 * @return Response object with Boolean containing the success or failure of operation
	 */
	static Response<Boolean> removeNodeFromKeyGroup(ZkController controller, String nodeID, String path) {
		try {
			if(isActive(controller, path)) {
				return removeNodeFromActiveKeyGroup(controller, nodeID, path);
			} else if (isTombstoned(controller, path)) {
				return removeNodeFromTombstonedKeyGroup(controller, nodeID, path);
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
	 * @param path The path to the KeyGroup to get information from
	 * @return Response object with String containing the KeyGroup information
	 */
	static Response<String> getKeyGroupInfoAuthorized(ZkController controller, String path) {
		try {
			String data = null;
			if(isActive(controller, path)) {
				data = controller.readNode(activePath(path)).toString();
			} else if (isTombstoned(controller, path)) {
				data = controller.readNode(tombstonedPath(path)).toString();
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
	 * @param path The path to the KeyGroup to get information from
	 * @return Response object with String containing the KeyGroup information
	 */
	static Response<String> getKeyGroupInfoUnauthorized(ZkController controller, String path) {
		try {
			byte[] data = null;
			if(isActive(controller, path)) {
				data = controller.readNode(activePath(path));
			} else if (isTombstoned(controller, path)) {
				data = controller.readNode(tombstonedPath(path));
			} else {
				return new Response<String>(null, ResponseCode.ERROR_DOESNT_EXIST);
			}
			
			try {
				KeyGroup keygroup = jsonToKeyGroup(data);
				
				keygroup.encryptionKey = "";
				keygroup.encryptionAlgorithm = "";
				
				data = toJson(keygroup);
			} catch (JsonProcessingException e) {
				e.printStackTrace();
				return new Response<String>(null, ResponseCode.ERROR_INVALID_CONTENT);
			} catch (IOException e) {
				e.printStackTrace();
				return new Response<String>(null, ResponseCode.ERROR_INTERNAL);
			}
			
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
	 * @param path The path to the KeyGroup to get update encryption information
	 * @param encryptionKey Encryption key for communication within the KeyGroup
	 * @param encryptionAlgorithm Encryption algorithm (symmetric) used for communication within the KeyGroup
	 * @return Response object with Boolean containing the success or failure of operation
	 */
	static Response<Boolean> updateKeyGroupCrypto(ZkController controller, String path, String encryptionKey, String encryptionAlgorithm) {
		try {
			if(isActive(controller, path)) {
				byte[] data = controller.readNode(activePath(path));
				
				try {
					KeyGroup entity = jsonToKeyGroup(data);
					
					entity.encryptionKey = encryptionKey;
					entity.encryptionAlgorithm = encryptionAlgorithm;
					
					data = toJson(entity);
				} catch (JsonProcessingException e) {
					e.printStackTrace();
					return new Response<Boolean>(false, ResponseCode.ERROR_INVALID_CONTENT);
				} catch (IOException e) {
					e.printStackTrace();
					return new Response<Boolean>(false, ResponseCode.ERROR_INTERNAL);
				}
				
				controller.updateNode(activePath(path), data);
				return new Response<Boolean>(true, ResponseCode.SUCCESS);
			} else if (isTombstoned(controller, path)) {
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
	 * @param path The path to the KeyGroup to remove
	 * @return Response object with Boolean containing the success or failure of operation
	 */
	static Response<Boolean> removeKeyGroup(ZkController controller, String path) {
		return removeEntity(controller, path);
	}
	
	/**
	 * Removes a node from an active KeyGroup
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param nodeID The node ID to be deleted from the KeyGroup
	 * @param path The path to the KeyGroup to delete the node from
	 * @return Response object with Boolean containing the success or failure of operation
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	private static Response<Boolean> removeNodeFromActiveKeyGroup(ZkController controller, String nodeID, String path) throws KeeperException, InterruptedException {
		byte[] data = controller.readNode(activePath(path));
		
		// Parse to object
		KeyGroup keygroup = null;
		try {
			keygroup = jsonToKeyGroup(data);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			return new Response<Boolean>(false, ResponseCode.ERROR_INVALID_CONTENT);
		} catch (IOException e) {
			e.printStackTrace();
			return new Response<Boolean>(false, ResponseCode.ERROR_INTERNAL);
		}
		
		// Find correct list
		if(keygroup.replicaNodes.contains(nodeID)) {
			// Check Node is not the last replica node
			if(keygroup.replicaNodes.size() == 1) {
				return new Response<Boolean>(false, ResponseCode.ERRROR_ILLEGAL_COMMAND);
			}
			
			keygroup.replicaNodes.remove(nodeID);
		} else if (keygroup.triggerNodes.contains(nodeID)) {
			keygroup.triggerNodes.remove(nodeID);
		} else {
			return new Response<Boolean>(false, ResponseCode.ERROR_DOESNT_EXIST);
		}
		
		try {
			data = toJson(keygroup);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			return new Response<Boolean>(false, ResponseCode.ERROR_INVALID_CONTENT);
		}
		
		// Update the ZkNode
		controller.updateNode(activePath(path), data);
		return new Response<Boolean>(true, ResponseCode.SUCCESS);
	}
	
	/**
	 * Removes a node from an tombstoned KeyGroup
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param nodeID The node ID to be deleted from the KeyGroup
	 * @param path The path to the KeyGroup to delete the node from
	 * @return Response object with Boolean containing the success or failure of operation
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	private static Response<Boolean> removeNodeFromTombstonedKeyGroup(ZkController controller, String nodeID, String path) throws KeeperException, InterruptedException {
		byte[] data = controller.readNode(tombstonedPath(path));
		
		// Parse to object
		KeyGroup keygroup = null;
		try {
			keygroup = jsonToKeyGroup(data);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			return new Response<Boolean>(false, ResponseCode.ERROR_INVALID_CONTENT);
		} catch (IOException e) {
			e.printStackTrace();
			return new Response<Boolean>(false, ResponseCode.ERROR_INTERNAL);
		}
		
		// Find correct list
		if(keygroup.replicaNodes.contains(nodeID)) {
			// Check Node if node is the last replica node
			if(keygroup.replicaNodes.size() == 1) {
				return destroyKeyGroup(controller, path);
			}
			
			keygroup.replicaNodes.remove(nodeID);
		} else if (keygroup.triggerNodes.contains(nodeID)) {
			keygroup.triggerNodes.remove(nodeID);
		} else {
			return new Response<Boolean>(false, ResponseCode.ERROR_DOESNT_EXIST);
		}
		
		try {
			data = toJson(keygroup);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			return new Response<Boolean>(false, ResponseCode.ERROR_INVALID_CONTENT);
		}
		
		// Update the ZkNode
		controller.updateNode(tombstonedPath(path), data);
		return new Response<Boolean>(true, ResponseCode.SUCCESS);
	}
	
	/**
	 * Permanently removes KeyGroup and all empty parents from system
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param path The path to the KeyGroup to delete the node from
	 * @return Response object with Boolean containing the success or failure of operation
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	private static Response<Boolean> destroyKeyGroup(ZkController controller, String path) throws KeeperException, InterruptedException {
		String[] pathNodes = pathToNodes(path);
		
		// Remove KeyGroup ZkNode
		controller.deleteNode(tombstonedPath(path));
		
		// Remove higher level nodes if necessary
		if(controller.getChildren(tombstonedPath(pathNodes[0] + "/" + pathNodes[1])).isEmpty()) {
			controller.deleteNode(tombstonedPath(pathNodes[0] + "/" + pathNodes[1]));
			
			if(controller.getChildren(tombstonedPath(pathNodes[0])).isEmpty()) {
				controller.deleteNode(tombstonedPath(pathNodes[0]));
			}
		}
		
		return new Response<Boolean>(true, ResponseCode.SUCCESS);
	}
	
	/**
	 * Converts JSON byte[] to KeyGroup object
	 * 
	 * @param json Byte array to convert to KeyGroup
	 * @return KeyGroup object representing the input JSON object
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	private static KeyGroup jsonToKeyGroup(byte[] json) throws JsonProcessingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		
		KeyGroup keygroup = mapper.readValue(json, KeyGroup.class);
		
		return keygroup;
	}
	
	/**
	 * Splits KeyGroup path into three separate strings
	 * 
	 * @param path KeyGroup path
	 * @return String array with &lt;App&gt;/&lt;User&gt;/&lt;Group&gt split into indices 0, 1, and 2 respectively
	 */
	private static String[] pathToNodes(String path) {
		String[] pathNodes = path.split("/");
		
		if(pathNodes.length != 3) {
			throw new IllegalArgumentException("Invalid path structure");
		}
		
		for(String s : pathNodes) {
			if(s.length() < 1) {
				throw new IllegalArgumentException("Invalid path structure");
			}
		}
		
		return pathNodes;
	}
}
