package nz.ac.massey.cs.care.dependency;

import nz.ac.massey.cs.care.refactoring.executers.CareRefactoring;
import nz.ac.massey.cs.care.refactoring.executers.MoveClassRefactoring;
import nz.ac.massey.cs.care.refactoring.movehelper.Candidate;

/**
 * Dependency category of "Other".
 * The default refactoring for this category is Move Class refactoring.
 * @author  Ali Shah
 */
public class OtherDependency extends Dependency {

	public OtherDependency(Candidate c) {
		super(c);
	}

	@Override
	public String getName() {
		return "Other";
	}

	@Override
	public CareRefactoring getRefactoring() {
		return new MoveClassRefactoring(candidate);
	}

}
