package nz.ac.massey.cs.care.refactoring.manipulators;

import gr.uom.java.ast.ClassObject;

import java.util.ArrayList;
import java.util.Collection;

import nz.ac.massey.cs.care.ast.ConstructorInvocationVisitor;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.ChangeDescriptor;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

public class ReplaceByNullRefactoring extends Refactoring {

	private ClassObject sourceClass = null;
	private ClassObject targetClass = null;
	private AbstractionCandidateRefactoring candidate = null;
	private MultiTextEdit sourceMultiTextEdit;
	private CompilationUnitChange compilationUnitChange;
	private CompilationUnit sourceCompilationUnit;
	
	
	public ReplaceByNullRefactoring(AbstractionCandidateRefactoring candidate) {
		this.candidate = candidate;
	}
	public void loadInstatiations() {
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
		return "Replace by Null";
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
		if (!candidate.instantiateParticipants()) {
			RefactoringStatusEntry entry = new RefactoringStatusEntry(
					Status.ERROR, "isInstantiationFailed");
			status.addEntry(entry);
			status.addError("Source or target class object not found");
			return;
		}
		sourceClass = candidate.getSourceClassObject();
		targetClass = candidate.getTargetClassObject();
		loadInstatiations();

		ConstructorInvocationVisitor visitor = new ConstructorInvocationVisitor(targetClass);
		sourceClass.getCompilationUnit().accept(visitor);
		AST ast = sourceClass.getCompilationUnit().getAST();
		ASTRewrite sourceRewriter = ASTRewrite.create(ast);
		for(ClassInstanceCreation init : visitor.getConstructorInvocationsToReplace()) {
			sourceRewriter.replace(init, ast.newNullLiteral(), null);
		}

		writeDownCodeChanges(sourceRewriter);

	}
	
	
	private void writeDownCodeChanges(ASTRewrite sourceRewriter) {
		try {
			TextEdit sourceEdit = sourceRewriter.rewriteAST();
			try {
				sourceMultiTextEdit.addChild(sourceEdit);
			}catch(Exception e) { return; }
			compilationUnitChange.addTextEditGroup(new TextEditGroup("Add Null Refactoring", new TextEdit[] {sourceEdit}));
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
			final Collection<TextFileChange> changes = new ArrayList<TextFileChange>();
			changes.add(compilationUnitChange);
			CompositeChange change = new CompositeChange(getName(), changes.toArray(new Change[changes.size()])) {
				@Override
				public ChangeDescriptor getDescriptor() {
					ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
					String project = sourceICompilationUnit.getJavaProject().getElementName();
					String description = "ReplaceByNull Refactoring";//MessageFormat.format("Extract from method ''{0}''", new Object[] { sourceTypeDeclaration.getName().getIdentifier()});
					String comment = "";
					return new RefactoringChangeDescriptor(new NullRefactoringDescriptor("1", project, description, comment, 0));
				}
			};
			return change;
		} finally {
			pm.done();
		}
	}
	private TryStatement getTryStatement(ClassInstanceCreation e) {
		ASTNode node = e.getParent();
		while (node!=null && !(node instanceof TryStatement)) {
			node = node.getParent();
		}
		if(node != null && node instanceof TryStatement) return (TryStatement) node;
		else return null;
	}
}
