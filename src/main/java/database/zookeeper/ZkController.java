package database.zookeeper;

import java.util.List;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.KeeperException.BadArgumentsException;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.apache.zookeeper.KeeperException.NotEmptyException;
import org.apache.zookeeper.ZooDefs.Ids;

import database.IControllable;

public class ZkController implements IControllable {
	
	private ZooKeeper zk;
	
	public ZkController(ZooKeeper zk) {
		this.zk = zk;
	}
	
	@Override
	public void addNode(String path, byte[] data) throws IllegalArgumentException {
		try {
			zk.create(path, data, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
		} catch (KeeperException e) {
			if(e instanceof NodeExistsException) {
				throw new IllegalArgumentException("Path '" + path + "' already exists.");
			} else if(e instanceof NoNodeException) {
				throw new IllegalArgumentException("Intermediate node in path '" + path + "' does not exist.");
			} else {
				e.printStackTrace();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public byte[] readNode(String path) throws IllegalArgumentException {
		byte[] data = null;
		try {
			data = zk.getData(path, true, zk.exists(path, true));
		} catch (KeeperException e) {
			if(e instanceof NoNodeException) {
				throw new IllegalArgumentException("Path '" + path + "' does not exist");
			} else {
				e.printStackTrace();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		return data;
	}
	
	@Override
	public void updateNode(String path, byte[] data) throws IllegalArgumentException {
		try {
			zk.setData(path, data, zk.exists(path, true).getVersion());
		} catch (KeeperException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (NullPointerException e){
			throw new IllegalArgumentException("Argument evalutates to null path");
		}
	}
	
	@Override
	public void deleteNode(String path) throws IllegalArgumentException {
		try {
			zk.delete(path, zk.exists(path, true).getVersion());
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (KeeperException e) {
			if(e instanceof NotEmptyException) {
				throw new IllegalArgumentException("Directory not empty for '" + path + "'");
			} else if(e instanceof BadArgumentsException) {
				throw new IllegalArgumentException("Invalid delete path");
			} else {
				e.printStackTrace();
			}
		} catch (NullPointerException e){
			throw new IllegalArgumentException("Argument evalutates to null path");
		} 
	}
	
	@Override
	public List<String> getChildren(String path) throws IllegalArgumentException {
		List<String> znodeList = null;
		try {
			znodeList = zk.getChildren(path, true);
		} catch (KeeperException e) {
			if(e instanceof NoNodeException) {
				throw new IllegalArgumentException("Intermediate node in path '" + path + "' does not exist.");
			} else {
				e.printStackTrace();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		return znodeList;
	}
	
	@Override
	public boolean exists(String path) {
		try {
			return (zk.exists(path, true) != null) ? true : false;
		} catch (KeeperException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		return false;
	}

}
