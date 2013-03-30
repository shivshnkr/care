package nz.ac.massey.cs.care.move.scripts.stat;

import static nz.ac.massey.cs.care.move.scripts.stat.ScriptUtils.parseData;
import static nz.ac.massey.cs.care.move.util.Utils.log;
import static nz.ac.massey.cs.care.move.util.Utils.removeExtension;
import static nz.ac.massey.cs.care.move.util.Utils.replaceExtension;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;



/**
 * Script to merge the data about removed edges and pattern counts into a single cvs file for statistical analysis.
 * @author jens dietrich, Ali
 */
public class MergeStrongerInstances {
	
	public final static String FOLDER = "output/cd-all/";
	public final static int MAX_ROWS = 51;
	public final static int COLUMN2MERGE = 2; // relative percentage
	public final static String OUTPUT_FILE = "output/stats/"+"merged_instances_stronger_model.csv";
	public final static String POSTFIX2REMOVE = "_instances.csv"; // will be removed from file name to get graph name
	public final static String INPUT_FOLDER = FOLDER;
	public static String GRAPH_PROPERTIES = "output/graph-properties";
	public final static String SEP = ",";
	private static String[] prevValues = null;

	public static void main(String[] args) throws Exception {
		Map<String,List<List<String>>> data = parseData(INPUT_FOLDER, POSTFIX2REMOVE);
		
		PrintWriter out = new PrintWriter(new FileWriter(OUTPUT_FILE));
		Map<String, Integer> dataMap = new HashMap<String,Integer>();
		for (String fileName:data.keySet()) {
			Properties properties = readProperties(fileName);
			Integer vertexCount = Integer.valueOf((String) properties.get("vertexcount"));
			dataMap.put(fileName, vertexCount);
		}
		Map<String, Integer> sortedDataMap = dataMap;//sortByValue(dataMap); ;//use dataMap if no sorting required
		// print 1st row - data file names
		out.print("program");
		for (String fileName:sortedDataMap.keySet()) {
			out.print(SEP);
			out.print(removeExtension(fileName,POSTFIX2REMOVE));
		}
		out.println();
		prevValues = new String[sortedDataMap.size()];
		
		// print 2nd row - vertex counts
		out.print("vertex count");
		for (String fileName:sortedDataMap.keySet()) {
			Properties properties = readProperties(fileName);
			out.print(SEP);
			out.print(properties==null?"?":properties.get("vertexcount"));
			
		}
		out.println();
		
		// print 3rd row - edge counts
		out.print("edge count");
		for (String fileName:sortedDataMap.keySet()) {
			Properties properties = readProperties(fileName);
			out.print(SEP);
			out.print(properties==null?"?":properties.get("edgecount"));
			
		}
		out.println();
		
		for (int row=0;row<MAX_ROWS;row++) {
			out.print(row);
			for(int p=0; p<sortedDataMap.size();p++){
				out.print(SEP);
				Object[] array = sortedDataMap.keySet().toArray();
				String filename = (String) array[p];
				List<List<String>> fileData = data.get(filename);
				String value = getValue(fileData,row,COLUMN2MERGE,p);
				out.print(value);
			}
			out.println();
		}
		
		out.close();
		
		System.out.println("Outpout written to " + new File(OUTPUT_FILE).getAbsolutePath());
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	static Map<String, Integer> sortByValue(Map<String, Integer> edgeRanks) {
	     List list = new LinkedList(edgeRanks.entrySet());
	     Collections.sort(list, new Comparator() {
	          public int compare(Object o1, Object o2) {
	               return ((Comparable) ((Map.Entry) (o1)).getValue())
	              .compareTo(((Map.Entry) (o2)).getValue());
	          }
	     });

	    Map result = new LinkedHashMap();
	    for (Iterator it = list.iterator(); it.hasNext();) {
	        Map.Entry entry = (Map.Entry)it.next();
	        result.put(entry.getKey(), entry.getValue());
	    }
	    return result;
	}
	private static String getValue(List<List<String>> data, int recordNo, int col,int program)  {
		try {
				String v = data.get(recordNo).get(col);
				if(!v.equals("instances(%)")) {
					prevValues[program] = v;
					return v;
				} 
				return v;
		}
		catch (Exception x) {
			return prevValues[program];
		}
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
	
}
