package org.jvnet.hudson.plugins.fortify360.fortifyclient.schemawrapper;

import java.io.PrintWriter;
import java.util.List;
import com.fortify.manager.schema.IssueInstance;
import com.fortify.ws.client.AuditClient;

public class IssueList {

	private List<IssueInstance> issues;
	private AuditClient auditClient;
	private PrintWriter log;
	
	public IssueList(List<IssueInstance> issues, AuditClient auditClient, PrintWriter log) {
		this.issues = issues;
		this.auditClient = auditClient;
		this.log = log;
	}
	
	public int size() {
		return issues.size();
	}
	
	public Issue get(int x) {
		return new Issue(issues.get(x), auditClient, log);
	}
}
