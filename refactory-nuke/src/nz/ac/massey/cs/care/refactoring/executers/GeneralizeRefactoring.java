package nz.ac.massey.cs.care.refactoring.executers;

import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.ClassObject;
import nz.ac.massey.cs.care.ast.Checker;
import nz.ac.massey.cs.care.ast.CheckerASTVisitor;
import nz.ac.massey.cs.care.ast.FieldASTInfo;
import nz.ac.massey.cs.care.ast.ParameterASTInfo;
import nz.ac.massey.cs.care.ast.ReturnASTInfo;
import nz.ac.massey.cs.care.ast.VariableASTInfo;
import nz.ac.massey.cs.gql4jung.Edge;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Type;
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

public class GeneralizeRefactoring extends Refactoring {

	private ClassObject sourceClass = null;
	private ClassObject targetClass = null;
	private CheckerASTVisitor visitor = null;
	private Edge winner = null;
	private CompilationUnitChange compilationUnitChange;
	private MultiTextEdit sourceMultiTextEdit;
	private CompilationUnit sourceCompilationUnit;
	private ASTRewrite sourceRewriter = null;
	private ImportRewrite importRewrite = null;
	
	public GeneralizeRefactoring(Edge winner) {
		this.winner = winner;
		this.sourceClass = ASTReader.getSystemObject().getClassObject(winner.getStart().getFullname());
		this.sourceCompilationUnit = (CompilationUnit) sourceClass.getTypeDeclaration().getRoot();
		ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
		this.compilationUnitChange = new CompilationUnitChange("", sourceICompilationUnit);
		this.sourceMultiTextEdit = new MultiTextEdit();
		this.compilationUnitChange.setEdit(sourceMultiTextEdit);
	}

	public GeneralizeRefactoring(Edge winner2, ASTRewrite sourceRewriter,
			ImportRewrite importRewrite, ClassObject sourceClass, ClassObject targetClass) {
		this.winner = winner2;
		this.sourceRewriter = sourceRewriter;
		this.importRewrite = importRewrite;
		this.sourceClass = sourceClass;
		this.targetClass = targetClass;
		this.sourceClass = ASTReader.getSystemObject().getClassObject(winner.getStart().getFullname());
		this.sourceCompilationUnit = (CompilationUnit) sourceClass.getTypeDeclaration().getRoot();
		ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
		this.compilationUnitChange = new CompilationUnitChange("", sourceICompilationUnit);
		this.sourceMultiTextEdit = new MultiTextEdit();
		this.compilationUnitChange.setEdit(sourceMultiTextEdit);
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
		this.sourceRewriter = ASTRewrite.create(this.sourceClass.getTypeDeclaration().getAST());
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
				ITypeBinding supertypetouse = visitor.getSupertypeToUse();
				IType suptype = (IType) supertypetouse.getJavaElement();
				String supertype = suptype.getFullyQualifiedName();
				if(supertype.equals("java.lang.Object")) supertype = "Object";
				for(FieldASTInfo field : visitor.getFieldsTobeReplaced()) {
					addRefactoring(field, sourceRewriter, supertype);
				}
				for(VariableASTInfo variable : visitor.getVariablesTobeReplaced()) { 
					addRefactoring(variable, sourceRewriter, supertype);
				}
				for(ParameterASTInfo parameter : visitor.getParametersTobeReplaced()) {
					addRefactoring(parameter, sourceRewriter, supertype);
				}
				for(ReturnASTInfo returnType : visitor.getReturnTypesTobeReplaced()) {
					addRefactoring(returnType, sourceRewriter, supertype);
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
	private void addRefactoring(FieldASTInfo field, ASTRewrite sourceRewriter, String supertype) {
		Type t = field.getFieldDeclaration().getType();
		AST ast = sourceClass.getTypeDeclaration().getAST();
		sourceRewriter.replace(t, ast.newSimpleType(ast.newName(supertype)), null);
	}
	private void addRefactoring(VariableASTInfo var, ASTRewrite sourceRewriter, String supertype) {
		Type t = var.getVariable().getType();
		AST ast = sourceClass.getTypeDeclaration().getAST();
		sourceRewriter.replace(t, ast.newSimpleType(ast.newName(supertype)), null);
	}
	private void addRefactoring(ParameterASTInfo param, ASTRewrite sourceRewriter, String supertype) {
		Type t = param.getParameter().getType();
		AST ast = sourceClass.getTypeDeclaration().getAST();
		sourceRewriter.replace(t, ast.newSimpleType(ast.newName(supertype)), null);
	}
	private void addRefactoring(ReturnASTInfo returnn, ASTRewrite sourceRewriter, String supertype) {
		Type t = returnn.getReturnType();
		AST ast = sourceClass.getTypeDeclaration().getAST();
		sourceRewriter.replace(t, ast.newSimpleType(ast.newName(supertype)), null);
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
		writeDownCodeChanges(sourceRewriter);
		CompositeChange change = new CompositeChange("ABSTRACTION");
		change.add(compilationUnitChange);
		return change;
	}
	
	private void writeDownCodeChanges(ASTRewrite sourceRewriter) {
		try {
			TextEdit sourceEdit = sourceRewriter.rewriteAST();
			try {
				sourceMultiTextEdit.addChild(sourceEdit);
			}catch(Exception e) { return; }
			compilationUnitChange.addTextEditGroup(new TextEditGroup("Add Factory Refactoring", new TextEdit[] {sourceEdit}));
		}
		catch(JavaModelException javaModelException) {
			return;
		}
	}
}
