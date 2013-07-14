package nz.ac.massey.cs.care.refactoring.constraints;

import nz.ac.massey.cs.care.Precondition;
import nz.ac.massey.cs.care.Vertex;
import nz.ac.massey.cs.care.refactoring.movehelper.Candidate;

public class CheckInnerClass implements Precondition {

	@Override
	public boolean isFailed(Candidate candidate) {
		Vertex source = candidate.getEdge().getStart();
		Vertex target = candidate.getEdge().getEnd();
		if(source.isAnonymousClass() || target.isAnonymousClass()) {
			return false;
		}
		return true;
	}

	@Override
	public boolean isGraphLevel() {
		return false;
	}

	@Override
	public String getName() {
		return "Inner Class";
	}

}
