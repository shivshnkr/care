package nz.ac.massey.cs.care.move.scripts.stat;

import java.io.File;
import java.io.FileFilter;

public class CorpusNamesExtractor {
	public final static String FOLDER = "projects-data/all-92/";
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		File srcDir = new File(FOLDER);
		File[] srcFiles = srcDir.listFiles(new FileFilter (){
			@Override
			public boolean accept(File f) {
				if(f.getName().endsWith(".svn")) return false;
				else return true;
			}
			
		});
		int count = 0;
		for(File file : srcFiles) {
			if(count <= 3) {
				String name = file.getName();
				if(name.contains("_")) {
					name = name.replace("_", "\\_");
				}
				System.out.print(name + " ");
				count ++;
				if(count == 3) {
					System.out.print("\\\\\n");
					count = 0;
				} else {
					System.out.print("& ");
				}
			}
		}
	}

}
