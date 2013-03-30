package nz.ac.massey.cs.care.move.refactoring.constraints;

import nz.ac.massey.cs.care.move.Postcondition;
import nz.ac.massey.cs.care.move.refactoring.movehelper.MoveCandidate;

public class CheckInstanceCountPostcondition implements Postcondition {

	@Override
	public boolean check(MoveCandidate candidate) {
		if(candidate.getInstancesAfter() <= candidate.getInstancesBefore()) return false;
		else return true;
	}

	@Override
	public boolean isGraphLevel() {
		return true;
	}

	@Override
	public String getName() {
		return "Instance Count";
	}

}
