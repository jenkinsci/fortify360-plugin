package org.jvnet.hudson.plugins.fortify360;

import java.awt.image.BufferedImage;
import java.io.*;

import static org.junit.Assert.*;

import org.jfree.chart.encoders.SunPNGEncoderAdapter;
import org.jfree.data.category.DefaultCategoryDataset;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hudson.plugins.fortify360.ChartAction;
import org.jfree.chart.JFreeChart;

public class ChartActionTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testCreateChart() throws Exception {
		int data[] = {10, 11, 9, 12, 13, 14, 16};
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		for(int i=0; i<data.length; i++) {
			dataset.addValue(data[i], Integer.valueOf(0), new Integer(i));
		}

		JFreeChart chart = ChartAction.createChart(dataset);
		
		BufferedImage image = chart.createBufferedImage(400, 200);
		SunPNGEncoderAdapter png = new SunPNGEncoderAdapter(); 
		
		File tmp = File.createTempFile("test", ".png");
		FileOutputStream out = new FileOutputStream(tmp);
		png.encode(image, out);
		
		// no exception, that means ok
		System.out.println("PNG = " + tmp.getAbsolutePath());
	}	
}
