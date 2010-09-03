package org.jvnet.hudson.plugins.fortify360;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * 
 * @deprecated will be removed in the next release
 * @author sng
 *
 */
public class SCAMetaInfo {
	
	private static String scaVersion = null;
	private static Boolean useNewFPO = null;
	// private static Boolean noReportGenerator = null;
	
	/**
	 * @deprecated will be removed in the next release
	 */
	public static synchronized boolean isNewFPO() throws IOException, InterruptedException{
		if ( null == useNewFPO ) {
			String versionStr = getSCAVersion();
			VersionNumber version = new VersionNumber(versionStr);
			if ( version.compareTo(new VersionNumber("5.8")) >= 0 ) {
				useNewFPO = Boolean.TRUE;
			} else {
				useNewFPO = Boolean.FALSE;
			}
		}
		
		return useNewFPO;
	}
	
	/**
	 * 
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 * @deprecated will be removed in the next release
	 */
	public static synchronized String getSCAVersion() throws IOException, InterruptedException {
		if ( null == scaVersion ) {
			ArrayList<String> cmd = new ArrayList<String>();
			cmd.add("sourceanalyzer");
			cmd.add("-version");
			
			ProcessBuilder pb = new ProcessBuilder(cmd);
			Process proc = pb.start();
			proc.waitFor();
			InputStream in = proc.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String line = br.readLine();
			if ( null != line ) {
				line = line.trim();
				int x = line.lastIndexOf(' ');
				if ( -1 != x ) {
					scaVersion = line.substring(x).trim();
				}
			}
		}
		
		// there are chances scaVersion is still null
		return scaVersion;
	}
}
