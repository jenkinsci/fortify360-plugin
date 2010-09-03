package org.jvnet.hudson.plugins.fortify360;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;

public class PathUtils {

	public static File[] locateBaesnameInPath(String filename) {
		Map<String, String> env = System.getenv();
		String path = null;
		for(String key : env.keySet()) {
			// the key may be case sensitive on Unix
			if ( "PATH".equalsIgnoreCase(key) ) {
				path = env.get(key);
				break;
			}
		}
		
		String pathSep = System.getProperty("path.separator"); // ":" on Unix, ";" on Win
		if ( null != path ) {
			String[] array = path.split(Pattern.quote(pathSep)); // need to quote the pathSep coz it is metachar
			for(String s : array ) {
				File[] files = findBasenameInFolder(s, filename);
				if ( null != files && files.length > 0 ) return files;
			}
		}
		return null;
	}
	
	public static File[] findBasenameInFolder(String pathname, String filename) {
		File path = new File(pathname);
		File[] files = null;
		if ( path.exists() && path.isDirectory() ) {
			final String finalBasename = FilenameUtils.getBaseName(filename);
			FilenameFilter filter = new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return finalBasename.equalsIgnoreCase(FilenameUtils.getBaseName(name));
				}
			}; 
			files = path.listFiles(filter);
		}
		return files;
	}
	
}
