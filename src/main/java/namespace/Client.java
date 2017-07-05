package namespace;

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
	 * Client ID string
	 */
	String clientID;
	
	/**
	 * Public encryption key of the client
	 */
	String publicKey;
	
	/**
	 * Constructor for Client containing all client fields used
	 * within FBase.
	 * 
	 * @param clientID Client ID string
	 * @param publicKey Public encryption key of the client
	 */
	Client(String clientID, String publicKey) {
		this.clientID = clientID;
		this.publicKey = publicKey;
	}
	
	/**
	 * Registers a client with the FBase system
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param clientID Requested ID of new client
	 * @param publicKey Public encryption key of the client
	 * @return Response object with Boolean containing the success or failure of operation
	 */
	static Response<Boolean> registerClient(ZkController controller, String clientID, String publicKey) {
		// Create Client to register
		Client entity = new Client(clientID, publicKey);
				
		return registerEntity(controller, clientID, entity);
	}
	
	/**
	 * Responds with all information about the client
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param clientID ID of client to get information from
	 * @return Response object with String containing the Client information
	 */
	static Response<String> getClientInfo(ZkController controller, String clientID) {
		return getEntityInfo(controller, clientID);
	}
	
	/**
	 * Updates information kept on the client with the matching client ID
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param clientID ID of client to update
	 * @param publicKey Public encryption key of the client
	 * @return Response object with Boolean containing the success or failure of operation
	 */
	static Response<Boolean> updateClientInfo(ZkController controller, String clientID, String publicKey) {
		// Create Client to update
		Client entity = new Client(clientID, publicKey);
		
		return updateEntityInfo(controller, clientID, entity);
	}
	
	/**
	 * Permanently tombstones a client in the system. Tombstoned clients wishing to
	 * enter the system again must register as a new client with a new ID
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param clientID Client to tombstone
	 * @return Response object with Boolean containing the success or failure of operation
	 */
	static Response<Boolean> removeClient(ZkController controller, String clientID) {
		return removeEntity(controller, clientID);
	}
}
