package org.jvnet.hudson.plugins.fortify360;


import java.io.File;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

public class PathUtilsTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testLocateBaesnameInPath() throws Exception {
		File[] list = PathUtils.locateBaesnameInPath("fortifyclient");
		Assert.assertNotNull(list);
		Assert.assertEquals(1, list.length);
		File file = new File("C:\\Program Files\\Fortify Software\\Fortify 360 v2.6.5\\bin\\fortifyclient.bat");
		Assert.assertEquals(file, list[0]);
	}
}
