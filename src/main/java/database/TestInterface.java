package database;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.common.PathUtils;

import database.localfiles.LocalFileController;
import database.zookeeper.ZkConnector;
import database.zookeeper.ZkController;

public class TestInterface {

	private static IControllable controller;
	public static Scanner scanner = new Scanner(System.in);

	private enum FileSystem {
		LOCAL, ZOOKEEPER
	}

	private static void chooseSystem() {
		System.out.println("Welcome! Please choose a system to use:\n" + "1) Local File System\n"
				+ "2) ZooKeeper");

		System.out.print("\nType the number of the system to use: ");
		boolean error = false;
		FileSystem choice = null;
		do {
			error = false;

			try {
				int input = Integer.parseInt(scanner.nextLine());

				switch (input) {
				case 1:
					choice = FileSystem.LOCAL;
					break;
				case 2:
					choice = FileSystem.ZOOKEEPER;
					break;
				default:
					error = true;
				}

			} catch (NumberFormatException e) {
				error = true;
			}

			if (error == true) {
				System.out.print(
						"\nNumber not recognized. Please enter the number of the system to use: ");
			}

			System.out.print("\n");
		} while (error);

		switch (choice) {
		case LOCAL:
			useLocalFiles();
			break;
		case ZOOKEEPER:
			useZooKeeper();
			break;
		default:
			System.exit(0);
		}
	}

	private static void printInstructions() {
		System.out.println("Welcome! Valid commands for testing the system:\n"
				+ "addNode          - Adds a node to the system\n"
				+ "readNode         - Reads a node from the system\n"
				+ "updateNode       - Updates a node in the system\n"
				+ "deleteNode       - Deletes a node from the system\n"
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

		if (!matcher.matches()) {
			throw new IllegalArgumentException("Invalid character in path.");
		}

		return path;
	}

	private static String getData() {
		System.out.println("Please enter JSON data:");
		String input = scanner.nextLine();

		return input;
	}

	private static void useLocalFiles() {
		File f = null;
		boolean error = false;
		do {
			error = false;
			System.out.print("Please enter the starting file path: ");
			String input = scanner.nextLine();
			System.out.print("\n");
			f = new File(input);

			if (f.exists() == false) {
				error = true;
				System.err.println("Invalid path");
			}
		} while (error);

		controller = new LocalFileController(f);
	}

	private static void useZooKeeper() {
		ZkConnector connector = new ZkConnector();

		ZooKeeper zk = null;
		try {
			zk = connector.connect("localhost");
		} catch (IllegalStateException | IOException | InterruptedException e) {
			e.printStackTrace();
		}

		controller = new ZkController(zk);
	}

	public static void main(String[] args) {
		chooseSystem();
		printInstructions();

		String command = null;

		do {
			System.out.println("\nPlease enter a command:");
			command = scanner.nextLine();

			try {
				switch (command) {
				case "addNode":
					controller.addNode(getPath(), getData());
					break;

				case "readNode":
					String data = controller.readNode(getPath());
					System.out.println(data);
					break;

				case "updateNode":
					controller.updateNode(getPath(), getData());
					break;
				case "deleteNode":
					controller.deleteNode(getPath());
					break;

				case "getChildren":
					List<String> children = controller.getChildren(getPath());
					for (String child : children) {
						System.out.println(child);
					}
					break;

				case "exists":
					boolean exists = controller.exists(getPath());
					System.out.println(exists);
					break;

				case "help":
					printInstructions();
					break;

				case "quit":
					break;

				default:
					System.err.println(
							"Not a valid command. Enter 'help' to see a list of valid commands.");
					break;
				}
			} catch (IllegalArgumentException e) {
				System.err.println("Error: " + e.getMessage());
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} while (!command.equals("quit"));

		scanner.close();

		System.out.println("\nProgram quit");
	}

}
