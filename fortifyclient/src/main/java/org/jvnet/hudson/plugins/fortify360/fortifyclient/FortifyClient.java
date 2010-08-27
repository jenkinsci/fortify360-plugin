package org.jvnet.hudson.plugins.fortify360.fortifyclient;

import java.io.*;
import java.util.*;
import javax.script.*;
import javax.xml.bind.JAXBException;

import org.jvnet.hudson.plugins.fortify360.fortifyclient.schemawrapper.IssueList;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.InputStreamResource;
import com.fortify.manager.schema.*;
import com.fortify.ws.client.*;
import com.fortify.ws.core.WSTemplateProvider;

/** FortifyClient is a sub-module under Fortify360 Hudson plugin
 * <p>
 * It is basically a wrapper around wsclient.jar and wsobjects.jar
 * <p>
 * fortifyclient.jar is F360 server version dependent. Therefore, when you build, you
 * have to build fortifyclient.jar against different version of wsclient.jar and wsobjects.jar
 * <p>
 * When Fortify360 plugin runs, it will load the correct version of fortifyclient.jar, wsclient.jar 
 * and wsobjects.jar dynamically. And Fortify360 plugin will call FortifyClient via reflection, hence 
 * removing all the dependency between Fortify360 hudson plugin with F360 server version. Therefore, 
 * Fortify360 plugin can work with any versions of F360server.
 * <p>
 * 
 * @author sng
 *
 */
public class FortifyClient {
	
	private static final String[] CONFIG_FILES = {"fortify-wsclient-config.xml"};

	private WSTemplateProvider wsTemplateProvider;
	
	public FortifyClient() { 
		wsTemplateProvider = null;
	}
	
	/** You have to call this init function before performance any operations
	 * 
	 * @param uri e.g. http://localhost:8180/f360
	 * @param token e.g. the AuditToken or AnalysisUploadToken
	 */
	public void init(String uri, String token) {
		// I can't use ClassPathXmlApplicationContext here
		// the plugin, as well as the CONFIG_FILE, are loaded by Plugin ClassLoader
		// the current running thread is probably using another ClassLoader
		// therefore, this ClassLoader may not be able to find the CONFIG_FILE
			
		InputStream in = FortifyClient.class.getClassLoader().getResourceAsStream(CONFIG_FILES[0]);
		GenericApplicationContext ctx = new GenericApplicationContext();
		XmlBeanDefinitionReader xmlReader = new XmlBeanDefinitionReader(ctx);
		xmlReader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
		xmlReader.loadBeanDefinitions(new InputStreamResource(in));
		ctx.refresh();
		ContextTemplateProvider provider = (ContextTemplateProvider)ctx.getBean("templateProvider");
		
		provider.setToken(token);
		provider.setUri(uri);
		
		wsTemplateProvider = provider;
	}
	
	/** Retrieve the project list from F360 server
	 * 
	 * @return
	 * @throws FortifyWebServiceException
	 */
	public Map<String, Long> getProjectList() throws FortifyWebServiceException {
		
		ProjectClient projectClient = new ProjectClient(wsTemplateProvider, null, null);
		List<Project> projects = projectClient.getProjects();
		Map<Long, String> pmap = new HashMap<Long, String>();
		for(Project project : projects) {
			pmap.put(project.getId(), project.getName());
		}
		
		ProjectVersionClient pvClient = new ProjectVersionClient(wsTemplateProvider, null, null);
		List<ProjectVersionLite> pversions = pvClient.getProjectVersions();
		Map<String, Long> projectList = new HashMap<String, Long>();
		for(ProjectVersionLite version : pversions) {
			String projectName = pmap.get(version.getProjectId());
			projectList.put(projectName + " (" + version.getName() + ")", version.getId());
		}
		
		return projectList;
			
	}
	
	/** Upload an FPR to F360 server
	 * 
	 * @param fprFile the FPR file to be uploaded
	 * @param projectVersionId the F360 server project ID, you can get the project ID via getProjectList()
	 * @throws FortifyWebServiceException
	 * @throws IOException
	 */
	public void uploadFPR(File fprFile, Long projectVersionId) throws FortifyWebServiceException, IOException {
		
		FPRTransferClient wsclient = new FPRTransferClient(wsTemplateProvider, null, null);
		Status status = wsclient.uploadFPR(fprFile, projectVersionId);
		// for v2.6, the return code is -10001... don't ask me why..
		if ( 0 == status.getCode() || "Background submission succeeded.".equals(status.getMsg()) ) {
			// ok, successful
		} else {
			FortifyWebServiceException e = new FortifyWebServiceException(status);
			throw e;
		}
	}
	
	/** Run audit script, usually for job assignment
	 * <p>
	 * This function will load the auditScript as a Javascript, and provide all the necessary info to the Javascript
	 * engine context. 
	 * <p>
	 * There are three variables in the JS context:
	 * <ol>
	 *   <li>issues: the list of all issues, from F360 server</li>
	 *   <li>changeLog: the SCM changelog, from Hudson build info</li>
	 *   <li>users: the available user list, from F360 server</li>
	 * </ol>
	 *   
	 * 
	 * @param projectVersionId the F360 server project Id
	 * @param auditScript the Javascript to be executed, as a string
	 * @param filterSet if provided, will switch to a particular filterSet before we retrieve the issue list
	 * @param changeLog the SCM changelog info
	 * @param log error log stream
	 * @throws ScriptException
	 * @throws JAXBException
	 * @throws FortifyWebServiceException
	 */
	public void auditFPR(Long projectVersionId, String auditScript, String filterSet, Map<String, String> changeLog, PrintWriter log)  
		throws ScriptException, JAXBException, FortifyWebServiceException {
		
		AuditClient auditClient = new AuditClient(wsTemplateProvider, null, null);
		auditClient.startSession(projectVersionId);
		try {

			IssueListDescription ild = new IssueListDescription();
			ild.setIncludeHidden(false);
			ild.setIncludeRemoved(false);
			ild.setIncludeSuppressed(false);

			AuditView auditView = auditClient.getAuditView();
			if (null != filterSet) {
				List<FilterSetDescription> filters = auditView.getFilterSets();
				for (FilterSetDescription fd : filters) {
					if (filterSet.equalsIgnoreCase(fd.getName())) {
						// set the filter set
						log.printf("Job Assignment using FilterSet: %s %s\n",
								fd.getName(), fd.getFilterSetId());
						ild.setFilterSetId(fd.getFilterSetId());
						break;
					}
				}
			}

			List<String> users = auditView.getUserList().getUser();
			Set<String> allowedUsers = new HashSet<String>();
			allowedUsers.addAll(users);
			allowedUsers = Collections.unmodifiableSet(allowedUsers);

			IssueListing list = auditClient.listIssues(ild);
			List<IssueInstance> issues = list.getIssues().getIssue();

			ScriptEngineManager factory = new ScriptEngineManager();
			ScriptEngine engine = factory.getEngineByName("JavaScript");
			engine.getContext().setWriter(log);
			engine.getContext().setErrorWriter(log);

			IssueList wrappedIssues = new IssueList(issues, auditClient, log);
			engine.put("issues", wrappedIssues);
			engine.put("changeLog", changeLog);
			engine.put("users", allowedUsers);

			engine.eval(auditScript);

		} finally {
			auditClient.endSession();
		}
	}
	
	/** Check if the auditScrpt is valid or not, basically as a validation function for the GUI
	 * <p>
	 * This is a simple hack to run the auditScript with some dummy objects, it may not be able to find all 
	 * errors in the auditScript. And if it doesn't throw exception, we can consider it as valid
	 * </p>
	 * 
	 * @param auditScript
	 * @throws ScriptException
	 */
	public void checkAuditScript(String auditScript) throws ScriptException {
		List<String> users = new ArrayList<String>();
		users.add("admin");
		users.add("user01");
		users.add("user02");
		
		Set<String> allowedUsers = new HashSet<String>();
		allowedUsers.addAll(users);
		allowedUsers = Collections.unmodifiableSet(allowedUsers);

		AuditClient auditClient = null;

		ScriptEngineManager factory = new ScriptEngineManager();
		ScriptEngine engine = factory.getEngineByName("JavaScript");
		PrintWriter log = new PrintWriter(System.out);
		engine.getContext().setWriter(log);
		engine.getContext().setErrorWriter(log);

		IssueInstance dummyInstance = new IssueInstance();
		List<IssueInstance> issues = new ArrayList<IssueInstance>();
		issues.add(dummyInstance);
		IssueList wrappedIssues = new IssueList(issues, auditClient, log);
		engine.put("issues", wrappedIssues);
		engine.put("changeLog", new HashMap<String, String>());
		engine.put("users", allowedUsers);

		engine.eval(auditScript);
	}
}
