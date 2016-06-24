package org.jvnet.hudson.plugins.fortify360;

import java.io.InputStream;
import java.io.File;
import java.util.Properties;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class PathUtilsTest {

	private static String FORTIFYCLIENT_PATH = "C:\\Program Files\\HP_Fortify\\HP_Fortify_SCA_and_Apps_{version}\\bin\\fortifyclient.bat";

	@BeforeClass
	public static void setUp() throws Exception {
		InputStream in = null;
		Properties prop = new Properties();
		try {
			in = PathUtilsTest.class.getClassLoader().getResourceAsStream("fortifyclient.properties");
			prop.load(in);
			String ver = prop.getProperty("latest.version");
			Assert.assertNotNull(ver);			
			FORTIFYCLIENT_PATH = FORTIFYCLIENT_PATH.replace("{version}", ver);
		} finally {
			if ( null != in ) try { in.close(); } catch ( Exception e ) {}
		}		
	}

	@Test
	public void testLocateBaesnameInPath() throws Exception {
		File[] list = PathUtils.locateBaesnameInPath("fortifyclient");
		Assert.assertNotNull(list);
		Assert.assertEquals(1, list.length);
		File file = new File(FORTIFYCLIENT_PATH);
		Assert.assertEquals(file, list[0]);
	}
}
