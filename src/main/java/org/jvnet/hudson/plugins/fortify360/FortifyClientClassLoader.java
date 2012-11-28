package org.jvnet.hudson.plugins.fortify360;

import hudson.FilePath;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;

/** A special ClassLoader for loading the fortifyclient.jar which is a sub-module of Fortify360 hudson plugin 
 * <p>
 * When it loads, it will first try to locate wsclient.jar and wsobjects.jar, and then deduce the F360 server version
 * by checking the md5 of the jars. And then load the corresponding fortifyclient.jar.
 * <p>
 * Note the fortifyclient.jar is located in Fortify360Plugin inside the resources folder, not inside the lib, which will be 
 * automatically loaded
 * <p>
 * 
 * @author sng
 *
 */
public class FortifyClientClassLoader extends URLClassLoader {

	private static FortifyClientClassLoader _instance;
	private ClassLoader currentThreadLoader;
	
	private static Properties prop = new Properties();
	private static Set<String> supportedVersions = new TreeSet<String>();
	private static Map<String, String> wsclientMd5 = new HashMap<String, String>();
	private static Map<String, String> wsobjectsMd5 = new HashMap<String, String>();
	
	public FortifyClientClassLoader(URL[] urls, ClassLoader parent) { 
		super(urls, parent);
		currentThreadLoader = null;
	}
	
	public static Set<String> getSupportedVersions() {
		initMd5Map();
		return Collections.unmodifiableSet(supportedVersions);
	}
	
	public static synchronized void initMd5Map() {
		InputStream in = null;
		try {
			in = FortifyClientClassLoader.class.getClassLoader().getResourceAsStream("fortify360.properties");
			prop.load(in);
		} catch (IOException e) {
			e.printStackTrace(System.err);
		} finally {
			if ( null != in ) try { in.close(); } catch ( Exception e ) {}
		}		
		Enumeration<?> names = prop.propertyNames();
		for (Enumeration<?> e = prop.propertyNames(); e.hasMoreElements();) {
			String pname = (String)e.nextElement();
			if ( pname.startsWith("wsclient-") ) {
				String ver = pname.substring(9);
				supportedVersions.add(ver);
				wsclientMd5.put(prop.getProperty(pname), ver);
			} else if ( pname.startsWith("wsobjects-") ) {
				String ver = pname.substring(10);
				supportedVersions.add(ver);
				wsobjectsMd5.put(prop.getProperty(pname), ver);
			}
		}
	}
	
	public static synchronized FortifyClientClassLoader getInstance(String jarsPath, String version, PrintStream log) throws Exception { 
		
		initMd5Map();
		
		if ( null == _instance ) {
		
			ClassLoader parent = FortifyClientClassLoader.class.getClassLoader();
			List<URL> urls = new ArrayList<URL>();
			// get the wsclient.jar and wsobject.jar
			if ( StringUtils.isBlank(jarsPath) ) jarsPath = findWSClientPath(); 
			log.println("####################################################################");
			log.println("FortifyClientClassLoader: JarsPath = " + jarsPath);
			File wsclient = findFileByJarBasename(new File(jarsPath), "wsclient");
			File wsobjects = findFileByJarBasename(new File(jarsPath), "wsobjects");
			if ( !wsclient.exists() || !wsobjects.exists() ) {
				throw new RuntimeException("Invalid JarsPath: " + jarsPath + ", can't locate wsclient.jar or wsobjects.jar");
			}
			urls.add(wsclient.toURI().toURL());
			urls.add(wsobjects.toURI().toURL());

			// now, I need to deduce the F360 server version, this is a bit tricky...
			// 1. I will check the md5 of the wsclient.jar and wsobject.jar
			// 2. if md5 is not found, I will check the path name directly
			String wsclientVer = wsclientMd5.get(new FilePath(wsclient).digest());
			String wsobjectsVer = wsobjectsMd5.get(new FilePath(wsobjects).digest());
			
			if ( StringUtils.isBlank(version) ) {
				// the two versions need to be consistent
				if ( null != wsclientVer && wsclientVer.equals(wsobjectsVer) ) {
					version = wsclientVer;
					log.println("FortifyClientClassLoader: version = " + version + " (by checking md5 of wsclient.jar and wsobjects.jar)");
				} else {
					// check the path name, the checking is pretty simple
					// supprtedVersions is something like ["2.1", "2.5", ... "3.50", "3.60", ....], initialized in initMd5Map()
					for(String ver : supportedVersions) {
						if ( -1 != jarsPath.indexOf(ver) ) {
							version = ver;
							break;
						}
					}
					log.println("FortifyClientClassLoader: version = " + version + " (by checking the path of jarsPath)");
				}
			} else {
				log.println("FortifyClientClassLoader: version = " + version);				
			}

			if ( StringUtils.isBlank(version) ) {
				throw new RuntimeException("Can't determine Fortify 360 Server version");
			}
			
			if ( null != version && version.startsWith("3") ) {
				File fortifyCommon = new File(jarsPath, "fortify-common-" + version + ".jar");
				if ( !fortifyCommon.exists() ) {
					throw new RuntimeException("Invalid JarsPath: " + jarsPath + ", can't locate fortify-common-" + version + ".jar");
				}
				urls.add(fortifyCommon.toURI().toURL());
				
			} else if ( !version.startsWith("2.5") ) {
				// if not 3.0.0, then we will need the common.jar and common13.jar unless for v2.5
				// for 2.6, we need to include common.jar and common13.jar as well
				File common = new File(jarsPath, "common.jar");
				File common13 = new File(jarsPath, "common13.jar");
				if ( !common.exists() || !common13.exists() ) {
					throw new RuntimeException("Invalid JarsPath: " + jarsPath + ", can't locate common.jar or common13.jar");
				}
				urls.add(common.toURI().toURL());
				urls.add(common13.toURI().toURL());
			}
			
			// ok, load the correct version of fortifyclient
			urls.add(parent.getResource("fortifyclient-" + version + ".jar"));

			for(URL x : urls) {
				log.println("FortifyClientClassLoader URL: " + x.toString());
			}
			log.println("####################################################################");
			_instance = new FortifyClientClassLoader(urls.toArray(new URL[0]), parent);
		}
		
		return _instance;
	}
	
	public static void reset() {
		_instance = null;
	}
	
	/** A special hack for Hudson
	 * <p>
	 * When Hudson loads Fortify360Plugin, it use a special ClassLoader, let's call it PluginClassLoader.
	 * But when a Hudson job runs, it will run as a normal worker thread, and this worker thread is not PluginClassLoader.
	 * Therefore, it won't not found the fortifyclient.jar which is stored in Fortify360plugin as a resources.
	 * <p>
	 * Therefore, the solution is when we need to run FortifyClient, we will need to change the worker thread ClassLoader 
	 * to *this* ClassLoader (which is the PluginClassLoader)
	 * </p> 
	 * 
	 */
	public void bindCurrentThread() {
		if ( null != currentThreadLoader ) {
			throw new IllegalStateException(this.getClass().getName() + ".bindCurrentThread(), rebinding is not allowed. Probably previously binded without unbinding. currentThreadLoader is " + currentThreadLoader);
		}
		currentThreadLoader =  Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(this);
	}
	
	/** A special hack for Hudson
	 * <p>
	 * When Hudson loads Fortify360Plugin, it use a special ClassLoader, let's call it PluginClassLoader.
	 * But when a Hudson job runs, it will run as a normal worker thread, and this worker thread is not PluginClassLoader.
	 * Therefore, it won't not found the fortifyclient.jar which is stored in Fortify360plugin as a resources.
	 * <p>
	 * Therefore, the solution is when we need to run FortifyClient, we will need to change the worker thread ClassLoader 
	 * to *this* ClassLoader (which is the PluginClassLoader)
	 * </p> 
	 * 
	 */
	public void unbindCurrentThread() {
		if ( null != currentThreadLoader ) {
			Thread.currentThread().setContextClassLoader(currentThreadLoader);
			currentThreadLoader = null;
		}
	}
	
	@Override
	protected Class<?> findClass(String className) throws ClassNotFoundException {
		try {
			return super.findClass(className);
		} catch ( ClassNotFoundException e ) {
			if ( null != currentThreadLoader ) {
				return currentThreadLoader.loadClass(className);
			} else {
				throw e;
			}
		}
	}
	
	@Override
	public URL getResource(String name) {
		URL url = findResource(name);
		if ( null == url ) {
			if ( null != currentThreadLoader ) {
				url = currentThreadLoader.getResource(name);
			}
		}
		return url;
	}	
	
	public static String findWSClientPath() {
		File[] list = PathUtils.locateBaesnameInPath("fortifyclient");
		if ( null != list && list.length > 0 ) {
			File f = list[0];
			// e.g. f is "C:\\Program Files\\Fortify Software\\Fortify 360 v2.6.5\\bin\\fortifyclient.bat"
			// we need to change this to "C:\\Program Files\\Fortify Software\\Fortify 360 v2.6.5\\Core\\lib
			File core = new File(f.getParentFile().getParentFile(), "Core");
			File lib = new File(core, "lib");
			File wsclient = findFileByJarBasename(lib, "wsclient");
			if ( null != wsclient ) return lib.toString();
		}
		return null;
	}
	
	/** Find the file by using the jar basename
	 * <p>E.g. you can find wsclient in a given path, then we will try to find
	 * wsclient.jar, wsclient-x.x.x.jar in the path
	 * <p>If we can find such a jar, the full name of the jar will be returned
	 * 
	 * @param path e.g. a library path
	 * @param basename e.g. wsclient
	 * @return the jar file with the given basename
	 */
	public static File findFileByJarBasename(File path, String basename) {
		if ( !path.exists() || !path.isDirectory() ) return null;
		
		// find by exact basename.jar
		File exactFile = new File(path, basename + ".jar");
		if ( exactFile.exists() ) return exactFile;;
		
		// find by basename-x.y.z.jar
		File[] list = path.listFiles();
		Pattern p = Pattern.compile(basename + "\\-\\d(\\.\\d+)+\\.jar");
		for(File file : list) {
			if (p.matcher(file.getName()).matches()) return file;
		}
		return null;
	}
	
//	/** By using the search paths, retrieve the path that contains the wsclient.jar
//	 * 
//	 * @return the path that contains the wsclient.jar, null if not found
//	 */
//	public static String findWSClientPath() {
//		Map<String, String> env = System.getenv();
//		String path = null;
//		for(String key : env.keySet()) {
//			// the key may be case sensitive on Unix
//			if ( "PATH".equalsIgnoreCase(key) ) {
//				path = env.get(key);
//				break;
//			}
//		}
//		String pathSep = System.getProperty("path.separator"); // ":" on Unix, ";" on Win
//		if ( null != path ) {
//			String[] array = path.split(Pattern.quote(pathSep)); // need to quote the pathSep coz it is metachar
//			for(String s : array ) {
//				String p = fortifyclientPath(new File(s));
//				if ( null != p ) return p;
//			}
//		}
//		return null;
//	}
//	
//	/** Check if the path is a Fortify Product path, and if yes, return the path that contains wsclient.jar
//	 * 
//	 * @param path a segment of the search PATH
//	 * @return the path that contains the wsclient.jar, null if it the path is not a F360 product path
//	 */
//	public static String fortifyclientPath(File path) {
//		if ( path.exists() && path.isDirectory() ) {
//			FilenameFilter filter = new FilenameFilter() {
//				@Override
//				public boolean accept(File dir, String name) {
//					int x = name.lastIndexOf(".");
//					String basename = name;
//					if ( x >= 0 ) {
//						basename = name.substring(0, x);
//					}
//					if ( basename.equalsIgnoreCase("fortifyclient") ) return true;
//					else return false;
//				}
//			}; 
//			String[] files = path.list(filter);
//			if ( null != files && files.length > 0 ) {
//				// ok, this path contains "fortifyclient"
//				// there can be two possibilities
//				// 1. the machine installed SCA, <SCA>\bin\fortifyclient, <SCA>\Core\lib\wsclient.jar
//				// 2. the machine installed F360 server Demo Suite and changed the search path
//				// <F360Demo>\Tools\fortifyclient\bin\fortifyclient, <F360Demo>\Tools\fortifyclient\Core\lib\wsclient.jar
//				String s = System.getProperty("file.separator");
//				File jar = new File(path, ".." + s + "Core" + s + "lib" + s + "wsclient.jar");
//				if ( jar.exists() ) return jar.getParentFile().getAbsolutePath();
//			}
//		}
//		return null;
//	}
}
