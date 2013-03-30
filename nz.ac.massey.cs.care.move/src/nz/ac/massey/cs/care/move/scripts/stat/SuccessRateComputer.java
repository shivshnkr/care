package nz.ac.massey.cs.care.move.scripts.stat;

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
import static nz.ac.massey.cs.care.move.util.Utils.*;



/**
 * Script to compute the success rate of each refactoring performed.
 * @author Ali
 */
public class SuccessRateComputer {
	
	public final static String FOLDER = "output/cd/";
	public final static int MAX_ROWS = 1;
	public final static int COLUMN2MERGE = 2; //class name
	public final static String OUTPUT_FILE = "output/stats/"+"error-classes.csv";
	public final static String POSTFIX2REMOVE = "_g2c_success.csv"; // will be removed from file name to get graph name
	public final static String INPUT_FOLDER = FOLDER;

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		Map<String,List<List<String>>> data = parseData();
		double total = 0.0;
		double failed = 0.0;
		for (int p = 0; p < data.size(); p++) {
			Object[] array = data.keySet().toArray();
			String filename = (String) array[p];
			List<List<String>> fileData = data.get(filename);
			for(int row=0; row<fileData.size();row++){
				String value = getValue(fileData, row, COLUMN2MERGE, p);
				System.out.println(value);
				if(value.equals("passed") || value.equals("failed")) total++;
				if(value.equals("failed")) failed ++;
			}
		}
		System.out.println("failed are: " + failed + " and total are: " + total);
		double r = (failed*100) / total;
		System.out.println(r);
		
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
