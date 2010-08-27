package org.jvnet.hudson.plugins.fortify360;

import static org.junit.Assert.*;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import junit.framework.Assert;

import org.apache.commons.beanutils.MethodUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class FortifyClientClassLoaderTest {
	
	@Test
	public void testMd5() throws NoSuchAlgorithmException, IOException {
		//aeb7869405a91b3f9d3e173d4b669f66 *test1.fpr
		//e429d498f0ce4a5f012b743cad78062c *webgoat_57.fpr
		//eb1790a10e2311757adfcff24485b237 *WebGoat_Audited.fpr
		String[] files = {"test1.fpr", "webgoat_57.fpr", "WebGoat_Audited.fpr"};
		String[] md5s = {"aeb7869405a91b3f9d3e173d4b669f66", "e429d498f0ce4a5f012b743cad78062c", "eb1790a10e2311757adfcff24485b237"};
		
		for(int i=0; i<3; i++) {
			File file = TestUtils.resourceToFile(files[i]);
			String md5 = FortifyClientClassLoader.md5(file);
			Assert.assertEquals(md5s[i], md5);
		}
	}
	
	@Test
	public void testFindWSClientPath() throws IOException {
		String path = FortifyClientClassLoader.findWSClientPath();
		Assert.assertNotNull(path);
		File file = new File(path);
		Assert.assertEquals("C:\\Program Files\\Fortify Software\\Fortify 360 v2.6.0\\Core\\lib", file.getCanonicalPath());
	}

	@Test
	public void testGetInstance() throws Exception {
		FortifyClientClassLoader loader = FortifyClientClassLoader.getInstance(null, null);
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
		FortifyClientClassLoader loader = FortifyClientClassLoader.getInstance(null, null);
		loader.bindCurrentThread();
		try {
			Object fortifyclient = loader.loadClass("org.jvnet.hudson.plugins.fortify360.fortifyclient.FortifyClient").newInstance();
			Assert.assertNotNull(fortifyclient);
			MethodUtils.invokeMethod(fortifyclient, "init", new String[] {"http://localhost:8180/f360/fm-ws/services", "ef0f4237-b951-4e88-b550-0d44a266d643"});
			Object out = MethodUtils.invokeMethod(fortifyclient, "getProjectList", null);
			Map<String, Long> map = (Map<String, Long>)out;
			// as long as it is not null and map.size > 0, no exception thrown, that's fine
			Assert.assertNotNull(map);
			Assert.assertTrue(map.size() > 0);
			for(String s : map.keySet()) {
				System.out.println("Project [: " + map.get(s) + "]: " + s);
			}
		} finally {
			loader.unbindCurrentThread();
		}
	}
}
