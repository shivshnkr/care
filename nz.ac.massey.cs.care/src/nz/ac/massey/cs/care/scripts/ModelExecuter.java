package nz.ac.massey.cs.care.scripts;

import static nz.ac.massey.cs.care.util.Printery.printRemovalStepStats;
import static nz.ac.massey.cs.care.util.Printery.println;
import static nz.ac.massey.cs.care.util.Utils.*;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nz.ac.massey.cs.care.Edge;
import nz.ac.massey.cs.care.Vertex;
import nz.ac.massey.cs.care.io.GraphMLReader;
import nz.ac.massey.cs.care.scoring.DefaultScoringFunction;
import nz.ac.massey.cs.care.scoring.ScoringFunction;
import nz.ac.massey.cs.care.util.ResultCounter;
import nz.ac.massey.cs.guery.Motif;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import com.google.common.base.Function;

import edu.uci.ics.jung.graph.DirectedGraph;

/**
 * Move class refactoring execution process on the graph level only. 
 * 
 * @author Ali, Jens
 */
public class ModelExecuter {
	private static String WORKSPACE_PATH = "output/";//"/Volumes/Data2/PhD/workspaces/CARE/nz.ac.massey.cs.care.move/";
	private static final int MAX_ITERATIONS = 10; // stop after this number of edges have been removed
	private static double initialInstances = 0;
	private Set<Edge> useLessEdges = new HashSet<Edge>();
	private static boolean DO_NOT_REMOVE_EDGES_WITH_NULL_SCORE = true;
	public static ScoringFunction scoringfunction = new DefaultScoringFunction();
	private IProject iProject = null;
	private static List<String> blacklisted = new ArrayList<String>();
	private static List<Motif<Vertex, Edge>> motifs;
	private static String[] outfiles;
	
	public static class VertexComparator implements Comparator<Vertex> {
		@Override
		public int compare(Vertex o1, Vertex o2) {
			return o1.getFullname().compareTo(o2.getFullname());
		}
	}
	public static void main(String[] args) throws Exception {
		File folder = new File("projects-todo/");
		loadMotifs();
		for(File f : folder.listFiles()) {
			if(!f.getName().endsWith(".graphml")) continue;
			GraphMLReader gmlReader = new GraphMLReader(new FileReader(f));
			setOutputFiles(f.getName());
			DirectedGraph<Vertex, Edge> g = gmlReader.readGraph();
			prepare(g);
			execute(g,0);
			gmlReader.close();
			System.gc();
		}
	}
	public static void execute(DirectedGraph<Vertex, Edge> g, int i)
			throws Exception {

		long ts1 = System.currentTimeMillis();

		if (i > MAX_ITERATIONS) return; // only check the first 50 iterations
		final ResultCounter registry = countAllInstances(g, getMotifs());
		int allInstances = registry.getNumberOfInstances();
		
		if (i == 0) {
			log("analysing cd queries");
			initialInstances = allInstances;
		}
		Double percent = (allInstances * 100) / initialInstances;
		if (allInstances == 0) {
			log("No more instances found at step ", i);
			printRemovalStepStats(getOutputFiles()[0], i, allInstances,
					round(percent));
			return;
		}
		
		long ts2 = System.currentTimeMillis();
		log("Iteration ", i, ", instances ", allInstances, ", instances ",
				round(percent), "%, detection took ",
				(ts2 - ts1) / 1000, "s");
		// find edges with highest rank
		final Map<Edge, Integer> map = new HashMap<Edge, Integer>();

		Set<Edge> edgesWithHighestRank = findLargestByIntRanking(g.getEdges(),
				new Function<Edge, Integer>() {
					@Override
					public Integer apply(Edge e) {
						return registry.getCount(e);
					}
				});
		Edge winner = edgesWithHighestRank.iterator().next();
		int topScore = registry.getCount(winner);
		
		if (topScore == 0 && DO_NOT_REMOVE_EDGES_WITH_NULL_SCORE) {
			System.out.println("only edges with score==0 found, will not remove edges");
			return;
		} else {
			Vertex s = winner.getStart();
			Vertex t = winner.getEnd();
			// remove the edge
			g.removeEdge(winner);
			s.getOutEdges().remove(winner);
			t.getInEdges().remove(winner);
			printRemovalStepStats(getOutputFiles()[0], i, allInstances,round(percent));
			map.clear();
			execute(g, i + 1);
		}
		
	}
	public void refreshProject() {
		try {
			iProject.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		} catch (CoreException e1) {
			e1.printStackTrace();
		}
		System.gc();
	}
	public static void setMotifs(List<Motif<Vertex, Edge>> motifss) {
		motifs = motifss;
	}
	/**
	 * <pre>
	 * Motifs order is as follows:
	 * 0. Strong CD
	 * 1. Weak CD
	 * </pre>
	 * @return
	 */
	public static List<Motif<Vertex, Edge>>getMotifs() {
		return motifs;
	}
	
	/**
	 * Classes that either should not be moved in the process or
	 * those classes that create error if they are moved outside
	 * their original package.
	 * @param classes
	 */
	public void addUnMovableClasses(List<String> classes) {
		blacklisted.addAll(classes);
	}
	
	public void addSkippedEdges(Set<Edge> edges) {
		useLessEdges.addAll(edges);
	}
	
	public static String getSimplename(String cname) {
		return cname.substring(cname.lastIndexOf(".") + 1);
	}
	


	
	/**<pre>
	 * Files are in this order:
	 * 0. instances.csv
	 * 1. details.csv
	 * 2. skipped_edges.csv
	 * 3. error_edge.csv
	 * 4. g2c_success.csv
	 * 5. constraints.csv
	 * </pre>
	 * @return outfiles
	 */
	public static String[] getOutputFiles() {
		return outfiles; 
	}
	public static void setOutputFiles(String graphfile) throws IOException {
		outfiles = getOutputFiles(graphfile);
	}
	private static void loadMotifs(){
		File qFolder = new File("queries");
		File[] queryFiles = qFolder
				.listFiles(getExcludeHiddenFilesFilter());
		List<Motif<Vertex, Edge>> motifss = new ArrayList<Motif<Vertex, Edge>>();
		for (int i = 0; i < queryFiles.length; i++) {
			File f = queryFiles[i];
			Motif<Vertex, Edge> m;
			try {
				m = loadMotif("queries/"+f.getName());
				if (m != null)
					motifss.add(m);
				motifs = motifss;
			} catch (Exception e) {
				System.out.println("could not load motif files");
				
			}
		}
		
	}
	public static String[] getOutputFiles(String graphfile) throws IOException {
		//the path looks like this /Volumes/Data2/PhD/workspaces/corpus2010/marauroa-3.8.1/bin
//		String separator = System.getProperty("file.separator");
		//first remove bin from the graph file name
//		graphfile = graphfile.substring(0, graphfile.lastIndexOf(separator));
		//now remove the first part to just get the name of the project
//		graphfile = graphfile.substring(graphfile.lastIndexOf(separator)+1);
		File outputFolder = new File(WORKSPACE_PATH);
		String[] filenames = new String[1];
		String f1 = outputFolder.getPath();
		f1 = f1 + "/" + graphfile + "_instances.csv";
		//print header of _instances file
		println(f1,"iteration,instances,instances(%)");
//		String f2 = outputFolder.getPath();
//		f2 = f2 + separator + graphfile + "_details.csv";
//		//print header of _details file
//		println(f2,"iteration,source,type,target,move,to,cd,src D before," +
//								 "src D after,tar D before,tar D after,edges skipped,time (ms)");
//		String f3 = outputFolder.getPath();
//		f3 = f3 + separator + graphfile + "_skipped_edges.csv";
//		//print header of _skipped edges
//		println(f3,System.getProperty("line.separator"));
//		println(f3,"iteration,source,type,target");
//		String f4 = outputFolder.getPath();
//		f4 = f4 + separator + graphfile + "_error_edge.csv";
//		//print header of _error_edge file
//		println(f4,"source,type,target,class2move,to package,error message");
//		String f5 = outputFolder.getPath();
//		f5 = f5 + separator + graphfile + "_g2c_success.csv";//graph to code level success
//		//print header of _g2c_success file
//		println(f5,"class2move,topackage,result");
//		String f6 = outputFolder.getPath();
//		f6 = f6 + separator + graphfile + "_constraints.csv";
//		//print header of constraints file
//		println(f6, "class2move,topackage,rename,blacklisted,accessability,instance count,compilation,result");
		filenames[0] = f1;
//		filenames[1] = f2;
//		filenames[2] = f3;
//		filenames[3] = f4;
//		filenames[4] = f5;
//		filenames[5] = f6;
		return filenames;
	}


}
