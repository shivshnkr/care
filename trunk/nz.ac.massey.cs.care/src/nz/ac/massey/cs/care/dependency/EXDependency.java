package nz.ac.massey.cs.care.dependency;

import nz.ac.massey.cs.care.refactoring.executers.CareRefactoring;
import nz.ac.massey.cs.care.refactoring.executers.MoveClassRefactoring;
import nz.ac.massey.cs.care.refactoring.movehelper.Candidate;
/**
 * Extends type of dependency
 * @author  Ali Shah
 *
 */
public class EXDependency extends Dependency {

	public EXDependency(Candidate c) {
		super(c);
	}

	@Override
	public String getName() {
		return "Extends";
	}

	@Override
	public CareRefactoring getRefactoring() {
		return new MoveClassRefactoring(candidate);
	}

}
