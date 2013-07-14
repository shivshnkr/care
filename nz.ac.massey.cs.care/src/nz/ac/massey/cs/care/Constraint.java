package nz.ac.massey.cs.care;

import nz.ac.massey.cs.care.refactoring.movehelper.Candidate;

public interface Constraint {

	/**
	 * Returns true if this postcondition fails.
	 * @param candidate
	 * @return boolean
	 */
	public abstract boolean isFailed(Candidate candidate);

	public abstract boolean isGraphLevel();

	public abstract String getName();

}