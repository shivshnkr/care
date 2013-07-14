package nz.ac.massey.cs.care.refactoring.executers;

import java.util.ArrayList;
import java.util.List;

import nz.ac.massey.cs.care.Precondition;
import nz.ac.massey.cs.care.refactoring.movehelper.Candidate;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class CompositeRefactoring extends CareRefactoring {
	
	public CompositeRefactoring(Candidate candidate) {
		super(candidate, new Precondition[]{});
	}

	private List<CareRefactoring> refactorings = new ArrayList<CareRefactoring>();
	@Override
	public String getName() {
		
		StringBuffer b = new StringBuffer();
		for(CareRefactoring cr : refactorings) {
			b.append(cr.getName()).append("+");
		}
		String name = b.toString();
		return name.substring(0, name.lastIndexOf("+") );
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		RefactoringStatus status = new RefactoringStatus();
		for(CareRefactoring cr : refactorings) {
			status.merge(cr.checkInitialConditions(pm));
		}
		return status;
	}
	
	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		RefactoringStatus status = new RefactoringStatus();
		for(CareRefactoring cr : refactorings) {
			status.merge(cr.checkFinalConditions(pm));
		}
		return status;
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException,
			OperationCanceledException {
		for(CareRefactoring cr : refactorings) {
			cr.createChange(pm);
		}
		return null;
	}
	
	public void addRefactoring(CareRefactoring refactoring) {
		this.refactorings.add(refactoring);
	}
	
	@Override
	public void clear() {
		undoList.clear();
		JavaCore.removeElementChangedListener(this);
		for(CareRefactoring cr : refactorings) {
			cr.clear();
		}
	}

}
