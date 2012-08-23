package nz.ac.massey.cs.care.refactoring.scripts;

import java.io.File;
import java.io.FileFilter;

public class Renamer {

	/**
	 * @param args
	 */
	static String pathToCopy = "projects-data/all-92/";
	static String pathToRename = "output/simulation-results/";
	public static void main(String[] args) {
		
		File srcDir = new File(pathToCopy);
		File destDir = new File(pathToRename);
		File[] destFiles = destDir.listFiles(new FileFilter (){

			@Override
			public boolean accept(File f) {
				if(f.getName().endsWith("_details.csv")) return true;
				else return false;
			}
			
		});
		File[] srcFiles = srcDir.listFiles(new FileFilter (){

			@Override
			public boolean accept(File f) {
				if(f.getName().endsWith(".svn")) return false;
				else return true;
			}
			
		});
		for(File file : srcDir.listFiles()) {
			for(File toRename : destFiles) {
				String destName = toRename.getName();
				destName = destName.substring(0,destName.lastIndexOf("_details"));
				if (file.getName().contains(destName)) {
					File newName = new File(pathToRename+file.getName()+"_details.csv");
					System.out.println(toRename.renameTo(newName));
				}
			}
		}
	}

}
