package communication;

import org.apache.log4j.Logger;
import org.zeromq.ZMQ;

import communication.AbstractSender;
import crypto.CryptoProvider;
import crypto.CryptoProvider.EncryptionAlgorithm;
import model.JSONable;
import model.messages.Envelope;

/**
 * Sends requests to designated receivers.
 * 
 * @author jonathanhasenburg
 *
 */
public class NamespaceSender extends AbstractSender {

	private static Logger logger = Logger.getLogger(NamespaceSender.class.getName());

	/**
	 * Initializes the NamespaceSender, it then can be used without further modifications.
	 */
	public NamespaceSender(String address, int port, String secret, EncryptionAlgorithm algorithm) {
		super(address, port, ZMQ.REQ);
	}

	/**
	 * Sends an envelope to the specified address.
	 * 
	 * @param envelope
	 * @return the response
	 */
	@Override
	public String send(Envelope envelope, String secret, EncryptionAlgorithm algorithm) {
		logger.debug("Sending envelope with keygroup " + envelope.getKeygroupID());
		sender.sendMore(envelope.getKeygroupID().getID());
		sender.send(
				CryptoProvider.encrypt(JSONable.toJSON(envelope.getMessage()), secret, algorithm));
		return CryptoProvider.decrypt(sender.recvStr(), secret, algorithm);
	}

}
