package namespace;

import namespace.ClientResponse.ClientResponseCode;

/**
 * The Client class performs all operations on the Clients section of
 * the system. This is primarily designed to keep track of information
 * about all Clients similar to a phone book so their details
 * are available.
 * 
 * @author Wm. Keith van der Meulen
 */
class Client {
	
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
	 * Responds with a random string unused by any client at the time of the call
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @return Response object with String containing an unused client ID
	 */
	static ClientResponse<String> getUnusedClientID(ZkController controller) {
		// TODO Fill out stub function
		
		return new ClientResponse<String>(null, ClientResponseCode.NULL); // XXX Placeholder return... must be changed
	}
	
	/**
	 * Registers a client with the FBase system
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param clientID Requested ID of new client
	 * @param publicKey Public encryption key of the client
	 * @return Response object with Boolean containing the success or failure of operation
	 */
	static ClientResponse<Boolean> registerClient(ZkController controller, String clientID, String publicKey) {
		// TODO Fill out stub function
		
		return new ClientResponse<Boolean>(null, ClientResponseCode.NULL); // XXX Placeholder return... must be changed
	}
	
	/**
	 * Responds with all information about the client
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param clientID ID of client to get information from
	 * @return Response object with String containing the Client information
	 */
	static ClientResponse<String> getClientInfo(ZkController controller, String clientID) {
		// TODO Fill out stub function
		
		return new ClientResponse<String>(null, ClientResponseCode.NULL); // XXX Placeholder return... must be changed
	}
	
	/**
	 * Updates information kept on the client with the matching client ID
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param clientID ID of client to update
	 * @param publicKey Public encryption key of the client
	 * @return Response object with Boolean containing the success or failure of operation
	 */
	static ClientResponse<Boolean> updateClientInfo(ZkController controller, String clientID, String publicKey) {
		// TODO Fill out stub function
		
		return new ClientResponse<Boolean>(null, ClientResponseCode.NULL); // XXX Placeholder return... must be changed
	}
	
	/**
	 * Permanently tombstones a client in the system. Tombstoned clients wishing to
	 * enter the system again must register as a new client with a new ID
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param clientID Client to tombstone
	 * @return Response object with Boolean containing the success or failure of operation
	 */
	static ClientResponse<Boolean> removeClient(ZkController controller, String clientID) {
		// TODO Fill out stub function
		
		return new ClientResponse<Boolean>(null, ClientResponseCode.NULL); // XXX Placeholder return... must be changed
	}
}
