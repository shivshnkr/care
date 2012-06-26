package nz.ac.massey.cs.care.refactoring.slhelper;

import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.ClassObject;

import java.util.ArrayList;
import java.util.List;

import nz.ac.massey.cs.care.ast.Checker;
import nz.ac.massey.cs.care.ast.CheckerASTVisitor;
import nz.ac.massey.cs.care.ast.FieldASTInfo;
import nz.ac.massey.cs.care.ast.ParameterASTInfo;
import nz.ac.massey.cs.care.ast.ReturnASTInfo;
import nz.ac.massey.cs.care.ast.VariableASTInfo;
import nz.ac.massey.cs.gql4jung.Edge;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.corext.refactoring.structure.ChangeTypeRefactoring;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;

@SuppressWarnings("restriction")
public class GeneralizeRefactoring extends Refactoring {

	private ClassObject sourceClass = null;
	private ClassObject targetClass = null;
	private List<ChangeTypeRefactoring> changeTypeRefactorings = new ArrayList<ChangeTypeRefactoring>();
	private CheckerASTVisitor visitor = null;
	private Edge winner = null;
	public GeneralizeRefactoring(Edge winner) {
		this.winner = winner;
	}

	@Override
	public String getName() {
		return "Type Generalisation";
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
			int numOfDeclarations =  visitor.getNumberOfDeclarationElements();
			int numOfOtherUses =  visitor.getNumberOfOtherUses(); // ClassInstanceCreation|Static member invocation
			//precondition 1
			if(numOfDeclarations > 0) {
				visitor = new CheckerASTVisitor(checker, sourceClass, targetClass ,true);
				if(sourceClass.isAnony()) {
					sourceClass.getAnonymousClassDeclaration().accept(visitor);
				} else {
					sourceClass.getTypeDeclaration().accept(visitor);
				}
				if(!visitor.isRefactorable()) {
					status.addError("The target type does not exist in the source type");
					return;
				}
				if(visitor.getSupertypeToUse() == null) {
					status.addError("The target type does not exist in the source type");
					return;
				}
//				IType supertype = (IType) visitor.getSupertypeToUse().getJavaElement();
				ICompilationUnit sourceICUnit = (ICompilationUnit) sourceClass.getCompilationUnit().getJavaElement();
				String supertype1 = visitor.getSupertypeToUse().getQualifiedName();
				for(FieldASTInfo field : visitor.getFieldsTobeReplaced()) {
					addRefactoring(sourceICUnit, field.getStartPosition(), field.getLength(), supertype1);
				}
				for(VariableASTInfo variable : visitor.getVariablesTobeReplaced()) { 
					addRefactoring(sourceICUnit, variable.getStartPosition(), variable.getLength(), supertype1);
				}
				for(ParameterASTInfo parameter : visitor.getParametersTobeReplaced()) {
					addRefactoring(sourceICUnit, parameter.getStartPosition(), parameter.getLength(), supertype1);
				}
				for(ReturnASTInfo returyType : visitor.getReturnTypesTobeReplaced()) {
					addRefactoring(sourceICUnit, returyType.getStartPosition(), returyType.getLength(), supertype1);

				}
			} else {
				//precondition 2: it should have astnodes (ClassInstanceCreation|Static member invocation) of target type
				if(numOfOtherUses == 0) {
					status.addError("The target type does not exist in the source type");
					return;
				}
			}
			
		}catch (JavaModelException e) {
			e.printStackTrace();
		} catch (CoreException e) {
			e.printStackTrace();
		}
        
	}
	private void addRefactoring(ICompilationUnit targeticu, int startPosition,
			int length, String fullyQualifiedName) {
		ChangeTypeRefactoring r  = new ChangeTypeRefactoring(targeticu, startPosition, length, fullyQualifiedName);
		try {
			r.checkInitialConditions(new NullProgressMonitor());
			r.checkFinalConditions(new NullProgressMonitor());
		} catch (CoreException e) {
			e.printStackTrace();
		}
		changeTypeRefactorings.add(r);
		
	}

	public int getNoOfDeclarationElements() {
		return visitor.getNumberOfDeclarationElements();
	}
	
	public int getNoOfDICases() {
		return visitor.getNumberOfOtherUses();
		
	}
	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException,
			OperationCanceledException {
		CompositeChange change = new CompositeChange("ABSTRACTION");
		for(ChangeTypeRefactoring r : changeTypeRefactorings) {
			change.add(r.createChange(pm));
		}
		return change;
	}
}
