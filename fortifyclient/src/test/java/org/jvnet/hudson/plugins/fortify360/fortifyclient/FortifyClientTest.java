package org.jvnet.hudson.plugins.fortify360.fortifyclient;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.InputStream;
import java.net.*;
import java.util.*;
import java.util.regex.*;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.ws.soap.axiom.AxiomSoapMessageException;

import com.fortify.manager.schema.IssueInstance;
import com.fortify.ws.client.FortifyWebServiceException;

public class FortifyClientTest {

	private boolean noF360;
	private String url;
	private String auditToken;
	private String token;
	private String fprName;
	private String projName;	
	private Map<String, Long> projects;
	private FortifyClient fortifyclient;
	
	@Before
	public void g_init() throws Exception {
		noF360 = false;
		
		projName = "EightBall (1.0)";
		fprName = "eightball_59.fpr";
		url = "http://localhost:8180/f360/fm-ws/services";
		
		InputStream in = null;
		Properties prop = new Properties();
		try {
			in = FortifyClientTest.class.getClassLoader().getResourceAsStream("fortifyclient.properties");
			prop.load(in);
		} finally {
			if ( null != in ) try { in.close(); } catch ( Exception e ) {}
		}
			
		String version = fortifyVersion();
		token = prop.getProperty("token-" + version);
		auditToken = prop.getProperty("auditToken-" + version);
		String contextPath = prop.getProperty("contextPath-" + version, "f360");
		if ( null == token || null == auditToken ) {
			System.out.println("##################################################################");
			System.out.println("Unsupported version: " + version);
			System.out.println("##################################################################");						
			return;
		}
		url = url.replace("f360", "ssc");
		
		URL u = new URL(url);
		try {
			HttpURLConnection http = (HttpURLConnection) u.openConnection();
			int code = http.getResponseCode();
		} catch (IOException e) {
			noF360 = true;
			System.out.println("###############################################");
			System.out.println("F360 Server is not running, this test is skipped");
			System.out.println("###############################################");
		}
		
		fortifyclient = new FortifyClient();
		fortifyclient.init(url, token);		
	}
	
	@Test
	public void allTests() throws Exception {		
		// we have to call this manually because we need to call in this particular order
		testProjectList();
		testUploadFPR();
		System.out.println("Just uploaded an FPR to F360 Server, will sleep for 30s to let it process the FPR");
		Thread.sleep(30*1000);
		testAuditFPR();
		
		testCheckAuditScript();
	}
	
	private void testProjectList() throws Exception {
		if ( noF360 ) return;
		
		try {
			projects = fortifyclient.getProjectList();
			for(String s : projects.keySet()) {
				System.out.println(s + " -> " + projects.get(s));
			}
		} catch ( FortifyWebServiceException e ) {
			// if due to connection error, probably f360 server is not started up
			if ( containsRootCause(e, ConnectException.class) ) {
				// we should run this anyway...., ignore it
			} else {
				throw e;
			}
		}
	}
	
	private void testUploadFPR() throws Exception {
		if ( noF360 ) return;
		// we assume you have called testProjectList(), so projects shouldn't be null
		if ( null == projects ) return;
		
		try {
			File fpr = TestUtils.resourceToFile(fprName);
			fortifyclient.uploadFPR(fpr, projects.get(projName));
		} catch ( FortifyWebServiceException e ) {
			// if due to connection error, probably f360 server is not started up
			if ( containsRootCause(e, ConnectException.class) ) {
				// we should run this anyway...., ignore it
			} else {
				throw e;
			}
		}
	}
	
	private void testAuditFPR() throws Exception {
		if ( noF360 ) return;
		// we assume you have called testProjectList(), so projects shouldn't be null
		if ( null == projects ) return;
		
		// assign all issues to "admin"
		String auditScript = 
			"var size = issues.size();\n" +
			"for(var i=0; i<size; i++) {\n" + 
			"	var instance = issues.get(i);\n" + 
			"	instance.setAssignedUser(\"admin\");\n" +
			"	instance.addComment(\"Auto-Assigned by FortifyClientTest.testAuditFPR()\");\n" +
			"}\n";
		
		Map<String, String> changeLog = new HashMap<String, String>();
		PrintWriter log = new PrintWriter(System.out);
		
		try {
			//change to use auditToken
			fortifyclient = new FortifyClient();
			fortifyclient.init(url, auditToken);				
			fortifyclient.auditFPR(projects.get(projName), auditScript, null, changeLog, log);
		} catch ( FortifyWebServiceException e ) {
			// if due to connection error, probably f360 server is not started up
			if ( containsRootCause(e, ConnectException.class) ) {
				// we should run this anyway...., ignore it
			} else {
				throw e;
			}
		}
	}
	
	private void testCheckAuditScript() throws Exception {
		// assign all issues to "admin"
		String auditScript = 
			"var size = issues.size();\n" +
			"for(var i=0; i<size; i++) {\n" + 
			"	var instance = issues.get(i);\n" + 
			"	instance.setAssignedUser(\"admin\");\n" +
			"	instance.addComment(\"Auto-Assigned by FortifyClientTest.testAuditFPR()\");\n" +
			"}\n";
		
		FortifyClient dummy = new FortifyClient();
		dummy.checkAuditScript(auditScript);
		System.out.println("FortifyClient.checkAuditScript() finished without any Exception");
	}	
	
	private boolean containsRootCause(Throwable t, Class c) {
		while ( null != t ) {
			t = t.getCause();
			if ( null != t && c.isInstance(t) ) return true;
		}
		return  false;
	}
	
	private static String fortifyVersion() {
		String s = System.getenv("MAVEN_CMD_LINE_ARGS");
		if ( null == s ) s = ""; // otherwise, it will throw NPE, it won't match anyway
		String x = "(.*\\s)?-Dfortify\\.version=(\\d+(?:.\\d+)*)(\\s.*)?";
		Pattern p = Pattern.compile(x);
		Matcher m = p.matcher(s);
		if ( m.matches() ) {
			// it's alwasy group2
			return m.group(2);
		} else {
			System.out.println("###############################################");
			System.out.println("Please specify mvn -Dfortify.version=[ver]");
			System.out.println("###############################################");
			return null;
		}
	}
}
