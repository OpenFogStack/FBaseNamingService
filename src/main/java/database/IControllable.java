package database;

import java.util.List;

public interface IControllable {

	public void addNode(String path, String data) throws IllegalArgumentException, InterruptedException;
	public String readNode(String path) throws IllegalArgumentException, InterruptedException;
	public void updateNode(String path, String data) throws IllegalArgumentException, InterruptedException;
	public void deleteNode(String path) throws IllegalArgumentException, InterruptedException;
	public List<String> getChildren(String path) throws IllegalArgumentException, InterruptedException;
	public boolean exists(String path) throws IllegalArgumentException, InterruptedException;

}
