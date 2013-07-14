package nz.ac.massey.cs.care.dependency;

import nz.ac.massey.cs.care.refactoring.executers.CareRefactoring;
import nz.ac.massey.cs.care.refactoring.movehelper.Candidate;

public abstract class Dependency {
	protected Candidate candidate;
	public Dependency(Candidate c) {
		this.candidate = c;
	}
	public abstract String getName();
	public abstract CareRefactoring getRefactoring();
}
