package org.jvnet.hudson.plugins.fortify360;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class SCAMetaInfo {
	
	private static String scaVersion = null;
	private static Boolean useNewFPO = null;
	// private static Boolean noReportGenerator = null;
	
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

	/** Determined if reportGenerator is available on this machine
	 * <br/> ReportGenerator is only available on Windows/Linux/Mac
	 * 
	 * @return true if this is a Windows, Linux or Mac
	 * @throws InterruptedException
	 */
	public static boolean hasReportGenerator() {
		String os = System.getProperty("os.name");
		System.out.println("os.name = " + os);
		if ( os.matches("[Ww]in.*") || os.matches("[Ll]inux.*") || os.matches("[Mm]ac.*") ) {
			return true;
		} else {
			return false;
		}
	}
	
	/*
	public static synchronized boolean hasReportGenerator() throws InterruptedException {
		if ( null == noReportGenerator ) {
			noReportGenerator = Boolean.FALSE;
			try {
				String os = System.getProperty("os.name");
				String image = os.matches("Win.*|.*win.*") ? "reportGenerator.bat" : "ReportGenerator";
		
				ArrayList<String> cmd = new ArrayList<String>();
				cmd.add(image);
				cmd.add("-help"); // no such option, but this is fine
				
				ProcessBuilder pb = new ProcessBuilder(cmd);
				Process proc = pb.start();
				proc.waitFor();
				
			} catch (IOException e) {
				if ( e.getMessage().startsWith("CreateProcess:") ) {
					noReportGenerator = Boolean.TRUE;
				}
			}
		}
		
		// I'm sure noReportGenerator is non-null
		return noReportGenerator;
	}*/
}
