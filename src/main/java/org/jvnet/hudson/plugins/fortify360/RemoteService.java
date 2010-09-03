package org.jvnet.hudson.plugins.fortify360;

import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.io.*;

import hudson.FilePath;
import hudson.remoting.VirtualChannel;

import org.apache.commons.io.*;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

public class RemoteService implements FilePath.FileCallable<FPRSummary> {
	
	private static final long serialVersionUID = 229830219491170076L;
	private String fpr;
	private String filterSet;
	private String searchCondition;
	private String suggestedFortifyHome;
	
	private StringBuilder logMsg;
	
	private String sourceanalyzerPath;
	private String reportGeneratorPath;
	
	public RemoteService(String fpr, String filterSet, String searchCondition, String suggestedFortifyHome) {
		this.fpr = fpr;
		this.filterSet = filterSet;
		this.searchCondition = searchCondition;
		this.suggestedFortifyHome = suggestedFortifyHome;
		
		this.logMsg = new StringBuilder(); 
		
		this.sourceanalyzerPath = null;
		this.reportGeneratorPath = null;
	}
	
	public FPRSummary invoke(File workspace, VirtualChannel channel) throws IOException {
		File template = null;
		File outputXml = null;
		File template2 = null;
		File outputXml2 = null;
		FPRSummary summary = new FPRSummary();
		this.logMsg = new StringBuilder(); 
		try {
			// we have to locate the FPR even if we don't need to run re
			File realFPR = locateFPR(workspace, fpr); 
			if ( null == realFPR ) {
				throw new RuntimeException("Can't locate FPR file");
			}
			summary.setFprFile(new FilePath(realFPR));

			// locate sourceanalyzer
			sourceanalyzerPath = locateSourceanalyzer(suggestedFortifyHome, logMsg);
			if ( null == sourceanalyzerPath ) {
				logMsg.append("Cannot locate sourceanalyzer, will skip plotting NVS chart\n");
				copyLogMsgToFPRSummary(summary);		
				return summary;
			}
			String versionStr = getSCAVersion(sourceanalyzerPath);
			if ( null == versionStr ) {
				logMsg.append("Cannot determine SCA version, will skip plotting NVS chart\n");
				copyLogMsgToFPRSummary(summary);		
				return summary;				
			}
			boolean useNewFPO = isNewFPO(versionStr);
			
			// locate reportGenerator
			reportGeneratorPath = locateReportGenerator(suggestedFortifyHome, logMsg); 
			if ( null == reportGeneratorPath ) {
				logMsg.append("Cannot locate reportGenerator, will skip plotting NVS chart\n");
				copyLogMsgToFPRSummary(summary);		
				return summary;
			}			
			
			template = saveReportTemplate();
			outputXml = createXMLReport(realFPR, template, filterSet);
			double nvs = calculateNvsFromReport(outputXml, useNewFPO, logMsg);	
			summary.setNvs(nvs);
		
			if ( !isEmpty(searchCondition) ) {
				template2 = saveReportTemplate(true, searchCondition);
				outputXml2 = createXMLReport(realFPR, template2, filterSet);
				int count = getCountFromReport(outputXml2);	
				if ( count > 0 ) {
					summary.setFailedCount(count);
				}
			}
			
		} catch (InterruptedException e) {
			IOException x = new IOException();
			x.initCause(e);
			throw x;
		} catch (DocumentException e) {
			e.printStackTrace();
			IOException x = new IOException();
			x.initCause(e);
			throw x;
		} finally {			
			deleteFile(template);
			deleteFile(outputXml);
			deleteFile(template2);
			deleteFile(outputXml2);			
		}

		// setup log message to FPRSummary
		copyLogMsgToFPRSummary(summary);		
		return summary;
	}
	
	private void copyLogMsgToFPRSummary(FPRSummary summary) {
		String s = logMsg.toString();
		if ( !StringUtils.isBlank(s) ) summary.log(s);		
		logMsg = new StringBuilder();
	}
	
	private static void deleteFile(File file) {
		if ( null != file && file.exists() ) {
			try {
				//System.out.println("Delete: " + file.getAbsolutePath());
				file.delete();
			} catch ( Exception e ) { }
		}
	}
	
	private File saveReportTemplate() throws IOException {
		return saveReportTemplate(false, null);
	}
	
	private File saveReportTemplate(boolean searchReport, String refinement) throws IOException {
		InputStream in = null;
		FileOutputStream out = null;
		try {
			String reportTemplate = null;
			if ( searchReport ) reportTemplate = "org/jvnet/hudson/plugins/fortify360/FPRPublisher/SearchReportDefinition.xml";
			else                reportTemplate = "org/jvnet/hudson/plugins/fortify360/FPRPublisher/ReportDefinition.xml";
			
			in = this.getClass().getClassLoader().getResourceAsStream(reportTemplate);
			byte b[] = new byte[8*1024];
			int n = in.read(b);
			
			if ( searchReport ) {
				String fileContent = new String(b, 0, n, "UTF-8");
				fileContent = fileContent.replace("<Refinement/>", "<Refinement>" + StringEscapeUtils.escapeHtml(refinement) + "</Refinement>");
				b = fileContent.getBytes("UTF-8");
				n = b.length;
			}
			
			File template = File.createTempFile("template", ".xml");
			out = new FileOutputStream(template);
			out.write(b, 0, n);
			return template;
		} finally {
			IOUtils.closeQuietly(in);
			IOUtils.closeQuietly(out);
		}
	}
	
	private File createXMLReport(File fpr, File template, String filterSet) throws InterruptedException, IOException {		
		File outputXml = File.createTempFile("report", ".xml");
		
		// reportGenerator -format xml -f xx.xml -template c:\issue_by_fpo.xml -source c:\WebGoat5.0\webgoat_57.fpr
		ArrayList<String> cmd = new ArrayList<String>();
		cmd.add(reportGeneratorPath);
		cmd.add("-format"); 	cmd.add("xml");
		cmd.add("-f"); 			cmd.add(outputXml.getAbsolutePath());
		cmd.add("-template"); 	cmd.add(template.getAbsolutePath());
		cmd.add("-source");		cmd.add(fpr.getAbsolutePath());		
		
		if ( !isEmpty(filterSet) ) {
			cmd.add("-filterSet");
			cmd.add(filterSet);
		}
		
		ProcessBuilder pb = new ProcessBuilder(cmd);
		logMsg.append("EXE: " + cmd.toString() + "\n");
		Process proc = pb.start();
		proc.waitFor();
		int exitValue = proc.exitValue();
		//System.out.println("Exit Value = " + exitValue);
		if ( 0 != exitValue ) {
			String err = "While running reportGenerator: " + IOUtils.toString(proc.getErrorStream());
			throw new RuntimeException(err);
		}
		
		return outputXml;
	}
	
	/** Calculate NSF of the FPR
	 * <p>For SCA 5.7, the NVS equation is
	 * <br/> NVS = ((((HFPO*10)+(MFPO*1)+(LFPO*.01))*.5)+(((P1*2)+P2*4)+(P3*16)+(PABOVE*64))*.5))/(ExecutableLOC/1000)
	 * </p>
	 * <p>For SCA 5.8 (F360 v2.5), according to Deprecation Note Normalized Vulnerability Score
	 * <br/> NVS = ((((CFPO*10)+(HFPO*5)+(MFPO*1)+(LFPO*0.1))*.5)+(((P1*2)+(P2*4)+(P3*16)+(PABOVE*64))*.5))/(ExecutableLOC/1000)
	 * 
	 * @param outputXml
	 * @return
	 * @throws DocumentException
	 */
	@SuppressWarnings("unchecked")
	private static double calculateNvsFromReport(File outputXml, boolean useNewFPO, StringBuilder logMsg) throws DocumentException {
		SAXReader reader = new SAXReader();
		Document document = reader.read(outputXml);
		
		String xPath1 = "//ReportDefinition/ReportSection/SubSection[Title='LOC']/Text";
		String loc = document.selectSingleNode(xPath1).getText();
		// System.out.println("Loc = " + loc);
		
		String xPath2 = "//ReportDefinition/ReportSection/SubSection/IssueListing/Chart/GroupingSection";
		List<Node> nodes = document.selectNodes(xPath2);
		
		double nvs = 0.0;
		// NVS = ((((HFPO*10)+(MFPO*1)+(LFPO*.01))*.5)+(((P1*2)+P2*4)+(P3*16)+(PABOVE*64))*.5))/(ExecutableLOC/1000)
		// High, Medium, Low
		// Not an Issue, Reliability Issue, Bad Practice, Suspicious, Real Issue|Expliotable
		for( Node groupSectionNode : nodes ) {
			String count = groupSectionNode.valueOf("@count");
			String title = groupSectionNode.selectSingleNode("groupTitle").getText();
			
			// System.out.println(title + " " + count);
						
			if ( "Critical".equalsIgnoreCase(title) ) {
				nvs += 0.5*10*Integer.parseInt(count);
			} if ( "High".equalsIgnoreCase(title) ) {
				if ( useNewFPO ) nvs += 0.5*5*Integer.parseInt(count);
				else             nvs += 0.5*10*Integer.parseInt(count);
			} else if ( "Medium".equalsIgnoreCase(title) ) {
				// both newFPO and oldFPO are 1
				nvs += 0.5*Integer.parseInt(count);
			} else if ( "Low".equalsIgnoreCase(title) ) {
				// oldFPO equation is 0.01, I think that's a typo
				// newFPO is 0.1
				nvs += 0.5*0.1*Integer.parseInt(count);
			} else if ( "Reliability Issue".equalsIgnoreCase(title) ) {
				nvs += 0.5*2*Integer.parseInt(count);
			} else if ( "Bad Practice".equalsIgnoreCase(title) ) {
				nvs += 0.5*4*Integer.parseInt(count);
			} else if ( "Suspicious".equalsIgnoreCase(title) ) {
				nvs += 0.5*16*Integer.parseInt(count);
			} else if ( "Expliotable".equalsIgnoreCase(title) || "Real Issue".equalsIgnoreCase(title) ) {
				// Sam NG is probably the only one who will change the analysis label, I use "Real Issue" instead of "Exploitable"
				nvs += 0.5*64*Integer.parseInt(count);
			}
		}

		nvs = nvs/(Integer.parseInt(loc)/1000.0);
		return nvs;
	}
	
	@SuppressWarnings("unchecked")
	private static int getCountFromReport(File outputXml) throws DocumentException {
		SAXReader reader = new SAXReader();
		Document document = reader.read(outputXml);
		
		String xPath2 = "//ReportDefinition/ReportSection/SubSection/IssueListing/Chart/GroupingSection";
		List<Node> nodes = document.selectNodes(xPath2);
		
		int total = 0;
		// NVS = ((((HFPO*10)+(MFPO*1)+(LFPO*.01))*.5)+(((P1*2)+P2*4)+(P3*16)+(PABOVE*64))*.5))/(ExecutableLOC/1000)
		// High, Medium, Low
		// Not an Issue, Reliability Issue, Bad Practice, Suspicious, Real Issue|Expliotable
		for( Node groupSectionNode : nodes ) {
			String count = groupSectionNode.valueOf("@count");
			String title = groupSectionNode.selectSingleNode("groupTitle").getText();
			
			// System.out.println(title + " " + count);
			
			// this works for both SCA 5.7 and 5.8
			if ( "Critical".equalsIgnoreCase(title) ) {
				total += Integer.parseInt(count);
			} else if ( "High".equalsIgnoreCase(title) ) {
				total += Integer.parseInt(count);
			} else if ( "Medium".equalsIgnoreCase(title) ) {
				total += Integer.parseInt(count);
			} else if ( "Low".equalsIgnoreCase(title) ) {
				total += Integer.parseInt(count);
			}
		}
		return total;
	}
	
	@SuppressWarnings("unchecked")
	private static File locateFPR(File path, String preferredFileName) {
		String ext[] = {"fpr"};
		Iterator<File> iterator = FileUtils.iterateFiles(path, ext, true);
		
		long latestTime = 0;
		File latestFile = null;
		while(iterator.hasNext()) {
			File file = iterator.next();
			if ( isEmpty(preferredFileName) || preferredFileName.equalsIgnoreCase(file.getName()) ) {
				if ( null == latestFile ) {
					latestTime = file.lastModified();
					latestFile = file;
				} else {
					// if this file is newer, we will use this file
					if ( latestTime < file.lastModified() ) {
						latestTime = file.lastModified();
						latestFile = file;
					
						// else if the last modified time is the same, but this file's file name is shorter, we will use this one
						// this to assume, if you copy the file, the file name is usually "Copy of XXX.fpr"
					} else if ( latestTime == file.lastModified() && latestFile.getName().length() > file.getName().length() ) {
						latestFile = file;
					}
				}
			}
		}
		
		return latestFile;
	}
	
	private static boolean isEmpty(String str) {
		return ( null == str || str.length() == 0 );
	}


	public static String locateReportGenerator(String suggestedFortifyHome, StringBuilder logMsg) {
		String imageName = "reportGenerator";
		return locateBinary(imageName, suggestedFortifyHome, logMsg);
	}
	
	public static String locateSourceanalyzer(String suggestedFortifyHome, StringBuilder logMsg) {
		String imageName = "sourceanalyzer";
		return locateBinary(imageName, suggestedFortifyHome, logMsg);
	}
	
	public static String locateBinary(String imageName, String suggestedFortifyHome, StringBuilder logMsg) {
		File[] list = null; 
		if ( null != suggestedFortifyHome ) {
			list = PathUtils.findBasenameInFolder(suggestedFortifyHome, imageName);
			if ( null != list && list.length > 0 ) 
				logMsg.append("Found " + imageName + " by using JARs Path Fortify360 Plugin global confir\n");
		}
		if ( null == list || list.length == 0 ) {
			String fortifyHome = System.getenv("FORTIFY_HOME");
			if ( null != fortifyHome ) {
				File bin = new File(fortifyHome, "bin");
				if ( bin.exists() && bin.isDirectory() ) {
					list = PathUtils.findBasenameInFolder(bin.toString(), imageName);
					if ( null != list && list.length > 0 ) 
						logMsg.append("Found " + imageName + " in FORTIFY_HOME environment variable\n");
				}
			}
		}
		if ( null == list || list.length == 0 ) {
			list = PathUtils.locateBaesnameInPath(imageName);
			if ( null != list && list.length > 0 ) 
				logMsg.append("Found " + imageName + " in PATH environment variable\n");
		}
		if ( null != list && list.length > 0 ) {
			String s = list[0].toString();
			logMsg.append(imageName + ": " + s + "\n");
			return s;
		} else {
			return null;
		}
	} 
	
	public static boolean isNewFPO(String versionStr) throws IOException, InterruptedException{
		VersionNumber version = new VersionNumber(versionStr);
		if ( version.compareTo(new VersionNumber("5.8")) >= 0 ) {
			return true;
		} else {
			return false;
		}
	}
	
	public static String getSCAVersion(String sourceanalyzerPath) throws IOException, InterruptedException {
		ArrayList<String> cmd = new ArrayList<String>();
		cmd.add(sourceanalyzerPath);
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
				String scaVersion = line.substring(x).trim();
				return scaVersion;
			}
		}
		
		return null;
	}		
}
