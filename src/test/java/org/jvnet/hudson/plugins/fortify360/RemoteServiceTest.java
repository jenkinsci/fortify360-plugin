package org.jvnet.hudson.plugins.fortify360;

import java.io.*;
import java.util.ArrayList;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.jvnet.hudson.plugins.fortify360.FPRSummary;
import org.jvnet.hudson.plugins.fortify360.RemoteService;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.FilenameUtils;

public class RemoteServiceTest {

	// if the machine don't have reportGenerator, we will by-pass these test cases
	private static boolean noReportGenerator;
	
	// the NVS for old version and new version are different
	private static boolean useNewFPO;
	
	private static StringBuilder logMsg;
	
	@BeforeClass
	public static void setUp() throws Exception {
		logMsg = new StringBuilder();
		
		noReportGenerator = false;
		// locate sourceanalyzer
		String sourceanalyzerPath = RemoteService.locateSourceanalyzer(null, logMsg);
		if ( null == sourceanalyzerPath ) {
			System.out.print("Cannot locate sourceanalyzer, will skip plotting NVS chart\n");
			noReportGenerator = true;
			return;
		}
		String versionStr = RemoteService.getSCAVersion(sourceanalyzerPath);
		if ( null == versionStr ) {
			System.out.print("Cannot determine SCA version, will skip plotting NVS chart\n");
			noReportGenerator = true;
			return;				
		}
		
		String reportGeneratorPath = RemoteService.locateReportGenerator(null, logMsg); 
		if ( null == reportGeneratorPath ) {
			System.out.print("Cannot locate reportGenerator, will skip plotting NVS chart\n");
			noReportGenerator = true;
			return;
		}			
		
		useNewFPO = RemoteService.isNewFPO(versionStr);		
		if ( useNewFPO ) {
			System.out.println("Calculate NVS base on Critical/High/Medium/Low");
		} else {
			System.out.println("Calculate NVS base on Hot/Warning/Info");
		}
	}

	private File resourceToFile(String filename) throws IOException {
		InputStream in = null;
		OutputStream out = null;
		try {
			File tmp = File.createTempFile("test", "." + FilenameUtils.getExtension(filename));
			tmp.deleteOnExit();
			in = this.getClass().getClassLoader().getResourceAsStream(filename);
			out = new FileOutputStream(tmp);
			IOUtils.copy(in, out);
			return tmp;
		} finally {
			IOUtils.closeQuietly(in);
			IOUtils.closeQuietly(out);
		}
	}

	@Test
	public void testInvoke() throws Exception {
		if ( !noReportGenerator ) {
			String fpr = "WebGoat_57.fpr";
			File fprFile = resourceToFile(fpr);
			File path = fprFile.getParentFile();
			RemoteService service = new RemoteService(fprFile.getName(), null, null, null);
			FPRSummary summary = service.invoke(path, null);
			double nvs = summary.getNvs();
			Integer count = summary.getFailedCount();
			System.out.println("NVS = " + nvs);
			System.out.println("Fail Count = " + count);
			if ( useNewFPO ) {
				assertEquals(107.34, nvs, 0.1);
				assertEquals(0, count);
			} else {
				assertEquals(95.52, nvs, 0.1);
				assertEquals(0, count);				
			}
		}
	}

	@Test
	public void testInvoke2() throws Exception {
		if ( !noReportGenerator ) {
			String fpr = "WebGoat_Audited.fpr";
			File fprFile = resourceToFile(fpr);
			File path = fprFile.getParentFile();
			
			String search = null;
			if ( useNewFPO ) {
				search = "[fortify priority order]:critical category:/SQL Injection|Cross-Site Scripting/";
 			} else {
 				search = "[fortify priority order]:high category:/SQL Injection|Cross-Site Scripting/";
 			}
			
			RemoteService service = new RemoteService(fprFile.getName(), "Likely", search, null);
			FPRSummary summary = service.invoke(path, null);
			double nvs = summary.getNvs();
			Integer count = summary.getFailedCount();
			System.out.println("NVS = " + nvs);
			System.out.println("Fail Count = " + count);
			if ( useNewFPO ) {
				assertEquals(431.75, nvs, 0.1);
				assertEquals(148, count);
			} else {
				assertEquals(413.25, nvs, 0.1);
				assertEquals(139, count);				
			}
		}
	}
	
	@Test
	public void testInvoke3() throws Exception {
		if ( !noReportGenerator ) {
			String fpr = "test1.fpr";
			File fprFile = resourceToFile(fpr);
			File path = fprFile.getParentFile();
			RemoteService service = new RemoteService(fprFile.getName(), "Fortify Priority Order", "[fortify priority order]:high category:/SQL Injection|Cross-Site Scripting/", null);
			FPRSummary summary = service.invoke(path, null);
			double nvs = summary.getNvs();
			Integer count = summary.getFailedCount();
			System.out.println("NVS = " + nvs);
			System.out.println("Fail Count = " + count);
			assertEquals(50.0, nvs, 0.1);
			assertEquals(0, count);
		}
	}
	
	@Test
	public void testInvoke4() throws Exception {
		if ( !noReportGenerator ) {
			String fpr = "test1.fpr";
			File fprFile = resourceToFile(fpr);
			File path = fprFile.getParentFile();
			RemoteService service = new RemoteService(fprFile.getName(), "Fortify Priority Order", "category:j2ee bad practices\\: leftover debug code", null);
			FPRSummary summary = service.invoke(path, null);
			double nvs = summary.getNvs();
			Integer count = summary.getFailedCount();
			System.out.println("NVS = " + nvs);
			System.out.println("Fail Count = " + count);
			assertEquals(50.0, nvs, 0.1);
			assertEquals(2, count);
		}
	}
	
	@Test
	public void testInvoke5() throws Exception {
		if ( !noReportGenerator ) {
			String fpr = "test1.fpr";
			File fprFile = resourceToFile(fpr);
			File path = fprFile.getParentFile();
			RemoteService service = new RemoteService(fprFile.getName(), "Likely", "category:j2ee bad practices\\: leftover debug code", null);
			FPRSummary summary = service.invoke(path, null);
			double nvs = summary.getNvs();
			Integer count = summary.getFailedCount();
			System.out.println("NVS = " + nvs);
			System.out.println("Fail Count = " + count);
			assertEquals(30.0, nvs, 0.1);
			assertEquals(0, count);
		}
	}
}
