package org.jvnet.hudson.plugins.fortify360;

/** A simple case to handle version number comparison
 * <p>
 * Only accept numbers in the version number, e.g. 1.2.3.4. Doesn't support alphabets, e.g. 1.2.4a
 * <p>
 * E.g. 1.10 is large than 1.2 <br/>
 * E.g. 1.2.1 is large than 1.2
 * 
 * @author sng
 *
 */
@SuppressWarnings("unchecked")
public class VersionNumber implements Comparable {
	
	private int[] version;
	
	public VersionNumber(String versionStr) {
		String[] array = versionStr.trim().split("\\.");
		version = new int[array.length];
		for(int i=0; i<array.length; i++) {
			version[i] = Integer.parseInt(array[i].trim());
		}
	}

	public int compareTo(Object o) {
		if ( null == o ) return 1;
		if ( o instanceof VersionNumber ) {
			VersionNumber that = (VersionNumber)o;
			if ( null == that.version && null == this.version ) return 0;
			else if ( null == that.version && null != this.version) return 1;
			else if ( null == this.version && null != that.version ) return -1;
			else {
				// both version are not null
				for(int i=0; i<this.version.length; i++) {
					if ( i >= that.version.length ) return 1;
					else {
						if ( this.version[i] > that.version[i] ) return 1;
						else if ( that.version[i] > this.version[i] ) return -1;
						// else --> that are equal
					}
				}
				// reach here, can be two conditions
				if ( this.version.length == that.version.length ) return 0;
				return -1;
			}
		} else {
			return 1;
		}
	}

	
}
