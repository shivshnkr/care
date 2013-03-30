package nz.ac.massey.cs.care.move.scripts.stat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
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

import nz.ac.massey.cs.care.move.util.Printery;
import nz.ac.massey.cs.care.move.util.Utils;

import au.com.bytecode.opencsv.CSVReader;


import static nz.ac.massey.cs.care.move.util.Printery.println;
import static nz.ac.massey.cs.care.move.util.Utils.*;

/**
 * Script to merge the data about removed edges and pattern counts into a single cvs file for statistical analysis.
 * @author jens dietrich
 */
public class MergeSCCMetrics {
	
	public final static String FOLDER = "output/cd/";
//	public final static String FOLDER = "C:/Documents and Settings/mashah/Desktop/Experiment-MoveRefactoring/output-20-05-2011/cd/";
	public final static int MAX_ROWS = 101;
	public final static int COLUMN2MERGE = 2; // relative percentage
	public final static String OUTPUT_FILE = "output/stats/"+"merged_scc.csv";
	public final static String POSTFIX2REMOVE = "_metrics.csv"; // will be removed from file name to get graph name
	public final static String INPUT_FOLDER = FOLDER;
	public static String GRAPH_PROPERTIES = "output/graphproperties";
	public final static String SEP = ",";
	private static final String NL = System.getProperty("line.separator");
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		File folder = new File(INPUT_FOLDER);
		Printery.println(OUTPUT_FILE,"program,compression before,compression after,max scc size before,"+
				"max scc size after,density before,density after,tangledness before," +
				"tangledness after,count before,count after,"+
				"modularity before,modularity after,distance before,distance after," +
				"total time");
		PrintWriter out = new PrintWriter(new FileWriter(OUTPUT_FILE),true);
		for (File file : folder.listFiles(getExtensionFileFilter(POSTFIX2REMOVE))) {
			CSVReader reader = new CSVReader(new FileReader(file));
			String[] nextLine;
			String filename = removeExtension(file.getName(), POSTFIX2REMOVE);
			while ((nextLine = reader.readNext()) != null) {
				if (!nextLine[0].equals("compression before")) {
					out.print(filename); out.print(SEP);
					out.print(nextLine[0]);out.print(SEP); 
					out.print(nextLine[1]); out.print(SEP);
					out.print(nextLine[2]);out.print(SEP); 
					out.print(nextLine[3]); out.print(SEP);
					out.print(nextLine[4]);out.print(SEP);
					out.print(nextLine[5]); out.print(SEP);
					out.print(nextLine[6]); out.print(SEP);
					out.print(nextLine[7]); out.print(SEP);
					out.print(nextLine[8]); out.print(SEP);
					out.print(nextLine[9]); out.print(SEP);
					out.print(nextLine[10]);out.print(SEP);
					out.print(nextLine[11]); out.print(SEP);
					out.print(nextLine[12]); out.print(SEP);
					out.print(nextLine[13]); out.print(SEP);
					out.print(nextLine[14]); out.print(SEP);
					out.print(NL);
					
				}
			}
			reader.close();
		}
			
		out.close();
		System.out.println("Outpout written to " + new File(OUTPUT_FILE).getAbsolutePath());
	}
	
	public static FileFilter getExtensionFileFilter(final String extension) {
		return new FileFilter(){
			@Override
			public boolean accept(File f) {
				return f.getName().endsWith(extension) && !f.getName().contains("package");
			}
		};
	}
	private static String getValue(List<List<String>> data, int recordNo, int col)  {
		try {
			return data.get(recordNo).get(col);
		}
		catch (Exception x) {
			return "0"; // no value
		}
	}

	public static Map<String,List<List<String>>> parseData(String inputFolder, final String postfix2remove2) throws Exception {
		Map<String,List<List<String>>> data = new LinkedHashMap<String,List<List<String>>>();
		File folder = new File(inputFolder);
		FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(postfix2remove2) & !name.contains("package");
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
