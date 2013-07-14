package nz.ac.massey.cs.care.dependency;

import nz.ac.massey.cs.care.refactoring.executers.CareRefactoring;
import nz.ac.massey.cs.care.refactoring.executers.MoveClassRefactoring;
import nz.ac.massey.cs.care.refactoring.movehelper.Candidate;

public class METDependency extends Dependency {

	public METDependency(Candidate c) {
		super(c);
	}

	@Override
	public String getName() {
		return "MET";
	}

	@Override
	public CareRefactoring getRefactoring() {
		return new MoveClassRefactoring(candidate);
	}

}
