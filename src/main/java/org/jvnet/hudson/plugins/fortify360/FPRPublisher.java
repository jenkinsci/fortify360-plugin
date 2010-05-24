package org.jvnet.hudson.plugins.fortify360;

import java.io.*;

import hudson.*;
import hudson.remoting.Callable;
import hudson.remoting.VirtualChannel;
import hudson.tasks.*;
import hudson.FilePath.FileCallable;
import hudson.model.*;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/** Fortify 360 Publisher Hudson Plugin
 * 
 * <p>This plugin will mainly do three things
 * <ul>
 *   <li>Calculate NVS from the FPR and plot graph in the project main page</li>
 *   <li>Upload FPR to Fortify 360 Server</li>    
 *   <li>Make a build to be UNSTABLE if some critical vulnerabilities are found</li>
 * </ul>
 * 
 * @author sng
 *
 */
public class FPRPublisher extends Recorder {
	
	private String fpr;
	private String filterSet;
	private String searchCondition;
	private Integer f360projId;
	
	@DataBoundConstructor
	public FPRPublisher(String fpr, String filterSet, String searchCondition, Integer f360projId) {
		this.fpr = fpr;
		this.filterSet = filterSet;
		this.searchCondition = searchCondition;
		this.f360projId = f360projId;

		/*
		System.out.println("###########################################");
		System.out.printf("FPR=%s, filterSet=%s, searchCondition=%s, UploadToF360=%s\n", this.fpr, this.filterSet, this.searchCondition, this.f360projId);
		System.out.println("###########################################");
		*/
	}
	
	public String getFpr() {
		return fpr;
	}
	
	public String getFilterSet() {
		return filterSet;
	}
	
	public String getSearchCondition() {
		return searchCondition;
	}
	
	public Integer getF360projId() {
		return f360projId;
	}
	
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}
	
	@Override
    public Action getProjectAction(AbstractProject<?,?> project) {
        return new ChartAction(project);
    }

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
		
		PrintStream log = launcher.getListener().getLogger();
		
		/*
		Result result = build.getResult();
		if (!(Result.SUCCESS.equals(result) || Result.UNSTABLE.equals(result))) {
			log.println("Build is not successful, no dependency analysis.");
			return false;
		}
		*/
		
		log.println("Publishing Fortify 360 FPR Data");
		
		// calling the remote slave to retrieve the NVS
		// build.getActions().add(new ChartAction(build.getProject()));
		RemoteService service = new RemoteService(fpr, filterSet, searchCondition);
		FPRSummary summary = build.getWorkspace().act(service);
		
		//log.printf("Using FPR: %s\n", summary.getFprFullPath());
		FilePath fprFile = summary.getFprFile();
		log.printf("Using FPR: %s\n", (null != fprFile) ? fprFile.toURI() : "null");
		log.printf("Calculated NVS = %f\n", summary.getNvs());
		
		// save data under the builds directory, this is always in Hudson master node
		log.println("Saving FPR summary");
		summary.save(new File(build.getRootDir(), FPRSummary.FILE_BASENAME));
		
		// if the project ID is not null, then we need to upload the FPR to 360 server
		if ( null != f360projId && DESCRIPTOR.canUploadToF360() ) {
			// the FPR may be in remote slave, we need to call launcher to do this for me
			if ( null == fprFile ) {
				log.printf("Can't upload FPR to Fortify 360 Server, FPR File is NULL");
			} else {
				log.printf("Uploading FPR to Fortify 360 Server at %s\n", DESCRIPTOR.getUrl());
				//uploadToF360(summary.getFprFullPath(), launcher);
				UploadFprService uploadService = new UploadFprService(DESCRIPTOR.getUrl(), DESCRIPTOR.getToken(), f360projId);
				fprFile.act(uploadService);
			}
		}
		
		// now check if the fail count
		if ( null != searchCondition ) {
			Integer failedCount = summary.getFailedCount();
			if ( null != failedCount && failedCount > 0 ) {
				log.printf("Fortify 360 Plugin: this build is unstable because there are %d critical vulnerabilities\n", failedCount);
				build.setResult(Result.UNSTABLE);
			}
		}
		
		return true;
	}
	
	@Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();
	
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
    	
    	/** The Fortify 360 Server URL, e.g. http://localhost:8080/f360 */
    	private String url;
    	
    	/** The Fortify 360 Server AnalysisUploadToken */
    	private String token;
    	
    	public DescriptorImpl() {
    		super(FPRPublisher.class);
    		load();
    	}

    	@Override
    	public boolean isApplicable(Class<? extends AbstractProject> jobType) {
    		return true; // applicable to all project type
    	}

    	@Override
    	public String getDisplayName() {
    		return "Fortify FPR Publisher";
    	}
    	
    	public String getUrl() {
    		return url;
    	}
    	
    	public String getToken() {
    		return token;
    	}
    	
    	public boolean canUploadToF360() {
    		return ( null != url && null != token );
    	}
    	
        @Override
        public boolean configure(StaplerRequest req, JSONObject o) throws FormException {
            // to persist global configuration information,
            // set that to properties and call save().
            url = o.getString("url");
            token = o.getString("token");
            /*
            System.out.println("url: " + o.getString("url"));
            System.out.println("token: " + o.getString("token"));
            */
            save();
            return super.configure(req,o);
        }    	
    }
}
