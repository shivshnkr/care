package nz.ac.massey.cs.care.scripts.stat;

import static nz.ac.massey.cs.care.scripts.stat.ScriptUtils.getValue;
import static nz.ac.massey.cs.care.scripts.stat.ScriptUtils.parseData;
import static nz.ac.massey.cs.care.util.Utils.round;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

public class MergeAllConstraintsStats {
	public final static String FOLDER = "output/cd-all/";
	private final static int[] COLUMNS = {2,3,4,5,6};
	public final static String OUTPUT_FILE = "output/stats/"+"prepostconditions.csv";
	public final static String POSTFIX2REMOVE = "_constraints.csv"; // will be removed from file name to get graph name
	public final static String INPUT_FOLDER = FOLDER;
	public static String GRAPH_PROPERTIES = "output/graph-properties";
	
	public static void main(String[] args) throws Exception {
		Map<String,List<List<String>>> data = parseData(INPUT_FOLDER, POSTFIX2REMOVE);
		PrintWriter out = new PrintWriter(new FileWriter(OUTPUT_FILE));
		out.println("constraint,passed,failed");
		for(int col : COLUMNS) {
			double attempted = 0.0;
			double passed = 0.0;
			double failed = 0.0;
			for (int p = 0; p < data.size(); p++) {
				Object[] array = data.keySet().toArray();
				String filename = (String) array[p];
				List<List<String>> fileData = data.get(filename);
				for(int row=1; row<fileData.size();row++){
					String value = getValue(fileData, row, col, p);
					if(value.equals("n/a")) continue;
					else if(value.equals("passed")) passed ++;
					else failed ++;
					attempted ++;
				}
			}
			double passedratio = round((passed * 100) / attempted);
			double failedratio = round((failed * 100) / attempted);
			String constraintName = getName(col);
			out.println(constraintName + "," + passedratio + "," +failedratio);
		}
		out.close();
		System.out.println("Outpout written to " + new File(OUTPUT_FILE).getAbsolutePath());	
	}
	private static String getName(int col) {
		String constraintName = null;
		switch (col) {
		case 2:
			constraintName = "rename";
			break;
		case 3:
			constraintName = "blacklisted";
			break;
		case 4:
			constraintName = "accessability";
			break;
		case 5:
			constraintName = "instance count";
			break;
		case 6:
			constraintName = "compilation";
			break;
		default:
			constraintName = "";
		}
		return constraintName;
	}
}
