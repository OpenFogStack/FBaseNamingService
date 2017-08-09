package namespace;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
import model.config.NodeConfig;
import model.data.NodeID;
import model.messages.Command;
import model.messages.Envelope;
import model.messages.Message;
import model.messages.Response;
import model.messages.ResponseCode;

public class NodeTest {
	
	private static IControllable controller;
	private static NamingService ns;
	
	private static final String activePath = "/node/active/";
	private static final String tombstonedPath = "/node/tombstoned/";
	
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
	public void createNodeTest() throws IllegalArgumentException, InterruptedException {
		// Set up node
		NodeID id = new NodeID("test_node");
		String key = "my_public_key";
		EncryptionAlgorithm alg = EncryptionAlgorithm.AES;
		List<String> machines = new ArrayList<String>();
		machines.add("m1");
		machines.add("m2");
		machines.add("m3");
		Integer pPort = 1001;
		Integer mPort = 2001;
		Integer rPort = 3001;
		String loc = "my_location";
		String desc = "my_description";
		NodeConfig n = new NodeConfig(id, key, alg, machines, pPort, mPort, rPort, loc, desc);
		
		// Run tests
		createNode(n);
	}
	
	@Test
	public void readNodeTest() throws IllegalArgumentException, InterruptedException {
		// Set up node
		NodeID id = new NodeID("test_node");
		String key = "my_public_key";
		EncryptionAlgorithm alg = EncryptionAlgorithm.AES;
		List<String> machines = new ArrayList<String>();
		machines.add("m1");
		machines.add("m2");
		machines.add("m3");
		Integer pPort = 1001;
		Integer mPort = 2001;
		Integer rPort = 3001;
		String loc = "my_location";
		String desc = "my_description";
		NodeConfig n = new NodeConfig(id, key, alg, machines, pPort, mPort, rPort, loc, desc);
		
		// Run tests
		createNode(n);
		readNode(id, n);
	}
	
	@Test
	public void updateNodeTest() throws IllegalArgumentException, InterruptedException {
		// Set up original version of node
		NodeID id = new NodeID("test_node");
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
		NodeConfig c = new NodeConfig(id, key1, alg1, machines1, pPort1, mPort1, rPort1, loc1, desc1);
		
		// Change all the node fields except id
		String key2 = "my_new_public_key";
		EncryptionAlgorithm alg2 = EncryptionAlgorithm.RSA_PRIVATE_ENCRYPT;
		List<String> machines2 = new ArrayList<String>();
		machines2.add("m4");
		machines2.add("m5");
		machines2.add("m6");
		Integer pPort2 = 1002;
		Integer mPort2 = 2002;
		Integer rPort2 = 3002;
		String loc2 = "my_new_location";
		String desc2 = "my_new_description";
		NodeConfig u = new NodeConfig(id, key2, alg2, machines2, pPort2, mPort2, rPort2, loc2, desc2);
		
		// Run tests
		createNode(c);
		updateNode(c, u);
	}
	
	@Test
	public void deleteNodeTest() throws IllegalArgumentException, InterruptedException {
		// Set up node
		NodeID id = new NodeID("test_node");
		String key = "my_public_key";
		EncryptionAlgorithm alg = EncryptionAlgorithm.AES;
		List<String> machines = new ArrayList<String>();
		machines.add("m1");
		machines.add("m2");
		machines.add("m3");
		Integer pPort = 1001;
		Integer mPort = 2001;
		Integer rPort = 3001;
		String loc = "my_location";
		String desc = "my_description";
		NodeConfig n = new NodeConfig(id, key, alg, machines, pPort, mPort, rPort, loc, desc);
		
		// Run tests
		createNode(n);
		deleteNode(id);
	}
	
	@Test
	public void updateNodeBadSenderTest() throws IllegalArgumentException, InterruptedException {
		// Set up original version of node
		NodeID id = new NodeID("test_node");
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
		NodeConfig c = new NodeConfig(id, key1, alg1, machines1, pPort1, mPort1, rPort1, loc1, desc1);
		
		// Change all the node fields except id
		String key2 = "my_new_public_key";
		EncryptionAlgorithm alg2 = EncryptionAlgorithm.RSA_PRIVATE_ENCRYPT;
		List<String> machines2 = new ArrayList<String>();
		machines2.add("m4");
		machines2.add("m5");
		machines2.add("m6");
		Integer pPort2 = 1002;
		Integer mPort2 = 2002;
		Integer rPort2 = 3002;
		String loc2 = "my_new_location";
		String desc2 = "my_new_description";
		NodeConfig u = new NodeConfig(id, key2, alg2, machines2, pPort2, mPort2, rPort2, loc2, desc2);
		
		// Run tests
		createNode(c);
		updateNodeDifferentSender(c, u);
	}
	
	@Test
	public void deleteNodeBadSenderTest() throws IllegalArgumentException, InterruptedException {
		// Set up node
		NodeID id = new NodeID("test_node");
		String key = "my_public_key";
		EncryptionAlgorithm alg = EncryptionAlgorithm.AES;
		List<String> machines = new ArrayList<String>();
		machines.add("m1");
		machines.add("m2");
		machines.add("m3");
		Integer pPort = 1001;
		Integer mPort = 2001;
		Integer rPort = 3001;
		String loc = "my_location";
		String desc = "my_description";
		NodeConfig n = new NodeConfig(id, key, alg, machines, pPort, mPort, rPort, loc, desc);
		
		// Run tests
		createNode(n);
		deleteNodeDifferentSender(id);
	}
	
	public void createNode(NodeConfig c) throws IllegalArgumentException, InterruptedException {
		Message message = new Message(Command.NODE_CONFIG_CREATE, JSONable.toJSON(c));
		Envelope envelope = new Envelope(c.getID(), message);
		
		@SuppressWarnings("unchecked")
		Response<Boolean> response = (Response<Boolean>) MessageParser.runCommand(controller, envelope);
		assertTrue("Proper success response", response.getValue());
		assertEquals("Proper response code", ResponseCode.SUCCESS, response.getResponseCode());
		assertTrue("Node in active", controller.exists(activePath + c.getNodeID()));
		assertFalse("Node not in tombstoned", controller.exists(tombstonedPath + c.getNodeID()));
	}
	
	public void readNode(NodeID id, NodeConfig expected) throws IllegalArgumentException, InterruptedException {
		assertTrue("Node started in active", controller.exists(activePath + id));
		
		Message message = new Message(Command.NODE_CONFIG_READ, JSONable.toJSON(id));
		Envelope envelope = new Envelope(id, message);
		
		@SuppressWarnings("unchecked")
		Response<String> response = (Response<String>) MessageParser.runCommand(controller, envelope);
		assertNotNull("Node properly read", response.getMessage());
		assertEquals("Proper response code", ResponseCode.SUCCESS, response.getResponseCode());
		
		NodeConfig r = JSONable.fromJSON(response.getValue(), NodeConfig.class);
		
		assertEquals("Reads correct ID", expected.getNodeID(), r.getNodeID());
		assertEquals("Reads correct key", expected.getPublicKey(), r.getPublicKey());
		assertEquals("Reads correct algorithm", expected.getEncryptionAlgorithm(), r.getEncryptionAlgorithm());
		assertEquals("Reads correct machines", expected.getMachines(), r.getMachines());
		assertEquals("Reads correct publisher port", expected.getPublisherPort(), r.getPublisherPort());
		assertEquals("Reads correct messenger port", expected.getMessagePort(), r.getMessagePort());
		assertEquals("Reads correct rest port", expected.getRestPort(), r.getRestPort());
		assertEquals("Reads correct location", expected.getLocation(), r.getLocation());
		assertEquals("Reads correct description", expected.getDescription(), r.getDescription());
	}
	
	public void updateNode(NodeConfig original, NodeConfig updated) throws IllegalArgumentException, InterruptedException {
		assertTrue("Node original started in active", controller.exists(activePath + original.getNodeID()));
		assertEquals("Node to update has same ID as original", original.getNodeID(), updated.getNodeID());
		
		Message message = new Message(Command.NODE_CONFIG_UPDATE, JSONable.toJSON(updated));
		Envelope envelope = new Envelope(original.getID(), message);
		
		@SuppressWarnings("unchecked")
		Response<Boolean> response = (Response<Boolean>) MessageParser.runCommand(controller, envelope);
		assertTrue("Proper success response", response.getValue());
		assertEquals("Proper response code", ResponseCode.SUCCESS, response.getResponseCode());
		
		readNode(original.getNodeID(), updated);
	}
	
	public void deleteNode(NodeID id) throws IllegalArgumentException, InterruptedException {
		assertTrue("Node started in active", controller.exists(activePath + id));
		
		Message message = new Message(Command.NODE_CONFIG_DELETE, JSONable.toJSON(id));
		Envelope envelope = new Envelope(id, message);
		
		@SuppressWarnings("unchecked")
		Response<Boolean> response = (Response<Boolean>) MessageParser.runCommand(controller, envelope);
		assertTrue("Proper success response", response.getValue());
		assertEquals("Proper response code", ResponseCode.SUCCESS, response.getResponseCode());
		assertFalse("Node deleted from active", controller.exists(activePath + id));
		assertTrue("Node moved to tombstoned", controller.exists(tombstonedPath + id));
	}
	
	public void updateNodeDifferentSender(NodeConfig original, NodeConfig updated) throws IllegalArgumentException, InterruptedException {
		assertTrue("Node original started in active", controller.exists(activePath + original.getNodeID()));
		assertEquals("Node to update has same ID as original", original.getNodeID(), updated.getNodeID());
		
		Message message = new Message(Command.NODE_CONFIG_UPDATE, JSONable.toJSON(updated));
		Envelope envelope = new Envelope(new NodeID("wrong_id"), message);
		
		@SuppressWarnings("unchecked")
		Response<Boolean> response = (Response<Boolean>) MessageParser.runCommand(controller, envelope);
		assertFalse("Proper failure response", response.getValue());
		assertEquals("Proper response code", ResponseCode.ERROR_ILLEGAL_COMMAND, response.getResponseCode());
	}
	 
	public void deleteNodeDifferentSender(NodeID id) throws IllegalArgumentException, InterruptedException {
		assertTrue("Node started in active", controller.exists(activePath + id));
		
		Message message = new Message(Command.NODE_CONFIG_DELETE, JSONable.toJSON(id));
		Envelope envelope = new Envelope(new NodeID("wrong_id"), message);
		
		@SuppressWarnings("unchecked")
		Response<Boolean> response = (Response<Boolean>) MessageParser.runCommand(controller, envelope);
		assertFalse("Proper failure response", response.getValue());
		assertEquals("Proper response code", ResponseCode.ERROR_ILLEGAL_COMMAND, response.getResponseCode());
		assertTrue("Node still in active", controller.exists(activePath + id));
		assertFalse("Node not in tombstoned", controller.exists(tombstonedPath + id));
	}
}
