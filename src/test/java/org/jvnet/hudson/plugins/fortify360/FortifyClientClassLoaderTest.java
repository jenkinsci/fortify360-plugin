package org.jvnet.hudson.plugins.fortify360;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.util.Map;
import java.util.Properties;

import junit.framework.Assert;

import org.apache.commons.beanutils.MethodUtils;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;


public class FortifyClientClassLoaderTest {

	@Rule
	public JenkinsRule jenkinsRule = new JenkinsRule();

	private static String F360_PATH = "C:\\Program Files\\HP_Fortify\\HP_Fortify_SCA_and_Apps_{version}\\Core\\lib";
	private static String F360_URL = "http://localhost:8180/ssc/fm-ws/services";
	private static String F360_TOKEN = null;

	@WithoutJenkins
	@BeforeClass
	public static void setUp() throws Exception {
		InputStream in = null;
		Properties prop = new Properties();
		try {
			in = FortifyClientClassLoaderTest.class.getClassLoader().getResourceAsStream("fortifyclient.properties");
			prop.load(in);
			String ver = prop.getProperty("latest.version");
			Assert.assertNotNull(ver);			
			F360_PATH = F360_PATH.replace("{version}", ver);
			String contextPath = prop.getProperty("contextPath-" + ver, "f360");
			F360_URL = F360_URL.replace("f360", contextPath);
			F360_TOKEN = prop.getProperty("token-" + ver);
			Assert.assertNotNull(F360_TOKEN);	
		} finally {
			if ( null != in ) try { in.close(); } catch ( Exception e ) {}
		}	
	}

	@Test
	@WithoutJenkins
	public void testFindWSClientPath() throws IOException {
		String path = FortifyClientClassLoader.findWSClientPath();
		Assert.assertNotNull(path);
		File file = new File(path);
		Assert.assertEquals(F360_PATH, file.getCanonicalPath());
	}

	@Test
	public void testGetInstance() throws Exception {
		FortifyClientClassLoader loader = FortifyClientClassLoader.getInstance(null, null, System.out);
		loader.bindCurrentThread();
		try {
			Object fortifyclient = loader.loadClass("org.jvnet.hudson.plugins.fortify360.fortifyclient.FortifyClient").newInstance();
			Assert.assertNotNull(fortifyclient);
		} finally {
			loader.unbindCurrentThread();
		}
	}

	@Test
	@Ignore(value="Fortify Server required for test")
	public void testGetProjectList() throws Exception {
		FortifyClientClassLoader loader = FortifyClientClassLoader.getInstance(null, null, System.out);
		loader.bindCurrentThread();
		try {
			Object fortifyclient = loader.loadClass("org.jvnet.hudson.plugins.fortify360.fortifyclient.FortifyClient").newInstance();
			Assert.assertNotNull(fortifyclient);
			MethodUtils.invokeMethod(fortifyclient, "init", new String[] {F360_URL, F360_TOKEN});
			Object out = MethodUtils.invokeMethod(fortifyclient, "getProjectList", null);
			Map<String, Long> map = (Map<String, Long>)out;
			// as long as it is not null and map.size > 0, no exception thrown, that's fine
			Assert.assertNotNull(map);
			Assert.assertTrue(map.size() > 0);
			for(String s : map.keySet()) {
				System.out.println("Project [: " + map.get(s) + "]: " + s);
			}
		} catch ( Exception e ) {
			// if due to connection error, probably f360 server is not started up
			if ( containsRootCause(e, ConnectException.class) ) {
				// ignore it
			} else {
				throw e;
			}
		} finally {
			loader.unbindCurrentThread();
		}
	}
	
	private boolean containsRootCause(Throwable t, Class c) {
		while ( null != t ) {
			t = t.getCause();
			if ( null != t && c.isInstance(t) ) return true;
		}
		return  false;
	}	
}
