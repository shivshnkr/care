package nz.ac.massey.cs.care.refactoring.executers;

import gr.uom.java.ast.ClassObject;
import gr.uom.java.ast.FieldObject;
import gr.uom.java.ast.MethodObject;
import gr.uom.java.ast.util.TypeVisitor;

import java.util.Set;
import java.util.TreeMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

public class MethodInlineRefactoring extends Refactoring {

	private ClassObject sourceClass = null;
	private ImportRewrite importRewrite = null;
	private ASTRewrite sourceRewriter = null;
	private MultiTextEdit sourceMultiTextEdit;
	private CompilationUnitChange compilationUnitChange;
	private CompilationUnit sourceCompilationUnit;
	private Set<MethodObject> staticMethodsToInline ;
	private Set<FieldObject> fieldsToInline;
	private Set<MethodInvocation> methodInvocations2Replace;
	TreeMap<String,ImportDeclaration> unitImports = new TreeMap<String, ImportDeclaration>();
	TreeMap<String,ImportDeclaration> unitNewImports = new TreeMap<String, ImportDeclaration>();
	private Set<SimpleName> fieldInvocations2Replace;
	
	public MethodInlineRefactoring(ClassObject sourceClass2, 
			Set<MethodObject> staticMethodsToInline, Set<FieldObject> fieldstoInline, Set<MethodInvocation> toReplace, Set<SimpleName> fieldInvocations2Replace) {
		this.sourceClass = sourceClass2;
		this.staticMethodsToInline = staticMethodsToInline;
		this.fieldsToInline = fieldstoInline;
		this.methodInvocations2Replace = toReplace;
		this.fieldInvocations2Replace = fieldInvocations2Replace;
		if(!sourceClass.isAnony()){
			this.sourceCompilationUnit = (CompilationUnit) sourceClass.getTypeDeclaration().getRoot();
		} else {
			this.sourceCompilationUnit = (CompilationUnit) sourceClass.getAnonymousClassDeclaration().getRoot();
		}
		ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
		this.compilationUnitChange = new CompilationUnitChange("", sourceICompilationUnit);
		this.sourceMultiTextEdit = new MultiTextEdit();
		this.compilationUnitChange.setEdit(sourceMultiTextEdit);
	}
	public MethodInlineRefactoring(ClassObject sourceClass2, ASTRewrite sourceRewriter,ImportRewrite importRewrite,
			Set<MethodObject> staticMethodsToInline, Set<FieldObject> fieldstoInline, Set<MethodInvocation> toReplace, Set<SimpleName> fieldInvocations2Replace) {
		this.sourceClass = sourceClass2;
		this.sourceRewriter = sourceRewriter;
		this.importRewrite = importRewrite;
		this.staticMethodsToInline = staticMethodsToInline;
		this.fieldsToInline = fieldstoInline;
		this.methodInvocations2Replace = toReplace;
		this.fieldInvocations2Replace = fieldInvocations2Replace;
		if(!sourceClass.isAnony()){
			this.sourceCompilationUnit = (CompilationUnit) sourceClass.getTypeDeclaration().getRoot();
		} else {
			this.sourceCompilationUnit = (CompilationUnit) sourceClass.getAnonymousClassDeclaration().getRoot();
		}
		ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
		this.compilationUnitChange = new CompilationUnitChange("", sourceICompilationUnit);
		this.sourceMultiTextEdit = new MultiTextEdit();
		this.compilationUnitChange.setEdit(sourceMultiTextEdit);
	}
	@Override
	public String getName() {
		return "inline fields and methods";
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
//		ASTRewrite sourceRewriter = null;
		ListRewrite list = null;
		if(sourceClass.isAnony()) {
			ast = sourceClass.getAnonymousClassDeclaration().getAST();
//			sourceRewriter = ASTRewrite.create(ast);
			list = sourceRewriter.getListRewrite(sourceClass.getAnonymousClassDeclaration(), AnonymousClassDeclaration.BODY_DECLARATIONS_PROPERTY);
		} else {
			ast = sourceClass.getTypeDeclaration().getAST();
//			sourceRewriter = ASTRewrite.create(ast);
			list = sourceRewriter.getListRewrite(sourceClass.getTypeDeclaration(), TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
		}
		
//		ImportRewrite importRewrite = ImportRewrite.create(sourceCompilationUnit, true);
		for(MethodObject m : staticMethodsToInline){
			MethodDeclaration node = m.getMethodDeclaration();
			MethodDeclaration md = (MethodDeclaration) ASTNode.copySubtree(node.getAST(), node);
			addRequiredTargetImportDeclarations(node, importRewrite);
			list.insertLast(md, null);
		}
		
		for(FieldObject f : fieldsToInline) {
			FieldDeclaration node = (FieldDeclaration) f.getVariableDeclarationFragment().getParent();
			FieldDeclaration fd = (FieldDeclaration) ASTNode.copySubtree(node.getAST(), node);
			addRequiredTargetImportDeclarations(node, importRewrite);
			list.insertLast(fd, null);
		}
		for(MethodInvocation mi : methodInvocations2Replace) {
			MethodInvocation miNew = (MethodInvocation) ASTNode.copySubtree(mi.getAST(), mi);
			miNew.setExpression(null);
			sourceRewriter.replace(mi, miNew, null);
		}
		for(SimpleName sn : fieldInvocations2Replace) {
			if(sn.getParent() instanceof QualifiedName){
				QualifiedName node = (QualifiedName) sn.getParent();
				SimpleName newName = ast.newSimpleName(node.getName().toString());
				sourceRewriter.replace(node, newName, null);
			}
		}
//		if(!status.hasError()){
//			writeDownCodeChanges(sourceRewriter, importRewrite);
//		}
	}
	private void addRequiredTargetImportDeclarations(ASTNode method, ImportRewrite importRewrite) {
		TypeVisitor typeVisitor = new TypeVisitor();
		method.accept(typeVisitor);
		for(ITypeBinding typeBinding : typeVisitor.getTypeBindings()) {
			importRewrite.addImport(typeBinding);
		}
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

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException,
			OperationCanceledException {
			writeDownCodeChanges(sourceRewriter, importRewrite);
		try {
			pm.beginTask("Creating change...", 1);
			CompositeChange changes = new CompositeChange("Copy Methods");
			changes.add(compilationUnitChange);
			return changes;
		} finally {
			pm.done();
		}
	}
}
