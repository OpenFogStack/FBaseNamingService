package communication;

import org.apache.log4j.Logger;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;

import communication.AbstractReceiver;
import control.NamingService;
import crypto.CryptoProvider.EncryptionAlgorithm;
import model.JSONable;
import model.config.NodeConfig;
import model.data.NodeID;
import model.messages.Envelope;
import model.messages.Message;
import model.messages.Response;
import namespace.MessageParser;
import namespace.Node;

public class NamespaceReceiver extends AbstractReceiver {
	
	private NamingService ns;

	private static Logger logger = Logger.getLogger(NamespaceReceiver.class.getName());
	
	public NamespaceReceiver(NamingService ns, String address, int port) {
		super(address, port, ZMQ.REP);
		this.ns = ns;
	}

	@Override
	protected void interpreteReceivedEnvelope(Envelope envelope, Socket responseSocket) {
		logger.debug("Interpreting message.");
		// Decrypt with own private key
		envelope.getMessage().decryptFields(ns.configuration.getPrivateKey(), EncryptionAlgorithm.RSA_PUBLIC_ENCRYPT);
		
		// Decrypt with sending node's public key
		// XXX Add error checking
		NodeID senderID = (NodeID) envelope.getConfigID();
		Response<String> r = Node.getInstance().getNodeInfo(ns.controller, senderID);
		NodeConfig sender = JSONable.fromJSON(r.getValue(), NodeConfig.class);
		envelope.getMessage().decryptFields(sender.getPublicKey(), EncryptionAlgorithm.RSA_PRIVATE_ENCRYPT);
		
		// XXX Add response to message
		Response<?> response = MessageParser.runCommand(ns.controller, envelope);
		Message m = new Message();
		m.encryptFields(sender.getPublicKey(), EncryptionAlgorithm.RSA_PUBLIC_ENCRYPT);
		responseSocket.send(JSONable.toJSON(m));
	}

}
