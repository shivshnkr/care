package nz.ac.massey.cs.care.refactoring.executers;

import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.ClassObject;
import nz.ac.massey.cs.care.ast.Checker;
import nz.ac.massey.cs.care.ast.CheckerASTVisitor;
import nz.ac.massey.cs.gql4jung.Edge;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;

/**
 * This refactoring deals with constructor invocations of target type.
 * This introduces service locator pattern.
 * @author alishah
 *
 */
public class SLRefactoring extends Refactoring {

	private ClassObject sourceClass = null;
	private ClassObject targetClass = null;
	private ReplaceByFactoryRefactoring refactoring2 = null; //to create a method stub in Factory class
	private ReplaceByFactoryCallRefactoring refactoring3 = null; //to create factory method invocation 
	private CheckerASTVisitor visitor = null;
	private Edge winner = null;
	private String targetSuperclassName = null;
	
	
	public SLRefactoring(Edge winner) {
		this.winner = winner;
	}

	@Override
	public String getName() {
		return "Service Locator";
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		RefactoringStatus status= new RefactoringStatus();
		try {
			pm.beginTask("Checking preconditions...", 1);
			if(!instantiateSourceNTarget()) {
				RefactoringStatusEntry entry = new RefactoringStatusEntry(Status.ERROR,"isInstantiationFailed");
				status.addEntry(entry);
				status.addError("Source or target class object not found");
			}
		} finally {
			pm.done();
		}
		return status;
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		final RefactoringStatus status= new RefactoringStatus();
		try {
			pm.beginTask("Checking preconditions...", 2);
			apply(status);
		} finally {
			pm.done();
		}
		return status;
	}
	public boolean instantiateSourceNTarget(){
		boolean succeed = false;
		String sourceClass = this.winner.getStart().getFullname();
		String targetClass = this.winner.getEnd().getFullname();
		if(sourceClass.contains("$")) sourceClass = sourceClass.replace("$", ".");
		if(targetClass.contains("$")) targetClass = targetClass.replace("$", ".");
		ClassObject sourceClassObject = ASTReader.getSystemObject().getClassObject(sourceClass);
		if(sourceClassObject == null) return succeed;
		else this.sourceClass = sourceClassObject;
		ClassObject targetClassObject = ASTReader.getSystemObject().getClassObject(targetClass);
		if(targetClassObject == null) return succeed; 
		else this.targetClass = targetClassObject;
		return true;
	}
	private void apply(RefactoringStatus status) {
		try {
			Checker checker = new Checker(ASTReader.getExaminedProject().getProject(), targetClass);
			visitor = new CheckerASTVisitor(checker, sourceClass, targetClass ,false);
			if(sourceClass.isAnony()) {
				sourceClass.getAnonymousClassDeclaration().accept(visitor);
			} else {
				sourceClass.getTypeDeclaration().accept(visitor);
			}
			int numOfConstructorInvocations =  visitor.getNumberOfOtherUses(); // ClassInstanceCreation only
			
			if(numOfConstructorInvocations > 0 ) { 
				if(visitor.isRefactorable()){
					refactoring2 = new ReplaceByFactoryRefactoring(sourceClass, targetClass, targetSuperclassName, visitor);
					status.merge(refactoring2.checkFinalConditions(new NullProgressMonitor()));
					if(!status.hasError()) {
						refactoring3 = new ReplaceByFactoryCallRefactoring(sourceClass, visitor);
						status.merge(refactoring3.checkFinalConditions(new NullProgressMonitor()));
					}
				} else {
					status.addError("Factory method could not be created");
				}
				
			}
			
		}catch (JavaModelException e) {
			e.printStackTrace();
		} catch (CoreException e) {
			e.printStackTrace();
		}
        
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException,
			OperationCanceledException {
		CompositeChange change = new CompositeChange("Service Locator");
		if(refactoring2 != null) {
			Change change2 = refactoring2.createChange(pm);
			change.add(change2);
		}
		if(refactoring3 != null) {
			Change change3 = refactoring3.createChange(pm);
			change.add(change3);
		}
		return change;
	}
}
