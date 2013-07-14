package nz.ac.massey.cs.care.scripts.stat;

import static nz.ac.massey.cs.care.util.Utils.getExtensionFileFilter;
import static nz.ac.massey.cs.care.util.Utils.loadGraph;
import static nz.ac.massey.cs.care.util.Utils.log;
import static nz.ac.massey.cs.care.util.Utils.removeExtension;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import nz.ac.massey.cs.care.Edge;
import nz.ac.massey.cs.care.Vertex;
import au.com.bytecode.opencsv.CSVReader;
import edu.uci.ics.jung.graph.DirectedGraph;

/**
 * Script to merge the data about removed edges and pattern counts into a single cvs file for statistical analysis.
 * @author jens dietrich
 */
public class PackageMergeChecker {
	
	public final static String FOLDER = "output/cd/";
	public final static String OUT_FOLDER = "output/packages_merged/";
	public final static int MAX_ROWS = 101;
	public final static int COLUMN2MERGE = 2; // relative percentage
	public final static String OUTPUT_FILE = "output/stats/"+"merged_packages_stats.csv";
	public final static String OUTPUT_FILE1 = "output/stats/"+"packages_stats.csv";
	public final static String POSTFIX2REMOVE = "_package_metrics.csv"; // will be removed from file name to get graph name
	public final static String INPUT_FOLDER = FOLDER;
	public final static String SEP = ",";
	public final static String POSTFIX = "_details.csv";
	public final static String DATA_FOLDER = "corpus-todo"; // jars are here
	static PrintWriter out2 = null;
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		org.apache.log4j.PropertyConfigurator.configure("log4j.properties");
		new PrintWriter(OUTPUT_FILE1);
		Map<String,List<List<String>>> filedata = parseData();
		File outfolder = new File(OUT_FOLDER);
		if(!outfolder.exists())outfolder.mkdirs();
//		PrintWriter out = new PrintWriter(new FileWriter(OUTPUT_FILE));
		List<PackageStats> statsList = new ArrayList<PackageStats>();
		// print 1st row - data file names
//		out.print("program");
//		for (String fileName:filedata.keySet()) {
//			out.print(SEP);
//			out.print(removeExtension(fileName,POSTFIX2REMOVE));
//		}
//		out.println();
//		
//		// print 2nd row - packages merged
//		out.print("packages merged");
		PrintWriter out1 = null;
		out2 = new PrintWriter(new FileWriter(OUTPUT_FILE1));
		File folder = new File(INPUT_FOLDER);
		for (File file : folder.listFiles(getExtensionFileFilter(POSTFIX))) {
			
			String name = removeExtension(file.getName(), POSTFIX);
		
			File data = new File(DATA_FOLDER + "/" + name + ".jar");
			try {
				if (!data.exists())
					throw new Exception("cannot find data file "
							+ data.getAbsolutePath());
			} catch(Exception e){
				continue;
			}
			out1 = new PrintWriter(new FileWriter(OUT_FOLDER + name + ".csv"));
			//print header row. 
			out1.println("iteration,moved,to,ns total,ns after");
		
			log("Analysing " + data.getAbsolutePath());
			DirectedGraph<Vertex, Edge> g = loadGraph(data.getAbsolutePath());
			Set<String> namespaces = getNamespacesCount(g);
			for(String ns : namespaces) {
				out2.println(ns);out2.print(SEP);
			}
			out2.println();
			
			int totalNSTemp = namespaces.size();
			// read data for edge to be removed
			CSVReader reader = new CSVReader(new FileReader(file));
			String[] nextLine;
			int counter = 0;
			while ((nextLine = reader.readNext()) != null) {
				if (!nextLine[0].equals("iteration")){
					log("iteration ", nextLine[0]);
					if(Integer.valueOf(nextLine[0]) < 100){
						String src = nextLine[4];
						String tgt = nextLine[5];
						Vertex source = getVertex(g,src);
						if(source == null) continue;
						moveRefactoring(source, tgt, g);
						Set<String> namespacesAfter = getNamespacesCount(g);
						for(String ns : namespacesAfter) {
							out2.println(ns);out2.print(SEP);
						}
						out2.println();
						if(totalNSTemp > namespacesAfter.size()) {
							counter ++;
							totalNSTemp = namespacesAfter.size();
						}
						out1.print(nextLine[0]);out1.print(SEP);
						out1.print(src);out1.print(SEP);
						out1.print(tgt);out1.print(SEP);
						out1.print(namespaces.size());out1.print(SEP);
						out1.print(namespacesAfter.size());
						out1.println();
					} else break;
				}
			}
//			out.print(SEP);
//			out.print(counter);
			PackageStats stat = new PackageStats();
			stat.setProgramName(name);
			stat.setMergedPackages(counter);
			stat.setTotalPackages(namespaces.size());
			statsList.add(stat);
			out2.close();
			out1.close();
		}
		printOutputSummary(statsList);
//		out.close();
		
		System.out.println("Outpout written to " + new File(OUTPUT_FILE).getAbsolutePath());
	
	}
	private static void printOutputSummary(List<PackageStats> statsList) throws IOException {
		PrintWriter out = new PrintWriter(new FileWriter(OUTPUT_FILE));
		out.println("program,merged packages,total packages");
		for(PackageStats stat : statsList) {
			out.print(stat.getProgramName());
			out.print(SEP);
			out.print(stat.getMergedPackages());
			out.print(SEP);
			out.print(stat.getTotalPackages());
			out.println();
		}
		out.close();
		
	}
	// Move(s,t.namespace)
	public static List<Vertex> moveRefactoring(Vertex s, String targetNS,
			DirectedGraph<Vertex, Edge> g) {
		String className = s.getFullname();
		if (!className.contains("$")) {
			List<Vertex> classesToMove = getInnerClasses(className, g);
			classesToMove.add(s);
			for (Vertex toMove : classesToMove) {
				toMove.setNamespace(targetNS);
			}
			return classesToMove;
		} else {
			className = className.substring(0, className.lastIndexOf("$"));
			Vertex outerClass = getOuterClass(className, g);
			List<Vertex> classesToMove = getInnerClasses(outerClass.getFullname(), g);
			classesToMove.add(s);
			for (Vertex toMove : classesToMove) {
				toMove.setNamespace(targetNS);
			}
			return classesToMove;
		}
	}
	
	private static Vertex getOuterClass(String className,
			DirectedGraph<Vertex, Edge> g) {
		for (Vertex v : g.getVertices()) {
			if (v.getFullname().equals(className))
				return v;
		}
		return null;
	}

	private static List<Vertex> getInnerClasses(String className,
			DirectedGraph<Vertex, Edge> g) {
		String classToCompare = className + "$"; //inner classes have $ in their name. 
		List<Vertex> innerClasses = new ArrayList<Vertex>();
		for (Vertex v : g.getVertices()) {
			if (v.getFullname().startsWith(classToCompare))
				innerClasses.add(v);
		}
		return innerClasses;
	}

	private static Set<String> getNamespacesCount(DirectedGraph<Vertex, Edge> g) {
		Set<String> namespaces = new HashSet<String>();
		for(Vertex v : g.getVertices()){
			namespaces.add(v.getNamespace());
		}
		return namespaces;
	}


	private static Vertex getVertex(DirectedGraph<Vertex, Edge> g, String src) {
		for(Vertex v : g.getVertices()){
			if(v.getFullname().equals(src)) return v;
		}
		return null;
	}


	private static String getValue(List<List<String>> data, int recordNo, int col)  {
		try {
			return data.get(recordNo).get(col);
		}
		catch (Exception x) {
			return "0"; // no value
		}
	}

	private static Map<String,List<List<String>>> parseData() throws Exception {
		Map<String,List<List<String>>> data = new LinkedHashMap<String,List<List<String>>>();
		File folder = new File(INPUT_FOLDER);
		FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(POSTFIX2REMOVE);
			}
		};
		for (File file:folder.listFiles(filter)) {
			List<List<String>> fileData = parseData(file);
			if (fileData!=null) data.put(file.getName(),fileData);
		}
		return data;
	}

	private static List<List<String>> parseData(File file) throws Exception {
		List<List<String>> data = new ArrayList<List<String>>();
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line = null;
		while ((line=reader.readLine())!=null) {
			List<String> lineData = parseLine(line);
			if (lineData!=null) data.add(lineData);
		}
		reader.close();
		return data;
	}

	private static List<String> parseLine(String line) {
		List<String> data = new ArrayList();
		for (StringTokenizer tok = new StringTokenizer(line,SEP);tok.hasMoreTokens();) {
			data.add(tok.nextToken());
		}
		return data;
	}
	

	

}
