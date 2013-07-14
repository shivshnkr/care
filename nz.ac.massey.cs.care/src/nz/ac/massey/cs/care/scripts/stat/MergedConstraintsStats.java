package nz.ac.massey.cs.care.scripts.stat;

import static nz.ac.massey.cs.care.scripts.stat.ScriptUtils.getValue;
import static nz.ac.massey.cs.care.scripts.stat.ScriptUtils.parseData;
import static nz.ac.massey.cs.care.util.Utils.log;
import static nz.ac.massey.cs.care.util.Utils.replaceExtension;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class MergedConstraintsStats {
	public final static String FOLDER = "output/cd-all/";
	public final static int COLUMN2MERGE = 7; //class name
	public final static String OUTPUT_FILE = "output/stats/"+"refactorability_vertexCount.csv";
	public final static String POSTFIX2REMOVE = "_constraints.csv"; // will be removed from file name to get graph name
	public final static String INPUT_FOLDER = FOLDER;
	public static String GRAPH_PROPERTIES = "output/graph-properties";
	
	public static void main(String[] args) throws Exception {
		Map<String,List<List<String>>> data = parseData(INPUT_FOLDER, POSTFIX2REMOVE);
		PrintWriter out = new PrintWriter(new FileWriter(OUTPUT_FILE));
		out.println("vertex count,refactorability");
		for (int p = 0; p < data.size(); p++) {
			int attempted = 0;
			int passed = 0;
			int failed = 0;
			Object[] array = data.keySet().toArray();
			String filename = (String) array[p];
			List<List<String>> fileData = data.get(filename);
			for(int row=1; row<fileData.size();row++){
				String value = getValue(fileData, row, COLUMN2MERGE, p);
				if(value.equals("n/a")) continue;
				else if(value.equals("passed")) passed ++;
				else failed ++;
				attempted ++;
			}
			if(attempted ==0) continue;
			//use vertex count if needed.
			Properties properties = readProperties(filename);
			Integer vertexCount = Integer.valueOf((String) properties.get("vertexcount"));
//			double modularity = readModularity(filename);
			int refactorability = (passed * 100) / attempted;
			out.println(vertexCount + "," + refactorability);
		}
		out.close();
		System.out.println("Outpout written to " + new File(OUTPUT_FILE).getAbsolutePath());	
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
