package org.jvnet.hudson.plugins.fortify360.fortifyclient;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
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
	public void g_init() throws MalformedURLException {
		noF360 = false;
		
		projName = "EightBall (1.0)";
		fprName = "eightball_59.fpr";
		url = "http://localhost:8180/f360/fm-ws/services";
		
		String version = fortifyVersion();
		if ( "2.5".equals(version) ) {
			token = "1b475dc8-663d-4210-a26c-f70bb8fea7c6";
			auditToken = "e99be68d-5c37-4f98-893f-6a3c29eafabf";
		} else if ( "2.6".equals(version) ) {
//			projName = "WebGoat (5.0)";
//			fprName = "WebGoat_59.fpr";
			token = "6538f06e-b4b8-4214-aadd-40d1ae701b23";
			auditToken = "d0415ffe-151e-4fd6-be0c-2c23793227d2";
		} else if ( "2.6.1".equals(version) ) {
			token = "7ecaccb1-82ec-4e58-a6a4-a48635e7a9fc";
			auditToken = "281edd83-4688-4ce3-9aa4-dcaa5815a905";
		} else if ( "2.6.5".equals(version) ) {
			token = "5d8f3575-70a9-4293-8cd6-6c18692cf6aa";
			auditToken = "51570cca-5d67-4a80-8cb2-76562c6dd875";
		} else if ( "3.0.0".equals(version) ) {
			token = "762f8f67-9e3e-4b29-b9a7-47d44eb5d202";
			auditToken = "a6449fa6-8e09-4109-aa5d-560689094014";
		} else if ( "3.1.0".equals(version) ) {
			token = "ffbd38e8-fb40-4d77-a2dc-2c6b80855822";
			auditToken = "7a839915-98de-4bde-acf5-630f8e11ffa6";
		} else if ( "3.20".equals(version) ) {
			token = "c999c73e-2179-48d9-8893-753126687457";
			auditToken = "d579d1bb-1061-448e-b079-95597b283251";
		} else if ( "3.30".equals(version) ) {
			token = "d05eb1fa-0c71-4420-9122-e72059c316fa";
			auditToken = "c8102e40-c2cf-47d6-9e53-1e4c9c36babe";
			url = url.replace("f360", "ssc");
		} else if ( "3.40".equals(version) ) {
			token = "127403a6-2ced-4114-b591-3e4d7617c0ef";
			auditToken = "4d4f86d6-f7bd-4e58-8ce2-f5309700404d";
			url = url.replace("f360", "ssc");
		} else if ( "3.50".equals(version) ) {
			token = "3d4015f2-5a62-4843-a9fc-5b7ebd3ec538";
			auditToken = "4241463e-d5a2-4d55-a11c-484b199ee09b";
			url = url.replace("f360", "ssc");
		} else {
			System.out.println("##################################################################");
			System.out.println("Only F360 Server 2.5 - 3.50 are supported, your version is: " + version);
			System.out.println("##################################################################");			
		}
		
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
