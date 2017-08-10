package namespace;

import java.io.File;

import database.IControllable;
import model.JSONable;
import model.data.NodeID;
import model.messages.Command;
import model.messages.Envelope;
import model.messages.Message;
import model.messages.Response;

public class TestUtil {
	static void deleteDir(File file) {
		File[] contents = file.listFiles();
		if (contents != null) {
			for (File f : contents) {
				deleteDir(f);
			}
		}
		file.delete();
	}

	static Response<?> run(Command command, JSONable j, NodeID senderNode,
			IControllable controller) {
		Message message = new Message(command, JSONable.toJSON(j));
		Envelope envelope = new Envelope(senderNode, message);

		return MessageParser.runCommand(controller, envelope);
	}
}
