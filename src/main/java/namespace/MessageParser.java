package namespace;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;

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
import model.messages.ConfigIDToKeygroupWrapper;
import model.messages.ConfigToKeygroupWrapper;
import model.messages.CryptoToKeygroupWrapper;
import model.messages.Envelope;
import model.messages.Message;
import model.messages.Response;
import model.messages.ResponseCode;

public class MessageParser {
	
	private static Logger logger = Logger.getLogger(MessageParser.class.getName());
	
	public static Response<?> runCommand(IControllable controller, Envelope envelope) {
		NodeID senderID = envelope.getNodeID();
		Message message = envelope.getMessage();
		Command command = message.getCommand();
		String content = message.getContent();
		
		logger.debug("Running " + command);
		
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
				return keygroupCreate(controller, content, senderID);
			case KEYGROUP_CONFIG_ADD_CLIENT:
				return keygroupAddClient(controller, content, senderID);
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
			case KEYGROUP_CONFIG_DELETE_CLIENT:
				return keygroupDeleteClient(controller, content, senderID);
			case KEYGROUP_CONFIG_DELETE_NODE:
				return keygroupDeleteNode(controller, content, senderID);
			default:
				return new Response<Boolean>(false, ResponseCode.ERROR_ILLEGAL_COMMAND);
		}
	}
	
	private static Response<Boolean> clientCreate(IControllable controller, String content) {
		ClientConfig client = JSONable.fromJSON(content, ClientConfig.class);
		return Client.getInstance().createClient(controller, client);
	}
	
	private static Response<String> clientRead(IControllable controller, String content) {
		ClientID clientID = JSONable.fromJSON(content, ClientID.class);
		return Client.getInstance().readClient(controller, clientID);
	}
	
	private static Response<Boolean> clientUpdate(IControllable controller, String content) {
		ClientConfig client = JSONable.fromJSON(content, ClientConfig.class);
		return Client.getInstance().updateClient(controller, client);
	}
	
	private static Response<Boolean> clientDelete(IControllable controller, String content) {
		ClientID clientID = JSONable.fromJSON(content, ClientID.class);
		return Client.getInstance().deleteClient(controller, clientID);
	}
	
	private static Response<Boolean> nodeCreate(IControllable controller, String content) {
		NodeConfig node = JSONable.fromJSON(content, NodeConfig.class);
		return Node.getInstance().createNode(controller, node);
	}
	
	private static Response<String> nodeRead(IControllable controller, String content) {
		NodeID nodeID = JSONable.fromJSON(content, NodeID.class);
		return Node.getInstance().readNode(controller, nodeID);
	}
	
	private static Response<Boolean> nodeUpdate(IControllable controller, String content, NodeID senderID) {
		NodeConfig node = JSONable.fromJSON(content, NodeConfig.class);
		if(senderID.equals(node.getNodeID())) {
			return Node.getInstance().updateNode(controller, node);
		} else {
			logger.warn("Sending node " + senderID + " not allowed to update " + node.getID());
			return new Response<Boolean>(false, ResponseCode.ERROR_ILLEGAL_COMMAND);
		}
	}
	
	private static Response<Boolean> nodeDelete(IControllable controller, String content, NodeID senderID) {
		NodeID nodeID = JSONable.fromJSON(content, NodeID.class);
		if(senderID.equals(nodeID)) {
			return Node.getInstance().deleteNode(controller, nodeID);
		} else {
			logger.warn("Sending node " + senderID + " not allowed to delete " + nodeID);
			return new Response<Boolean>(false, ResponseCode.ERROR_ILLEGAL_COMMAND);
		}
	}
	
	private static Response<Boolean> keygroupCreate(IControllable controller, String content, NodeID senderID) {
		KeygroupConfig keygroup = JSONable.fromJSON(content, KeygroupConfig.class);
		return Keygroup.getInstance().createKeygroup(controller, keygroup);
	}
	
	private static Response<Boolean> keygroupAddClient(IControllable controller, String content, NodeID senderID) {
		// Get KeygroupID and client from JSON via wrapper
		ConfigIDToKeygroupWrapper<ClientID> wrapper = JSONable.fromJSON(content, new TypeReference<ConfigIDToKeygroupWrapper<ClientID>>() {});
		KeygroupID keygroupID = wrapper.getKeygroupID();
		ClientID client = wrapper.getConfigID();
		
		// Get keygroup specified from the KeygroupID
		String keygroupJSON = Keygroup.getInstance().readKeygroupAuthorized(controller, keygroupID).getValue();
		KeygroupConfig keygroup = JSONable.fromJSON(keygroupJSON, KeygroupConfig.class);
		
		if(keygroup.containsReplicaNode(senderID)) {
			return Keygroup.getInstance().addClient(controller, client, keygroup.getKeygroupID());
		} else {
			logger.warn("Sending node " + senderID + " is not a replica node in " + keygroupID);
			return new Response<Boolean>(false, ResponseCode.ERROR_ILLEGAL_COMMAND);
		}
	}
	
	private static Response<Boolean> keygroupAddReplicaNode(IControllable controller, String content, NodeID senderID) {
		// Get KeygroupID and node from JSON via wrapper
		ConfigToKeygroupWrapper<ReplicaNodeConfig> wrapper = JSONable.fromJSON(content, new TypeReference<ConfigToKeygroupWrapper<ReplicaNodeConfig>>() {});
		KeygroupID keygroupID = wrapper.getKeygroupID();
		ReplicaNodeConfig replicaNode = wrapper.getConfig();
		
		// Get keygroup specified from the KeygroupID
		String keygroupJSON = Keygroup.getInstance().readKeygroupAuthorized(controller, keygroupID).getValue();
		KeygroupConfig keygroup = JSONable.fromJSON(keygroupJSON, KeygroupConfig.class);
		
		if(keygroup.containsReplicaNode(senderID)) {
			return Keygroup.getInstance().addReplicaNode(controller, replicaNode, keygroup.getKeygroupID());
		} else {
			logger.warn("Sending node " + senderID + " is not a replica node in " + keygroupID);
			return new Response<Boolean>(false, ResponseCode.ERROR_ILLEGAL_COMMAND);
		}
	}
	
	private static Response<Boolean> keygroupAddTriggerNode(IControllable controller, String content, NodeID senderID) {
		// Get KeygroupID and node from JSON via wrapper
		ConfigToKeygroupWrapper<TriggerNodeConfig> wrapper = JSONable.fromJSON(content, new TypeReference<ConfigToKeygroupWrapper<TriggerNodeConfig>>() {});
		KeygroupID keygroupID = wrapper.getKeygroupID();
		TriggerNodeConfig triggerNode = wrapper.getConfig();
		
		// Get keygroup specified from the KeygroupID
		String keygroupJSON = Keygroup.getInstance().readKeygroupAuthorized(controller, keygroupID).getValue();
		KeygroupConfig keygroup = JSONable.fromJSON(keygroupJSON, KeygroupConfig.class);
		
		if(keygroup.containsReplicaNode(senderID)) {
			return Keygroup.getInstance().addTriggerNode(controller, triggerNode, keygroup.getKeygroupID());
		} else {
			logger.warn("Sending node " + senderID + " is not a replica node in " + keygroupID);
			return new Response<Boolean>(false, ResponseCode.ERROR_ILLEGAL_COMMAND);
		}
	}
	
	private static Response<String> keygroupRead(IControllable controller, String content, NodeID senderID) {
		KeygroupID keygroupID = JSONable.fromJSON(content, KeygroupID.class);
		return Keygroup.getInstance().readKeygroup(controller, keygroupID, senderID);
	}
	
	private static Response<Boolean> keygroupUpdateCrypto(IControllable controller, String content, NodeID senderID) {
		// Get KeygroupID and node from JSON via wrapper
		CryptoToKeygroupWrapper wrapper = JSONable.fromJSON(content, CryptoToKeygroupWrapper.class);
		KeygroupID keygroupID = wrapper.getKeygroupID();
		String encryptionSecret =  wrapper.getEncryptionSecret();
		EncryptionAlgorithm encryptionAlgorithm = wrapper.getEncryptionAlgorithm();
		
		// Get keygroup specified from the KeygroupID
		String keygroupJSON = Keygroup.getInstance().readKeygroupAuthorized(controller, keygroupID).getValue();
		KeygroupConfig keygroup = JSONable.fromJSON(keygroupJSON, KeygroupConfig.class);
		
		if(keygroup.containsReplicaNode(senderID)) {
			return Keygroup.getInstance().updateKeygroupCrypto(controller, keygroupID, encryptionSecret, encryptionAlgorithm);
		} else {
			logger.warn("Sending node " + senderID + " is not a replica node in " + keygroupID);
			return new Response<Boolean>(false, ResponseCode.ERROR_ILLEGAL_COMMAND);
		}
	}
	
	private static Response<Boolean> keygroupDelete(IControllable controller, String content, NodeID senderID) {
		KeygroupID keygroupID = JSONable.fromJSON(content, KeygroupID.class);
		
		// Get keygroup specified from the KeygroupID
		String keygroupJSON = Keygroup.getInstance().readKeygroupAuthorized(controller, keygroupID).getValue();
		KeygroupConfig keygroup = JSONable.fromJSON(keygroupJSON, KeygroupConfig.class);
		
		if(keygroup.containsReplicaNode(senderID)) {
			return Keygroup.getInstance().removeKeygroup(controller, keygroupID);
		} else {
			logger.warn("Sending node " + senderID + " is not a replica node in " + keygroupID);
			return new Response<Boolean>(false, ResponseCode.ERROR_ILLEGAL_COMMAND);
		}
	}
	
	private static Response<Boolean> keygroupDeleteClient(IControllable controller, String content, NodeID senderID) {
		// Get KeygroupID and ClientID from JSON via wrapper
		ConfigIDToKeygroupWrapper<ClientID> wrapper = JSONable.fromJSON(content, new TypeReference<ConfigIDToKeygroupWrapper<ClientID>>() {});
		KeygroupID keygroupID = wrapper.getKeygroupID();
		ClientID client = wrapper.getConfigID();
		
		// Get keygroup specified from the KeygroupID
		String keygroupJSON = Keygroup.getInstance().readKeygroupAuthorized(controller, keygroupID).getValue();
		KeygroupConfig keygroup = JSONable.fromJSON(keygroupJSON, KeygroupConfig.class);
		
		if(keygroup.containsReplicaNode(senderID)) {
			return Keygroup.getInstance().removeClient(controller, client, keygroup.getKeygroupID());
		} else {
			logger.warn("Sending node " + senderID + " is not a replica node in " + keygroupID);
			return new Response<Boolean>(false, ResponseCode.ERROR_ILLEGAL_COMMAND);
		}
	}
	
	private static Response<Boolean> keygroupDeleteNode(IControllable controller, String content, NodeID senderID) {
		// Get KeygroupID and node from JSON via wrapper
		ConfigIDToKeygroupWrapper<NodeID> wrapper = JSONable.fromJSON(content, new TypeReference<ConfigIDToKeygroupWrapper<NodeID>>() {});
		KeygroupID keygroupID = wrapper.getKeygroupID();
		NodeID node = wrapper.getConfigID();
		
		// Get keygroup specified from the KeygroupID
		String keygroupJSON = Keygroup.getInstance().readKeygroupAuthorized(controller, keygroupID).getValue();
		KeygroupConfig keygroup = JSONable.fromJSON(keygroupJSON, KeygroupConfig.class);
		
		if(keygroup.containsNode(senderID)) {
			return Keygroup.getInstance().deleteNode(controller, node, keygroup.getKeygroupID());
		} else {
			logger.warn("Sending node " + senderID + " is not a node in " + keygroupID);
			return new Response<Boolean>(false, ResponseCode.ERROR_ILLEGAL_COMMAND);
		}
	}
}
