package nz.ac.massey.cs.care.scripts.stat;

import static nz.ac.massey.cs.care.scripts.stat.ScriptUtils.getValue;
import static nz.ac.massey.cs.care.scripts.stat.ScriptUtils.parseData;
import static nz.ac.massey.cs.care.util.Utils.log;
import static nz.ac.massey.cs.care.util.Utils.removeExtension;
import static nz.ac.massey.cs.care.util.Utils.replaceExtension;
import static nz.ac.massey.cs.care.util.Utils.round;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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
}
