package org.jvnet.hudson.plugins.fortify360;

import static org.junit.Assert.*;
import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import junit.framework.Assert;

import org.apache.commons.beanutils.MethodUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class FortifyClientClassLoaderTest {
	
	private static final String F360_PATH = "C:\\Program Files\\Fortify Software\\Fortify 360 v2.6.5\\Core\\lib";
	private static final String F360_URL = "http://localhost:8180/f360/fm-ws/services";
	private static final String F360_TOKEN = "9aa190e1-9f92-4aed-9438-a1a48d7f9153";
	
	@Test
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
