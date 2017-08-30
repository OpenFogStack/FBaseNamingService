package namespace;

import org.apache.log4j.Logger;

import database.IControllable;
import model.config.ClientConfig;
import model.data.ClientID;
import model.messages.Response;
import model.messages.ResponseCode;

/**
 * The Client class performs all operations on the Clients section of
 * the system. This is primarily designed to keep track of information
 * about all Clients similar to a phone book so their details
 * are available.
 * 
 * @author Wm. Keith van der Meulen
 */
public class Client extends SystemEntity {
	
	private static Logger logger = Logger.getLogger(Client.class.getName());
	
	private static Client instance;
	
	public static Client getInstance() {
		if(instance == null) {
			instance = new Client();
		}
		
		return instance;
	}
	
	private Client() {
		super("client");
	}
	
	/**
	 * Registers a client with the FBase system
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param entity The ClientConfig to be registered to the system
	 * @return Response object with Boolean containing the success or failure of operation
	 */
	Response<Boolean> createClient(IControllable controller, ClientConfig entity) {
		logger.debug("Adding client " + entity.getID());
		return responseStringToBool(createEntity(controller, entity.getClientID(), entity));
		
	}
	
	/**
	 * Responds with all information about the client
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param id ID of client to get information from
	 * @return Response object with String containing the Client information
	 */
	public Response<String> readClient(IControllable controller, ClientID id) {
		logger.debug("Reading client " + id);
		return readEntity(controller, id);
	}
	
	/**
	 * Updates information kept on the client with the matching client ID
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param entity The new ClientConfig object to store
	 * @return Response object with Boolean containing the success or failure of operation
	 */
	Response<Boolean> updateClient(IControllable controller, ClientConfig entity) {
		logger.debug("Updating client " + entity.getID());
		return responseStringToBool(updateEntity(controller, entity.getClientID(), entity));
	}
	
	/**
	 * Permanently tombstones a client in the system. Tombstoned clients wishing to
	 * enter the system again must register as a new client with a new ID
	 * 
	 * @param controller Controller for interfacing with base distributed system
	 * @param id Client to tombstone
	 * @return Response object with Boolean containing the success or failure of operation
	 */
	Response<Boolean> deleteClient(IControllable controller, ClientID id) {
		logger.debug("Removing client " + id);
		return deleteEntity(controller, id);
	}
}
