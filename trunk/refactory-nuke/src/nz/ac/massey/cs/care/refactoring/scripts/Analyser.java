package nz.ac.massey.cs.care.refactoring.scripts;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Map.Entry;
import com.google.common.base.Function;

import nz.ac.massey.cs.care.refactoring.views.CareView;
import nz.ac.massey.cs.gql4jung.DefaultScoringFunction;
import nz.ac.massey.cs.gql4jung.Edge;
import nz.ac.massey.cs.gql4jung.ResultCounter;
import nz.ac.massey.cs.gql4jung.ScoringFunction;
import nz.ac.massey.cs.gql4jung.Vertex;

import nz.ac.massey.cs.guery.ComputationMode;
import nz.ac.massey.cs.guery.GQL;
import nz.ac.massey.cs.guery.Motif;
import nz.ac.massey.cs.guery.MotifInstance;
import nz.ac.massey.cs.guery.Path;
import nz.ac.massey.cs.guery.PathFinder;

import nz.ac.massey.cs.guery.adapters.jung.JungAdapter;

import nz.ac.massey.cs.guery.impl.BreadthFirstPathFinder;
import nz.ac.massey.cs.guery.impl.GQLImpl;
import nz.ac.massey.cs.guery.impl.MultiThreadedGQLImpl;
import nz.ac.massey.cs.guery.util.ResultCollector;
import edu.uci.ics.jung.algorithms.scoring.BetweennessCentrality;
import edu.uci.ics.jung.algorithms.scoring.PageRank;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.Graph;
import static nz.ac.massey.cs.care.refactoring.scripts.Utils.*;
/**
 * Utility to batch process input files. Run with -Xmx1024M
 * @author Jens, Ali
 */
public class Analyser {
	public static final String TODO_DATAFOLDER = "corpus-todo";
	public static final String DONE_DATAFOLDER = "corpus-done";
	public static final String QUERY_FOLDER = "queries";
	private static final String WORKSPACE_PATH = "/Volumes/Data2/PhD/workspaces/CARE/refactory-nuke/";
	public static final String OUTPUT_FOLDER = WORKSPACE_PATH+"output/";
	public static final String SEP = ","; // used in csv files
	public static final String NL = System.getProperty("line.separator");
	public static double totalInstances = 0;
	public static Map<Edge,Integer> initBetwRanks = new LinkedHashMap<Edge,Integer>();
	public static int MAX_ITERATIONS = 50; // stop after this number of edges have been removed
	public static boolean MOVE_DONE = true;
	public static boolean DO_NOT_REMOVE_EDGES_WITH_NULL_SCORE = true;
	
	public static ScoringFunction scoringfunction = new DefaultScoringFunction();
	
	public static class VertexComparator implements Comparator<Vertex> {
		@Override
		public int compare(Vertex o1, Vertex o2) {
			// note that the container is ignored for performance reasons - this is ok, there is only one container per program
			return o1.getFullname().compareTo(o2.getFullname());
		}
	}
	
	
	public static void main(String[] args) throws Exception {
		
		org.apache.log4j.PropertyConfigurator.configure("log4j.properties");
		
		File data = new File(TODO_DATAFOLDER);
		assert data.isDirectory() && data.exists();
		File qFolder = new File(QUERY_FOLDER);
		assert qFolder.isDirectory() && qFolder.exists();
		File output = getOrAddFolder(OUTPUT_FOLDER);
		File[] dataFiles = data.listFiles(getExtensionFileFilter("jar"));
		int counter = 1, counter2=1 ;
		for (File datafile : dataFiles) {
			DirectedGraph<Vertex, Edge> g = loadGraph(datafile.getAbsolutePath());
			log("analysing file "+counter+"/"+dataFiles.length+" - "+datafile);
			log("using scoring function: ",scoringfunction.getClass());
			prepare(g);
			
			File[] queryFiles = qFolder.listFiles(getExcludeHiddenFilesFilter());
			List<Motif<Vertex,Edge>> motifs = new ArrayList<Motif<Vertex,Edge>>();
			for (int i = 0; i < queryFiles.length; i++) {
				File f = queryFiles[i];
				Motif<Vertex,Edge> m = loadQuery(f);
				if (m!=null) motifs.add(m);
			}
			analyse(g,datafile.getName(),0,motifs);
			counter2++;
			System.gc();
			counter++;
			//datafile.delete();
			if (MOVE_DONE) {
				File usedDatefile = new File(DONE_DATAFOLDER+'/'+datafile.getName());
				org.apache.commons.io.FileUtils.moveFile(datafile,usedDatefile);
			}
			System.gc();
		}
		log("done.");
	}

	public static void analyse (DirectedGraph<Vertex, Edge> g, String graphSource,int i,List<Motif<Vertex,Edge>> motifs) throws IOException {
		
		long ts1 = System.currentTimeMillis();
		
		if (i>MAX_ITERATIONS ) return; // only check the first 20 iterations

		String outfolder = "all-motifs";
		
//		final ResultCollector registry1 = countAllInstances(g, motifs);
//		List isntances = registry1.getInstances();
		final ResultCounter registry = CareView.countAllInstances(g, motifs);
		// log("Queries finished, starting aggregation and printing");
		
		File outputFolder = new File(OUTPUT_FOLDER+outfolder);
		if(!outputFolder.exists()) outputFolder.mkdir();
		String[] outfiles = getOutputFiles(graphSource,outputFolder);
		int instances = registry.getNumberOfInstances();
		if(i==0) {
			log("analysing "+outputFolder.getName()+" queries");
			totalInstances = instances;
		}
		Double instPercent = (instances * 100) / totalInstances ;

		if(registry.getNumberOfInstances()==0) {		
			log("No more instances found at step ",i);
			return;
		}

		// find edge with highest rank
		final Map<Edge,Integer> map = new HashMap<Edge,Integer>();
		
		List<Edge> edgesWithHighestRank = findSingleLargestByIntRanking(g.getEdges(),new Function<Edge,Integer>() {
			@Override
			public Integer apply(Edge e) {
				return registry.getCount(e);
			}});
//			

		// sort betweenness values descending order;
		final Map<Edge,Double> sortedBetwMap = getSortedBetwValues(g);
		
		// calculate betweenness rank
		final Map<Edge,Integer> betwRanks = rankBetwValues(sortedBetwMap);
		
		//compute the initial graph betweenness rank.
		if(i==0){
			initBetwRanks = new HashMap<Edge,Integer>(betwRanks); // copy as we clean this map at the end
		}

		// sort edges with highest rank according to lexicographical order of source and target
		// we tried to do this with betweenness rank - but this seems to make it non-deterministic
		Comparator<Edge> comp = new Comparator<Edge>() {
			@Override
			public int compare(Edge e1, Edge e2) {
				String s1 = e1.getStart().getFullname() + ">"+e1.getEnd().getFullname();
				String s2 = e2.getStart().getFullname() + ">"+e2.getEnd().getFullname();
				return s1.compareTo(s2);
			}
		};
		Collections.sort(edgesWithHighestRank,comp);
		
		Edge winner = edgesWithHighestRank.iterator().next();
		int topScore = registry.getCount(winner);
//		System.out.println(topScore);
		long ts2 = System.currentTimeMillis();
		
		if (topScore==0 && DO_NOT_REMOVE_EDGES_WITH_NULL_SCORE) {
			System.out.println("only edges with score==0 found, will not remove edges");return;
		}
		else {
			Vertex s = winner.getStart();
			Vertex t = winner.getEnd();
			// remove the edge
			g.removeEdge(winner);
			s.getOutEdges().remove(winner);
			t.getInEdges().remove(winner);
		}

		
		log("Iteration ",i,", instances ",instances,", instances ",round(instPercent),"%, detection took ",(ts2-ts1)/1000,"s");
		if (i==0) {
			// first line with table headers
			println(outfiles[0],"counter,instances,instances(%)");
			println(outfiles[1],"iteration,source,type,target,btwn rank,init btwn rank,awd,cd,deginh,stk,time (ms)");
		}
		
		printRemovalStepStats(outfiles[0],i,instances,round(instPercent));
		printRemovedEdgeDetails(outfiles[1],i+1,winner,registry.getCount("awd",winner),registry.getCount("cd",winner),registry.getCount("deginh",winner),registry.getCount("stk",winner),betwRanks.get(winner),initBetwRanks.get(winner),ts2-ts1);
		
		
		// release tmp variables before recursing
		map.clear();
		sortedBetwMap.clear();
		betwRanks.clear();
		System.gc();	
		
		analyse(g,graphSource,i+1,motifs);
	}


	private static Double round(Double val) {
		if ("NaN".equals(val.toString())) return new Double(-1);
	    int decimalPlace = 2;
	    BigDecimal bd = new BigDecimal(val.doubleValue());
	    bd = bd.setScale(decimalPlace,BigDecimal.ROUND_UP);
	    return bd.doubleValue();
	}
	public static ResultCollector countAllInstances(DirectedGraph<Vertex, Edge> g,
			List<Motif<Vertex, Edge>> motifs) {
		String outfolder = "";
		MultiThreadedGQLImpl<Vertex, Edge> engine = new MultiThreadedGQLImpl<Vertex, Edge>();
		PathFinder<Vertex, Edge> pFinder = new BreadthFirstPathFinder<Vertex, Edge>(
				true);

		final ResultCollector registry = new ResultCollector();

		for (Motif<Vertex, Edge> motif : motifs) {
			outfolder = outfolder + motif.getName() + "_";
			engine.query(new JungAdapter<Vertex, Edge>(g), motif, registry,
					ComputationMode.ALL_INSTANCES, pFinder);
		}
		return registry;
	}
	private static void rankEdges(MotifInstance<Vertex,Edge> inst, Map<Edge, Integer> map,Map<Edge,Map<String,Integer>> edgeInMotif) {


		Motif<Vertex,Edge> q = inst.getMotif();
		for (String pathRole : q.getPathRoles()) {
			Path<Vertex,Edge> p = inst.getPath(pathRole);
			for (Edge e : p.getEdges()) {
				Integer counter = 1;
				//computing the edge participation in different motifs
				Map<String,Integer> motifCounter = edgeInMotif.get(e);
				if(motifCounter == null){
					motifCounter = new HashMap<String,Integer>();
					motifCounter.put(q.getName(),counter);
					edgeInMotif.put(e, motifCounter);
				} else {
					counter = motifCounter.get(q.getName());
					if(counter==null) motifCounter.put(q.getName(), 1);
					else motifCounter.put(q.getName(), counter+1);
					edgeInMotif.put(e, motifCounter);
				}
				//assigning rank to edges in the motif instances
				Integer rank = map.get(e);
				if (rank == null)
					map.put(e, 1);
				else
					map.put(e, rank + 1);
			}
		}

	}

	private static Double getPageRank(DirectedGraph<Vertex, Edge> g,Vertex v) {
		PageRank<Vertex,Edge> rank = new PageRank<Vertex, Edge>(g, 0.1);
		rank.initialize();
		rank.step();
		return rank.getVertexScore(v);
	}

	private static SortedSet<Edge> getSortedEdges(final Map<Edge, Integer> map,final Map<Edge, Double> sortedBetwMap) {
		SortedSet<Edge> sortedEdges = new TreeSet<Edge>(new Comparator<Edge>(){

			@Override
			public int compare(Edge e1, Edge e2) {
				//check for rank comparison
				Integer e1Rank = map.get(e1);
				Integer e2Rank = map.get(e2);
				int rankCompare = e1Rank.compareTo(e2Rank);
				if(rankCompare!=0) return rankCompare;
				//check for betweenness comparison
				Double e1Betweennes = sortedBetwMap.get(e1);
				Double e2Betweennes = sortedBetwMap.get(e2);
				int betweennessCompare = e1Betweennes.compareTo(e2Betweennes);
				if(betweennessCompare!=0) return betweennessCompare;
				//check for source names
				String e1Source = e1.getStart().getFullname();
				String e2Source = e2.getStart().getFullname();
				int sourceNameCompare = e1Source.compareTo(e2Source);
				if(sourceNameCompare != 0) return sourceNameCompare;
				//check for target names
				String e1Target= e1.getEnd().getFullname();
				String e2Target = e2.getEnd().getFullname();
				int targetNameCompare = e1Target.compareTo(e2Target);
				if(targetNameCompare != 0) return targetNameCompare;
				return 0;
			}
		});
		sortedEdges.addAll(map.keySet());
		return sortedEdges;
	}

	private static File getOutputFolder(String outfolder) {
		String foldername = outfolder.substring(0, outfolder.lastIndexOf("_"));
		return new File(OUTPUT_FOLDER+foldername);
	}

	private static String[] getOutputFiles(String graphSource, File outputFolder) {
		String graphfile = graphSource;//.substring(0,graphSource.lastIndexOf("."));
		String[] filenames = new String[2];
		String f1 = outputFolder.getPath();
		f1 = f1 + "/" + graphfile + "_instances.csv";
		String f2 = outputFolder.getPath();
		f2 = f2 + "/" + graphfile + "_details.csv";
		filenames[0]=f1;
		filenames[1]=f2;
		return filenames;
	}

	private static int getHighestRank(Map<Edge, Integer> betwRankMap) {
		int hRank = 0;
		for(Map.Entry<Edge, Integer> entry:betwRankMap.entrySet()){
			if(entry.getValue()>hRank) hRank=entry.getValue();
		}
		return hRank;
	}

	static Map<Edge, Double> getSortedBetwValues(Graph<Vertex,Edge> g) {
		final BetweennessCentrality<Vertex, Edge> edgeBetweenness = new BetweennessCentrality<Vertex, Edge>(g);
		final Map<Edge,Double> betwMap = new LinkedHashMap<Edge,Double>();
		//computing edge betweenness.
		for(Edge e:g.getEdges()){
			betwMap.put(e, edgeBetweenness.getEdgeScore(e));
		}
		//sort betweenness map by values
		List<Map.Entry<Edge, Double>> list = new LinkedList<Map.Entry<Edge, Double>>(betwMap.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<Edge, Double>>() {
			@Override
			public int compare(Entry<Edge, Double> o1,Entry<Edge, Double> o2) {
				return -o1.getValue().compareTo(o2.getValue());
			}
		});
		Map<Edge, Double> result = new LinkedHashMap<Edge, Double>();
		for (Iterator<Map.Entry<Edge, Double>> it = list.iterator(); it.hasNext();) {
			Map.Entry<Edge, Double> entry = it.next();
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}
	//rank according to betweenness position in the map
	static Map<Edge, Integer> rankBetwValues(Map<Edge, Double> map) {
		Integer count = 0;
		Double previous =0.0;
		Map<Edge,Integer> result = new LinkedHashMap<Edge,Integer>();
		for(Map.Entry<Edge, Double> entry: map.entrySet()){
			if(!entry.getValue().equals(previous)){
				result.put(entry.getKey(), ++count);
				previous = entry.getValue();
			} else {
				result.put(entry.getKey(), count);
				previous = entry.getValue();
			}
		}
		return result;
	}

	private static void printRemovalStepStats(String filename, int i, int instances, double instPercent) throws IOException {
		FileWriter out = new FileWriter(filename,true);
		StringBuffer b = new StringBuffer()
			.append(i)
			.append(SEP)
			.append(instances)
			.append(SEP)
			.append(instPercent)
			.append(NL);
		out.write(b.toString());
		out.close();
		//log("result summary added to ",filename);
	}

	private static void printRemovedEdgeDetails(String filename, int i, Edge winner,int awd,int cd,int deginh,int stk,int betwRank, int initBetwRank,long time) throws IOException {
		FileWriter out = new FileWriter(filename, true);

		StringBuffer b = new StringBuffer()
			.append(i)
			.append(SEP).append(winner.getStart().getFullname())
			.append(SEP)
			.append(winner.getType())
			.append(SEP)
			.append(winner.getEnd().getFullname())
			.append(SEP)
			.append(betwRank)
			.append(SEP)
			.append(initBetwRank)
			.append(SEP)
			.append(awd)
			.append(SEP)
			.append(cd)
			.append(SEP)
			.append(deginh)
			.append(SEP)
			.append(stk)
			.append(SEP)
			.append(time)
			.append(NL);
		out.write(b.toString());
		out.close();
	}
	
	private static void println(String filename,String text) throws IOException {
		FileWriter out = new FileWriter(filename, true);
		out.write(text);
		out.write(NL);
		out.close();
	}
	
	/**
	 * Normalise the graph to make the outcomes of algorithms search for paths satisfying certain conditions predictable. 
	 * I.e., this means sorting incoming/outgoing edges.
	 * @param g
	 * @return
	 * @throws Exception
	 */
	/**
	 * @param g
	 * @throws Exception
	 */
	private static void prepare(DirectedGraph<Vertex, Edge> g) throws Exception {
		long before = System.currentTimeMillis();
		log("Start normalising graph - sort incoming/outgoing edges");
		
		final BetweennessCentrality<Vertex, Edge> edgeBetweenness = new BetweennessCentrality<Vertex, Edge>(g);
		Comparator<Edge> comp = new Comparator<Edge>() {
			@Override
			public int compare(Edge e1, Edge e2) {
				// compare betweenness scores first
				double d = edgeBetweenness.getEdgeScore(e1)-edgeBetweenness.getEdgeScore(e2);
				if (d!=0) {
					return d<0?-1:1;
				}
				// compare fully qualified names
				String n1 = getFullName(e1);
				String n2 = getFullName(e2);
				return n1.compareTo(n2);
			}
			private String getFullName(Edge e) {
				return new StringBuffer()
					.append(e.getStart().getFullname())
					.append(">")
					.append(e.getEnd().getFullname())
					.toString();
			}
		};

		for (Vertex v:g.getVertices()) {
			v.sortEdges(comp);
		}
		
		log("Graph normalised, this took " + (System.currentTimeMillis()-before),"ms" );
	}	

}
