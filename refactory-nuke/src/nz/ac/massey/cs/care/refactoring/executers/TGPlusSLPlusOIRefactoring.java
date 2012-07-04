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
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

/**
 * This class is used to deal with a composite refactoring consisting of 
 * Type Generalization (TG), Service Locator (SL) and Object In-lining (OI)
 * @author Ali
 *
 */
public class TGPlusSLPlusOIRefactoring extends Refactoring {

	private ClassObject sourceClass = null;
	private ClassObject targetClass = null;
	private CheckerASTVisitor visitor = null;
	private OIRefactoring refactoring1 = null;
	private GeneralizeRefactoring r2 = null;
	private Edge winner = null;
	private ReplaceByFactoryRefactoring refactoring2 = null; //to create a method stub in Factory class
	private ReplaceByFactoryCallRefactoring refactoring3 = null; //to create factory method invocation 
	private String targetSuperclassName = null;
	private ASTRewrite sourceRewriter = null;
	private ImportRewrite importRewrite = null;
	private CompilationUnit sourceCompilationUnit;
	private CompilationUnitChange compilationUnitChange;
	private MultiTextEdit sourceMultiTextEdit;

	public TGPlusSLPlusOIRefactoring(Edge winner) {
		this.winner = winner;
		this.sourceClass = ASTReader.getSystemObject().getClassObject(winner.getStart().getFullname());
		this.sourceCompilationUnit = (CompilationUnit) sourceClass.getTypeDeclaration().getRoot();
		ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
		this.compilationUnitChange = new CompilationUnitChange("", sourceICompilationUnit);
		this.sourceMultiTextEdit = new MultiTextEdit();
		this.compilationUnitChange.setEdit(sourceMultiTextEdit);
	}

	@Override
	public String getName() {
		return "TG+SL+OI refactorings";
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
		this.sourceRewriter = ASTRewrite.create(this.sourceClass.getTypeDeclaration().getAST());
		this.importRewrite = ImportRewrite.create(this.sourceClass.getCompilationUnit(), true);
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
				r2 = new GeneralizeRefactoring(winner, sourceRewriter, importRewrite, sourceClass, targetClass);
				status.merge(r2.checkFinalConditions(new NullProgressMonitor()));
			} else {
				//precondition 2: it should have astnodes (ClassInstanceCreation|Static member invocation) of target type
				if(numOfOtherUses == 0) {
					status.addError("The target type does not exist in the source type");
					return;
				}
			}
			if(numOfOtherUses > 0 ) {
				if(visitor.isRefactorable()){
					refactoring2 = new ReplaceByFactoryRefactoring(sourceClass, targetClass, targetSuperclassName, visitor);
					status.merge(refactoring2.checkFinalConditions(new NullProgressMonitor()));
					if(!status.hasError()) {
						refactoring3 = new ReplaceByFactoryCallRefactoring(sourceClass, visitor, sourceRewriter, importRewrite);
						status.merge(refactoring3.checkFinalConditions(new NullProgressMonitor()));
					}
					refactoring1 = new OIRefactoring(winner, sourceRewriter, importRewrite, sourceClass, targetClass);
					status.merge(refactoring1.checkFinalConditions(new NullProgressMonitor()));
				}
			}
			writeDownCodeChanges(sourceRewriter, importRewrite);
		}catch (JavaModelException e) {
			e.printStackTrace();
		} catch (CoreException e) {
			e.printStackTrace();
		}
        
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
		CompositeChange change = new CompositeChange("TG+SL+OI");
		change.add(compilationUnitChange);
		change.add(refactoring2.createChange(pm));
		return change;
	}
	private void writeDownCodeChanges(ASTRewrite sourceRewriter, ImportRewrite importRewrite) {

		try {
			TextEdit targetImportEdit = importRewrite.rewriteImports(new NullProgressMonitor());
			if(importRewrite.getCreatedImports().length > 0) {
				sourceMultiTextEdit.addChild(targetImportEdit);
				compilationUnitChange.addTextEditGroup(new TextEditGroup("Add required import declarations", new TextEdit[] {targetImportEdit}));
			}
		}
		catch(CoreException coreException) {
			coreException.printStackTrace();
		}
		try {
			TextEdit sourceEdit = sourceRewriter.rewriteAST();
			try {
				sourceMultiTextEdit.addChild(sourceEdit);
			}catch(Exception e) { return; }
			compilationUnitChange.addTextEditGroup(new TextEditGroup("copy methods", new TextEdit[] {sourceEdit}));
		}
		catch(JavaModelException javaModelException) {
			return;
		}
	}
}
