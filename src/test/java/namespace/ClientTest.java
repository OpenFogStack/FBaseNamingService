package namespace;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import control.Configuration;
import control.NamingService;
import crypto.CryptoProvider.EncryptionAlgorithm;
import database.IControllable;
import database.localfiles.LocalFileController;
import model.JSONable;
import model.config.ClientConfig;
import model.data.ClientID;
import model.data.NodeID;
import model.messages.Command;
import model.messages.Envelope;
import model.messages.Message;
import model.messages.Response;
import model.messages.ResponseCode;

public class ClientTest {
	
	private static IControllable controller;
	private static NodeID sender;
	private static NamingService ns;
	
	private static final String activePath = "/client/active/";
	private static final String tombstonedPath = "/client/tombstoned/";
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		Configuration configuration = new Configuration();
		controller = new LocalFileController(new File(configuration.getRoot()));
		sender = new NodeID("sender");
		ns = new NamingService(controller, configuration);
	}

	@After
	public void tearDown() throws Exception {
		// Wait required so that all files are fully created before deleting
		java.util.concurrent.TimeUnit.SECONDS.sleep(5);
		
		Configuration configuration = new Configuration();
		File root = new File(configuration.getRoot());
		TestUtil.deleteDir(new File(root, "client"));
		TestUtil.deleteDir(new File(root, "node"));
		TestUtil.deleteDir(new File(root, "keygroup"));
	}
	
	@Test
	public void createClientTest() throws IllegalArgumentException, InterruptedException {
		// Set up client
		ClientID id = new ClientID("test_client");
		String key = "my_public_key";
		EncryptionAlgorithm alg = EncryptionAlgorithm.AES;
		ClientConfig c = new ClientConfig(id, key, alg);
		
		// Run tests
		createClient(c);
	}
	
	@Test
	public void readClientTest() throws IllegalArgumentException, InterruptedException {
		//Set up client
		ClientID id = new ClientID("test_client");
		String key = "my_public_key";
		EncryptionAlgorithm alg = EncryptionAlgorithm.AES;
		ClientConfig c = new ClientConfig(id, key, alg);
		
		// Run tests
		createClient(c);
		readClient(c.getClientID(), c);
	}
	
	@Test
	public void updateClientTest() throws IllegalArgumentException, InterruptedException {
		// Set up original version of client
		ClientID id = new ClientID("test_client");
		String key1 = "my_public_key";
		EncryptionAlgorithm alg1 = EncryptionAlgorithm.AES;
		ClientConfig c = new ClientConfig(id, key1, alg1);
		
		// Change all the client fields except id
		String key2 = "my_new_public_key";
		EncryptionAlgorithm alg2 = EncryptionAlgorithm.RSA_PRIVATE_ENCRYPT;
		ClientConfig u = new ClientConfig(id, key2, alg2);
		
		// Run tests
		createClient(c);
		updateClient(c, u);
	}
	
	@Test
	public void deleteClientTest() throws IllegalArgumentException, InterruptedException {
		// Set up client
		ClientID id = new ClientID("test_client");
		String key = "my_public_key";
		EncryptionAlgorithm alg = EncryptionAlgorithm.AES;
		ClientConfig c = new ClientConfig(id, key, alg);
		
		// Run tests
		createClient(c);
		deleteClient(c.getClientID());
	}

	public void createClient(ClientConfig c) throws IllegalArgumentException, InterruptedException {
		Message message = new Message(Command.CLIENT_CONFIG_CREATE, JSONable.toJSON(c));
		Envelope envelope = new Envelope(sender, message);
		
		@SuppressWarnings("unchecked")
		Response<Boolean> response = (Response<Boolean>) MessageParser.runCommand(controller, envelope);
		assertTrue("Proper success response", response.getValue());
		assertEquals("Proper response code", ResponseCode.SUCCESS, response.getResponseCode());
		assertTrue("Client in active", controller.exists(activePath + c.getClientID()));
		assertFalse("Client not in tombstoned", controller.exists(tombstonedPath + c.getClientID()));
	}
	
	public void readClient(ClientID id, ClientConfig expected) throws IllegalArgumentException, InterruptedException {
		assertTrue("Client started in active", controller.exists(activePath + id));
		
		Message message = new Message(Command.CLIENT_CONFIG_READ, JSONable.toJSON(id));
		Envelope envelope = new Envelope(sender, message);
		
		@SuppressWarnings("unchecked")
		Response<String> response = (Response<String>) MessageParser.runCommand(controller, envelope);
		assertNotNull("Client properly read", response.getMessage());
		assertEquals("Proper response code", ResponseCode.SUCCESS, response.getResponseCode());
		
		ClientConfig r = JSONable.fromJSON(response.getValue(), ClientConfig.class);
		
		assertEquals("Reads correct ID", expected.getClientID(), r.getClientID());
		assertEquals("Reads correct key", expected.getPublicKey(), r.getPublicKey());
		assertEquals("Reads correct algorithm", expected.getEncryptionAlgorithm(), r.getEncryptionAlgorithm());
	}
	
	public void updateClient(ClientConfig original, ClientConfig updated) throws IllegalArgumentException, InterruptedException {
		assertTrue("Client original started in active", controller.exists(activePath + original.getClientID()));
		assertEquals("Client to update has same ID as original", original.getClientID(), updated.getClientID());
		
		Message message = new Message(Command.CLIENT_CONFIG_UPDATE, JSONable.toJSON(updated));
		Envelope envelope = new Envelope(sender, message);
		
		@SuppressWarnings("unchecked")
		Response<Boolean> response = (Response<Boolean>) MessageParser.runCommand(controller, envelope);
		assertTrue("Proper success response", response.getValue());
		assertEquals("Proper response code", ResponseCode.SUCCESS, response.getResponseCode());
		
		readClient(original.getClientID(), updated);
	}
	
	public void deleteClient(ClientID id) throws IllegalArgumentException, InterruptedException {
		assertTrue("Client started in active", controller.exists(activePath + id));
		
		Message message = new Message(Command.CLIENT_CONFIG_DELETE, JSONable.toJSON(id));
		Envelope envelope = new Envelope(sender, message);
		
		@SuppressWarnings("unchecked")
		Response<Boolean> response = (Response<Boolean>) MessageParser.runCommand(controller, envelope);
		assertTrue("Proper success response", response.getValue());
		assertEquals("Proper response code", ResponseCode.SUCCESS, response.getResponseCode());
		assertFalse("Client deleted from active", controller.exists(activePath + id));
		assertTrue("Client moved to tombstoned", controller.exists(tombstonedPath + id));
	}
}
