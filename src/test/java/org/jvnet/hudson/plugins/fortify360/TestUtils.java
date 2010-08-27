package org.jvnet.hudson.plugins.fortify360;

import java.io.*;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

public class TestUtils {
	public static File resourceToFile(String filename) throws IOException {
		InputStream in = null;
		OutputStream out = null;
		try {
			File tmp = File.createTempFile("test", "." + FilenameUtils.getExtension(filename));
			tmp.deleteOnExit();
			in = TestUtils.class.getClassLoader().getResourceAsStream(filename);
			out = new FileOutputStream(tmp);
			IOUtils.copy(in, out);
			return tmp;
		} finally {
			IOUtils.closeQuietly(in);
			IOUtils.closeQuietly(out);
		}
	}
}
