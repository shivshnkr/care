package nz.ac.massey.cs.care.move.refactoring.constraints;

import nz.ac.massey.cs.care.move.Precondition;
import nz.ac.massey.cs.care.move.refactoring.movehelper.MoveCandidate;
import nz.ac.massey.cs.care.move.scripts.MoveExecuter;

public class CheckBlacklistedPrecondition implements Precondition {

	@Override
	public boolean check(MoveCandidate candidate) {
		String classToMove = candidate.getClassToMove();
		return MoveExecuter.isBlacklisted(classToMove);
	}

	@Override
	public boolean isGraphLevel() {
		return true;
	}

	@Override
	public String getName() {
		return "Blacklisted";
	}

}
