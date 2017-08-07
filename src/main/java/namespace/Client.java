package namespace;

import database.IControllable;
import model.config.ClientConfig;
import model.data.ClientID;

/**
 * The Client class performs all operations on the Clients section of
 * the system. This is primarily designed to keep track of information
 * about all Clients similar to a phone book so their details
 * are available.
 * 
 * @author Wm. Keith van der Meulen
 */
class Client extends SystemEntity {
	
	/**
	 * Name of the entity type
	 */
	static final String type = "client";
	
	/**
	 * Registers a client with the FBase system
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param entity The ClientConfig to be registered to the system
	 * @return Response object with Boolean containing the success or failure of operation
	 */
	static Response<Boolean> registerClient(IControllable controller, ClientConfig entity) {
		return registerEntity(controller, entity.getClientID(), entity);
	}
	
	/**
	 * Responds with all information about the client
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param clientID ID of client to get information from
	 * @return Response object with String containing the Client information
	 */
	static Response<String> getClientInfo(IControllable controller, ClientID clientID) {
		return getEntityInfo(controller, clientID);
	}
	
	/**
	 * Updates information kept on the client with the matching client ID
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param entity The new ClientConfig object to store
	 * @return Response object with Boolean containing the success or failure of operation
	 */
	static Response<Boolean> updateClientInfo(IControllable controller, ClientConfig entity) {
		return updateEntityInfo(controller, entity.getClientID(), entity);
	}
	
	/**
	 * Permanently tombstones a client in the system. Tombstoned clients wishing to
	 * enter the system again must register as a new client with a new ID
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param clientID Client to tombstone
	 * @return Response object with Boolean containing the success or failure of operation
	 */
	static Response<Boolean> removeClient(IControllable controller, ClientID clientID) {
		return removeEntity(controller, clientID);
	}
}
