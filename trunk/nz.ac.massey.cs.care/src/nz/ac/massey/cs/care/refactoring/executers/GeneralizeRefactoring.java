package nz.ac.massey.cs.care.refactoring.executers;

import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.ClassObject;

import java.util.ArrayList;
import java.util.List;

import nz.ac.massey.cs.care.Precondition;
import nz.ac.massey.cs.care.ast.Checker;
import nz.ac.massey.cs.care.ast.CheckerASTVisitor;
import nz.ac.massey.cs.care.ast.FieldASTInfo;
import nz.ac.massey.cs.care.ast.ParameterASTInfo;
import nz.ac.massey.cs.care.ast.ReturnASTInfo;
import nz.ac.massey.cs.care.ast.VariableASTInfo;
import nz.ac.massey.cs.care.refactoring.movehelper.Candidate;
import nz.ac.massey.cs.care.refactoring.movehelper.ConstraintsResult;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.internal.corext.refactoring.structure.ChangeTypeRefactoring;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class GeneralizeRefactoring extends CareRefactoring {

	private CheckerASTVisitor visitor = null;
	private ConstraintsResult result = candidate.getConstraintsResult();
	
	public GeneralizeRefactoring(Candidate c) {
		super(c, new Precondition[]{});
		sourceICompilationUnit = (ICompilationUnit) c.getSourceClassObject().getCompilationUnit().getJavaElement();
	}
	
	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		final RefactoringStatus status= new RefactoringStatus();
		try {
			pm.beginTask("Checking preconditions...", 2);
			visitSource(status);
		} finally {
			pm.done();
		}
		return status;
	}
	private void visitSource(RefactoringStatus status) {
		ClassObject sourceClass = candidate.getSourceClassObject();
		ClassObject targetClass = candidate.getTargetClassObject();
		try {
			Checker checker = new Checker(ASTReader.getExaminedProject().getProject(), targetClass);
			visitor = new CheckerASTVisitor(checker, sourceClass, targetClass ,true);
			visitor.visitme();
			if(!visitor.isRefactorable() || Helper.supertypeToUse==null) {
				result.setNoValidSupertype(false);
				status.addError("No Valid Supertype");
				return;
			} else {
				result.setNoValidSupertype(true);
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
		RefactoringStatus status = new RefactoringStatus();
		visitSource(status);
		for(int i = 0; i< visitor.getNumberOfDeclarationElements() ; i++) {
			if(i!=0) visitSource(status);
			if(status.hasError()) {
				IStatus status1 = new Status(IStatus.ERROR, ResourcesPlugin.PI_RESOURCES, "No Valid Supertype");
				throw new CoreException(status1);
			}
			ASTNode typeToGeneralize = getTypesToGeneralize().iterator().next();
			String supertype = Helper.supertypeToUse;//suptype.getFullyQualifiedName();
			ChangeTypeRefactoring r = new ChangeTypeRefactoring(sourceICompilationUnit, typeToGeneralize.getStartPosition(), typeToGeneralize.getLength(), supertype);
			try{
				status.merge(r.checkAllConditions(pm));
				Change change2 = r.createChange(pm);
				undoList.add(change2.perform(pm));
			}catch(Exception e) {
				IStatus status1 = new Status(IStatus.ERROR, ResourcesPlugin.PI_RESOURCES, "No Valid Supertype");
				throw new CoreException(status1);
			}
		}
		return null;
	}
	
	private List<ASTNode> getTypesToGeneralize() {
		List<ASTNode> types = new ArrayList<ASTNode>();
		for(FieldASTInfo field : visitor.getFieldsTobeReplaced()) {
			types.add(field.getFieldDeclaration());
		}
		for(VariableASTInfo variable : visitor.getVariablesTobeReplaced()) { 
			types.add(variable.getVariable());
		}
		for(ParameterASTInfo parameter : visitor.getParametersTobeReplaced()) {
			types.add(parameter.getParameter());
		}
		for(ReturnASTInfo returnType : visitor.getReturnTypesTobeReplaced()) {
			types.add(returnType.getReturnType());
		}
		return types;
	}

	@Override
	public String getName() {
		return "Type Generalisation";
	}

}
