package namespace;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

public class MessageParserTest {
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
		deleteDir(new File(root, "client"));
		deleteDir(new File(root, "node"));
		deleteDir(new File(root, "keygroup"));
	}
	
	void deleteDir(File file) {
	    File[] contents = file.listFiles();
	    if (contents != null) {
	        for (File f : contents) {
	            deleteDir(f);
	        }
	    }
	    file.delete();
	}
	
	@Test
	public void clientCRUDTest() throws InterruptedException {
		// Test create
		
		ClientID idCreate = new ClientID("test_client");
		String keyCreate = "my_public_key";
		EncryptionAlgorithm algorithmCreate = EncryptionAlgorithm.AES;
		
		ClientConfig clientCreate = new ClientConfig(idCreate, keyCreate, algorithmCreate);
		
		Message messageCreate = new Message();
		messageCreate.setCommand(Command.CLIENT_CONFIG_CREATE);
		messageCreate.setContent(JSONable.toJSON(clientCreate));
		
		Envelope envelopeCreate = new Envelope(sender, messageCreate);
		
		Response<Boolean> responseCreate = (Response<Boolean>) MessageParser.runCommand(controller, envelopeCreate);
		assertTrue("Client properly registered", responseCreate.getValue());
		assertTrue("Client in system", controller.exists("/client/active/test_client"));
		
		// Test Read
		
		ClientID idRead = new ClientID("test_client");
		
		Message messageRead = new Message();
		messageRead.setCommand(Command.CLIENT_CONFIG_READ);
		messageRead.setContent(JSONable.toJSON(idRead));
		
		Envelope envelopeRead = new Envelope(sender, messageRead);
		
		Response<String> responseRead = (Response<String>) MessageParser.runCommand(controller, envelopeRead);
		assertNotNull("Client properly read", responseRead.getMessage());
		
		ClientConfig clientReadCheck = JSONable.fromJSON(responseRead.getValue(), ClientConfig.class);
		
		assertEquals("Reads correct ID", "test_client", clientReadCheck.getClientID().getID());
		assertEquals("Reads correct key", "my_public_key", clientReadCheck.getPublicKey());
		assertEquals("Reads correct algorithm", EncryptionAlgorithm.AES, clientReadCheck.getEncryptionAlgorithm());
		
		// Test Update
		
		ClientID idUpdate = new ClientID("test_client");
		String keyUpdate = "my_public_key_new";
		EncryptionAlgorithm algorithmUpdate = EncryptionAlgorithm.AES;
		
		ClientConfig clientUpdate = new ClientConfig(idUpdate, keyUpdate, algorithmUpdate);
		
		Message messageUpdate = new Message();
		messageUpdate.setCommand(Command.CLIENT_CONFIG_UPDATE);
		messageUpdate.setContent(JSONable.toJSON(clientUpdate));
		
		Envelope envelopeUpdate = new Envelope(sender, messageUpdate);
		
		Response<Boolean> responseUpdate = (Response<Boolean>) MessageParser.runCommand(controller, envelopeUpdate);
		assertTrue("Client properly updated", responseUpdate.getValue());
		
		Response<String> responseReadUpdate = (Response<String>) MessageParser.runCommand(controller, envelopeRead);
		
		ClientConfig clientReadUpdate = JSONable.fromJSON(responseReadUpdate.getValue(), ClientConfig.class);
		
		assertEquals("Reads correct updated ID", "test_client", clientReadUpdate.getClientID().getID());
		assertEquals("Reads correct updated key", "my_public_key_new", clientReadUpdate.getPublicKey());
		assertEquals("Reads correct updated algorithm", EncryptionAlgorithm.AES, clientReadUpdate.getEncryptionAlgorithm());
		
		// Test Delete
		
		ClientID idDelete = new ClientID("test_client");
		
		Message messageDelete = new Message();
		messageDelete.setCommand(Command.CLIENT_CONFIG_DELETE);
		messageDelete.setContent(JSONable.toJSON(idDelete));
		
		Envelope envelopeDelete = new Envelope(sender, messageDelete);
		
		Response<Boolean> responseDelete = (Response<Boolean>) MessageParser.runCommand(controller, envelopeDelete);
		assertTrue("Client properly registered", responseDelete.getValue());
		assertFalse("Client in system", controller.exists("/client/active/test_client"));
		assertTrue("Client in system", controller.exists("/client/tombstoned/test_client"));
	}
	
	/*
	@Test
	public void nodeCRUDTest() {
		fail("Not yet implemented");
	}
	
	@Test
	public void keygroupCRUDTest() {
		fail("Not yet implemented");
	}
	*/
}
