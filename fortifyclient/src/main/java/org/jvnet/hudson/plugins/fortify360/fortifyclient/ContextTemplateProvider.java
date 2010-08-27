package org.jvnet.hudson.plugins.fortify360.fortifyclient;

import org.springframework.ws.client.core.WebServiceTemplate;
import com.fortify.ws.core.WSAuthenticationProvider;
import com.fortify.ws.core.WSTemplateProvider;

public class ContextTemplateProvider implements WSTemplateProvider {

	private WebServiceTemplate webServiceTemplate;
	private String token;
	private String uri;
	
	public ContextTemplateProvider() throws Exception {
	}
	
	public WSAuthenticationProvider getAuthenicationProvider() {
		return new WSAuthenticationProvider() {
			public String getUserName() { return null; }
			public String getPassword() { return null; }
			public boolean isUsingTokenAuth() { return true; }
			public String getToken() { return token; }
		};
	}

	public WebServiceTemplate getTemplate() {
		return webServiceTemplate;
	}

	public void setWebServiceTemplate(WebServiceTemplate webServiceTemplate) {
		this.webServiceTemplate = webServiceTemplate;
	}
	
	public String getUri() {
		return uri;
	}	

	public void setUri(String uri) {
		this.uri = uri;
		if ( null != webServiceTemplate ) {
			webServiceTemplate.setDefaultUri(uri);
		}
	}	
	
	public void setToken(String token) {
		this.token = token;
	}
}
