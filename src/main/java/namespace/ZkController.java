package namespace;

import java.util.List;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooDefs.Ids;

public class ZkController {
	
	private ZooKeeper zk;
	
	public ZkController(ZooKeeper zk) {
		this.zk = zk;
	}
	
	public void addNode(String path, byte[] data) throws KeeperException, InterruptedException {
		zk.create(path, data, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
	}
	
	public byte[] readNode(String path) throws KeeperException, InterruptedException {
		byte[] data = zk.getData(path, true, zk.exists(path, true));
		
		return data;
	}
	
	public void updateNode(String path, byte[] data) throws KeeperException, InterruptedException {
		zk.setData(path, data, zk.exists(path, true).getVersion());
	}
	
	public void deleteNode(String path) throws KeeperException, InterruptedException {
		zk.delete(path, zk.exists(path, true).getVersion());
	}
	
	public List<String> getChildren(String path) throws KeeperException, InterruptedException {
		List<String> znodeList = zk.getChildren(path, true);
		
		return znodeList;
	}
	
	public boolean exists(String path) throws KeeperException, InterruptedException {
		return (zk.exists(path, true) != null) ? true : false;
	}

}
