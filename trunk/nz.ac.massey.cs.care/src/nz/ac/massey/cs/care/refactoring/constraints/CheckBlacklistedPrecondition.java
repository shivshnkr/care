package nz.ac.massey.cs.care.refactoring.constraints;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Display;

import nz.ac.massey.cs.care.Activator;
import nz.ac.massey.cs.care.Precondition;
import nz.ac.massey.cs.care.io.MoveJarReader;
import nz.ac.massey.cs.care.refactoring.movehelper.Candidate;

public class CheckBlacklistedPrecondition implements Precondition {

	@Override
	public boolean isFailed(Candidate candidate) {
		String classToMove = candidate.getClassToMove();
		List<String> blacklisted = getFixedClasses();
		for(String classBlacklisted : blacklisted){
			if(classBlacklisted.equals(classToMove)) return true;
			String outerclass = MoveJarReader.getOuterClass(classToMove);
			if(outerclass != null && outerclass.equals(classBlacklisted)) return true;
		}
		return false;
	}

	@Override
	public boolean isGraphLevel() {
		return true;
	}

	@Override
	public String getName() {
		return "Blacklisted";
	}
	
	public List<String> getFixedClasses() {
		final List<String> classes = new ArrayList<String>();
		Display.getDefault().syncExec( new Runnable() {  
			public void run() { 
				String[] blacklisted = Activator.getDefault().getBlacklistedPreference();
				for (int i = 0; i < blacklisted.length; i++) {
					String blacklistedClass = blacklisted[i];
					classes.add(blacklistedClass);
				}
		} });
		return classes;
 }

}
