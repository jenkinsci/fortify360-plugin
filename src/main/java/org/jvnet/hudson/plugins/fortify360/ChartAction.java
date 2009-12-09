package org.jvnet.hudson.plugins.fortify360;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.*;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.ui.RectangleInsets;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import hudson.util.ColorPalette;
import hudson.util.DataSetBuilder;
import hudson.util.ChartUtil;
import hudson.util.ShiftedCategoryAxis;
import hudson.util.ChartUtil.NumberOnlyBuildLabel;

/** Responsible for plotting the NVS chart
 * 
 * @author sng
 *
 */
public class ChartAction implements Action {

	private AbstractProject<?,?> project;
	
	public ChartAction(AbstractProject<?,?> project) {
		this.project = project;
	}
	
	public String getDisplayName() {
		return "Fortify 360 Plugin";
	}

	public String getIconFileName() {
		//return "clipboard.gif";
		return null;
	}

	public String getUrlName() {
		return "fortify360-plugin";
	}
	
	public void doGraph(StaplerRequest req, StaplerResponse rsp) throws IOException {
		DataSetBuilder<String, NumberOnlyBuildLabel> dsb = new DataSetBuilder<String, NumberOnlyBuildLabel>();
		
		for(AbstractBuild<?, ?> b : project.getBuilds() ) {
			if ( b.isBuilding() ) continue;
			FPRSummary fprData = new FPRSummary();
			try {
				fprData.load(new File(b.getRootDir(), FPRSummary.FILE_BASENAME));
				dsb.add(fprData.getNvs(), "NVS", new NumberOnlyBuildLabel(b));
			} catch ( FileNotFoundException e ) { }
		}
		
		ChartUtil.generateGraph(req, rsp, createChart(dsb.build()), 400, 200);
	}
	
	// we need to unit test this API, need to make it public
	public static JFreeChart createChart(CategoryDataset dataset) throws IOException {
		JFreeChart chart = ChartFactory.createLineChart(
				"Normalized Vulnerability Score (NVS)", // chart title 
				"Build ID",  // categoryAxisLabel
				null,        // valueAxisLabel
				dataset, 
				PlotOrientation.VERTICAL, 
				false,       // legend 
				true,        // tooltips
				false        // urls
		);
		
		//final LegendTitle legend = chart.getLegend();
		//legend.setPosition(RectangleEdge.RIGHT);

		chart.setBackgroundPaint(Color.white);

		final CategoryPlot plot = chart.getCategoryPlot();

		//plot.setAxisOffset(new RectangleInsets(5.0, 0.0, 0.0, 5.0));
		plot.setBackgroundPaint(Color.WHITE);
		plot.setOutlinePaint(null);
		plot.setRangeGridlinesVisible(true);
		plot.setRangeGridlinePaint(Color.black);

		//CategoryAxis domainAxis = new ShiftedCategoryAxis(null);
		//plot.setDomainAxis(domainAxis);
		CategoryAxis domainAxis = plot.getDomainAxis();
		//domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_90);
		domainAxis.setLowerMargin(0.0);
		domainAxis.setUpperMargin(0.0);
		//domainAxis.setCategoryMargin(20.0);
		
		final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
		rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

		final LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
		renderer.setBaseStroke(new BasicStroke(1.0f));
		ColorPalette.apply(renderer);

		// crop extra space around the graph
		//plot.setInsets(new RectangleInsets(5.0, 5.0, 5.0, 5.0));

		return chart;
	}
	
}
