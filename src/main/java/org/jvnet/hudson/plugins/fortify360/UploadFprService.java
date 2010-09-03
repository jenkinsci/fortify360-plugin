package org.jvnet.hudson.plugins.fortify360;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.*;

import hudson.FilePath.FileCallable;
import hudson.remoting.VirtualChannel;

/**
 * 
 * @author sng
 * @deprecated will now call FortifyClientClassLoader to use reflection to invoke FortifyClient object directly
 */
public class UploadFprService implements FileCallable<Integer>, Serializable {
	
	private static final long serialVersionUID = 1L;
	private String url;
	private String token;
	private Integer f360projId;
	
	public UploadFprService(String url, String token, Integer f360projId) {
		this.url = url;
		this.token = token;
		this.f360projId = f360projId;
	}

	public Integer invoke(File f, VirtualChannel channel) throws IOException {
		System.out.println("Inside uploadToF360 FileCallable invoke");
		System.out.println("f is " + f);
		
		// fortifyclient uploadFPR –projectID <proj_ID> -file XXX.fpr -url http://fortify.ca.com:8080/f360 -authtoken XXXX
		String array[] = {"fortifyclient", "uploadFPR", "-projectID", "unknown", "-file", "unknown", "-url", "unknown", "-authtoken", "unknown"};
		
		String os = System.getProperty("os.name");
		String image = os.matches("Win.*|.*win.*") ? "fortifyclient.bat" : "fortifyclient";
		array[0] = image;
		
		array[3] = f360projId.toString();   // project Id
		array[5] = f.getAbsolutePath();     // the fpr file
		array[7] = url;                     // F360 server url
		array[9] = token;                   // authentication token
		
		try {
			ProcessBuilder pb = new ProcessBuilder(array);
			StringBuilder buf = new StringBuilder();
			for(int i=0; i<array.length; i++) buf.append(array[i]).append(' ');
			System.out.println("EXE: " + buf.toString());
			Process proc = pb.start();
			proc.waitFor();
			int exitValue = proc.exitValue();
			//System.out.println("Exit Value = " + exitValue);
			if ( 0 != exitValue ) {
				String err = IOUtils.toString(proc.getErrorStream());
				String out = IOUtils.toString(proc.getInputStream());
				if ( StringUtils.isEmpty(err) ) err = "While running uploadFPR: " + out;
				else err = "While running uploadFPR: " + err + " " + out;
				throw new RuntimeException(err);
			} else {
				return 0;
			}
		} catch (InterruptedException e) {
			IOException e2 = new IOException(e);
			throw e2;
		}
	}
}
