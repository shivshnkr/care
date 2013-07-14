package nz.ac.massey.cs.care.scripts.stat;

import static nz.ac.massey.cs.care.scripts.stat.ScriptUtils.getValue;
import static nz.ac.massey.cs.care.scripts.stat.ScriptUtils.parseData;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MergeRefactoringTypeStats {
	public final static String FOLDER = "output/constraints/";
	public final static String OUTPUT_FILE = "output/stats/"+"refactype.csv";
	public final static String POSTFIX2REMOVE = "_constraints.csv"; // will be removed from file name to get graph name
	public final static String INPUT_FOLDER = FOLDER;
	public static String GRAPH_PROPERTIES = "output/graph-properties";
	static PrintWriter out = null;//
	
	public static void main(String[] args) throws Exception {
		Map<String,Integer> types = new HashMap<String,Integer>();
		Map<String,List<List<String>>> data = parseData(INPUT_FOLDER, POSTFIX2REMOVE);
		out = new PrintWriter(new FileWriter(OUTPUT_FILE));
//		out.println("move,generalize,inlining,locator,g+l,g+i,");
		for (int p = 0; p < data.size(); p++) {
			Object[] array = data.keySet().toArray();
			String filename = (String) array[p];
			List<List<String>> fileData = data.get(filename);
			for(int row=1; row<fileData.size();row++){
				String val0 = getValue(fileData, row, 4, p);
				if(val0.equals("0")){
					String type = getValue(fileData, row, 3, p);
					if(type.equals("Same Namespace") || 
							type.equals("Instance Count") ||
							type.equals("Compilation")) continue;
					if(types.get(type)==null) {
						types.put(type, 1);
					} else {
						Integer n  = types.get(type);
						types.put(type, n + 1);
					}
				}
			}
		}
		for(Map.Entry<String, Integer> e : types.entrySet()) {
			out.println(e.getKey() + ", " + e.getValue());
		}
		out.close();
		System.out.println("Outpout written to " + new File(OUTPUT_FILE).getAbsolutePath());	
	}
	public static void log(Object... s) {
		for (Object t:s) {
			out.print(t);
			out.print(",");
		}
		out.println();
	}
}
