package namespace;

import java.io.IOException;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooKeeper;

public class ZkConnector {
	
	private ZooKeeper zk;
	private java.util.concurrent.CountDownLatch connSignal = new java.util.concurrent.CountDownLatch(1);
	
	public ZooKeeper connect(String host) throws IOException, InterruptedException, IllegalStateException {
		zk = new ZooKeeper(host, 5000, new Watcher() {
			public void process(WatchedEvent event) {
				if(event.getState() == KeeperState.SyncConnected) {
					connSignal.countDown();
				}
			}
		});
		
		connSignal.await();
		return zk;
	}
	
	public void close() throws InterruptedException {
		zk.close();
	}

}
