package nz.ac.massey.cs.care.scripts.stat;

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

/**
 * Script to merge the data about removed edges and pattern counts into a single cvs file for statistical analysis.
 * @author Ali Shah
 */
public class MergeErrorClasses {
	
	public final static String FOLDER = "output/cd/";
	public final static int MAX_ROWS = 1;
	public final static int COLUMN2MERGE = 3; //class name
	public final static String OUTPUT_FILE = "output/stats/"+"error-classes.csv";
	public final static String POSTFIX2REMOVE = "_error_edge.csv"; // will be removed from file name to get graph name
	public final static String INPUT_FOLDER = FOLDER;
	public static String GRAPH_PROPERTIES = "output/graphproperties";
	public final static String SEP = ",";

	public static void main(String[] args) throws Exception {
		Map<String,List<List<String>>> data = parseData();
		
		PrintWriter out = new PrintWriter(new FileWriter(OUTPUT_FILE));
		
			for(int p=0; p<data.size();p++){
				Object[] array = data.keySet().toArray();
				String filename = (String) array[p];
				List<List<String>> fileData = data.get(filename);
				String value = getValue(fileData,0,COLUMN2MERGE,p);
				out.println(value);
			}
		
		out.close();
		
		System.out.println("Outpout written to " + new File(OUTPUT_FILE).getAbsolutePath());
	}
	private static String getValue(List<List<String>> data, int recordNo, int col,int program)  {
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
