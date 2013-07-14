package nz.ac.massey.cs.care.scripts.stat;

import static nz.ac.massey.cs.care.util.Utils.SEP;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public class ScriptUtils {
	public static Map<String,List<List<String>>> parseData(String INPUT_FOLDER, final String POSTFIX2REMOVE) throws Exception {
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

	public static List<List<String>> parseData(File file) throws Exception {
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

	public static void main(String[] args) {
		String line = "a.A,b.B,Extends,,Extends";
		List<String> tokens = parseLine(line);
		
//		System.out.println("tokens should be 2: " + getCount(tokens, "b.B"));
		
	}
	public static List<String> parseLine(String line) {
		List<String> data = new ArrayList<String>();
		for (StringTokenizer tok = new StringTokenizer(line,SEP,false);tok.hasMoreTokens();) {
			String val = tok.nextToken();
			data.add(val);
		}
		return data;
	}
	public static String getValue(List<List<String>> data, int recordNo, int col,int program)  {
		try {
			return data.get(recordNo).get(col);
		}
		catch (Exception x) {
			return "0"; // no value
		}
	}
	
	public static List<String> getRow(List<List<String>> data, int row) {
		try {
			return data.get(row);
		}
		catch (Exception x) {
			return new ArrayList<String>(); // no value
		}
	}
}
