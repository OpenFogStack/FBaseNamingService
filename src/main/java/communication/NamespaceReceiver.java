package communication;

import org.apache.log4j.Logger;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;

import communication.AbstractReceiver;
import control.NamingService;
import crypto.CryptoProvider.EncryptionAlgorithm;
import exceptions.FBaseEncryptionException;
import model.JSONable;
import model.config.NodeConfig;
import model.data.NodeID;
import model.messages.Command;
import model.messages.Envelope;
import model.messages.Message;
import model.messages.Response;
import model.messages.ResponseCode;
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
		try {
			logger.debug("Interpreting message.");
			// Decrypt with own private key
			envelope.getMessage().decryptFields(ns.configuration.getPrivateKey(), EncryptionAlgorithm.RSA);
			
			// Verify authenticity
			NodeID senderID = (NodeID) envelope.getConfigID();
			Response<String> r = Node.getInstance().readNode(ns.controller, senderID);
			NodeConfig sender = JSONable.fromJSON(r.getValue(), NodeConfig.class);
			boolean authenticated = envelope.getMessage().verifyMessage(sender.getPublicKey(), EncryptionAlgorithm.RSA);
			
			if (authenticated) {
				logger.debug("Node authenticated for message");
				
				Response<?> response = null;
				if (Command.RESET_NAMING_SERVICE.equals(envelope.getMessage().getCommand())) {
					// process delete request
					if (ns.configuration.isDebugMode()) {
						logger.debug("Resetting namingserivce data");
						try {
							response = new Response<Boolean>(ns.initializeDataStorage(true), ResponseCode.SUCCESS);
						} catch (InterruptedException e) {
							logger.error("Could not wipe storage: " + e.getMessage());
							response = new Response<Boolean>(false, ResponseCode.ERROR_INTERNAL);
							e.printStackTrace();
						}
					} else {
						logger.debug("Received request to reset namingservice data, "
								+ "but not in debug mode");
						response = new Response<Boolean>(false, ResponseCode.ERROR_ILLEGAL_COMMAND);
					}
				} else {
					// normally process command
					response = MessageParser.runCommand(ns.controller, envelope);
				}
				
				Message m = new Message();
				if (response.getValue() != null) {
					m.setContent(response.getValue().toString());
				}
				m.setTextualInfo(response.getResponseCode().toString());
				m.encryptFields(sender.getPublicKey(), EncryptionAlgorithm.RSA);
				logger.debug("Sending response");
				responseSocket.send(JSONable.toJSON(m));
				logger.debug("Response send");
			
			} else {
				logger.debug("Node is not authenticated");
				// TODO add unauthenticated stuff
			}
			
		} catch (FBaseEncryptionException e) {
			logger.error("Decryption failed");
			e.printStackTrace();
		}
	}

}
