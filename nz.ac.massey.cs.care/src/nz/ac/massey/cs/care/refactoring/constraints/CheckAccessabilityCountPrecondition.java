package nz.ac.massey.cs.care.refactoring.constraints;

import static nz.ac.massey.cs.care.io.MoveJarReader.getChangeAccessabilityRefactoringsCount;
import static nz.ac.massey.cs.care.io.MoveJarReader.getInnerClasses;
import static nz.ac.massey.cs.care.io.MoveJarReader.getOuterClass;
import static nz.ac.massey.cs.care.io.MoveJarReader.isOuterClass;
import nz.ac.massey.cs.care.Precondition;
import nz.ac.massey.cs.care.refactoring.movehelper.Candidate;

public class CheckAccessabilityCountPrecondition implements Precondition {

	@Override
	public boolean isFailed(Candidate candidate) {
		String classToMove = candidate.getClassToMove();
		int count = countRefacRequired(classToMove);
		if(count > 0) return true;
		else return false;
	}

	@Override
	public boolean isGraphLevel() {
		return true;
	}
	
	private static int countRefacRequired(String class2Move) throws NullPointerException {
		int counter = 0;
		if(isOuterClass(class2Move)){
			counter += getChangeAccessabilityRefactoringsCount(class2Move);
			for(String inner : getInnerClasses(class2Move)) {
				counter += getChangeAccessabilityRefactoringsCount(inner);
			}
		} else {
			//get the outer class and all its inner classes
			String outerclass = getOuterClass(class2Move);
			counter += getChangeAccessabilityRefactoringsCount(outerclass);
			for(String inner : getInnerClasses(outerclass)) {
				counter += getChangeAccessabilityRefactoringsCount(inner);
			}
		}
		return counter;
	}

	@Override
	public String getName() {
		return "Change Accessability";
	}
}
