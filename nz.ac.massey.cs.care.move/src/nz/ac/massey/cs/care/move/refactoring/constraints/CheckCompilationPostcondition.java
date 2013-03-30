package nz.ac.massey.cs.care.move.refactoring.constraints;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;

import nz.ac.massey.cs.care.move.Postcondition;
import nz.ac.massey.cs.care.move.refactoring.movehelper.MoveCandidate;

public class CheckCompilationPostcondition implements Postcondition {

	private String errorMessage = "";

	@Override
	public boolean check(MoveCandidate candidate) {
		IProject p = candidate.getProject();
		MyCompiler compiler = new MyCompiler(p);
		IStatus s = compiler.build();
		if(s.isOK()) return true;
		else {
			this.errorMessage = s.getMessage();
			return false;
		}
	}

	@Override
	public boolean isGraphLevel() {
		return false;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	@Override
	public String getName() {
		return "Compilation";
	}
}
