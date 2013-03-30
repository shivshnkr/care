package nz.ac.massey.cs.care.move.scripts;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.collections15.Transformer;

import edu.uci.ics.jung.graph.DirectedGraph;

import nz.ac.massey.cs.care.move.E;
import nz.ac.massey.cs.care.move.Edge;
import nz.ac.massey.cs.care.move.V;
import nz.ac.massey.cs.care.move.Vertex;
import nz.ac.massey.cs.care.move.metrics.Modularity;
import nz.ac.massey.cs.care.move.metrics.PackageMetrics;
import nz.ac.massey.cs.care.move.metrics.PackageMetrics.Metrics;
import nz.ac.massey.cs.care.move.metrics.SCCMetrics;
import nz.ac.massey.cs.guery.adapters.jung.JungAdapter;
import nz.ac.massey.cs.guery.impl.ccc.NullFilter;
import nz.ac.massey.cs.guery.impl.ccc.TarjansAlgorithm;
import static nz.ac.massey.cs.care.move.util.Utils.*;


public class MetricsComputer {

	private static final String WORKSPACE_PATH = "/Volumes/Data2/PhD/workspaces/corpus2010/";
	private static final String OUTPUT_FOLDER = "output/";
	private static final String PATH_SEP = System.getProperty("file.separator");
	private static final String SEP = ","; // used in csv files
	private static final String NL = System.getProperty("line.separator");
	private static final String SUMMARY = OUTPUT_FOLDER + PATH_SEP + "metrics_data.csv";
	static Transformer<Vertex,String> componentMembership = new Transformer<Vertex,String>() {
		@Override
		public String transform(Vertex s) {
			return s.getNamespace();//s.substring(0,s.indexOf('.')); // component is first token in name
		}
	};
	/**
	 * class to compute metrics (SCC, modularity, distance) before and after refactorings
	 * @param args
	 * @throws Exception 
	 */
//	public static void main(String[] args) throws Exception {
//		File[] projects = CareView.getProjectFiles(null);
//		printHeader();
//		int count = 0;
//		for(File project : projects) {
//			count ++;
//			System.out.println("analysing " + count + "/" + projects.length);
//			String path = WORKSPACE_PATH + project.getName() + PATH_SEP + "bin";
//			DirectedGraph<Vertex, Edge> g = loadGraph(path);
//			SCCMetrics scc = computeSCC(loadPowerGraph(g));
//			double modularity = Modularity.computeScaledModularity(g, componentMembership);
//			double distance = getDistance(g);
//			printMetrics(project.getName(), scc, modularity, distance);
//		}
//	}
	private static void printMetrics(String name, SCCMetrics scc,
			double modularity, double distance) throws Exception{
		FileWriter out = new FileWriter(SUMMARY, true);
		StringBuffer b = new StringBuffer()
			.append(name)
			.append(SEP)
			.append(modularity)
			.append(SEP)
			.append(distance)
			.append(SEP)
			.append(scc.getCompressionRatio())
			.append(SEP)
			.append(scc.getMaxSccSize())
			.append(SEP)
			.append(scc.getDensity())
			.append(SEP)
			.append(scc.getTangledness())
			.append(SEP)
			.append(scc.getCount())
			.append(NL);
		out.write(b.toString());
		out.close();
		
	}
	private static void printHeader() throws Exception{
		FileWriter out = new FileWriter(SUMMARY, true);
		StringBuffer b = new StringBuffer()
			.append("program")
			.append(SEP)
			.append("modularity")
			.append(SEP)
			.append("distance")
			.append(SEP)
			.append("compression")
			.append(SEP).append("Max SCC size")
			.append(SEP).append("density")
			.append(SEP).append("spl")
			.append(SEP).append("count")
			.append(NL);
		out.write(b.toString());
		out.close();
	}
	
	public static double getDistance(DirectedGraph<Vertex, Edge> g) throws Exception {
		PackageMetrics pm = new PackageMetrics();
		Map<String, PackageMetrics.Metrics> values = pm.compute(g, "");
		double totalpackages = values.size();
		double totalDistance = 0.0;
		for(Entry<String, Metrics> e : values.entrySet()){
			double d = e.getValue().getD();
			totalDistance += d;
		}
		return totalDistance/totalpackages;
	}
	public static SCCMetrics computeSCC(DirectedGraph<V, E> powergraph) {
		DirectedGraph<Set<V>,Integer> sccGraph = null;
		TarjansAlgorithm<V,E> sccBuilder = new TarjansAlgorithm<V,E>();
		sccBuilder.buildComponentGraph(new JungAdapter<V,E>(powergraph),NullFilter.DEFAULT);
		sccGraph = sccBuilder.getComponentGraph();
		SCCMetrics metrics = new SCCMetrics();
		metrics.compute(sccGraph, powergraph);
		return metrics;
	}
	
	public static void printSCCInfoHeader(String filename) throws IOException {
		FileWriter out = new FileWriter(filename, true);
		StringBuffer b = new StringBuffer()
				.append("compression before").append(SEP)
				.append("compression after")
				.append(SEP).append("Max SCC size before").append(SEP)
				.append("Max SCC size after")
				.append(SEP).append("density before")
				.append(SEP).append("density after")
				.append(SEP).append("spl before")
				.append(SEP).append("spl after")
				.append(SEP).append("count before")
				.append(SEP).append("count after")
				.append(NL);
		out.write(b.toString());
		out.close();
		// log("result summary added to ",filename);
	}
	
	private static void printModularityInfo(String filename, double modularityBefore,
			double modularityAfter) throws IOException {
		FileWriter out = new FileWriter(filename, true);
		StringBuffer b = new StringBuffer()
				.append(round(modularityBefore)).append(SEP)
				.append(round(modularityBefore))
				.append(NL);
		out.write(b.toString());
		out.close();
	}

	private static void printModularityInfoHeader(String filename) throws IOException {
		FileWriter out = new FileWriter(filename, true);
		StringBuffer b = new StringBuffer()
				.append("modularity before").append(SEP)
				.append("modularity after")
				.append(NL);
		out.write(b.toString());
		out.close();
		
	}
	public static void printSCCInfo(String filename, SCCMetrics before, SCCMetrics after) throws IOException {
		FileWriter out = new FileWriter(filename, true);
		StringBuffer b = new StringBuffer()
				.append(before.getCompressionRatio()).append(SEP)
				.append(after.getCompressionRatio())
				.append(SEP).append(before.getMaxSccSize()).append(SEP)
				.append(after.getMaxSccSize())
				.append(SEP).append(before.getDensity())
				.append(SEP).append(after.getDensity())
				.append(SEP).append(before.getTangledness())
				.append(SEP).append(after.getTangledness())
				.append(SEP).append(before.getCount())
				.append(SEP).append(after.getCount())
				.append(NL);
		out.write(b.toString());
		out.close();
		// log("result summary added to ",filename);
	}
}
