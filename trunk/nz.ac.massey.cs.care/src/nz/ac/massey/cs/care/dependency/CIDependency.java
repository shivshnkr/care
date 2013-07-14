package nz.ac.massey.cs.care.dependency;

import nz.ac.massey.cs.care.refactoring.executers.CareRefactoring;
import nz.ac.massey.cs.care.refactoring.executers.LocatorRefactoring;
import nz.ac.massey.cs.care.refactoring.movehelper.Candidate;

/**
 * Constructor Invocation type of Dependency
 * @author Ali Shah
 */
public class CIDependency extends Dependency {
	public CIDependency(Candidate c) {
		super(c);
	}
	@Override
	public String getName() {
		return "CI";
	}

	@Override
	public CareRefactoring getRefactoring() {
		return new LocatorRefactoring(candidate);
	}

}
