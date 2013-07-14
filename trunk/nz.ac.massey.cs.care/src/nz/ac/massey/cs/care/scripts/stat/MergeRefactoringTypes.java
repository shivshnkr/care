package nz.ac.massey.cs.care.scripts.stat;

import static nz.ac.massey.cs.care.util.Utils.removeExtension;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Script to merge the data about removed edges and pattern counts into a single cvs file for statistical analysis.
 * @author jens dietrich
 */
public class MergeRefactoringTypes {
	
	public final static String FOLDER = "output/refacs-applied/";
	public final static int MAX_ROWS = 51;
	public final static int COLUMN2MERGE = 1; // relative percentage
	public final static String OUTPUT_FILE = "output/stats/"+"merged_refac_types.csv";
	public final static String POSTFIX2REMOVE = "_refactorings_applied.csv"; // will be removed from file name to get graph name
	public final static String INPUT_FOLDER = "output/refacs-applied/";
	public static String GRAPH_PROPERTIES = "output/graphproperties";
	public final static String SEP = ",";

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		Map<String,List<List<String>>> data = parseData();
		
		PrintWriter out = new PrintWriter(new FileWriter(OUTPUT_FILE));
		
//		// print 1st row - data file names
		out.println("program, move, SL, SMI, SL+SMI");
		
			for (String fileName:data.keySet()) {
				int moveCount = 0, slCount = 0, smiCount = 0, slnsmiCount = 0; 
				double movePerc = 0, slPerc = 0, smiPerc = 0, slnsmiPerc = 0, iterations = 0;
				List<List<String>> fileData = data.get(fileName);
				out.print(removeExtension(fileName,POSTFIX2REMOVE));
				out.print(SEP);
				for (int row=1;row<fileData.size();row++) {
					String value = getValue(fileData,row,COLUMN2MERGE);
					if(value.equals(" refactoring")) continue;
					iterations ++;
					if(value.equals("Move")) moveCount ++;
					else if (value.equals("SMI")) smiCount ++;
					else if (value.equals("CI")) slCount ++;
					else if (value.equals("CI+VD/MPT/MRT/MET")) slCount ++;
					else slnsmiCount ++;
				}
				movePerc = moveCount * 100.0 / iterations;
				slPerc = slCount * 100.0 / iterations; 
				smiPerc = smiCount * 100.0 / iterations;
				slnsmiPerc = slnsmiCount * 100.0 / iterations;
				
				out.print(round(movePerc));
				out.print(SEP);
				out.print(round(slPerc));
				out.print(SEP);
				out.print(round(smiPerc));
				out.print(SEP);
				out.print(round(slnsmiPerc));
				out.println();
			}

		
		out.close();
		
		System.out.println("Outpout written to " + new File(OUTPUT_FILE).getAbsolutePath());
	}
	public static Double round(Double val) {
		if ("NaN".equals(val.toString()))
			return new Double(-1);
		int decimalPlace = 2;
		try{
			BigDecimal bd = new BigDecimal(val.doubleValue());
			bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_DOWN);
			return bd.doubleValue();
		}catch(Exception e){
			return val;
		}
		
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
