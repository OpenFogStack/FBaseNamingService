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
import model.messages.Response;
import model.messages.ResponseCode;

public class MessageParser {
	
	public static Response<?> runCommand(IControllable controller, Envelope envelope) {
		NodeID senderID = envelope.getNodeID();
		Message message = envelope.getMessage();
		Command command = message.getCommand();
		String content = message.getContent();
		
		switch(command) {
			case CLIENT_CONFIG_CREATE:
				return clientCreate(controller, content);
			case CLIENT_CONFIG_READ:
				return clientRead(controller, content);
			case CLIENT_CONFIG_UPDATE:
				return clientUpdate(controller, content);
			case CLIENT_CONFIG_DELETE:
				return clientDelete(controller, content);
			case NODE_CONFIG_CREATE:
				return nodeCreate(controller, content);
			case NODE_CONFIG_READ:
				return nodeRead(controller, content);
			case NODE_CONFIG_UPDATE:
				return nodeUpdate(controller, content, senderID);
			case NODE_CONFIG_DELETE:
				return nodeDelete(controller, content, senderID);
			case KEYGROUP_CONFIG_CREATE:
				return keygroupCreate(controller, content);
			case KEYGROUP_CONFIG_ADD_REPLICA_NODE:
				return keygroupAddReplicaNode(controller, content, senderID);
			case KEYGROUP_CONFIG_ADD_TRIGGER_NODE:
				return keygroupAddTriggerNode(controller, content, senderID);
			case KEYGROUP_CONFIG_READ:
				return keygroupRead(controller, content, senderID);
			case KEYGROUP_CONFIG_UPDATE_CRYPTO:
				return keygroupUpdateCrypto(controller, content, senderID);
			case KEYGROUP_CONFIG_DELETE:
				return keygroupDelete(controller, content, senderID);
			case KEYGROUP_CONFIG_DELETE_NODE:
				// TODO 
			default:
				return new Response<Boolean>(false, ResponseCode.ERROR_ILLEGAL_COMMAND);
		}
	}
	
	private static Response<?> clientCreate(IControllable controller, String content) {
		ClientConfig client = JSONable.fromJSON(content, ClientConfig.class);
		return Client.getInstance().registerClient(controller, client);
	}
	
	private static Response<?> clientRead(IControllable controller, String content) {
		ClientID clientID = JSONable.fromJSON(content, ClientID.class);
		return Client.getInstance().getClientInfo(controller, clientID);
	}
	
	private static Response<?> clientUpdate(IControllable controller, String content) {
		ClientConfig client = JSONable.fromJSON(content, ClientConfig.class);
		return Client.getInstance().updateClientInfo(controller, client);
	}
	
	private static Response<?> clientDelete(IControllable controller, String content) {
		ClientID clientID = JSONable.fromJSON(content, ClientID.class);
		return Client.getInstance().removeClient(controller, clientID);
	}
	
	private static Response<?> nodeCreate(IControllable controller, String content) {
		NodeConfig node = JSONable.fromJSON(content, NodeConfig.class);
		return Node.getInstance().registerNode(controller, node);
	}
	
	private static Response<?> nodeRead(IControllable controller, String content) {
		NodeID nodeID = JSONable.fromJSON(content, NodeID.class);
		return Node.getInstance().getNodeInfo(controller, nodeID);
	}
	
	private static Response<?> nodeUpdate(IControllable controller, String content, NodeID senderID) {
		NodeConfig node = JSONable.fromJSON(content, NodeConfig.class);
		if(senderID == node.getNodeID()) {
			return Node.getInstance().updateNodeInfo(controller, node);
		} else {
			return new Response<Boolean>(false, ResponseCode.ERROR_ILLEGAL_COMMAND);
		}
	}
	
	private static Response<?> nodeDelete(IControllable controller, String content, NodeID senderID) {
		NodeID nodeID = JSONable.fromJSON(content, NodeID.class);
		if(senderID == nodeID) {
			return Node.getInstance().removeNode(controller, nodeID);
		} else {
			return new Response<Boolean>(false, ResponseCode.ERROR_ILLEGAL_COMMAND);
		}
	}
	
	private static Response<?> keygroupCreate(IControllable controller, String content) {
		KeygroupConfig keygroup = JSONable.fromJSON(content, KeygroupConfig.class);
		return Keygroup.getInstance().createKeygroup(controller, keygroup);
	}
	
	private static Response<?> keygroupAddReplicaNode(IControllable controller, String content, NodeID senderID) {
		// Get KeygroupID and node from JSON via wrapper
		ConfigToKeygroupWrapper wrapper = JSONable.fromJSON(content, ConfigToKeygroupWrapper.class);
		KeygroupID keygroupID = wrapper.getKeygroupID();
		ReplicaNodeConfig replicaNode = (ReplicaNodeConfig) wrapper.getConfig();
		
		// Get keygroup specified from the KeygroupID
		String keygroupJSON = Keygroup.getInstance().getKeygroupInfoAuthorized(controller, keygroupID).getValue();
		KeygroupConfig keygroup = JSONable.fromJSON(keygroupJSON, KeygroupConfig.class);
		
		if(keygroup.containsReplicaNode(senderID)) {
			return Keygroup.getInstance().addReplicaNodeToKeygroup(controller, replicaNode, keygroup.getKeygroupID());
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
		String keygroupJSON = Keygroup.getInstance().getKeygroupInfoAuthorized(controller, keygroupID).getValue();
		KeygroupConfig keygroup = JSONable.fromJSON(keygroupJSON, KeygroupConfig.class);
		
		if(keygroup.containsReplicaNode(senderID)) {
			return Keygroup.getInstance().addTriggerNodeToKeygroup(controller, triggerNode, keygroup.getKeygroupID());
		} else {
			return new Response<Boolean>(false, ResponseCode.ERROR_ILLEGAL_COMMAND);
		}
	}
	
	private static Response<?> keygroupRead(IControllable controller, String content, NodeID senderID) {
		KeygroupConfig keygroup = JSONable.fromJSON(content, KeygroupConfig.class);
		if(keygroup.containsReplicaNode(senderID) || keygroup.containsTriggerNode(senderID)) {
			return Keygroup.getInstance().getKeygroupInfoAuthorized(controller, keygroup.getKeygroupID());
		} else {
			return Keygroup.getInstance().getKeygroupInfoUnauthorized(controller, keygroup.getKeygroupID());
		}
	}
	
	private static Response<?> keygroupUpdateCrypto(IControllable controller, String content, NodeID senderID) {
		// Get KeygroupID and node from JSON via wrapper
		CryptoToKeygroupWrapper wrapper = JSONable.fromJSON(content, CryptoToKeygroupWrapper.class);
		KeygroupID keygroupID = wrapper.getKeygroupID();
		String encryptionSecret =  wrapper.getEncryptionSecret();
		EncryptionAlgorithm encryptionAlgorithm = wrapper.getEncryptionAlgorithm();
		
		// Get keygroup specified from the KeygroupID
		String keygroupJSON = Keygroup.getInstance().getKeygroupInfoAuthorized(controller, keygroupID).getValue();
		KeygroupConfig keygroup = JSONable.fromJSON(keygroupJSON, KeygroupConfig.class);
		
		if(keygroup.containsReplicaNode(senderID)) {
			return Keygroup.getInstance().updateKeygroupCrypto(controller, keygroupID, encryptionSecret, encryptionAlgorithm);
		} else {
			return new Response<Boolean>(false, ResponseCode.ERROR_ILLEGAL_COMMAND);
		}
	}
	
	private static Response<?> keygroupDelete(IControllable controller, String content, NodeID senderID) {
		KeygroupID keygroupID = JSONable.fromJSON(content, KeygroupID.class);
		
		// Get keygroup specified from the KeygroupID
		String keygroupJSON = Keygroup.getInstance().getKeygroupInfoAuthorized(controller, keygroupID).getValue();
		KeygroupConfig keygroup = JSONable.fromJSON(keygroupJSON, KeygroupConfig.class);
		
		if(keygroup.containsReplicaNode(senderID)) {
			return Keygroup.getInstance().removeKeygroup(controller, keygroupID);
		} else {
			return new Response<Boolean>(false, ResponseCode.ERROR_ILLEGAL_COMMAND);
		}
	}
}
