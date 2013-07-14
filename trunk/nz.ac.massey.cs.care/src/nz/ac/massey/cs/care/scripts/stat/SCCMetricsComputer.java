package nz.ac.massey.cs.care.scripts.stat;

import static nz.ac.massey.cs.care.util.Printery.println;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import nz.ac.massey.cs.care.Edge;
import nz.ac.massey.cs.care.Vertex;
import nz.ac.massey.cs.care.io.GraphMLReader;
import nz.ac.massey.cs.care.metrics.MetricsResult;
import nz.ac.massey.cs.care.refactoring.views.CompositeRefactoringView;
import nz.ac.massey.cs.care.util.Printery;
import edu.uci.ics.jung.graph.DirectedGraph;

public class SCCMetricsComputer {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		String output = "output/metrics/";
		File graphFolder = new File("graphs-todo/");
		File folder = new File("projects-todo/");
		int size = folder.listFiles().length;
		int counter = 1;
		for(File p : folder.listFiles()) { 
			if(p.getName().contains("svn")) continue;
			System.out.println("project " + counter ++ + "/" + size);
			String projectName = p.getName();
			String graphBefore = projectName + "_before.graphml";
			String graphAfter = projectName + "_after.graphml";
			File before = getFile(graphFolder, graphBefore);
			File after = getFile(graphFolder, graphAfter);
			DirectedGraph<Vertex, Edge> beforeGraph = new GraphMLReader(new FileReader(before)).readGraph();
			MetricsResult beforeMetrics = CompositeRefactoringView.computeMetrics(beforeGraph);
			DirectedGraph<Vertex, Edge> afterGraph = new GraphMLReader(new FileReader(after)).readGraph();
			MetricsResult afterMetrics = CompositeRefactoringView.computeMetrics(afterGraph);
			String[] metricsFiles = getMetricsOutputFiles(p.getName());
			Printery.printMetrics(metricsFiles, beforeMetrics, afterMetrics);
		}

		System.out.println("done");
	}
	
	private static File getFile(File folder, String name) {
		
		for(File f : folder.listFiles()) {
			if(f.getName().equals(name)) return f;
		}
		
		return null;
	}
	
	public static String[] getMetricsOutputFiles(String graphfile) throws IOException {
		//the path looks like this /Volumes/Data2/PhD/workspaces/corpus2010/marauroa-3.8.1/bin
		String separator = System.getProperty("file.separator");
		//first remove bin from the graph file name
//		graphfile = graphfile.substring(0, graphfile.lastIndexOf(separator));
		//now remove the first part to just get the name of the project
//		graphfile = graphfile.substring(graphfile.lastIndexOf(separator)+1);
		File outputFolder = new File("output/metrics/");
		String[] filenames = new String[2];
		String f1 = outputFolder.getPath();
		f1 = f1 + separator + graphfile + "_metrics.csv";
		//print header of _metrics file
		println(f1,"compression before,compression after,max scc size before,"+
				"max scc size after,density before,density after,tangledness before," +
				"tangledness after,count before,count after,"+
				"modularity before,modularity after,distance before,distance after," +
				"total time");
		String f2 = outputFolder.getPath();
		f2 = f2 + separator + graphfile + "_package_metrics.csv";
		//print header of _distance file
		println(f2,"package,CE before,CE after,CA before,CA after,A before,A after," +
				"I before,I after,D before,D after");
		filenames[0] = f1;
		filenames[1] = f2;
		return filenames;
	}

}
