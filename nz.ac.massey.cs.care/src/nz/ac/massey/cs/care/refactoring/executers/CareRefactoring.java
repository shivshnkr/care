package nz.ac.massey.cs.care.refactoring.executers;

import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.ClassObject;
import gr.uom.java.ast.CompilationUnitCache;

import java.util.ArrayList;
import java.util.List;

import nz.ac.massey.cs.care.Postcondition;
import nz.ac.massey.cs.care.Precondition;
import nz.ac.massey.cs.care.refactoring.constraints.CheckCompilationPostcondition;
import nz.ac.massey.cs.care.refactoring.constraints.CheckInstanceCountPostcondition;
import nz.ac.massey.cs.care.refactoring.movehelper.Candidate;
import nz.ac.massey.cs.care.refactoring.movehelper.ConstraintsResult;
import nz.ac.massey.cs.care.refactoring.movehelper.RefactoringResult;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public abstract class CareRefactoring extends Refactoring implements IElementChangedListener{
	protected Candidate candidate = null;
	protected ICompilationUnit sourceICompilationUnit; 
	protected static List<Change> undoList =  new ArrayList<Change>();
	protected Precondition[] pres = new Precondition[]{};
	protected Postcondition[] posts = {new CheckCompilationPostcondition(), 
			new CheckInstanceCountPostcondition()};

	public CareRefactoring() {
		//Do nothing
	}
	public CareRefactoring(Candidate candidate) {
		super();
		this.candidate = candidate;
		JavaCore.addElementChangedListener(this);
	}
	public CareRefactoring(Candidate candidate, Precondition[] pres) {
		super();
		this.candidate = candidate;
		this.pres = pres; 
		JavaCore.addElementChangedListener(this);
	}
	public void elementChanged(ElementChangedEvent event) {
		IJavaElementDelta javaElementDelta = event.getDelta();
		processDelta(javaElementDelta);
	}
	protected void processDelta(IJavaElementDelta delta) {
		IJavaElement javaElement = delta.getElement();
		switch(javaElement.getElementType()) {
		case IJavaElement.COMPILATION_UNIT:
			ICompilationUnit compilationUnit = (ICompilationUnit)javaElement;
			if(delta.getKind() == IJavaElementDelta.CHANGED) {
				if(compilationUnit.equals(sourceICompilationUnit)) {
					CompilationUnitCache.getInstance().compilationUnitChanged(compilationUnit);
					new ASTReader(ASTReader.getExaminedProject(), ASTReader.getSystemObject(), new NullProgressMonitor());
					String sourceName = candidate.getSourceClass();
					if(sourceName.contains("$")) sourceName = sourceName.replace("$", ".");
					ClassObject sourceObject = ASTReader.getSystemObject().getClassObject(sourceName);
					candidate.setSourceClassObject(sourceObject);
					sourceICompilationUnit = (ICompilationUnit) candidate.getSourceClassObject().getCompilationUnit().getJavaElement();
				}
			}
		}
	}
	public void checkPreconditions(RefactoringResult result) {
		RefactoringStatus status= new RefactoringStatus();
		ConstraintsResult cResult = candidate.getConstraintsResult();
		//TO DO: Refactor this code to decouple implementation classes
		int failedPres = 0;
		for(Precondition pre : pres) {
			String name = pre.getName();
			boolean failed = pre.isFailed(candidate);
			if(failed) {
				failedPres ++;
				if(name.equals("No Valid Supertype")) cResult.setNoValidSupertype(false);
				else if(name.equals("Self Instance Creation")) cResult.setSelfInstanceCreation(false);
			} else {
				if(name.equals("No Valid Supertype")) cResult.setNoValidSupertype(true);
				else if(name.equals("Self Instance Creation")) cResult.setSelfInstanceCreation(true);
			}
		}
		if(failedPres > 0) {
			result.setError(true);
		}
		try {
			status.merge(checkInitialConditions(new NullProgressMonitor()));
			status.merge(checkFinalConditions(new NullProgressMonitor()));
		} catch (OperationCanceledException e) {
			e.printStackTrace();
		} catch (CoreException e) {
			e.printStackTrace();
		}
		if(status.hasError() || status.hasFatalError()) {
			result.setError(true);
		}
	}
	
	public void checkPostconditions(RefactoringResult result) {
		ConstraintsResult cResult = candidate.getConstraintsResult();
		int failedPosts = 0;
		for(Postcondition p : posts) {
			String name = p.getName();
			boolean failed = p.isFailed(candidate);
			if(failed) {
				failedPosts ++;
				if(name.equals("Compilation")) {
					cResult.setCompilation(false);
					//we break here to save execution time. If compilation fails, no
					//need to check the instance count post conition
					break;
				}
				else cResult.setInstanceCount(false);
			} else {
				if(name.equals("Compilation")) cResult.setCompilation(true);
				else cResult.setInstanceCount(true);
			}
		}
		if(failedPosts > 0) {
			result.setError(true);
		}
	}
	
	@Override
	public abstract String getName();
	
	public void perform(RefactoringResult result) {
		try {
			createChange(new NullProgressMonitor());
		} catch (OperationCanceledException e) {
			e.printStackTrace();
		} catch (CoreException e) {
			List<String> failedPreconditions = new ArrayList<String>();
			String failureCause = e.getStatus().getMessage();
			failedPreconditions.add(failureCause);
			result.setFailedPreconditions(failedPreconditions);
			result.setError(true);
			rollback();
		}
	}
	
	public void rollback() {
		try {
			for(int i= undoList.size() - 1; i >= 0 ; i--){
				Change subundo = undoList.get(i);
				subundo.perform(new NullProgressMonitor());
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		RefactoringStatus status= new RefactoringStatus();
		try {
			pm.beginTask("Checking preconditions...", 1);
		} finally {
			pm.done();
		}
		return status;
	}

	@Override
	public abstract RefactoringStatus checkFinalConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException;

	@Override
	public abstract Change createChange(IProgressMonitor pm) throws CoreException,
			OperationCanceledException;
	
	public void clear() {
		undoList.clear();
		Helper.supertypeToUse = null;
		JavaCore.removeElementChangedListener(this);
	}

}
