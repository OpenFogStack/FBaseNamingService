package namespace;

import static org.junit.Assert.fail;

import java.io.File;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import control.Configuration;
import control.NamingService;
import database.IControllable;
import database.localfiles.LocalFileController;
import model.data.ConfigID;

public class SystemEntityTest {

	private static IControllable controller;
	private static NamingService ns;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		Configuration configuration = new Configuration();
		controller = new LocalFileController(new File(configuration.getRoot()), configuration.getFolderSeparator());
		ns = new NamingService(controller, configuration);
	}

	@After
	public void tearDown() throws Exception {
		// Wait required so that all files are fully created before deleting
		java.util.concurrent.TimeUnit.SECONDS.sleep(5);
		
		Configuration configuration = new Configuration();
		File root = new File(configuration.getRoot());
		TestUtil.deleteDir(new File(root, "client"));
		TestUtil.deleteDir(new File(root, "node"));
		TestUtil.deleteDir(new File(root, "keygroup"));
	}
	
	//@Test
	public void test() {
		fail("Not yet implemented");
	}
	
	private class SubEntityID extends ConfigID {
		private String subEntityID = null;
		
		public SubEntityID(String id) {
			this.subEntityID = id;
		}
		
		public String getID() {
			return subEntityID;
		}
	}
	
	private class SubEntity extends SystemEntity {
		public SubEntity() {
			super("test");
		}
	}

}
