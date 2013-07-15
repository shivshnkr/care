package nz.ac.massey.cs.care.refactoring.executers;

import gr.uom.java.ast.ClassInstanceCreationObject;
import gr.uom.java.ast.ClassObject;
import gr.uom.java.ast.ConstructorObject;
import gr.uom.java.ast.CreationObject;
import gr.uom.java.ast.FieldObject;
import gr.uom.java.ast.MethodObject;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import nz.ac.massey.cs.care.Precondition;
import nz.ac.massey.cs.care.refactoring.movehelper.Candidate;
import nz.ac.massey.cs.care.util.Utils;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class LocatorRefactoring extends CareRefactoring {
	
	private Set<ClassInstanceCreation> constructorInvocations;
	private ClassObject sourceClass;
	private ClassObject targetClass;
	
	public LocatorRefactoring(Candidate candidate) {
		super(candidate, new Precondition[]{});
		sourceICompilationUnit = (ICompilationUnit) candidate.getSourceClassObject().getCompilationUnit().getJavaElement();
		this.constructorInvocations  = new HashSet<ClassInstanceCreation>();

	}
	@Override
	public String getName() {
		return "Service Locator";
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		final RefactoringStatus status= new RefactoringStatus();
		try {
			pm.beginTask("Checking preconditions...", 2);
		} finally {
			pm.done();
		}
		return status;
	}
	private void visitSource(RefactoringStatus status) {
		sourceClass = candidate.getSourceClassObject();
		targetClass = candidate.getTargetClassObject();
		constructorInvocations.clear();
		visitFields();
		visitMethods();
		visitConstructors();
	}
	private void visitFields() {
		Iterator<FieldObject> fieldIter = sourceClass.getFieldIterator();
		while(fieldIter.hasNext()) {
			FieldObject fo = fieldIter.next();
			String initializerClass = "";
			if(fo.isClassInstanceCreation()){
				initializerClass = fo.getInitializerClassName();
			}
			if(initializerClass.equals(targetClass.getSimpleName())) {
				ClassInstanceCreation cic = (ClassInstanceCreation) fo.getVariableDeclarationFragment().getInitializer();
				constructorInvocations.add(cic);
			}
		}
	}
	private void visitConstructors() {
		Iterator<ConstructorObject> ico = sourceClass.getConstructorIterator();
		while(ico.hasNext()) {
			ConstructorObject co = ico.next();
			visitMethodObject(new MethodObject(co));
		}
	}
	private void visitMethods() {
		Iterator<MethodObject> imo = sourceClass.getMethodIterator();
		while(imo.hasNext()) {
			MethodObject mo = imo.next();
			visitMethodObject(mo);
		}
	}
	private void visitMethodObject(MethodObject mo) {
		visitConstructorInvocations(mo);
	}
	private void visitConstructorInvocations(MethodObject mo) {
		
		for(CreationObject co : mo.getCreations()) {
			String name = co.getType().getClassType();
			if(name.contains(".")) name = Utils.getSimpleName(name);
			if(name.equals(targetClass.getSimpleName())) {
				if(co instanceof ClassInstanceCreationObject) {
					ClassInstanceCreationObject coObj = (ClassInstanceCreationObject) co;
					constructorInvocations.add(coObj.getClassInstanceCreation());
				}
			}
		}
	}
	
	private Set<ClassInstanceCreation> removeDuplicateCreations() {
		Set<ClassInstanceCreation> set  = new TreeSet<ClassInstanceCreation>(new Comparator<ClassInstanceCreation>(){

			@Override
			public int compare(ClassInstanceCreation o1, ClassInstanceCreation o2) {
				if(o1.resolveConstructorBinding().isEqualTo(o2.resolveConstructorBinding())) {
					return 0;
				} else {
					return -1;
				}
			}
			
		});
		
		for(ClassInstanceCreation cic : constructorInvocations){
			set.add(cic);
		}
		return set;
	}
	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException,
			OperationCanceledException {
		final RefactoringStatus status= new RefactoringStatus();
		visitSource(status);
		int numOfCIs = removeDuplicateCreations().size();
		for(int i=0; i<numOfCIs; i++) {
			if(i!=0) visitSource(status);
			ClassInstanceCreation cic = removeDuplicateCreations().iterator().next();
			IntroduceFactoryRefactoring ifr = new IntroduceFactoryRefactoring(((ICompilationUnit)sourceClass.getCompilationUnit().getJavaElement()), cic.getStartPosition(), cic.getLength(), targetClass);
			if(Helper.supertypeToUse != null) {
				ifr.setReturnType(Helper.supertypeToUse);
			} else {
				String superTypeToUse = getSuperToUse(cic);
				if(superTypeToUse!=null) ifr.setReturnType(superTypeToUse);
			}
			status.merge(ifr.checkAllConditions(new NullProgressMonitor()));
			if(!status.isOK()) {
				IStatus status1 = new Status(IStatus.ERROR, ResourcesPlugin.PI_RESOURCES, "Error creating factory");
				throw new CoreException(status1);
			}
			ifr.setProtectConstructor(false);
			ifr.setFactoryClass("registry.ServiceLocator");
			String factoryMethodName = "create" + candidate.getTargetClassObject().getSimpleName() + "_"+ new Random().nextInt(10000);
			ifr.setNewMethodName(factoryMethodName);
			Change change2 = ifr.createChange(pm);
			undoList.add(change2.perform(pm));
		}
		Helper.supertypeToUse = null;
		return null;
	}
	private String getSuperToUse(ClassInstanceCreation cic) {
		String result = getNameFromParentStatement(cic);
		if(result == null) result = getNameFromFieldDeclaration(cic);
		if(result == null) {
			IType[] resolvedSupertypes = Utils.getSupertypes(targetClass);
			if(resolvedSupertypes.length == 0) {
				result = "java.lang.Object";
			} else {
				result = resolvedSupertypes[0].getFullyQualifiedName();
			}
		}
		return result;
	}
	private String getNameFromParentStatement(ClassInstanceCreation reference) {
		String result = null;
		ASTNode node = reference;
		while (node!=null && !(node instanceof VariableDeclarationStatement)) {
			node = node.getParent();
		}
		if(node != null){
			VariableDeclarationStatement s = (VariableDeclarationStatement) node;
			result = s.getType().resolveBinding().getQualifiedName();
		} 
		
		return result;
	}
	private String getNameFromFieldDeclaration(ClassInstanceCreation cic) {
		String result = null;
		ASTNode node = cic;
		while(node!=null && !(node instanceof FieldDeclaration)) {
			node = node.getParent();
		}
		if(node != null) {
			FieldDeclaration fd = (FieldDeclaration) node;
			result = fd.getType().resolveBinding().getQualifiedName();
		}
		return result;
	}
}
