package nz.ac.massey.cs.care.refactoring.executers;

import gr.uom.java.ast.ClassObject;
import nz.ac.massey.cs.care.ast.CheckerASTVisitor;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

public class ReplaceByFactoryCallRefactoring extends Refactoring {

	private ClassObject sourceClass = null;
	private MultiTextEdit sourceMultiTextEdit;
	private CompilationUnitChange compilationUnitChange;
	private CompilationUnitChange factoryCompilationUnitChange;
	private CompilationUnit sourceCompilationUnit;
	private CheckerASTVisitor visitor;
	private ImportRewrite importRewrite = null;
	private ASTRewrite sourceRewriter = null;
	
	
	public ReplaceByFactoryCallRefactoring(ClassObject sourceClass2, CheckerASTVisitor visitor) {
		this.sourceClass = sourceClass2;
		this.visitor = visitor;
		
		if(!sourceClass.isAnony()){
			this.sourceCompilationUnit = (CompilationUnit) sourceClass.getTypeDeclaration().getRoot();
		} else {
			this.sourceCompilationUnit = (CompilationUnit) sourceClass.getAnonymousClassDeclaration().getRoot();
		}
		ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
		this.compilationUnitChange = new CompilationUnitChange("", sourceICompilationUnit);
		this.sourceMultiTextEdit = new MultiTextEdit();
		this.compilationUnitChange.setEdit(sourceMultiTextEdit);
		this.sourceRewriter = ASTRewrite.create(this.sourceClass.getTypeDeclaration().getAST());
	}

	public ReplaceByFactoryCallRefactoring(ClassObject sourceClass2,
			CheckerASTVisitor visitor2, ASTRewrite sourceRewriter,
			ImportRewrite importRewrite) {
		this.sourceClass = sourceClass2;
		this.visitor = visitor2;
		this.sourceCompilationUnit = (CompilationUnit) sourceClass.getTypeDeclaration().getRoot();
		ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
		this.compilationUnitChange = new CompilationUnitChange("", sourceICompilationUnit);
		this.sourceMultiTextEdit = new MultiTextEdit();
		this.compilationUnitChange.setEdit(sourceMultiTextEdit);
		this.sourceRewriter = sourceRewriter;
		this.importRewrite = importRewrite;
	}

	@Override
	public String getName() {
		return "Replace by Factory";
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

	private void apply(RefactoringStatus status) {
		AST ast = null;
		if(sourceClass.isAnony()) {
			ast = sourceClass.getAnonymousClassDeclaration().getAST();
		} else {
			ast = sourceClass.getTypeDeclaration().getAST();
		}
//		sourceRewriter = ASTRewrite.create(ast);
		for(ClassInstanceCreation instance : visitor.getConstructorInvocationsToReplace()) {
			String methodName = "init_" + instance.getType().resolveBinding().getQualifiedName();
			methodName = methodName.replace(".", "_");
			MethodInvocation factoryCall = getFactoryCall(methodName);
			if(instance.getAnonymousClassDeclaration() != null) {
				status.addError("anonymous class instance");
				return;
			}
			sourceRewriter.replace(instance, factoryCall, null);
		}
//		for(QualifiedName instance : visitor.getSfiToReplace()) {
//			String methodName = "initSFI_" + instance.getQualifier().getFullyQualifiedName() + "_" + instance.getName().toString();
//			methodName = methodName.replace(".", "_");
//			MethodInvocation factoryCall = getFactoryCall(methodName);
//			sourceRewriter.replace(instance, factoryCall, null);
//		}
//		for(MethodInvocation instance : visitor.getSmiToReplace()) {
//			String methodName = "initSMI_" + instance.getExpression().resolveTypeBinding().getQualifiedName() + "_" + instance.getName().toString();
//			methodName = methodName.replace(".", "_");
//			MethodInvocation factoryCall = getFactoryCall(methodName);
//			sourceRewriter.replace(instance, factoryCall, null);
//		}
//		if(!status.hasError()){
//			writeDownCodeChanges(sourceRewriter);
//		}
	}
	private MethodInvocation getFactoryCall(String methodName) {
		AST ast = sourceCompilationUnit.getAST();
		MethodInvocation call = ast.newMethodInvocation();
		call.setName(ast.newSimpleName(methodName));
		call.setExpression(ast.newName("registry.ServiceLocator"));
		return call;
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

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException,
			OperationCanceledException {
		try {
			pm.beginTask("Creating change...", 1);
			writeDownCodeChanges(sourceRewriter);
			CompositeChange changes = new CompositeChange("ReplaceByFactory");
			changes.add(factoryCompilationUnitChange);
			changes.add(compilationUnitChange);
			return changes;
		} finally {
			pm.done();
		}
	}
}
