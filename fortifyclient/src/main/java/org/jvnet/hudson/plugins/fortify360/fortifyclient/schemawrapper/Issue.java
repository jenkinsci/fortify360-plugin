package org.jvnet.hudson.plugins.fortify360.fortifyclient.schemawrapper;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.fortify.manager.schema.Comment;
import com.fortify.manager.schema.IssueInstance;
import com.fortify.manager.schema.Tag;
import com.fortify.ws.client.AuditClient;

/** A wrapper around com.fortify.manager.schema.IssueInstance
 * 
 * @author sng
 *
 */
public class Issue {

	private IssueInstance instance;
	private AuditClient auditClient;
	private PrintWriter log;
	
	public Issue(IssueInstance instance, AuditClient auditClient, PrintWriter log) {
		this.instance = instance;
		this.auditClient = auditClient;		
		this.log = log;
	}
	
	public void setAssignedUser(String user) {
		try {
			if ( null != auditClient ) auditClient.assignUser(instance, user);
		} catch ( Throwable t ) {
			String instanceId = "unknown";
			try {
				instanceId = instance.getInstanceId();
			} catch ( Throwable t2 ) { }
			log.println("Error setAssignedUser(): " + instanceId + ", " + user);
			t.printStackTrace(log);
		}
	}
	
	public void addComment(String comment) {
		try {
			if ( null != auditClient ) auditClient.addComment(instance, comment);
		} catch (Throwable t) {
			String instanceId = "unknown";
			try {
				instanceId = instance.getInstanceId();
			} catch ( Throwable t2 ) { }
			log.println("Error addComment(): " + instanceId + ", " + comment);
			t.printStackTrace(log);
		}
	}
	
	public String getInstanceId() {
		return instance.getInstanceId();
	}
	
	public String getAssignedUser() {
		return instance.getAssignedUser();
	}
	
	public int getRevision() {
		return instance.getRevision();
	}
	
	public List<Tag> getTag() {
		// TODO: make sure user can't modify Tag content as well
		return Collections.unmodifiableList(instance.getTag());
	}
	
	public boolean isSuppressed() {
		return instance.isSuppressed();
	}
	
	public List<String> getThreadComments() {
		if ( null != instance.getThreadedComments() ) {
			// TODO: make sure user can't modify the content of Comment as well
			List<Comment> comments = instance.getThreadedComments().getComment();
			List<String> out = new ArrayList<String>();
			for(Comment comment : comments) {
				String line = comment.getTimestamp() + " " + comment.getUsername() + ": " + comment.getContent();
				out.add(line);
			}
			return Collections.unmodifiableList(out);
		} else { 
			return Collections.unmodifiableList(new ArrayList<String>());
		}
	}
	
	public String getAnalysisEngine() {
		if ( null == instance.getClassInfo() ) return null;
		return instance.getClassInfo().getAnalysisEngine();
	}
	
	public String getAnalyzer() {
		if ( null == instance.getClassInfo() ) return null;
		return instance.getClassInfo().getAnalyzer();
	}
	
	public String getCategory() {
		if ( null == instance.getClassInfo() ) return null;
		return instance.getClassInfo().getCategory();
	}
	
	public String getCWE() {
		if ( null == instance.getClassInfo() ) return null;
		return instance.getClassInfo().getCWE();
	}
	
	public String getKingdom() {
		if ( null == instance.getClassInfo() ) return null;
		return instance.getClassInfo().getKingdom();
	}
	
	public String getOWASP2004() {
		if ( null == instance.getClassInfo() ) return null;
		return instance.getClassInfo().getOWASP2004();
	}
	
	public String getOWASP2007() {
		if ( null == instance.getClassInfo() ) return null;
		return instance.getClassInfo().getOWASP2007();
	}
	
	public String getRuleId() {
		if ( null == instance.getClassInfo() ) return null;
		return instance.getClassInfo().getRuleId();
	}
	
	public String getSubType() {
		if ( null == instance.getClassInfo() ) return null;
		return instance.getClassInfo().getSubType();
	}
	
	public String getType() {
		if ( null == instance.getClassInfo() ) return null;
		return instance.getClassInfo().getType();
	}
	
	public String getWASC24And2() {
		if ( null == instance.getClassInfo() ) return null;
		return instance.getClassInfo().getWASC24And2();
	}
	
	public double getConfidence() {
		if ( null == instance.getInstanceInfo() ) return 0;
		return instance.getInstanceInfo().getConfidence();
	}
		
	public double getSeverity() {
		if ( null == instance.getInstanceInfo() ) return 0;
		return instance.getInstanceInfo().getSeverity();
	}
	
	public String getSinkFunction() {
		if ( null == instance.getInstanceInfo() ) return null;
		return instance.getInstanceInfo().getSinkFunction();
	}
	
	public String getSourceFunction() {
		if ( null == instance.getInstanceInfo() ) return null;
		return instance.getInstanceInfo().getSourceFunction();
	}

	/*
	public IssueLocation getIssueLocation() {
		if ( null == instance.getInstanceInfo() ) return null;
		return instance.getInstanceInfo().getIssueLocation();
	}
	*/

	public String getClassName() {
		if ( null == instance.getInstanceInfo() || null == instance.getInstanceInfo().getIssueLocation() ) return null;
		return instance.getInstanceInfo().getIssueLocation().getClassName();
	}
	
	/** 
	 * 
	 * @return the filepath, e.g. "src\com\package1\bar.java"
	 */
	public String getFilePath() {
		if ( null == instance.getInstanceInfo() || null == instance.getInstanceInfo().getIssueLocation() ) return null;
		return instance.getInstanceInfo().getIssueLocation().getFilePath();
	}
	
	public String getFunction() {
		if ( null == instance.getInstanceInfo() || null == instance.getInstanceInfo().getIssueLocation() ) return null;
		return instance.getInstanceInfo().getIssueLocation().getFunction();
	}

	public int getLineNumber() {
		if ( null == instance.getInstanceInfo() || null == instance.getInstanceInfo().getIssueLocation() ) return -1;
		return instance.getInstanceInfo().getIssueLocation().getLineNumber();
	}
	
	public String getPackage() {
		if ( null == instance.getInstanceInfo() || null == instance.getInstanceInfo().getIssueLocation() ) return null;
		return instance.getInstanceInfo().getIssueLocation().getPackage();
	}

	public String getSourceFilePath() {
		if ( null == instance.getInstanceInfo() || null == instance.getInstanceInfo().getIssueLocation() ) return null;
		return instance.getInstanceInfo().getIssueLocation().getSourceFilePath();
	}

	public String getURL() {
		if ( null == instance.getInstanceInfo() || null == instance.getInstanceInfo().getIssueLocation() ) return null;
		return instance.getInstanceInfo().getIssueLocation().getURL();
	}
}
