package nz.ac.massey.cs.care.refactoring.constraints;

import nz.ac.massey.cs.care.Precondition;
import nz.ac.massey.cs.care.io.MoveJarReader;
import nz.ac.massey.cs.care.refactoring.movehelper.Candidate;

public class CheckRenamePrecondition implements Precondition {

	@Override
	public boolean isFailed(Candidate candidate) {
		String classToMove = candidate.getClassToMove();
		String targetPackage = candidate.getTargetPackage();
		String simplename = classToMove.substring(classToMove.lastIndexOf(".")+1);
		if(MoveJarReader.isRenameRequired(simplename, targetPackage)) return true;
		else return false;
	}

	@Override
	public boolean isGraphLevel() {
		return true;
	}

	@Override
	public String getName() {
		return "Rename";
	}

}
