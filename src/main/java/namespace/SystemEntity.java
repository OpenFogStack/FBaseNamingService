package namespace;

import java.io.IOException;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.apache.zookeeper.KeeperException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import database.IControllable;
import model.JSONable;
import model.config.Config;
import model.data.ConfigID;
import model.messages.Response;
import model.messages.ResponseCode;

/**
 * The SystemEntity class is the parent class for subsystems within the database
 * distributed system storing the namespace.
 * 
 * @author Wm. Keith van der Meulen
 */
public abstract class SystemEntity {
	
	private static Logger logger = Logger.getLogger(SystemEntity.class.getName());
	
	/**
	 * System prefix for location of all active SystemEntities
	 */
	protected final String pathPrefixActive;
	
	/**
	 * System prefix for location of all tombstoned SystemEntities
	 */
	protected final String pathPrefixTombstoned;
	
	/**
	 * Length of random strings generated in getUnusedNodeID()
	 */
	private static final int randomIDLength = 32;
	
	/**
	 * The type of the subclass of SystemEntity
	 */
	private final String type;
	
	/**
	 * Constructor for SystemEntity
	 * 
	 * @param type The name for the Entity type
	 */
	SystemEntity(String type) {
		this.type = type;
		pathPrefixActive = "/" + type + "/active/";
		pathPrefixTombstoned = "/" + type + "/tombstoned/";
	}

	/**
	 * Responds with a random string unused by any node at the time of the call
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @return Response object with String containing an unused node ID
	 */
	Response<String> getUnusedID(IControllable controller) {
		logger.debug("Creating random ID");
		return getUnusedID(controller, randomIDLength);
	}
	
	/**
	 * Responds with a random string unused by any node at the time of the call
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param length The length of the random string
	 * @return Response object with String containing an unused node I
	 */
	Response<String> getUnusedID(IControllable controller, int length) {
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
		} catch (InterruptedException e) {
			logger.error("Error creating random ID");
			e.printStackTrace();
			return new Response<String>(null, ResponseCode.ERROR_INTERNAL);
		}
	}
	
	/**
	 * Creates an entity in the FBase system
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param entityID Requested ID of new entity
	 * @param entity The entity to add
	 * @return Response object with Boolean containing the success or failure of operation
	 */
	protected Response<Boolean> createEntity(IControllable controller, ConfigID entityID, Config entity) {
		// Set version for new entity to 1
		entity.setVersion(1);
		
		// Parse entity to JSON
		String data = JSONable.toJSON(entity);
		
		if (data == null) {
			logger.error("Error parsing config to JSON");
			return new Response<Boolean>(false, ResponseCode.ERROR_INVALID_CONTENT);
		}
		
		// Add node to system
		try {
			// Check if node already exists
			if(exists(controller, entityID)) {
				logger.warn(entityID + "already exists");
				return new Response<Boolean>(false, ResponseCode.ERROR_ALREADY_EXISTS);
			}
			
			controller.addNode(activePath(entityID.toString()), data);
			return new Response<Boolean>(true, ResponseCode.SUCCESS);
		} catch (InterruptedException e) {
			logger.error("Error adding " + entityID);
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
	protected Response<String> readEntity(IControllable controller, ConfigID entityID) {
		try {
			String data = null;
			if(isActive(controller, entityID.toString())) {
				data = controller.readNode(activePath(entityID.toString())).toString();
				logger.debug("Reading " + entityID + " from active directory.");
			} else if (isTombstoned(controller, entityID.toString())) {
				data = controller.readNode(tombstonedPath(entityID.toString())).toString();
				logger.debug("Reading " + entityID + " from tombstoned directory.");
			} else {
				logger.debug(capitalize(type) + " " + entityID + " doesn't exist");
				return new Response<String>(null, ResponseCode.ERROR_DOESNT_EXIST);
			}

			return new Response<String>(data, ResponseCode.SUCCESS);
		} catch (InterruptedException e) {
			logger.error("Error reading " + entityID);
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
	protected Response<Boolean> updateEntity(IControllable controller, ConfigID entityID, Config entity) {
		// Set proper version number
		try {
			String json = readEntity(controller, entityID).getValue();
			HashMap<String,Object> o = new ObjectMapper().readValue(json, new TypeReference<HashMap<String,Object>>() {});
			int version = Integer.parseInt((o.get("version").toString()));
			
			// Increment version for entity
			entity.setVersion(version + 1);
		} catch (NumberFormatException | IOException e) {
			logger.error("Error parsing version from system");
			e.printStackTrace();
			return new Response<Boolean>(false, ResponseCode.ERROR_INTERNAL);
		}
		
		try {
			if(isActive(controller, entityID.toString())) {
				// Parse entity to JSON
				String data = JSONable.toJSON(entity);
				
				if(data == null) {
					logger.error("Error parsing config to JSON");
					return new Response<Boolean>(false, ResponseCode.ERROR_INVALID_CONTENT);
				}
				
				// Add client to system
				controller.updateNode(activePath(entityID.toString()), data);
				logger.debug("Updating " + entityID + " from active directory");
				return new Response<Boolean>(true, ResponseCode.SUCCESS);
			} else if (isTombstoned(controller, entityID.toString())) {
				logger.warn("Can't update " + entityID + " because it is tombstoned");
				return new Response<Boolean>(false, ResponseCode.ERROR_TOMBSTONED);
			} else {
				logger.error(capitalize(type) + " " + entityID + " doesn't exist");
				return new Response<Boolean>(false, ResponseCode.ERROR_DOESNT_EXIST);
			}
		} catch (InterruptedException e) {
			logger.error("Error updating " + entityID);
			e.printStackTrace();
			return new Response<Boolean>(false, ResponseCode.ERROR_INTERNAL);
		}
	}
	
	/**
	 * Permanently tombstones an entity in the system. Tombstoned entities wishing to
	 * enter the system again must register as a new entity with a new ID
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param entityID Config to tombstone
	 * @return Response object with Boolean containing the success or failure of operation
	 */
	protected Response<Boolean> deleteEntity(IControllable controller, ConfigID entityID) {
		try {
			if (controller.exists(activePath(entityID.toString()))) {
				logger.debug("Tombstoning " + entityID);
				
				// Get data from client
				String data = controller.readNode(activePath(entityID.toString()));
				
				// Copy client to tombstoned path
				controller.addNode(tombstonedPath(entityID.toString()), data);
				
				// Delete client from active path
				controller.deleteNode(activePath(entityID.toString()));
				
				return new Response<Boolean>(true, ResponseCode.SUCCESS);
			} else if (controller.exists(tombstonedPath(entityID.toString()))) {
				logger.warn(capitalize(type) + " " + entityID + " already tombstoned");
				return new Response<Boolean>(false, ResponseCode.ERROR_TOMBSTONED);
			} else {
				logger.error(capitalize(type) + " " + entityID + " doesn't exist");
				return new Response<Boolean>(false, ResponseCode.ERROR_DOESNT_EXIST);
			}
		}  catch (InterruptedException e) {
			logger.error("Error tombstoning " + entityID);
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
	protected String activePath(String suffix) {
		return pathPrefixActive + suffix;
	}
	
	/**
	 * Creates proper system path for an active SystemEntity
	 * 
	 * @param suffix The configID
	 * @return Proper system path to SystemEntity
	 */
	protected String activePath(ConfigID suffix) {
		return activePath(suffix.toString());
	}
	
	/**
	 * Creates proper system path for an tombstoned SystemEntity
	 * 
	 * @param suffix The variable part of the system path
	 * @return Proper system path to SystemEntity
	 */
	protected String tombstonedPath(String suffix) {
		return pathPrefixTombstoned + suffix;
	}
	
	/**
	 * Creates proper system path for an tombstoned SystemEntity
	 * 
	 * @param suffix The configID
	 * @return Proper system path to SystemEntity
	 */
	protected String tombstonedPath(ConfigID suffix) {
		return tombstonedPath(suffix.toString());
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
	protected boolean exists(IControllable controller, String suffix) throws InterruptedException {
		return isActive(controller, suffix) || isTombstoned(controller, suffix);
	}
	
	/**
	 * Checks if entity exists in either active or tombstoned directories
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param configID The ID of the config to find
	 * @return Boolean true if path exists, false otherwise
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	public boolean exists(IControllable controller, ConfigID configID) throws InterruptedException {
		return exists(controller, configID.toString());
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
	protected boolean isActive(IControllable controller, String suffix) throws InterruptedException {
		boolean temp = controller.exists(pathPrefixActive + suffix);
		
		return temp;
	}
	
	/**
	 * Checks if entity exists in the active directory
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param suffix The configID to create path for
	 * @return Boolean true if path exists, false otherwise
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	protected boolean isActive(IControllable controller, ConfigID suffix) throws InterruptedException {
		return isActive(controller, suffix.toString());
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
	protected boolean isTombstoned(IControllable controller, String suffix) throws InterruptedException {
		return controller.exists(pathPrefixTombstoned + suffix);
	}
	
	/**
	 * Checks if entity exists in the tombstoned directory
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param suffix The configID to create path for
	 * @return Boolean true if path exists, false otherwise
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	protected boolean isTombstoned(IControllable controller, ConfigID suffix) throws InterruptedException {
		return isTombstoned(controller, suffix.toString());
	}
	
	/**
	 * Capitalizes the first letter of a string
	 * 
	 * @param input The string to capitalize
	 * @return The string with the first letter capitalized
	 */
	private String capitalize(String input) {
		return input.substring(0, 1).toUpperCase() + input.substring(1);
	}
}
