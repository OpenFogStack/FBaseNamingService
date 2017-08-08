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

public class ClientTest {
	
	private static IControllable controller;
	private static NodeID sender;
	private static NamingService ns;
	
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
		ClientConfig c = new ClientConfig(new ClientID("test_client"), "my_public_key", EncryptionAlgorithm.AES);
		createClient(c);
	}
	
	@Test
	public void readClientTest() throws IllegalArgumentException, InterruptedException {
		ClientConfig c = new ClientConfig(new ClientID("test_client"), "my_public_key", EncryptionAlgorithm.AES);
		createClient(c);
		readClient(c.getClientID(), c);
	}
	
	@Test
	public void updateClientTest() throws IllegalArgumentException, InterruptedException {
		ClientConfig c = new ClientConfig(new ClientID("test_client"), "my_public_key", EncryptionAlgorithm.AES);
		createClient(c);
		ClientConfig u = new ClientConfig(c.getClientID(), "new_public_key", EncryptionAlgorithm.RSA_PUBLIC_ENCRYPT);
		updateClient(c, u);
	}
	
	@Test
	public void deleteClientTest() throws IllegalArgumentException, InterruptedException {
		ClientConfig c = new ClientConfig(new ClientID("test_client"), "my_public_key", EncryptionAlgorithm.AES);
		createClient(c);
		deleteClient(c.getClientID());
	}

	public void createClient(ClientConfig c) throws IllegalArgumentException, InterruptedException {
		Message message = new Message(Command.CLIENT_CONFIG_CREATE, JSONable.toJSON(c));
		Envelope envelope = new Envelope(sender, message);
		
		Response<Boolean> response = (Response<Boolean>) MessageParser.runCommand(controller, envelope);
		assertTrue("Proper success response", response.getValue());
		assertTrue("Client in active", controller.exists("/client/active/" + c.getClientID()));
		assertFalse("Client not in tombstoned", controller.exists("/client/tombstoned/" + c.getClientID()));
	}
	
	public void readClient(ClientID id, ClientConfig expected) throws IllegalArgumentException, InterruptedException {
		assertTrue("Client started in active", controller.exists("/client/active/" + id));
		
		Message message = new Message(Command.CLIENT_CONFIG_READ, JSONable.toJSON(id));
		Envelope envelope = new Envelope(sender, message);
		
		Response<String> response = (Response<String>) MessageParser.runCommand(controller, envelope);
		assertNotNull("Client properly read", response.getMessage());
		
		ClientConfig r = JSONable.fromJSON(response.getValue(), ClientConfig.class);
		
		assertEquals("Reads correct ID", expected.getClientID(), r.getClientID());
		assertEquals("Reads correct key", expected.getPublicKey(), r.getPublicKey());
		assertEquals("Reads correct algorithm", expected.getEncryptionAlgorithm(), r.getEncryptionAlgorithm());
	}
	
	public void updateClient(ClientConfig original, ClientConfig updated) throws IllegalArgumentException, InterruptedException {
		assertTrue("Client original started in active", controller.exists("/client/active/" + original.getClientID()));
		assertEquals("Client to update has same ID as original", original.getClientID(), updated.getClientID());
		
		Message message = new Message(Command.CLIENT_CONFIG_UPDATE, JSONable.toJSON(updated));
		Envelope envelope = new Envelope(sender, message);
		
		Response<Boolean> response = (Response<Boolean>) MessageParser.runCommand(controller, envelope);
		assertTrue("Proper success response", response.getValue());
		
		readClient(original.getClientID(), updated);
	}
	
	public void deleteClient(ClientID id) throws IllegalArgumentException, InterruptedException {
		assertTrue("Client started in active", controller.exists("/client/active/" + id));
		
		Message messageDelete = new Message(Command.CLIENT_CONFIG_DELETE, JSONable.toJSON(id));
		Envelope envelopeDelete = new Envelope(sender, messageDelete);
		
		Response<Boolean> responseDelete = (Response<Boolean>) MessageParser.runCommand(controller, envelopeDelete);
		assertTrue("Proper success response", responseDelete.getValue());
		assertFalse("Client deleted from active", controller.exists("/client/active/" + id));
		assertTrue("Client moved to tombstoned", controller.exists("/client/tombstoned/" + id));
	}
}
