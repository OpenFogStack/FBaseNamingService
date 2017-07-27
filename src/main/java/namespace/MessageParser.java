package namespace;

import ZkSystem.ZkController;
import crypto.CryptoProvider;
import crypto.CryptoProvider.EncryptionAlgorithm;
import model.JSONable;
import model.config.ClientConfig;
import model.config.KeygroupConfig;
import model.config.NodeConfig;
import model.config.ReplicaNodeConfig;
import model.config.TriggerNodeConfig;
import model.data.ClientID;
import model.data.KeygroupID;
import model.data.NodeID;
import model.messages.namingservice.Command;
import model.messages.namingservice.ConfigToKeygroupWrapper;
import model.messages.namingservice.CryptoToKeygroupWrapper;
import model.messages.namingservice.Envelope;
import model.messages.namingservice.Message;

public class MessageParser {
	
	public static Envelope decryptEnvelope(ZkController controller, Envelope encrypted) {
		// TODO Error handling (bad encryption, node doesn't exist, data format problems, etc.)
		NodeID senderID = encrypted.getSenderID();
		Message message = encrypted.getMessage();
		
		String nodeInfo = Node.getNodeInfo(controller, senderID).getValue();
		NodeConfig node = JSONable.fromJSON(nodeInfo, NodeConfig.class);
		String nodeKey = node.getPublicKey();
		
		Command command = Command.valueOf(CryptoProvider.decrypt(message.getCommand().toString(), nodeKey, CryptoProvider.EncryptionAlgorithm.AES)); // XXX Figure out enum decryption
		String content = CryptoProvider.decrypt(message.getContent(), nodeKey, CryptoProvider.EncryptionAlgorithm.AES);
		
		Envelope decrypted = new Envelope(senderID, new Message(command, content));
		
		return decrypted;
	}
	
	public static Envelope encryptEnvelope() {
		return null; // XXX
	}
	
	public static Response<?> runCommand(ZkController controller, Envelope envelope) {
		NodeID senderID = envelope.getSenderID();
		Command command = envelope.getMessage().getCommand();
		String content = envelope.getMessage().getContent();
		
		switch(command) {
			case CLIENT_CREATE:
				return clientCreate(controller, content);
			case CLIENT_READ:
				return clientRead(controller, content);
			case CLIENT_UPDATE:
				return clientUpdate(controller, content);
			case CLIENT_DELETE:
				return clientDelete(controller, content);
			case NODE_CREATE:
				return nodeCreate(controller, content);
			case NODE_READ:
				return nodeRead(controller, content);
			case NODE_UPDATE:
				return nodeUpdate(controller, content, senderID);
			case NODE_DELETE:
				return nodeDelete(controller, content, senderID);
			case KEYGROUP_CREATE:
				return keygroupCreate(controller, content);
			case KEYGROUP_ADD_REPLICA_NODE:
				return keygroupAddReplicaNode(controller, content, senderID);
			case KEYGROUP_ADD_TRIGGER_NODE:
				return keygroupAddTriggerNode(controller, content, senderID);
			case KEYGROUP_READ:
				return keygroupRead(controller, content, senderID);
			case KEYGROUP_UPDATE_CRYPTO:
				return keygroupUpdateCrypto(controller, content, senderID);
			case KEYGROUP_DELETE:
				return keygroupDelete(controller, content, senderID);
			default:
				return new Response<Boolean>(false, ResponseCode.ERROR_ILLEGAL_COMMAND);
		}
	}
	
	private static Response<?> clientCreate(ZkController controller, String content) {
		ClientConfig client = JSONable.fromJSON(content, ClientConfig.class);
		return Client.registerClient(controller, client);
	}
	
	private static Response<?> clientRead(ZkController controller, String content) {
		ClientID clientID = JSONable.fromJSON(content, ClientID.class);
		return Client.getClientInfo(controller, clientID);
	}
	
	private static Response<?> clientUpdate(ZkController controller, String content) {
		ClientConfig client = JSONable.fromJSON(content, ClientConfig.class);
		return Client.updateClientInfo(controller, client);
	}
	
	private static Response<?> clientDelete(ZkController controller, String content) {
		ClientID clientID = JSONable.fromJSON(content, ClientID.class);
		return Client.removeClient(controller, clientID);
	}
	
	private static Response<?> nodeCreate(ZkController controller, String content) {
		NodeConfig node = JSONable.fromJSON(content, NodeConfig.class);
		return Node.registerNode(controller, node);
	}
	
	private static Response<?> nodeRead(ZkController controller, String content) {
		NodeID nodeID = JSONable.fromJSON(content, NodeID.class);
		return Node.getNodeInfo(controller, nodeID);
	}
	
	private static Response<?> nodeUpdate(ZkController controller, String content, NodeID senderID) {
		NodeConfig node = JSONable.fromJSON(content, NodeConfig.class);
		if(senderID == node.getNodeID()) {
			return Node.updateNodeInfo(controller, node);
		} else {
			return new Response<Boolean>(false, ResponseCode.ERROR_ILLEGAL_COMMAND);
		}
	}
	
	private static Response<?> nodeDelete(ZkController controller, String content, NodeID senderID) {
		NodeID nodeID = JSONable.fromJSON(content, NodeID.class);
		if(senderID == nodeID) {
			return Node.removeNode(controller, nodeID);
		} else {
			return new Response<Boolean>(false, ResponseCode.ERROR_ILLEGAL_COMMAND);
		}
	}
	
	private static Response<?> keygroupCreate(ZkController controller, String content) {
		KeygroupConfig keygroup = JSONable.fromJSON(content, KeygroupConfig.class);
		return Keygroup.createKeygroup(controller, keygroup);
	}
	
	private static Response<?> keygroupAddReplicaNode(ZkController controller, String content, NodeID senderID) {
		// Get KeygroupID and node from JSON via wrapper
		ConfigToKeygroupWrapper wrapper = JSONable.fromJSON(content, ConfigToKeygroupWrapper.class);
		KeygroupID keygroupID = wrapper.getKeygroupID();
		ReplicaNodeConfig replicaNode = (ReplicaNodeConfig) wrapper.getConfig();
		
		// Get keygroup specified from the KeygroupID
		String keygroupJSON = Keygroup.getKeygroupInfoAuthorized(controller, keygroupID).getValue();
		KeygroupConfig keygroup = JSONable.fromJSON(keygroupJSON, KeygroupConfig.class);
		
		if(keygroup.containsReplicaNode(senderID)) {
			return Keygroup.addReplicaNodeToKeygroup(controller, replicaNode, keygroup.getKeygroupID());
		} else {
			return new Response<Boolean>(false, ResponseCode.ERROR_ILLEGAL_COMMAND);
		}
	}
	
	private static Response<?> keygroupAddTriggerNode(ZkController controller, String content, NodeID senderID) {
		// Get KeygroupID and node from JSON via wrapper
		ConfigToKeygroupWrapper wrapper = JSONable.fromJSON(content, ConfigToKeygroupWrapper.class);
		KeygroupID keygroupID = wrapper.getKeygroupID();
		TriggerNodeConfig triggerNode = (TriggerNodeConfig) wrapper.getConfig();
		
		// Get keygroup specified from the KeygroupID
		String keygroupJSON = Keygroup.getKeygroupInfoAuthorized(controller, keygroupID).getValue();
		KeygroupConfig keygroup = JSONable.fromJSON(keygroupJSON, KeygroupConfig.class);
		
		if(keygroup.containsReplicaNode(senderID)) {
			return Keygroup.addTriggerNodeToKeygroup(controller, triggerNode, keygroup.getKeygroupID());
		} else {
			return new Response<Boolean>(false, ResponseCode.ERROR_ILLEGAL_COMMAND);
		}
	}
	
	private static Response<?> keygroupRead(ZkController controller, String content, NodeID senderID) {
		KeygroupConfig keygroup = JSONable.fromJSON(content, KeygroupConfig.class);
		if(keygroup.containsReplicaNode(senderID) || keygroup.containsTriggerNode(senderID)) {
			return Keygroup.getKeygroupInfoAuthorized(controller, keygroup.getKeygroupID());
		} else {
			return Keygroup.getKeygroupInfoUnauthorized(controller, keygroup.getKeygroupID());
		}
	}
	
	private static Response<?> keygroupUpdateCrypto(ZkController controller, String content, NodeID senderID) {
		// Get KeygroupID and node from JSON via wrapper
		CryptoToKeygroupWrapper wrapper = JSONable.fromJSON(content, CryptoToKeygroupWrapper.class);
		KeygroupID keygroupID = wrapper.getKeygroupID();
		String encryptionSecret =  wrapper.getEncryptionSecret();
		EncryptionAlgorithm encryptionAlgorithm = wrapper.getEncryptionAlgorithm();
		
		// Get keygroup specified from the KeygroupID
		String keygroupJSON = Keygroup.getKeygroupInfoAuthorized(controller, keygroupID).getValue();
		KeygroupConfig keygroup = JSONable.fromJSON(keygroupJSON, KeygroupConfig.class);
		
		if(keygroup.containsReplicaNode(senderID)) {
			return Keygroup.updateKeygroupCrypto(controller, keygroupID, encryptionSecret, encryptionAlgorithm);
		} else {
			return new Response<Boolean>(false, ResponseCode.ERROR_ILLEGAL_COMMAND);
		}
	}
	
	private static Response<?> keygroupDelete(ZkController controller, String content, NodeID senderID) {
		KeygroupID keygroupID = JSONable.fromJSON(content, KeygroupID.class);
		
		// Get keygroup specified from the KeygroupID
		String keygroupJSON = Keygroup.getKeygroupInfoAuthorized(controller, keygroupID).getValue();
		KeygroupConfig keygroup = JSONable.fromJSON(keygroupJSON, KeygroupConfig.class);
		
		if(keygroup.containsReplicaNode(senderID)) {
			return Keygroup.removeKeygroup(controller, keygroupID);
		} else {
			return new Response<Boolean>(false, ResponseCode.ERROR_ILLEGAL_COMMAND);
		}
	}
}
