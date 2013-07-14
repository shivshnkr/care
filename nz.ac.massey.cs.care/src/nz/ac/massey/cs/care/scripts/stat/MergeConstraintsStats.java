package nz.ac.massey.cs.care.scripts.stat;

import static nz.ac.massey.cs.care.scripts.stat.ScriptUtils.getRow;
import static nz.ac.massey.cs.care.scripts.stat.ScriptUtils.getValue;
import static nz.ac.massey.cs.care.scripts.stat.ScriptUtils.parseData;
import static nz.ac.massey.cs.care.util.Utils.replaceExtension;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class MergeConstraintsStats {
	public final static String FOLDER = "output/constraints/";
	public final static String OUTPUT_FILE = "output/stats/"+"prepostconditions.csv";
	public final static String POSTFIX2REMOVE = "_constraints.csv"; // will be removed from file name to get graph name
	public final static String INPUT_FOLDER = FOLDER;
	public static String GRAPH_PROPERTIES = "output/graph-properties";
	static PrintWriter out = null;//
	
	public static void main(String[] args) throws Exception {
		List<String> constraints = new ArrayList<String>();
		Map<String,List<List<String>>> data = parseData(INPUT_FOLDER, POSTFIX2REMOVE);
		out = new PrintWriter(new FileWriter(OUTPUT_FILE));
		out.println("extnds,implemnts,other,anony,rename,selfInstantiate,noValidSuper,superAccessed," +
				"compilation,nonStaticInner,instanceCount,changeVisibility");
		for (int p = 0; p < data.size(); p++) {
			Object[] array = data.keySet().toArray();
			String filename = (String) array[p];
			List<List<String>> fileData = data.get(filename);
			for(int row=1; row<fileData.size();row++){
				List<String> rowData = getRow(fileData, row);
				String val0 = getValue(fileData, row, 2, p);
				if(val0.equals("Extends") || val0.equals("Implements") || val0.equals("Other")||
						val0.equals("Anonymous class")) {
					constraints.add(val0);
				}
				for(int col=4;col<rowData.size();col++){
					
					String val = getValue(fileData, row, col, p);
					if(!val.equals("0")) {
						if(val.contains("already exists")) {
							constraints.add("Rename field / method required");
						} else if (val.contains("Static members can be declared"))  {
							constraints.add("cannot move member to non-static inner class");
						}
						else if(val.equals("Self Instantiation") ||
								val.equals("No Valid Supertype") ||
								val.equals("Supertype Accessed") ||
								val.equals("Compilation") ||
								val.equals("Instance Count")
								) {
							constraints.add(val);
						}
						else if(val.contains("visibility") || val.contains("visible")) {
							constraints.add("Change visibility refactoring");
						}
						else {
//							constraints.add("Compilation");
						}
					}
				}
			}
		}
		int extnds = getCount(constraints, "Extends");
		int implemnts = getCount(constraints, "Implements");
		int other = getCount(constraints, "Other");
		int anony= getCount(constraints, "Anonymous class");
		int rename = getCount(constraints, "Rename field / method required");
		int selfInstantiate = getCount(constraints, "Self Instantiation");
		int noValidSuper = getCount(constraints, "No Valid Supertype");
		int superAccessed = getCount(constraints, "Supertype Accessed");
		int compilation = getCount(constraints, "Compilation");
		int nonStaticInner = getCount(constraints, "cannot move member to non-static inner class");
		int instanceCount  = getCount(constraints, "Instance Count");
		int changeVisibility = getCount(constraints, "Change visibility refactoring");
		
		log(extnds,implemnts,other,anony,rename,selfInstantiate,noValidSuper,superAccessed,
				compilation, nonStaticInner, instanceCount, changeVisibility);
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
	private static int getCount(List<String> constraints, String key) {
		return Collections.frequency(constraints, key);
	}
}
