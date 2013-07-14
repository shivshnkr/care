package nz.ac.massey.cs.care.scripts.stat;

import static nz.ac.massey.cs.care.scripts.stat.ScriptUtils.getValue;
import static nz.ac.massey.cs.care.util.Utils.SEP;
import static nz.ac.massey.cs.care.util.Utils.removeExtension;

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

public class MergeSccCountStats {
	public final static String FOLDER = "output/all-metrics/";
	public final static int COLUMN2MERGE = 9; //distance before
	public final static String OUTPUT_FILE = "output/stats/"+"sscount.csv";
	public final static String POSTFIX2REMOVE = "_metrics.csv"; // will be removed from file name to get graph name
	public final static String INPUT_FOLDER = FOLDER;
	public static String GRAPH_PROPERTIES = "output/graph-properties";
	
	public static void main(String[] args) throws Exception {
		Map<String,List<List<String>>> data = parseData(INPUT_FOLDER, POSTFIX2REMOVE);
		PrintWriter out = new PrintWriter(new FileWriter(OUTPUT_FILE));
		out.println("program,count before,count after");
		for (int p = 0; p < data.size(); p++) {
			
			Object[] array = data.keySet().toArray();
			String filename = (String) array[p];
			//System.out.println(filename);
			List<List<String>> fileData = data.get(filename);
			for(int row=1; row<fileData.size();row++){
				String valueBefore = getValue(fileData, row, 8, p);
				//System.out.println(valueBefore);
				Integer countBefore = Integer.valueOf(valueBefore);
				Integer countAfter =  Integer.valueOf(getValue(fileData, row, 9, p));
				out.println(removeExtension(filename,POSTFIX2REMOVE)+ "," + countBefore + "," + countAfter);
			}
			
		}
		out.close();
		System.out.println("Outpout written to " + new File(OUTPUT_FILE).getAbsolutePath());	
	}
	public static Map<String,List<List<String>>> parseData(String INPUT_FOLDER, final String POSTFIX2REMOVE) throws Exception {
		Map<String,List<List<String>>> data = new LinkedHashMap<String,List<List<String>>>();
		File folder = new File(INPUT_FOLDER);
		FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(POSTFIX2REMOVE) && !name.contains("package");
			}
		};
		for (File file:folder.listFiles(filter)) {
			List<List<String>> fileData = parseData(file);
			if (fileData!=null) data.put(file.getName(),fileData);
		}
		return data;
	}
	public static List<List<String>> parseData(File file) throws Exception {
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

	public static List<String> parseLine(String line) {
		List<String> data = new ArrayList<String>();
		for (StringTokenizer tok = new StringTokenizer(line,SEP);tok.hasMoreTokens();) {
			data.add(tok.nextToken());
		}
		return data;
	}
}
