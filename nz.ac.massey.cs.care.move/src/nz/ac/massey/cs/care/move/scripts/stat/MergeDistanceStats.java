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

public class MergeDistanceStats {
	public final static String FOLDER = "output/cd-all/";
	public final static int COLUMN2MERGE = 9; //distance before
	public final static String OUTPUT_FILE = "output/stats/"+"distance1.csv";
	public final static String POSTFIX2REMOVE = "_package_metrics.csv"; // will be removed from file name to get graph name
	public final static String INPUT_FOLDER = FOLDER;
	public static String GRAPH_PROPERTIES = "output/graph-properties";
	
	public static void main(String[] args) throws Exception {
		Map<String,List<List<String>>> data = parseData(INPUT_FOLDER, POSTFIX2REMOVE);
		PrintWriter out = new PrintWriter(new FileWriter(OUTPUT_FILE));
//		out.println("program,avg D improved,avg D worsened");
		out.println("program,improvement");
		for (int p = 0; p < data.size(); p++) {
			int packagesImproved = 0;
			int packagesWorsened = 0;
			int unchanged = 0;
			double wor = 0.0;
			double imp = 0.0;
			
			Object[] array = data.keySet().toArray();
			String filename = (String) array[p];
			List<List<String>> fileData = data.get(filename);
			for(int row=1; row<fileData.size();row++){
				Double dBefore = Double.valueOf(getValue(fileData, row, 9, p));
				Double dAfter =  Double.valueOf(getValue(fileData, row, 10, p));
				if(dBefore == -1 || dAfter == -1) continue;
				if(dBefore > dAfter) {
					//D improved
					imp += Math.abs(dBefore - dAfter);
					packagesImproved ++;
				} else if (dBefore < dAfter) {
					wor += Math.abs(dBefore - dAfter);
					packagesWorsened ++;
				} else {
					unchanged ++;
				}
			}
			double improvedAvg = 0.0;
			if(packagesImproved > 0) {
				improvedAvg = round(imp/packagesImproved);
			}
			
			double worsenedAvg = 0.0;
			if(packagesWorsened > 0) {
				worsenedAvg = round(wor/packagesWorsened);
			}
			int total = packagesImproved - packagesWorsened;
//			out.println(removeExtension(filename,POSTFIX2REMOVE)+ "," + total);
			out.println(removeExtension(filename,POSTFIX2REMOVE)+ "," + improvedAvg + "," + worsenedAvg);
		}
		out.close();
		System.out.println("Outpout written to " + new File(OUTPUT_FILE).getAbsolutePath());	
	}
	private static double readModularity(String filename) throws Exception {
		String metricsFile = INPUT_FOLDER + replaceExtension(filename,POSTFIX2REMOVE,"_metrics.csv");
		List<List<String>> data = parseData(new File(metricsFile));
		//2nd row and 11th column of the metrics file (modularity before). index start from zero
		String value = getValue(data, 1, 10, 0);
		return Double.valueOf(value);
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
