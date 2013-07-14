package nz.ac.massey.cs.care.dependency;

import nz.ac.massey.cs.care.refactoring.executers.*;
import nz.ac.massey.cs.care.refactoring.movehelper.Candidate;
/**
 * Basically represents Variable or field type of dependency.
 * It also represents other declaration elements like method return type (MRT) and method param type (MPT)
 * We combine these because for VD/MRT/MPT we use the same Generalize refactoring. 
 * @author  Ali Shah
 */
public class VDDependency extends Dependency {

	public VDDependency(Candidate c) {
		super(c);
	}

	@Override
	public String getName() {
		return "VD/MRT/MPT";
	}

	@Override
	public CareRefactoring getRefactoring() {
		return new GeneralizeRefactoring(candidate);
	}

}
