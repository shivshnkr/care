package nz.ac.massey.cs.care.refactoring.scripts;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import static nz.ac.massey.cs.care.refactoring.scripts.Utils.*;



/**
 * Script to merge the data about removed edges and pattern counts into a single cvs file for statistical analysis.
 * @author jens dietrich
 */
public class MergeStrongerInstances {
	
	public final static String FOLDER = "output/instances/";
//	public final static String FOLDER = "C:/Documents and Settings/mashah/Desktop/Experiment-MoveRefactoring/output-20-05-2011/cd/";
	public final static int MAX_ROWS = 101;
	public final static int COLUMN2MERGE = 2; // relative percentage
	public final static String OUTPUT_FILE = "output/stats/"+"merged_instances_stronger.csv";
	public final static String POSTFIX2REMOVE = "_instances.csv"; // will be removed from file name to get graph name
	public final static String INPUT_FOLDER = "output/instances/";
	public static String GRAPH_PROPERTIES = "output/graphproperties";
	public final static String SEP = ",";

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		Map<String,List<List<String>>> data = parseData();
		
		PrintWriter out = new PrintWriter(new FileWriter(OUTPUT_FILE));
		
//		// print 1st row - data file names
		out.print("program");
		for (String fileName:data.keySet()) {
			out.print(SEP);
			out.print(removeExtension(fileName,POSTFIX2REMOVE));
		}
		out.println();
//		
//		// print 2nd row - vertex counts
//		out.print("vertex count");
//		for (String fileName:data.keySet()) {
//			Properties properties = readProperties(fileName);
//			out.print(SEP);
//			out.print(properties==null?"?":properties.get(ExtractGraphMetaData.VERTEXCOUNT));
//			
//		}
//		out.println();
		
		// print 3rd row - edge counts
//		out.print("edge count");
//		for (String fileName:data.keySet()) {
//			Properties properties = readProperties(fileName);
//			out.print(SEP);
//			out.print(properties==null?"?":properties.get(ExtractGraphMetaData.EDGECOUNT));
//			
//		}
//		out.println();
//		
		for (int row=0;row<MAX_ROWS;row++) {
			out.print(row);
			for (String fileName:data.keySet()) {
				out.print(SEP);
				List<List<String>> fileData = data.get(fileName); 
				String value = getValue(fileData,row,COLUMN2MERGE);
				out.print(value);
			}
			out.println();
		}
		
		
		out.close();
		
		System.out.println("Outpout written to " + new File(OUTPUT_FILE).getAbsolutePath());
	}
	
	private static Properties readProperties(String fileName) throws IOException {
		File pFile = new File(GRAPH_PROPERTIES+"/"+replaceExtension(fileName,POSTFIX2REMOVE,".properties"));
		if (!pFile.exists()) {
			log("Cannot find properties file " + pFile);
			return null;
		}
		Properties p = new Properties();
		Reader reader = new FileReader(pFile);
		p.load(reader);
		reader.close();
		return p;
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
