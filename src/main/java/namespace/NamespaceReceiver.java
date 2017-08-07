package namespace;

import org.apache.log4j.Logger;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;

import communication.AbstractReceiver;
import crypto.CryptoProvider;
import crypto.CryptoProvider.EncryptionAlgorithm;
import model.messages.Envelope;

public class NamespaceReceiver extends AbstractReceiver {

	private static Logger logger = Logger.getLogger(NamespaceReceiver.class.getName());
	
	public NamespaceReceiver(String address, int port, String secret, EncryptionAlgorithm algorithm) {
		super(address, port, secret, algorithm, ZMQ.REP);
	}

	@Override
	protected void interpreteReceivedEnvelope(Envelope envelope, Socket responseSocket) {
		logger.debug("Interpreting message.");
		try {
			Response<?> response = MessageParser.runCommand(controller, envelope);
		
			responseSocket.send(CryptoProvider.encrypt("Message logged.", secret, algorithm));
		} catch (IllegalArgumentException e) {
			logger.warn(e.getMessage());
			responseSocket.send(CryptoProvider.encrypt(e.getMessage(), secret, algorithm));
		}
		
	}

}
