package ZkSystem;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.BadArgumentsException;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.apache.zookeeper.KeeperException.NotEmptyException;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.common.PathUtils;

public class ZkTestInterface {
	
	private static ZkController controller;
	public static Scanner scanner = new Scanner(System.in);
	
	private static void printInstructions() {
		System.out.println("Welcome! Valid commands for testing ZooKeeper:\n"
				+ "addNode          - Adds a node to ZooKeeper\n"
				+ "readNode         - Reads a node from ZooKeeper\n"
				+ "updateNode       - Updates a node in ZooKeeper\n"
				+ "deleteNode       - Deletes a node from ZooKeeper\n"
				+ "getChildren      - Returns a list of children nodes\n"
				+ "exists           - Returns if node exists\n"
				+ "help             - Print this instruction dialog\n"
				+ "quit             - Exit program");
	}
	
	private static String getPath() throws IllegalArgumentException {
		System.out.println("Please enter a path:");
		String path = scanner.nextLine();
		
		PathUtils.validatePath(path);
		
		Pattern pattern = Pattern.compile("([/]([A-Za-z0-9][A-Za-z0-9|_|-|(|)|&|/|.]*)*)");
		Matcher matcher = pattern.matcher(path);
		
		if(!matcher.matches()) {
			throw new IllegalArgumentException("Invalid character in path.");
		}
		
		return path;
	}
	
	private static byte[] getData() {
		System.out.println("Please enter JSON data:");
		String input = scanner.nextLine();
		
		return input.getBytes();
	}
	
	private static void addNode(String path, byte[] data) {
		try {
			controller.addNode(path, data);
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
	
	private static byte[] readNode(String path) throws IllegalArgumentException {
		byte[] data = null;
		try {
			data = controller.readNode(path);
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
	
	private static void updateNode(String path, byte[] data) {
		try {
			controller.updateNode(path, data);
		} catch (KeeperException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (NullPointerException e){
			throw new IllegalArgumentException("Argument evalutates to null path");
		}
	}
	
	private static void deleteNode(String path) throws IllegalArgumentException {
		try {
			controller.deleteNode(path);
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
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private static List<String> getChildren(String path) {
		List<String> children = null;
		try {
			children = controller.getChildren(path);
		} catch (KeeperException e) {
			if(e instanceof NoNodeException) {
				throw new IllegalArgumentException("Intermediate node in path '" + path + "' does not exist.");
			} else {
				e.printStackTrace();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		return children;
	}
	
	private static boolean exists(String path) {
		boolean exists = false;
		try {
			exists = controller.exists(path);
		} catch (KeeperException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		return exists;
	}
	
	public static void main(String[] args) {
		ZkConnector connector = new ZkConnector();
		
		ZooKeeper zk = null;
		try {
			zk = connector.connect("localhost");
		} catch (IllegalStateException | IOException | InterruptedException e) {
			e.printStackTrace();
		}
		
		controller = new ZkController(zk);
		
		printInstructions();
		
		String command = null;
		
		do {
			System.out.println("\nPlease enter a command:");
			command = scanner.nextLine();
			
			try {
				switch(command) {
					case "addNode":
						addNode(getPath(), getData());
						break;
						
					case "readNode":
						byte[] data = readNode(getPath());
						for(byte b : data) {System.out.print((char) b);}
						System.out.print("\n");
						break;
						
					case "updateNode":
						updateNode(getPath(), getData());
						break;
					case "deleteNode":
						deleteNode(getPath());
						break;
						
					case "getChildren":
						List<String> children = getChildren(getPath());
						for(String child : children) {System.out.println(child);}
						break;
						
					case "exists":
						boolean exists = exists(getPath());
						System.out.println(exists);
						break;
						
					case "help":
						printInstructions();
						break;
						
					case "quit":
						break;
						
					default:
						System.err.println("Not a valid command. Enter 'help' to see a list of valid commands.");
						break;
				}
			} catch(IllegalArgumentException e) {
				System.err.println("Error: " + e.getMessage());
			}
		} while(!command.equals("quit"));
		
		scanner.close();
		
		System.out.println("\nProgram quit");
	}
	
}
