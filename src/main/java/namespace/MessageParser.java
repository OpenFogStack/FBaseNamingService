package namespace;

import crypto.CryptoProvider.EncryptionAlgorithm;
import database.IControllable;
import model.JSONable;
import model.config.ClientConfig;
import model.config.KeygroupConfig;
import model.config.NodeConfig;
import model.config.ReplicaNodeConfig;
import model.config.TriggerNodeConfig;
import model.data.ClientID;
import model.data.KeygroupID;
import model.data.NodeID;
import model.messages.Command;
import model.messages.ConfigToKeygroupWrapper;
import model.messages.CryptoToKeygroupWrapper;
import model.messages.Envelope;
import model.messages.Message;

public class MessageParser {
	
	public static Response<?> runCommand(IControllable controller, Envelope envelope) {
		NodeID senderID = envelope.getNodeID();
		Message message = envelope.getMessage();
		Command command = message.getCommand();
		String content = message.getContent();
		
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
	
	private static Response<?> clientCreate(IControllable controller, String content) {
		ClientConfig client = JSONable.fromJSON(content, ClientConfig.class);
		return Client.registerClient(controller, client);
	}
	
	private static Response<?> clientRead(IControllable controller, String content) {
		ClientID clientID = JSONable.fromJSON(content, ClientID.class);
		return Client.getClientInfo(controller, clientID);
	}
	
	private static Response<?> clientUpdate(IControllable controller, String content) {
		ClientConfig client = JSONable.fromJSON(content, ClientConfig.class);
		return Client.updateClientInfo(controller, client);
	}
	
	private static Response<?> clientDelete(IControllable controller, String content) {
		ClientID clientID = JSONable.fromJSON(content, ClientID.class);
		return Client.removeClient(controller, clientID);
	}
	
	private static Response<?> nodeCreate(IControllable controller, String content) {
		NodeConfig node = JSONable.fromJSON(content, NodeConfig.class);
		return Node.registerNode(controller, node);
	}
	
	private static Response<?> nodeRead(IControllable controller, String content) {
		NodeID nodeID = JSONable.fromJSON(content, NodeID.class);
		return Node.getNodeInfo(controller, nodeID);
	}
	
	private static Response<?> nodeUpdate(IControllable controller, String content, NodeID senderID) {
		NodeConfig node = JSONable.fromJSON(content, NodeConfig.class);
		if(senderID == node.getNodeID()) {
			return Node.updateNodeInfo(controller, node);
		} else {
			return new Response<Boolean>(false, ResponseCode.ERROR_ILLEGAL_COMMAND);
		}
	}
	
	private static Response<?> nodeDelete(IControllable controller, String content, NodeID senderID) {
		NodeID nodeID = JSONable.fromJSON(content, NodeID.class);
		if(senderID == nodeID) {
			return Node.removeNode(controller, nodeID);
		} else {
			return new Response<Boolean>(false, ResponseCode.ERROR_ILLEGAL_COMMAND);
		}
	}
	
	private static Response<?> keygroupCreate(IControllable controller, String content) {
		KeygroupConfig keygroup = JSONable.fromJSON(content, KeygroupConfig.class);
		return Keygroup.createKeygroup(controller, keygroup);
	}
	
	private static Response<?> keygroupAddReplicaNode(IControllable controller, String content, NodeID senderID) {
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
	
	private static Response<?> keygroupAddTriggerNode(IControllable controller, String content, NodeID senderID) {
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
	
	private static Response<?> keygroupRead(IControllable controller, String content, NodeID senderID) {
		KeygroupConfig keygroup = JSONable.fromJSON(content, KeygroupConfig.class);
		if(keygroup.containsReplicaNode(senderID) || keygroup.containsTriggerNode(senderID)) {
			return Keygroup.getKeygroupInfoAuthorized(controller, keygroup.getKeygroupID());
		} else {
			return Keygroup.getKeygroupInfoUnauthorized(controller, keygroup.getKeygroupID());
		}
	}
	
	private static Response<?> keygroupUpdateCrypto(IControllable controller, String content, NodeID senderID) {
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
	
	private static Response<?> keygroupDelete(IControllable controller, String content, NodeID senderID) {
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
