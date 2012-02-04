package nz.ac.massey.cs.care.refactoring.manipulators;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class NullRefactoringDescriptor extends RefactoringDescriptor {

	protected NullRefactoringDescriptor(String id, String project,
			String description, String comment, int flags) {
		super(id, project, description, comment, flags);
	}

	@Override
	public Refactoring createRefactoring(RefactoringStatus status)
			throws CoreException {
		return null;
	}

}
