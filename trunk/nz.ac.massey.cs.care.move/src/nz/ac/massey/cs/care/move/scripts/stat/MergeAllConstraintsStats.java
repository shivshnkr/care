package nz.ac.massey.cs.care.move.scripts.stat;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import static nz.ac.massey.cs.care.move.scripts.stat.ScriptUtils.*;
import static nz.ac.massey.cs.care.move.util.Utils.*;

public class MergeAllConstraintsStats {
	public final static String FOLDER = "output/cd-all/";
	private final static int RENAMECOLUMN = 2;
	private final static int BLACKLISTEDCOLUMN = 3;
	private final static int ACCESSABILITYCOLUMN = 4;
	private final static int INSTANCESCOLUMN = 5;
	private final static int COMPILATIONCOLUMN = 6;
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
