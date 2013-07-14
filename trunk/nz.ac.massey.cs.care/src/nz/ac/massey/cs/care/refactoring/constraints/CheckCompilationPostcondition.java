package nz.ac.massey.cs.care.refactoring.constraints;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;

import nz.ac.massey.cs.care.Postcondition;
import nz.ac.massey.cs.care.refactoring.movehelper.Candidate;
import nz.ac.massey.cs.care.util.Utils;

public class CheckCompilationPostcondition implements Postcondition {

	private String errorMessage = "";

	@Override
	public boolean isFailed(Candidate candidate) {
		IProject p = candidate.getProject();
		MyCompiler compiler = new MyCompiler(p);
		IStatus s = compiler.build();
		if(s.isOK()) {
			try {
				candidate.setGraph(Utils.loadGraph(candidate.getGraphSource()));
			} catch (Exception e) {
				e.printStackTrace();
			}
			return false;
		}
		else {
			this.errorMessage = s.getMessage();
			return true;
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
