package nz.ac.massey.cs.care.dependency;

import nz.ac.massey.cs.care.refactoring.executers.CareRefactoring;
import nz.ac.massey.cs.care.refactoring.executers.InlineRefactoring;
import nz.ac.massey.cs.care.refactoring.movehelper.Candidate;

/**
 * Static Member Invocation type dependency.
 * The default refactoring for this dependency type is static members inlining
 * @author  Ali Shah
 */
public class SMIDependency extends Dependency {
	public SMIDependency(Candidate c) {
		super(c);
	}

	@Override
	public String getName() {
		return "SMI";
	}

	@Override
	public CareRefactoring getRefactoring() {
		return new InlineRefactoring(candidate);
	}

}
