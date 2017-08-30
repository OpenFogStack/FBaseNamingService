package namespace;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import model.config.KeygroupConfig;
import model.config.NodeConfig;
import model.config.ReplicaNodeConfig;
import model.config.TriggerNodeConfig;
import model.data.ClientID;
import model.data.KeygroupID;
import model.data.NodeID;
import model.messages.Command;
import model.messages.ConfigToKeygroupWrapper;
import model.messages.Response;
import model.messages.ResponseCode;

public class KeygroupTest {
	
	private static IControllable controller;
	private static NodeConfig sender;
	private static NamingService ns;
	
	private static final String keygroupActivePath = "/keygroup/active/";
	private static final String keygroupTombstonedPath = "/keygroup/tombstoned/";
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
		// Wait required so that all files are fully created before deleting
		java.util.concurrent.TimeUnit.SECONDS.sleep(5);
		
		Configuration configuration = new Configuration();
		File root = new File(configuration.getRoot());
		TestUtil.deleteDir(new File(root, "client"));
		TestUtil.deleteDir(new File(root, "node"));
		TestUtil.deleteDir(new File(root, "keygroup"));
		controller = new LocalFileController(new File(configuration.getRoot()), configuration.getFolderSeparator());
		ns = new NamingService(controller, configuration);

		sender = makeNode(0);
		@SuppressWarnings("unchecked")
		Response<Boolean> response = (Response<Boolean>) TestUtil.run(Command.NODE_CONFIG_CREATE, sender, sender.getID(), controller);
		
		if(response.getResponseCode() != ResponseCode.SUCCESS) {
			fail("Can't add node properly");
		}
	}

	@After
	public void tearDown() throws Exception {
		ns.tearDown();
	}
	
	@Test
	public void createKeygroupTest() throws IllegalArgumentException, InterruptedException {
		ReplicaNodeConfig rNode = makeReplicaNode(sender.getID(), 10);
		KeygroupConfig kg = makeStartingKeygroup(rNode, 1);
		
		// Run tests
		createKeygroup(kg);
	}
	
	@Test
	public void readKeygroupAuthorizedReplicaNodeTest() throws IllegalArgumentException, InterruptedException {
		ReplicaNodeConfig rNode = makeReplicaNode(sender.getID(), 10);
		KeygroupConfig kg = makeStartingKeygroup(rNode, 1);
		
		// Run tests
		createKeygroup(kg);
		readKeygroup(kg.getID(), sender.getID(), kg, true);
	}
	
	@Test
	public void readKeygroupAuthorizedTriggerNodeTest() throws IllegalArgumentException, InterruptedException {
		// Set up initial keygroup 
		ReplicaNodeConfig rNode = makeReplicaNode(sender.getID(), 10);
		KeygroupConfig kg = makeStartingKeygroup(rNode, 1);
		createKeygroup(kg);
		
		// Create valid node in system and make a trigger node
		NodeConfig newTriggerNode = makeNode(1);
		TestUtil.run(Command.NODE_CONFIG_CREATE, newTriggerNode, newTriggerNode.getID(), controller);
		TriggerNodeConfig newNode = makeTriggerNode(newTriggerNode.getID());
		kg.addTriggerNode(newNode);
		addTriggerNode(kg.getID(), newNode, kg);
		
		// Run tests
		readKeygroup(kg.getID(), newTriggerNode.getID(), kg, true);
	}

	@Test
	public void readKeygroupUnauthorizedNodeTest() throws IllegalArgumentException, InterruptedException {
		// Set up initial keygroup 
		ReplicaNodeConfig rNode = makeReplicaNode(sender.getID(), 10);
		KeygroupConfig kg = makeStartingKeygroup(rNode, 1);
		createKeygroup(kg);
		
		// Make random unauthorized node
		NodeConfig unauthorized = makeNode(-1);
		@SuppressWarnings("unchecked")
		Response<Boolean> response = (Response<Boolean>) TestUtil.run(Command.NODE_CONFIG_CREATE, unauthorized, sender.getID(), controller);
		
		// Remove encryption algorithm and secret for expected
		kg.setEncryptionAlgorithm(null);
		kg.setEncryptionSecret(null);
		
		// Run tests
		readKeygroup(kg.getID(), unauthorized.getID(), kg, false);
	}

	@Test
	public void addReplicaNodeTest() throws IllegalArgumentException, InterruptedException {
		// Set up initial keygroup
		ReplicaNodeConfig rNode = makeReplicaNode(sender.getID(), 10);
		KeygroupConfig kg = makeStartingKeygroup(rNode, 1);
		createKeygroup(kg);
		
		// Create valid node in system and make a replica node
		NodeConfig newReplicaNode = makeNode(1);
		TestUtil.run(Command.NODE_CONFIG_CREATE, newReplicaNode, newReplicaNode.getID(), controller);
		ReplicaNodeConfig newNode = makeReplicaNode(newReplicaNode.getID(), 10);
		kg.addReplicaNode(newNode);
		
		// Run tests
		addReplicaNode(kg.getID(), newNode, kg);
	}

	@Test
	public void addTriggerNodeTest() throws IllegalArgumentException, InterruptedException {
		// Set up initial keygroup 
		ReplicaNodeConfig rNode = makeReplicaNode(sender.getID(), 10);
		KeygroupConfig kg = makeStartingKeygroup(rNode, 1);
		createKeygroup(kg);
		
		// Create valid node in system and make a trigger node
		NodeConfig newTriggerNode = makeNode(1);
		TestUtil.run(Command.NODE_CONFIG_CREATE, newTriggerNode, newTriggerNode.getID(), controller);
		TriggerNodeConfig newNode = makeTriggerNode(newTriggerNode.getID());
		kg.addTriggerNode(newNode);
		
		// Run tests
		addTriggerNode(kg.getID(), newNode, kg);
	}

	@Test
	public void removeTriggerNodeTest() {
		fail();
	}
	
	private void createKeygroup(KeygroupConfig c) throws IllegalArgumentException, InterruptedException {
		assertFalse("Keygroup not active at start", controller.exists(keygroupActivePath + c.getID()));
		assertFalse("Keygroup not in tombstoned at start", controller.exists(keygroupTombstonedPath + c.getID()));
		
		@SuppressWarnings("unchecked")
		Response<Boolean> response = (Response<Boolean>) TestUtil.run(Command.KEYGROUP_CONFIG_CREATE, c, sender.getID(), controller);
		
		assertTrue("Proper success response", response.getValue());
		assertEquals("Proper response code", ResponseCode.SUCCESS, response.getResponseCode());
		assertTrue("Keygroup in active", controller.exists(keygroupActivePath + c.getID()));
		assertFalse("Keygroup not in tombstoned", controller.exists(keygroupTombstonedPath + c.getID()));
	}
	
	private void readKeygroup(KeygroupID id, NodeID readingNode, KeygroupConfig expected, boolean authorized) throws IllegalArgumentException, InterruptedException {
		assertTrue("Keygroup started in active", controller.exists(keygroupActivePath + id));
		
		@SuppressWarnings("unchecked")
		Response<String> response = (Response<String>) TestUtil.run(Command.KEYGROUP_CONFIG_READ, id, readingNode, controller);
		
		assertNotNull("Keygroup properly read", response.getValue());
		assertEquals("Proper response code", ResponseCode.SUCCESS, response.getResponseCode());
		
		KeygroupConfig r = JSONable.fromJSON(response.getValue(), KeygroupConfig.class);
		
		assertEquals("Reads correct ID", expected.getID(), r.getID());
		assertEquals("Reads correct clients", expected.getClients(), r.getClients());
		assertEquals("Reads correct replica nodes", expected.getReplicaNodes(), r.getReplicaNodes());
		assertEquals("Reads correct trigger nodes", expected.getTriggerNodes(), r.getTriggerNodes());
		
		if(authorized) {
			assertEquals("Reads correct secret", expected.getEncryptionSecret(), r.getEncryptionSecret());
			assertEquals("Reads correct algorithm", expected.getEncryptionAlgorithm(), r.getEncryptionAlgorithm());
		}
		
		assertEquals("KeygroupConfigs are equal", expected, r);
	}
	
	private void addReplicaNode(KeygroupID id, ReplicaNodeConfig newNode, KeygroupConfig expected) throws IllegalArgumentException, InterruptedException {
		assertTrue("Keygroup started in active", controller.exists(keygroupActivePath + id));
		assertTrue("Replica Node is active", controller.exists(nodeActivePath + newNode.getID()));
		
		ConfigToKeygroupWrapper<ReplicaNodeConfig> wrapper = new ConfigToKeygroupWrapper<ReplicaNodeConfig>(id, newNode);
		@SuppressWarnings("unchecked")
		Response<Boolean> response = (Response<Boolean>) TestUtil.run(Command.KEYGROUP_CONFIG_ADD_REPLICA_NODE, wrapper, sender.getID(), controller);
		
		assertTrue("Replica node properly added", response.getValue());
		assertEquals("Proper response code", ResponseCode.SUCCESS, response.getResponseCode());
		
		readKeygroup(id, sender.getID(), expected, true);
	}
	
	private void addTriggerNode(KeygroupID id, TriggerNodeConfig newNode, KeygroupConfig expected) throws IllegalArgumentException, InterruptedException {
		assertTrue("Keygroup started in active", controller.exists(keygroupActivePath + id));
		assertTrue("Trigger Node is active", controller.exists(nodeActivePath + newNode.getID()));
		
		ConfigToKeygroupWrapper<TriggerNodeConfig> wrapper = new ConfigToKeygroupWrapper<TriggerNodeConfig>(id, newNode);
		@SuppressWarnings("unchecked")
		Response<Boolean> response = (Response<Boolean>) TestUtil.run(Command.KEYGROUP_CONFIG_ADD_TRIGGER_NODE, wrapper, sender.getID(), controller);
		
		assertTrue("Trigger node properly added", response.getValue());
		assertEquals("Proper response code", ResponseCode.SUCCESS, response.getResponseCode());
		
		readKeygroup(id, sender.getID(), expected, true);
	}
	
	private void removeTriggerNode() {
		// TODO Implement
	}
	
	private KeygroupConfig makeStartingKeygroup(ReplicaNodeConfig initNode, Integer intN) {
		String strN = Integer.toString(intN);
		
		// Set up original version of keygroup
		KeygroupID id = new KeygroupID("app_" + strN, "tenant_" + strN, "group_" + strN);
		Set<ClientID> clients = new HashSet<ClientID>();
		clients.add(new ClientID("client_a_" + strN));		
		clients.add(new ClientID("client_b_" + strN));		
		clients.add(new ClientID("client_c_" + strN));
		Set<ReplicaNodeConfig> rNodes = new HashSet<ReplicaNodeConfig>();
		rNodes.add(initNode);
		Set<TriggerNodeConfig> tNodes = new HashSet<TriggerNodeConfig>();
		String secret = "secret_" + strN;
		EncryptionAlgorithm algorithm = EncryptionAlgorithm.AES;
		
		return new KeygroupConfig(id, clients, rNodes, tNodes, secret, algorithm);
	}
	
	private KeygroupConfig makeUpdatedKeygroup(KeygroupConfig kg, Integer intN) {
		String strN = Integer.toString(intN);
		
		// Set up original version of keygroup
		Set<ClientID> clients = new HashSet<ClientID>();
		clients.add(new ClientID("client_d_" + strN));		
		clients.add(new ClientID("client_e_" + strN));		
		clients.add(new ClientID("client_f_" + strN));
		Set<ReplicaNodeConfig> rNodes = new HashSet<ReplicaNodeConfig>();
		Set<TriggerNodeConfig> tNodes = new HashSet<TriggerNodeConfig>();
		String secret = "secret_" + strN;
		EncryptionAlgorithm algorithm = EncryptionAlgorithm.AES;
		
		return new KeygroupConfig(kg.getID(), clients, rNodes, tNodes, secret, algorithm);
	}
	
	private ReplicaNodeConfig makeReplicaNode(NodeID id, Integer timeToLive) throws IllegalArgumentException, InterruptedException {
		assertTrue("Node exists", controller.exists(nodeActivePath + id));
		
		return new ReplicaNodeConfig(id, timeToLive);
	}
	
	private TriggerNodeConfig makeTriggerNode(NodeID id) throws IllegalArgumentException, InterruptedException {
		assertTrue("Node exists", controller.exists(nodeActivePath + id));
		
		return new TriggerNodeConfig(id);
	}
	
	private NodeConfig makeNode(Integer intN) {
		String strN = Integer.toString(intN);
		
		// Set up node
		NodeID id = new NodeID("node_" + strN);
		String key = "my_public_key" + strN;
		EncryptionAlgorithm alg = EncryptionAlgorithm.AES;
		List<String> machines = new ArrayList<String>();
		machines.add("m1_" + strN);
		machines.add("m2_" + strN);
		machines.add("m3_" + strN);
		Integer pPort = 1000 + intN;
		Integer mPort = 2000 + intN;
		Integer rPort = 3000 + intN;
		String loc = "my_location_" + strN;
		String desc = "my_description_" + strN;
		
		return new NodeConfig(id, key, alg, machines, pPort, mPort, rPort, loc, desc);
	}

}
