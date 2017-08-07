package namespace;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import crypto.CryptoProvider.EncryptionAlgorithm;
import database.zookeeper.ZkConnector;
import database.zookeeper.ZkController;
import model.JSONable;
import model.config.ClientConfig;
import model.data.ClientID;
import model.data.NodeID;
import model.messages.Command;
import model.messages.Envelope;
import model.messages.Message;

public class MessageParserTest {
	private static ZkController controller;
	private static NodeID sender;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		ZkConnector connector = new ZkConnector();
		
		ZooKeeper zk = null;
		try {
			zk = connector.connect("localhost");
		} catch (IllegalStateException | IOException | InterruptedException e) {
			e.printStackTrace();
		}
		
		controller = new ZkController(zk);
		
		
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}
	
	@Test
	public void clientCRUDTest() throws KeeperException, InterruptedException {
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
		assertTrue("Client in ZooKeeper system", controller.exists("/client/active/test_client"));
		
		// Test Read
		
		ClientID idRead = new ClientID("test_client");
		
		Message messageRead = new Message();
		messageRead.setCommand(Command.CLIENT_CONFIG_READ);
		messageRead.setContent(JSONable.toJSON(idRead));
		
		Envelope envelopeRead = new Envelope(sender, messageRead);
		
		Response<String> responseRead = (Response<String>) MessageParser.runCommand(controller, envelopeRead);
		assertTrue("Client properly read", responseRead.getMessage() != null);
		
		ClientConfig clientReadCheck = JSONable.fromJSON(responseRead.getMessage(), ClientConfig.class);
		
		assertTrue("Reads correct ID", clientReadCheck.getClientID().getID().equals("test_client"));
		assertTrue("Reads correct key", clientReadCheck.getPublicKey().equals("my_public_key"));
		assertTrue("Reads correct algorithm", clientReadCheck.getEncryptionAlgorithm().equals(EncryptionAlgorithm.AES));
		
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
		
		ClientConfig clientReadUpdate = JSONable.fromJSON(responseReadUpdate.getMessage(), ClientConfig.class);
		
		assertTrue("Reads correct updated ID", clientReadCheck.getClientID().getID().equals("test_client"));
		assertTrue("Reads correct updated key", clientReadCheck.getPublicKey().equals("my_public_key_new"));
		assertTrue("Reads correct updated algorithm", clientReadCheck.getEncryptionAlgorithm().equals(EncryptionAlgorithm.AES));
		
		// Test Delete
		
		ClientID idDelete = new ClientID("test_client");
		
		Message messageDelete = new Message();
		messageDelete.setCommand(Command.CLIENT_CONFIG_DELETE);
		messageDelete.setContent(JSONable.toJSON(idDelete));
		
		Envelope envelopeDelete = new Envelope(sender, messageDelete);
		
		Response<Boolean> responseDelete = (Response<Boolean>) MessageParser.runCommand(controller, envelopeDelete);
		assertTrue("Client properly registered", responseCreate.getValue());
		assertTrue("Client in ZooKeeper system", controller.exists("/client/active/test_client") == false);
	}
	
	@Test
	public void nodeCreateTest() {
		fail("Not yet implemented");
	}
	
	@Test
	public void nodeReadTest() {
		fail("Not yet implemented");
	}
	
	@Test
	public void nodeUpdateTest() {
		fail("Not yet implemented");
	}
	
	@Test
	public void nodeDeleteTest() {
		fail("Not yet implemented");
	}
	
	@Test
	public void keygroupCreateTest() {
		fail("Not yet implemented");
	}
	
	@Test
	public void keygroupAddReplicaNodeTest() {
		fail("Not yet implemented");
	}
	
	@Test
	public void keygroupAddTriggerNodeTest() {
		fail("Not yet implemented");
	}
	
	@Test
	public void keygroupReadTest() {
		fail("Not yet implemented");
	}
	
	@Test
	public void keygroupUpdateCryptoTest() {
		fail("Not yet implemented");
	}
	
	@Test
	public void keygroupDeletTest() {
		fail("Not yet implemented");
	}

}
