package nz.ac.massey.cs.care.scripts.stat;

import static nz.ac.massey.cs.care.util.Utils.removeExtension;
import static nz.ac.massey.cs.care.util.Utils.round;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import nz.ac.massey.cs.care.util.Dependency;

public class RefactorabilityComparator {

	public final static String FOLDER = "output/simulation-results/";
//	public final static String FOLDER = "C:/Documents and Settings/mashah/Desktop/Experiment-MoveRefactoring/output-20-05-2011/cd/";
	public final static int MAX_ROWS = 51;
	public final static int SOURCECOLUMN = 1; // source dependency
	public final static int TARGETCOLUMN = 3; // target dependency
	public final static String OUTPUT_FILE = "output/stats/"+"merged_refactorability.csv";
	public final static String POSTFIX2REMOVE = "_details.csv"; // will be removed from file name to get graph name
	public final static String INPUT_FOLDER_SIMULATION = FOLDER;
	public final static String INPUT_FOLDER_ORIGINAL = "output/original-results/";
	public static String GRAPH_PROPERTIES = "output/graphproperties";
	public final static String SEP = ",";

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		Map<String,List<List<String>>> dataSimulation = parseData(INPUT_FOLDER_SIMULATION);
		Map<String,List<List<String>>> dataOriginal = parseData(INPUT_FOLDER_ORIGINAL);
		
		PrintWriter out = new PrintWriter(new FileWriter(OUTPUT_FILE));
		
		// print 1st row - data file names
		out.print("program");
		for (String fileName:dataSimulation.keySet()) {
			out.print(SEP);
			out.print(removeExtension(fileName,POSTFIX2REMOVE));
		}
		out.println();
		File f;
		Map<String, List<Dependency>> simulationData = new LinkedHashMap<String, List<Dependency>>();
		Map<String, List<Dependency>> originalData = new LinkedHashMap<String, List<Dependency>>();

		
		for (String fileName:dataSimulation.keySet()) {
			List<Dependency> simulationDependencies = new ArrayList<Dependency>();
			List<Dependency> originalDependencies = new ArrayList<Dependency>();
			for (int row = 1; row < MAX_ROWS; row++) {
				List<List<String>> fileDataSim = dataSimulation.get(fileName);
				String sourceSim = getValue(fileDataSim, row, SOURCECOLUMN);
				String targetSim = getValue(fileDataSim, row, TARGETCOLUMN);
				if(!sourceSim.equals("0"))
					simulationDependencies.add(new Dependency(sourceSim, targetSim));
				
				List<List<String>> fileDataOriginal = dataOriginal.get(fileName);
				String sourceOriginal = getValue(fileDataOriginal, row, SOURCECOLUMN);
				String targetOriginal = getValue(fileDataOriginal, row, TARGETCOLUMN);
				if(!sourceOriginal.equals("0"))
					originalDependencies.add(new Dependency(sourceOriginal, targetOriginal));
				
			}
			simulationData.put(fileName, simulationDependencies);
			originalData.put(fileName, originalDependencies);
		}
		
		out.print("Refactorability");
		for(String filename : simulationData.keySet()) {
			out.print(SEP);
			double counter = 0.0;
			List<Dependency> simDependencies = simulationData.get(filename);
			List<Dependency> originalDependencies = originalData.get(filename);
			for(Dependency simDependency : simDependencies) {
				//now compare each simulation dependency with the original one
				for(Dependency orginalDependency : originalDependencies) {
					if(simDependency.equals(orginalDependency)) counter ++;
				}
			}
			double totalDeps = simDependencies.size();
			double ratio = counter / totalDeps;
			out.print(round(ratio));
		}
		
		out.close();
		System.out.println("Outpout written to " + new File(OUTPUT_FILE).getAbsolutePath());
	}
	private static String getValue(List<List<String>> data, int recordNo, int col)  {
		try {
			return data.get(recordNo).get(col);
		}
		catch (Exception x) {
			return "0"; // no value
		}
	}
	private static Map<String,List<List<String>>> parseData(String folder1) throws Exception {
		Map<String,List<List<String>>> data = new LinkedHashMap<String,List<List<String>>>();
		File folder = new File(folder1);
		FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(POSTFIX2REMOVE);
			}
		};
		for (File file:folder.listFiles(filter)) {
			List<List<String>> fileData = parseData(file);
			String filename = file.getName();
//			if(filename.contains("-")) filename = filename.substring(0,filename.indexOf("-"));
//			else if (filename.contains("_")) filename = filename.substring(0,filename.indexOf("_"));
			if (fileData!=null) data.put(filename,fileData);
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
