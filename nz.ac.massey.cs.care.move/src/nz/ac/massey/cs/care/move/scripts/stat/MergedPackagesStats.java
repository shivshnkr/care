package nz.ac.massey.cs.care.move.scripts.stat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static nz.ac.massey.cs.care.move.scripts.stat.ScriptUtils.*;
import static nz.ac.massey.cs.care.move.util.Utils.*;

public class MergedPackagesStats {
	public final static String FOLDER = "output/cd/";
	public final static int COLUMN2MERGE = 0; //class name
	public final static String OUTPUT_FILE = "output/stats/"+"error-classes.csv";
	public final static String POSTFIX2REMOVE = "_package_metrics.csv"; // will be removed from file name to get graph name
	public final static String INPUT_FOLDER = FOLDER;
	
	public static void main(String[] args) throws Exception {
		Map<String,List<List<String>>> data = parseData(INPUT_FOLDER, POSTFIX2REMOVE);
		double overalltotal = 0.0;
		double mergedprogramstotal = 0.0;
		double totalmerged = 0.0;
		Map<String, Double> ratio = new HashMap<String, Double>();
		List<PackageStats> statsList = new ArrayList<PackageStats>();
		for (int p = 0; p < data.size(); p++) {
			
			
			double merged = 0.0;
			double total = 0.0;
			double ratioThisProgram = 0.0;
			Object[] array = data.keySet().toArray();
			String filename = (String) array[p];
			List<List<String>> fileData = data.get(filename);
			for(int row=1; row<fileData.size();row++){
				String value = getValue(fileData, row, COLUMN2MERGE, p);
				if(value.equals("0")) break; //this means no data is available
				overalltotal ++;
				total++;
				String ceValue = getValue(fileData, row, 2, p);
				if(ceValue.equals("-1")) {
					merged++;
					totalmerged ++;
				}
			}
			if(merged > 0.0) {
				PackageStats stat = new PackageStats();
				stat.setProgramName(filename);
				stat.setMergedPackages(merged);
				stat.setTotalPackages(total);
				ratioThisProgram = (merged * 100) / total;
				stat.setRatio(ratioThisProgram);
				statsList.add(stat);
//				mergedprogramstotal ++;
				
//				ratio.put(filename, ratioThisProgram);
			}
//			System.out.println(filename + " total packages = " +total + 
//					" merged = " + merged);
			
		}
		System.out.println("total packages: " + overalltotal);
		System.out.println("total merged: " + totalmerged);
		System.out.println("total to merged ratio: " + (totalmerged*100)/overalltotal);
		System.out.println("total programs where packages merged: " + statsList.size());
		
		Collections.sort(statsList, new Comparator<PackageStats>() {

			@Override
			public int compare(PackageStats o1, PackageStats o2) {
				return o1.getRatio().compareTo(o2.getRatio());
			}
			
		});
		
		for(PackageStats ps : statsList) {
			System.out.println(ps.getProgramName().substring(0,ps.getProgramName().lastIndexOf("_package")));
			System.out.println(round(ps.getRatio()));
			System.out.println("total packages: " + ps.getTotalPackages());
			System.out.println("merged packages: " + ps.getMergedPackages());
		}
		
//		List<Map.Entry<String, Double>> list = new LinkedList<Map.Entry<String, Double>>(
//				ratio.entrySet());
//		Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
//			@Override
//			public int compare(Entry<String, Double> o1, Entry<String, Double> o2) {
//				return -o1.getValue().compareTo(o2.getValue());
//			}
//		});
//		Map<String, Double> result = new LinkedHashMap<String, Double>();
//		for (Iterator<Map.Entry<String, Double>> it = list.iterator(); it
//				.hasNext();) {
//			Map.Entry<String, Double> entry = it.next();
//			result.put(entry.getKey(), entry.getValue());
//		}
//		for(Map.Entry<String, Double> e : result.entrySet()){
//			System.out.println(e.getKey() + " \t\t\t\t-> " + round(e.getValue()));
//		}

	}

}
