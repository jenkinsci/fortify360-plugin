package org.jvnet.hudson.plugins.fortify360.fortifyclient.schemawrapper;

import javax.xml.datatype.XMLGregorianCalendar;

public class Comment {

	private com.fortify.manager.schema.Comment comment;
	
	public Comment(com.fortify.manager.schema.Comment comment) {
		this.comment = comment;
	}
	
	public String getConent() {
		return comment.getContent();
	}
	
	public String getUsername() {
		return comment.getUsername();
	}
	
	public XMLGregorianCalendar getTimestamp() {
		return comment.getTimestamp();
	}
}
