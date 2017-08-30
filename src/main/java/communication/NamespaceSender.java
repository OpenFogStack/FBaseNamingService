package communication;

import org.apache.log4j.Logger;
import org.zeromq.ZMQ;

import communication.AbstractSender;
import control.NamingService;
import crypto.CryptoProvider.EncryptionAlgorithm;
import exceptions.FBaseEncryptionException;
import model.JSONable;
import model.messages.Envelope;
import model.messages.Message;

/**
 * Sends requests to designated receivers.
 * 
 * @author jonathanhasenburg
 *
 */
public class NamespaceSender extends AbstractSender {
	
	private NamingService ns;
	private String nodePublicKey;
	private String nodePrivateKey;
	
	private static Logger logger = Logger.getLogger(NamespaceSender.class.getName());

	/**
	 * Initializes the NamespaceSender, it then can be used without further modifications.
	 */
	public NamespaceSender(NamingService ns, String address, int port, String secret, EncryptionAlgorithm algorithm) {
		super(address, port, ZMQ.REQ);
		this.ns = ns;
	}

	/**
	 * Sends an envelope to the specified address.
	 * 
	 * @param envelope
	 * @return the response
	 */
	@Override
	public String send(Envelope envelope, String secret, EncryptionAlgorithm algorithm) {
		try {
			logger.debug("Sending envelope with keygroup " + envelope.getNodeID());
			
			envelope.getMessage().signMessage(nodePrivateKey, EncryptionAlgorithm.RSA);
			envelope.getMessage().encryptFields(ns.configuration.getPublicKey(), EncryptionAlgorithm.RSA);
			
			sender.sendMore(envelope.getNodeID().getID());
			sender.send(JSONable.toJSON(envelope.getMessage()));
			
			logger.debug("Waiting for reply");
			
			Message m = JSONable.fromJSON(sender.recvStr(), Message.class);
			m.decryptFields(nodePrivateKey, EncryptionAlgorithm.RSA);
			
			return m.getContent();
		} catch (FBaseEncryptionException e) {
			logger.error("Error signing message", e);
			return null;
		}
	}

	public void setPublicKey(String publicKey) {
		this.nodePublicKey = publicKey;
	}
	
	public void setPrivateKey(String privateKey) {
		this.nodePrivateKey = privateKey;
	}
}
