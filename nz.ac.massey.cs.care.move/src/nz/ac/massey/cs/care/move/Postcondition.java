package nz.ac.massey.cs.care.move;

import nz.ac.massey.cs.care.move.refactoring.movehelper.MoveCandidate;

public interface Postcondition {
	/**
	 * Returns true if this postcondition fails.
	 * @param candidate
	 * @return boolean
	 */
	public boolean check(MoveCandidate candidate);
	public boolean isGraphLevel();
	public String getName();
}
