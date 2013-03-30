package nz.ac.massey.cs.care.move.scripts.stat;

import static nz.ac.massey.cs.care.move.util.Utils.*;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;

import au.com.bytecode.opencsv.CSVReader;

/**
 * Utility to determine the proportion of classes to move. We get a distribution
 * pattern on whether a source class should be moved to target or vice versa.
 * 
 * @author mashah
 */
public class MoveSrcOrTgtStats {
	public final static String FOLDER = "output/cd/";
	public final static String OUTPUT_FILE = "output/stats/" + "partitipation.csv";
	public final static String POSTFIX = "_details.csv";
	public final static String INPUT_FOLDER = FOLDER; // details files are here

	public static void main(String[] args) throws Exception {

		org.apache.log4j.PropertyConfigurator.configure("log4j.properties");

		File folder = new File(INPUT_FOLDER);

		// write out data as csv
		PrintWriter out = new PrintWriter(new FileWriter(OUTPUT_FILE));

		// first row
		out.println("program,source moved,target moved");

		for (File file : folder.listFiles(getExtensionFileFilter(POSTFIX))) {
			String name = removeExtension(file.getName(), POSTFIX);
			File details = new File(INPUT_FOLDER + name + POSTFIX);
			if (!details.exists()) {
				throw new Exception("datafile not found: " + details);
			}
			CSVReader reader = new CSVReader(new FileReader(details));
			String[] nextLine;
			int srcToMoveCounter = 0;
			int tgtToMoveCounter = 0;
			while ((nextLine = reader.readNext()) != null) {
				if (nextLine[0] != "iteration") {
					String src = nextLine[1];
					String tgt = nextLine[3];
					String classToMove = nextLine[4];
					if (classToMove.equals(src))
						srcToMoveCounter++;
					else if (classToMove.equals(tgt))
						tgtToMoveCounter++;
				}
			}
			out.print(name);
			out.print(SEP);
			out.print(srcToMoveCounter);
			out.print(SEP);
			out.print(tgtToMoveCounter);
			out.println();
		}
		out.close();

		log("Participation info merged to ",
				new File(OUTPUT_FILE).getAbsolutePath());

	}

}
