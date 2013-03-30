package nz.ac.massey.cs.care.move.refactoring.constraints;

import nz.ac.massey.cs.care.move.Precondition;
import nz.ac.massey.cs.care.move.io.JarReader;
import nz.ac.massey.cs.care.move.refactoring.movehelper.MoveCandidate;

public class CheckRenamePrecondition implements Precondition {

	@Override
	public boolean check(MoveCandidate candidate) {
		String classToMove = candidate.getClassToMove();
		String targetPackage = candidate.getTargetPackage();
		String simplename = classToMove.substring(classToMove.lastIndexOf(".")+1);
		if(JarReader.isRenameRequired(simplename, targetPackage)) return true;
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
