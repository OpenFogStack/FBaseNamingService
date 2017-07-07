package namespace;

import org.apache.zookeeper.KeeperException;

import ZkSystem.ZkController;
import model.Entity;

/**
 * The SystemEntity class is the parent class for subsystems within the ZooKeeper
 * distributed system storing the namespace.
 * 
 * @author Wm. Keith van der Meulen
 */
abstract class SystemEntity {
	
	/**
	 * Name of the entity type
	 */
	protected static String type;
	
	/**
	 * System prefix for location of all active SystemEntities
	 */
	protected static String pathPrefixActive = "/" + type + "/active/";
	
	/**
	 * System prefix for location of all tombstoned SystemEntities
	 */
	protected static String pathPrefixTombstoned = "/" + type + "/tombstoned/";
	
	/**
	 * Length of random strings generated in getUnusedNodeID()
	 */
	private static final int randomIDLength = 32;
	
	/**
	 * Responds with a random string unused by any node at the time of the call
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @return Response object with String containing an unused node ID
	 */
	static Response<String> getUnusedID(ZkController controller) {
		return getUnusedID(controller, randomIDLength);
	}
	
	/**
	 * Responds with a random string unused by any node at the time of the call
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param length The length of the random string
	 * @return Response object with String containing an unused node I
	 */
	static Response<String> getUnusedID(ZkController controller, int length) {
		try {
			String nodeID = null;
			
			// Create new random strings until an unused one is found
			do {
				char[] random = new char[length];
				
				// Loop making random chars until string length
				for(int i = 0; i < random.length; i++) {
					// Get random number 0 - 35 (36 possibilities = 0 to 9 + a to z)
					int randomAlphaNum = (int) Math.floor(Math.random() * 36);
					
					// Use single digit or if double digit, convert to ASCII letter
					if(randomAlphaNum > 9) {
						randomAlphaNum += 87;
					}
					
					random[i] = (char) randomAlphaNum;
				}
				
				nodeID = random.toString();
			} while(exists(controller, nodeID));
			
			return new Response<String>(nodeID, ResponseCode.SUCCESS);
		} catch (KeeperException e) {
			e.printStackTrace();
			return new Response<String>(null, ResponseCode.ERROR_OTHER);
		} catch (InterruptedException e) {
			e.printStackTrace();
			return new Response<String>(null, ResponseCode.ERROR_INTERNAL);
		}
	}
	
	/**
	 * Registers an entity with the FBase system
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param entityID Requested ID of new entity
	 * @param entity The entity to add
	 * @return Response object with Boolean containing the success or failure of operation
	 */
	protected static Response<Boolean> registerEntity(ZkController controller, String entityID, Entity entity) {
		// Parse entity to JSON and into byte[]
		byte[] data = entity.toJSON().getBytes();
		
		if (data == null) {
			return new Response<Boolean>(false, ResponseCode.ERROR_INVALID_CONTENT);
		}
		
		// Add ZkNode to system
		try {
			// Check if ZkNode already exists
			if(exists(controller, entityID)) {
				return new Response<Boolean>(false, ResponseCode.ERROR_ALREADY_EXISTS);
			}
			
			controller.addNode(activePath(entityID), data);
			return new Response<Boolean>(true, ResponseCode.SUCCESS);
		} catch (KeeperException e) {
			e.printStackTrace();
			return new Response<Boolean>(false, ResponseCode.ERROR_OTHER);
		} catch (InterruptedException e) {
			e.printStackTrace();
			return new Response<Boolean>(false, ResponseCode.ERROR_INTERNAL);
		}
	}
	
	/**
	 * Responds with all information about the entity
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param entityID ID of entity to get information from
	 * @return Response object with String containing the Client information
	 */
	protected static Response<String> getEntityInfo(ZkController controller, String entityID) {
		try {
			String data = null;
			if(isActive(controller, entityID)) {
				data = controller.readNode(activePath(entityID)).toString();
			} else if (isTombstoned(controller, entityID)) {
				data = controller.readNode(tombstonedPath(entityID)).toString();
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
	 * Updates information kept on the entity with the matching ID
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param entityID ID of client to update
	 * @param entity The new entity information to be stored
	 * @return Response object with Boolean containing the success or failure of operation
	 */
	protected static Response<Boolean> updateEntityInfo(ZkController controller, String entityID, Entity entity) {
		try {
			if(isActive(controller, entityID)) {
				// Parse entity to JSON and into byte[]
				byte[] data = entity.toJSON().getBytes();
				
				if(data == null) {
					return new Response<Boolean>(false, ResponseCode.ERROR_INVALID_CONTENT);
				}
				
				// Add client to system
				controller.updateNode(activePath(entityID), data);
				return new Response<Boolean>(true, ResponseCode.SUCCESS);
			} else if (isTombstoned(controller, entityID)) {
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
	 * Permanently tombstones an entity in the system. Tombstoned entities wishing to
	 * enter the system again must register as a new entity with a new ID
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param entityID Entity to tombstone
	 * @return Response object with Boolean containing the success or failure of operation
	 */
	protected static Response<Boolean> removeEntity(ZkController controller, String entityID) {
		try {
			if (controller.exists(activePath(entityID))) {
				// Get data from client
				byte[] data = controller.readNode(activePath(entityID));
				
				// Copy client to tombstoned path
				controller.addNode(tombstonedPath(entityID), data);
				
				// Delete client from active path
				controller.deleteNode(activePath(entityID));
				
				return new Response<Boolean>(true, ResponseCode.SUCCESS);
			} else if (controller.exists(tombstonedPath(entityID))) {
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
	 * Creates proper system path for an active SystemEntity
	 * 
	 * @param suffix The variable part of the system path
	 * @return Proper system path to SystemEntity
	 */
	protected static String activePath(String suffix) {
		return pathPrefixActive + suffix;
	}
	
	/**
	 * Creates proper system path for an tombstoned SystemEntity
	 * 
	 * @param suffix The variable part of the system path
	 * @return Proper system path to SystemEntity
	 */
	protected static String tombstonedPath(String suffix) {
		return pathPrefixTombstoned + suffix;
	}
	
	/**
	 * Checks if entity exists in either active or tombstoned directories
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param suffix The local path for the variable
	 * @return Boolean true if path exists, false otherwise
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	protected static boolean exists(ZkController controller, String suffix) throws KeeperException, InterruptedException {
		return isActive(controller, suffix) || isTombstoned(controller, suffix);
	}
	
	/**
	 * Checks if entity exists in the active directory
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param suffix The local path for the variable
	 * @return Boolean true if path exists, false otherwise
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	protected static boolean isActive(ZkController controller, String suffix) throws KeeperException, InterruptedException {
		return controller.exists(pathPrefixActive + suffix);
	}
	
	/**
	 * Checks if entity exists in the tombstoned directory
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param suffix The local path for the variable
	 * @return Boolean true if path exists, false otherwise
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	protected static boolean isTombstoned(ZkController controller, String suffix) throws KeeperException, InterruptedException {
		return controller.exists(pathPrefixTombstoned + suffix);
	}
}
