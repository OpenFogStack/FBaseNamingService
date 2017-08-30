package communication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

import org.javatuples.Pair;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import control.Configuration;
import control.NamingService;
import crypto.CryptoProvider.EncryptionAlgorithm;
import crypto.RSAHelper;
import database.IControllable;
import database.localfiles.LocalFileController;
import model.JSONable;
import model.config.NodeConfig;
import model.data.NodeID;
import model.messages.Command;
import model.messages.Envelope;
import model.messages.Message;
import model.messages.Response;
import model.messages.ResponseCode;
import namespace.TestUtil;

public class CommunicationTests {

	private static IControllable controller;
	private static NamingService ns;
	private NamespaceSender sender;
	private NodeConfig thisNode;
	
	private static String publicKey;
	private static String privateKey;
	
	private static final String nodeActivePath = "/node/active/";
	private static final String nodeTombstonedPath = "/node/tombstoned/";
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		Configuration configuration = new Configuration();
		File root = new File(configuration.getRoot());
		TestUtil.deleteDir(new File(root, "client"));
		TestUtil.deleteDir(new File(root, "node"));
		TestUtil.deleteDir(new File(root, "keygroup"));
		
		controller = new LocalFileController(new File(configuration.getRoot()), configuration.getFolderSeparator());
		ns = new NamingService(controller, configuration);
		
		sender = new NamespaceSender(ns, configuration.getAddress(), configuration.getPort(), null, null);
		
		Pair<PublicKey, PrivateKey> keys = RSAHelper.generateKeyPair(512);
		publicKey = RSAHelper.getEncodedStringFromKey(keys.getValue0());
		privateKey = RSAHelper.getEncodedStringFromKey(keys.getValue1());
		
		// Set up original version of node
		NodeID id = new NodeID("test_node");
		EncryptionAlgorithm alg1 = EncryptionAlgorithm.AES;
		List<String> machines1 = new ArrayList<String>();
		machines1.add("m1");
		machines1.add("m2");
		machines1.add("m3");
		Integer pPort1 = 1001;
		Integer mPort1 = 2001;
		Integer rPort1 = 3001;
		String loc1 = "my_location";
		String desc1 = "my_description";
		
		thisNode = new NodeConfig(id, publicKey, alg1, machines1, pPort1, mPort1, rPort1, loc1, desc1);
		createNode(thisNode);
	}

	@After
	public void tearDown() throws Exception {
		// Wait required so that all files are fully created before deleting
		java.util.concurrent.TimeUnit.SECONDS.sleep(5);
		
		ns.tearDown();
		sender.shutdown();
	}

	@Test
	public void testResetNamingService() throws IllegalArgumentException, InterruptedException {
		Message m = new Message();
		m.setCommand(Command.RESET_NAMING_SERVICE);
		m.setContent("");
		Envelope e = new Envelope(thisNode.getID(), m);
		
		sender.setPrivateKey(privateKey);
		sender.setPublicKey(publicKey);
		
		String response = sender.send(e, null, null);
		Thread.sleep(10000);
		assertTrue(Boolean.parseBoolean(response));
	}
	
	@Test
	public void testReadWithCommunication() {
		Message m = new Message(Command.NODE_CONFIG_READ, JSONable.toJSON(thisNode.getID()));
		Envelope e = new Envelope(thisNode.getID(), m);
		
		sender.setPublicKey(publicKey);
		sender.setPrivateKey(privateKey);
		
		String response = sender.send(e, null, null);
		@SuppressWarnings("unchecked")
		Response<String> read = (Response<String>) TestUtil.run(Command.NODE_CONFIG_READ, thisNode.getID(), thisNode.getID(), controller);
		
		assertEquals("Proper message received", read.getValue(), response);
	}
	
	@Test
	public void testWriteWithCommunication() {
		// Set up original version of node
		NodeID id = new NodeID("test_node_1");
		String key1 = "my_public_key";
		EncryptionAlgorithm alg1 = EncryptionAlgorithm.AES;
		List<String> machines1 = new ArrayList<String>();
		machines1.add("m1");
		machines1.add("m2");
		machines1.add("m3");
		Integer pPort1 = 1001;
		Integer mPort1 = 2001;
		Integer rPort1 = 3001;
		String loc1 = "my_location";
		String desc1 = "my_description";
		
		NodeConfig n = new NodeConfig(id, key1, alg1, machines1, pPort1, mPort1, rPort1, loc1, desc1);
		
		Message m = new Message(Command.NODE_CONFIG_CREATE, JSONable.toJSON(n));
		Envelope e = new Envelope(thisNode.getID(), m);
		
		sender.setPublicKey(publicKey);
		sender.setPrivateKey(privateKey);
		
		String response = sender.send(e, null, null);
		
		System.out.println(response);
		assertEquals("Proper message received", "true", response);
	}

	void createNode(NodeConfig c) throws IllegalArgumentException, InterruptedException {
		assertFalse("Node not active at start", controller.exists(nodeActivePath + c.getNodeID()));
		assertFalse("Node not in tombstoned at start", controller.exists(nodeTombstonedPath + c.getNodeID()));
		
		@SuppressWarnings("unchecked")
		Response<Boolean> response = (Response<Boolean>) TestUtil.run(Command.NODE_CONFIG_CREATE, c, c.getID(), controller);
		
		assertTrue("Proper success response", response.getValue());
		assertEquals("Proper response code", ResponseCode.SUCCESS, response.getResponseCode());
		assertTrue("Node in active", controller.exists(nodeActivePath + c.getNodeID()));
		assertFalse("Node not in tombstoned", controller.exists(nodeTombstonedPath + c.getNodeID()));
	}
}
