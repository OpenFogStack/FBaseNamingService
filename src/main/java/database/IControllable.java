package database;

import java.util.List;

import org.apache.zookeeper.KeeperException;

public interface IControllable {

	public void addNode(String path, byte[] data) throws KeeperException, InterruptedException;
	public byte[] readNode(String path) throws KeeperException, InterruptedException;
	public void updateNode(String path, byte[] data) throws KeeperException, InterruptedException;
	public void deleteNode(String path) throws KeeperException, InterruptedException;
	public List<String> getChildren(String path) throws KeeperException, InterruptedException;
	public boolean exists(String path) throws KeeperException, InterruptedException;

}
