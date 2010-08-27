package org.jvnet.hudson.plugins.fortify360;

public class ProjectVersionBean {
	private String name;
	private long id;
	
	public ProjectVersionBean() { }
	
	public ProjectVersionBean(String name, long id) {
		this.name = name;
		this.id = id;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public long getId() {
		return id;
	}
	
	public void setId(long id) {
		this.id = id;
	}
}
