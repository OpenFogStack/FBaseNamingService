package database;

import java.io.IOException;
import java.util.List;

public interface IControllable {

	public void addNode(String path, String data) throws IllegalArgumentException, InterruptedException;
	public String readNode(String path) throws IllegalArgumentException, InterruptedException;
	public void updateNode(String path, String data) throws IllegalArgumentException, InterruptedException;
	
	/**
	 * Deletes the node and its data at the given path recursively.
	 * @param path
	 * @throws IllegalArgumentException
	 * @throws InterruptedException
	 */
	public void deleteNodeRecursive(String path) throws IOException;
	
	public void deleteNode(String path) throws IllegalArgumentException, InterruptedException;
	public List<String> getChildren(String path) throws IllegalArgumentException, InterruptedException;
	public boolean exists(String path) throws IllegalArgumentException, InterruptedException;

}
