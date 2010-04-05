package org.jvnet.hudson.plugins.fortify360;

import org.junit.Before;
import org.junit.Test;
import org.springframework.util.Assert;

public class SCAMetaInfoTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testGetSCAVersion() throws Exception {
		String version = SCAMetaInfo.getSCAVersion();
		Assert.notNull(version);
		System.out.println("Version = " + version);
		Assert.isTrue(version.matches("[0-9]+(\\.[0-9]+)*"));
	}
	
	@Test 
	public void testIsNewFPO() throws Exception {
		// as long as no exception, that's fine
		@SuppressWarnings("unused")
		boolean x = SCAMetaInfo.isNewFPO();
	}
}
