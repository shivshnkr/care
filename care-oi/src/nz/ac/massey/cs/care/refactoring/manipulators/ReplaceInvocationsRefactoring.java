package nz.ac.massey.cs.care.refactoring.manipulators;

import gr.uom.java.ast.ClassObject;

import java.util.Set;

import nz.ac.massey.cs.care.ast.CheckerASTVisitor;
import nz.ac.massey.cs.care.ast.VariableBindingManager;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

public class ReplaceInvocationsRefactoring extends Refactoring {

	private ClassObject sourceClass = null;
	private MultiTextEdit sourceMultiTextEdit;
	private CompilationUnitChange compilationUnitChange;
	private CompilationUnitChange factoryCompilationUnitChange;
	private CompilationUnit sourceCompilationUnit;
	private CheckerASTVisitor visitor;
	private Set<MethodInvocation> toReplace;
	
	public ReplaceInvocationsRefactoring(ClassObject sourceClass2, Set<MethodInvocation> toReplace) {
		this.sourceClass = sourceClass2;
		this.toReplace = toReplace;
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
		return "Replace invocations";
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
	private Statement getParentStatement(ASTNode node) {
		while (!(node instanceof Statement)) {
			node = node.getParent();
		}
		return (Statement) node;
	}
	private void apply(RefactoringStatus status) {
		AST ast = null;
		if(sourceClass.isAnony()) {
			ast = sourceClass.getAnonymousClassDeclaration().getAST();
		} else {
			ast = sourceClass.getTypeDeclaration().getAST();
		}
		ASTRewrite sourceRewriter = ASTRewrite.create(ast);
		
		for(MethodInvocation mi : toReplace) {
			MethodInvocation miNew = ast.newMethodInvocation();
			miNew.setName(ast.newSimpleName(mi.getName().toString()));
			sourceRewriter.replace(mi, miNew, null);
		}
		if(!status.hasError()){
			writeDownCodeChanges(sourceRewriter);
		}
	}
	private void removeFieldDeclaration(VariableBindingManager manager, ASTRewrite rewrite) {
		VariableDeclarationFragment fragment = manager.getVariableDeclarationFragment();
		FieldDeclaration statement = (FieldDeclaration) fragment.getParent();
		if(!manager.getInitializer().isEmpty()){
			for(int i = manager.getInitializer().size()-1; i >= 0; i--) {
				Statement node = getParentStatement(manager.getInitializer().get(i));
				rewrite.remove(node, null);
			}
		}
		// add a remove command to the protocol
		rewrite.remove(fragment, null);
		ListRewrite fragmentsListRewrite = rewrite.getListRewrite(statement,
				FieldDeclaration.FRAGMENTS_PROPERTY);
		if (fragmentsListRewrite.getRewrittenList().size() == 0) {
			// add a remove command to the protocol
			rewrite.remove(statement, null);
		}
		
	}
	private void removeVariableDeclaration(VariableBindingManager manager, ASTRewrite rewrite) {
		VariableDeclarationFragment fragment = manager.getVariableDeclarationFragment();
		VariableDeclarationStatement statement = (VariableDeclarationStatement) fragment.getParent();
		if(!manager.getInitializer().isEmpty()){
			for(int i = manager.getInitializer().size()-1; i >= 0; i--) {
				Statement node = getParentStatement(manager.getInitializer().get(i));
				rewrite.remove(node, null);
			}
		}
		// add a remove command to the protocol
		rewrite.remove(fragment, null);
		ListRewrite fragmentsListRewrite = rewrite.getListRewrite(statement,
				VariableDeclarationStatement.FRAGMENTS_PROPERTY);
		if (fragmentsListRewrite.getRewrittenList().size() == 0) {
			// add a remove command to the protocol
			rewrite.remove(statement, null);
		}
		
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
			CompositeChange changes = new CompositeChange("REplaceByFactory");
			changes.add(compilationUnitChange);
			return changes;
		} finally {
			pm.done();
		}
	}
}
