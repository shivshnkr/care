package nz.ac.massey.cs.care.refactoring.constraints;

import nz.ac.massey.cs.care.Precondition;
import nz.ac.massey.cs.care.refactoring.movehelper.Candidate;

public class CheckSamePackagePrecondition implements Precondition {

	@Override
	public boolean isFailed(Candidate candidate) {
		if(candidate.getSourcePackage().equals(candidate.getTargetPackage())) return true;
		return false;
	}

	@Override
	public boolean isGraphLevel() {
		return true;
	}

	@Override
	public String getName() {
		return "same package";
	}

}
