package database.localfiles;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import database.IControllable;

public class LocalFileController implements IControllable {
	private File rootDir;
	private String dataFileName = "/data.txt";
	
	public LocalFileController(File rootDir) {
		this.rootDir = rootDir;
	}
	
	@Override
	public void addNode(String path, String data) throws IllegalArgumentException {
		File f = new File(rootDir, path);
		if(f.exists()) {
			throw new IllegalArgumentException("Path '" + path + "' already exists.");
		}
		
		f.mkdirs();
		
		f = new File(f, dataFileName);
		try {
			PrintWriter writer = new PrintWriter(f);
			writer.println(data);
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String readNode(String path) throws IllegalArgumentException {
		File f = new File(rootDir, path + dataFileName);
		
		String content = null;
		
		try {
			FileReader reader = new FileReader(f);
	        char[] chars = new char[(int) f.length()];
	        reader.read(chars);
	        content = new String(chars);
	        reader.close();
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException("Path '" + path + "' does not exist");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return content;
	}

	@Override
	public void updateNode(String path, String data) throws IllegalArgumentException {
		File f = new File(rootDir, path);
		if(!f.exists()) {
			throw new IllegalArgumentException("Path '" + path + "' doesn't exist.");
		}
		
		f = new File(f, dataFileName);
		try {
			PrintWriter writer = new PrintWriter(f);
			writer.println(data);
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
	}

	@Override
	public void deleteNode(String path) {
		File file = new File(rootDir, path + dataFileName);
		File dir = new File(rootDir, path);
		
		if(dir.exists() == false || file.exists() == false) {
			throw new IllegalArgumentException("Path '" + path + "' doesn't exist.");
		}
		
		if(getChildren(path).isEmpty() == false) {
			throw new IllegalArgumentException("Directory not empty for '" + path + "'");
		}
		
		file.delete();
		dir.delete();
	}

	@Override
	public List<String> getChildren(String path) {
		File f = new File(rootDir, path);
		File[] fileList = f.listFiles(File::isDirectory);
		
		List<String> stringList = new ArrayList<String>();
		for(File i : fileList) {
			stringList.add(i.getName());
		}
		
		return stringList;
	}

	@Override
	public boolean exists(String path) {
		File f = new File(rootDir, path);
		return f.exists();
	}
	
}
