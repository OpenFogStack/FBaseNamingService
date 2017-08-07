package database;

import java.util.List;

import org.apache.zookeeper.KeeperException;

public interface IControllable {

	public void addNode(String path, byte[] data) throws IllegalArgumentException, InterruptedException;
	public byte[] readNode(String path) throws IllegalArgumentException, InterruptedException;
	public void updateNode(String path, byte[] data) throws IllegalArgumentException, InterruptedException;
	public void deleteNode(String path) throws IllegalArgumentException, InterruptedException;
	public List<String> getChildren(String path) throws IllegalArgumentException, InterruptedException;
	public boolean exists(String path) throws IllegalArgumentException, InterruptedException;

}
