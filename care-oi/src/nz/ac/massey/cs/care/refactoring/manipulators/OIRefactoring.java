package nz.ac.massey.cs.care.refactoring.manipulators;

import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.ClassObject;
import gr.uom.java.ast.ConstructorObject;
import gr.uom.java.ast.CreationObject;
import gr.uom.java.ast.FieldInstructionObject;
import gr.uom.java.ast.FieldObject;
import gr.uom.java.ast.LocalVariableDeclarationObject;
import gr.uom.java.ast.MethodInvocationObject;
import gr.uom.java.ast.MethodObject;
import gr.uom.java.ast.TypeObject;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import nz.ac.massey.cs.gql4jung.Edge;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;

public class OIRefactoring extends Refactoring {

	private ClassObject sourceClass = null;
	private ClassObject targetClass = null;
	private Edge winner = null;
	private Set<MethodObject> smiToInline = new LinkedHashSet<MethodObject>();
	private Set<MethodObject> staticMethodsToInline = new LinkedHashSet<MethodObject>();
	private MethodInlineRefactoring r = null;
	private Set<MethodInvocation> methodInvocations2Replace = new LinkedHashSet<MethodInvocation>();
	private Set<FieldObject> fieldsToInline = new LinkedHashSet<FieldObject>();
	private Set<SimpleName> fieldInvocations2Replace = new LinkedHashSet<SimpleName>();
	
	public OIRefactoring(Edge winner) {
		this.winner = winner;
	}

	@Override
	public String getName() {
		return "OI Refactoring";
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
			//All preconditions are checked here.
			//precondition1: if we find MPT dependency we can't refactor using OI
			if(getNumOfMPT() > 0) {
				status.addError("MPT dependency found");
				return;
			}
			//precondition2: if we find MRT dependency, we can't refactor using OI
			if(getNumOfMRT() > 0) {
				status.addError("MRT dependency found");
				return;
			}
			//precondition3: if we find MET dependency, we can't refactor using OI
			if(getNumOfMET() > 0) {
				status.addError("MET dependency found");
				return;
			}
			if(getNumOfCI() > 0) {
				status.addError("CI dependency found");
				return;
			} 
			if(getNumOfVD() > 0) {
				status.addError("VD dependency found");
				return;
			}
			int sum = getNumOfSFI() + getNumOfSMI();;
			if(sum == 0) {
				status.addError("No target type dependency found");
				return;
			}
			//we reach here because we found either SMI or SFI type dependency.
			//in the case of SMI we need to recursively check internal dependencies for SMI in the source class.
			//i.e. other static methods and fields in the target class that have been used by the original SMI.
			computeAllStaticMembersToInline();
			
			boolean hasError = false; //this is the case when any of the target class's static method
			//to be in-lined has a new instance creation with the target type.
			for(MethodObject m : staticMethodsToInline) {
				for(CreationObject co : m.getCreations()) {
					if(co.getType().getClassType().equals(targetClass.getName())) hasError = true;
				}
			}
			for(FieldObject fo : fieldsToInline){
				VariableDeclarationFragment vdf = fo.getVariableDeclarationFragment();
				if(vdf != null) {
					if(vdf.getInitializer() != null && vdf.getInitializer() instanceof ClassInstanceCreation){
						if (vdf.getInitializer().resolveTypeBinding().getJavaElement() != null){
							if (vdf.getInitializer().resolveTypeBinding().getName().equals(targetClass.getSimpleName())){
								hasError = true;
							}
						}
					}
				}
			}
			if(hasError) {
				status.addError("One of the static method or field has new instance creation of the target type");
				return;
			} 
			
			r = new MethodInlineRefactoring(sourceClass, staticMethodsToInline, fieldsToInline, methodInvocations2Replace, fieldInvocations2Replace);
			status.merge(r.checkFinalConditions(new NullProgressMonitor()));
		}catch (JavaModelException e) {
			e.printStackTrace();
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
	
	private void computeAllStaticMembersToInline() {
		Queue<MethodObject> q = new LinkedBlockingQueue<MethodObject>();
		for(MethodObject m : smiToInline) {
			q.add(m);
			staticMethodsToInline.add(m);
		}
		while (!q.isEmpty()) {
			MethodObject m = q.poll();
			addFieldsToBeInlined(m);
			addMethodsToBeInlined(m, q);
		}
	}

	private void addMethodsToBeInlined(MethodObject m, Queue<MethodObject> q) {
		for (MethodInvocationObject mio : m.getInvokedStaticMethods()) {
			// here we take care of two things: recursive methods and
			// cycles between methods.
			//mio should contain any of the name of the list of the target class or one of its super classes
			String methodOwnerClass = mio.getOriginClassName();
			ClassObject ownerClassObject = ASTReader.getSystemObject().getClassObject(methodOwnerClass);
			if(ownerClassObject != null) {
				if (containsInHierarchy(methodOwnerClass, targetClass) 
						&& !mio.getMethodName().equals(m.getName())
						&& !staticMethodsToInline.contains(ownerClassObject.getMethod(mio))) {
					q.add(ownerClassObject.getMethod(mio));
					staticMethodsToInline.add(ownerClassObject.getMethod(mio));
				}
			}
		}
	}

	private void addFieldsToBeInlined(MethodObject m) {
		List<FieldInstructionObject> fieldInstructions = m.getFieldInstructions();
		for(FieldInstructionObject fieldInstruction : fieldInstructions){
			if(containsInHierarchy(fieldInstruction.getOwnerClass(), targetClass)) {
				FieldObject fo = getField(fieldInstruction);
				if(fo != null){
					fieldsToInline.add(fo);
				}
			}
		}
	}

	private FieldObject getField(FieldInstructionObject fieldInstruction) {
		ClassObject c = ASTReader.getSystemObject().getClassObject(fieldInstruction.getOwnerClass());
		if(c != null){
			for(Iterator<FieldObject> iter = c.getFieldIterator(); iter.hasNext();) {
				FieldObject fo = iter.next();
				if(fo.equals(fieldInstruction)) return fo;
			}
		}
		return null;
	}

	public boolean containsInHierarchy(String nameToCheck, ClassObject clazz) {
		if(clazz.getName().equals(nameToCheck)) return true;
		for(Iterator<TypeObject> iter = clazz.getSuperclassIterator(); iter.hasNext();) {
			TypeObject supertype = iter.next();
			if(supertype != null){
				if(supertype.getClassType().equals(nameToCheck)) return true;
			}
		}
		for(Iterator<TypeObject> iter = clazz.getInterfaceIterator(); iter.hasNext();) {
			TypeObject supertype = iter.next();
			if(supertype != null){
				if(supertype.getClassType().equals(nameToCheck)) return true;
			}
		}
		return false;
	}
	
	private int getNumOfSFI() {
		int numOfSFI = 0;
		ListIterator<MethodObject> methodIt = sourceClass.getMethodIterator();
		while(methodIt.hasNext()) {
			MethodObject m = methodIt.next();
			List<FieldInstructionObject> fios = m.getFieldInstructions();
			for(FieldInstructionObject fio : fios) {
				if(fio.getOwnerClass().equals(targetClass.getName())) {
					//get the field from the target class
					ListIterator<FieldObject> fieldsIt = targetClass.getFieldIterator();
					while(fieldsIt.hasNext()) {
						FieldObject fo = fieldsIt.next();
						if(fo.equals(fio)) {
							fieldsToInline.add(fo);
							fieldInvocations2Replace.add(fio.getSimpleName());
						}
					}
					numOfSFI++;
				}
			}
		}
		ListIterator<ConstructorObject> constructorIt = sourceClass.getConstructorIterator();
		while(constructorIt.hasNext()){
			ConstructorObject co = constructorIt.next();
			List<FieldInstructionObject> fios = co.getFieldInstructions();
			for(FieldInstructionObject fio : fios) {
				if(fio.getOwnerClass().equals(targetClass.getName())) {
					//get the field from the target class
					ListIterator<FieldObject> fieldsIt = targetClass.getFieldIterator();
					while(fieldsIt.hasNext()) {
						FieldObject fo = fieldsIt.next();
						if(fo.equals(fio)) {
							fieldsToInline.add(fo);
							fieldInvocations2Replace.add(fio.getSimpleName());
						}
					}
					numOfSFI++;
				}
			}
		}
		ListIterator<FieldObject> fieldIt = sourceClass.getFieldIterator();
		while(fieldIt.hasNext()) {
			FieldObject f = fieldIt.next();
			VariableDeclarationFragment fr = f.getVariableDeclarationFragment();
			if (fr.getInitializer() != null){
				if(fr.getInitializer() instanceof QualifiedName) {
					QualifiedName node = (QualifiedName) fr.getInitializer();
					String targetFieldName = node.getName().toString();
					IBinding b = node.resolveBinding();
					if(b.getKind() == IBinding.VARIABLE){
						String qualifier = node.getQualifier().toString();
						if(qualifier.contains(".")) qualifier = qualifier.substring(qualifier.lastIndexOf(".")+1);
						if(qualifier.equals(targetClass.getSimpleName())){
							ListIterator<FieldObject> fieldsIt = targetClass.getFieldIterator();
							while(fieldsIt.hasNext()) {
								FieldObject fo = fieldsIt.next();
								if(fo.getName().equals(targetFieldName)) {
									fieldsToInline.add(fo);
									fieldInvocations2Replace.add(node.getName());
								}
							}
							numOfSFI ++;
						}
					}
				}
			}
		}
		return numOfSFI;
	}
	private int getNumOfVD() {
		int numOfVD = 0;
		ListIterator<MethodObject> methodIt = sourceClass.getMethodIterator();
		while(methodIt.hasNext()) {
			MethodObject m = methodIt.next();
			for(LocalVariableDeclarationObject lvd : m.getLocalVariableDeclarations()) {
				if(lvd.getType().getClassType().equals(targetClass.getName())) {
					numOfVD ++;
				}
				
			}
		}
		ListIterator<ConstructorObject> constructorIt = sourceClass.getConstructorIterator();
		while(constructorIt.hasNext()){
			ConstructorObject co = constructorIt.next();
			for(LocalVariableDeclarationObject lvd : co.getLocalVariableDeclarations()) {
				if(lvd.getType().getClassType().equals(targetClass.getName())) {
					numOfVD ++;
				}
			}
		}
		return numOfVD;
	}
	private int getNumOfSMI() {
		int numOfSMI = 0;
		ListIterator<MethodObject> methodIt = sourceClass.getMethodIterator();
		while(methodIt.hasNext()) {
			MethodObject m = methodIt.next();
			for(MethodInvocationObject mio : m.getInvokedStaticMethods()) {
				if(mio.getOriginClassName().equals(targetClass.getName())) {
					numOfSMI ++;
					smiToInline.add(targetClass.getMethod(mio));
					methodInvocations2Replace.add(mio.getMethodInvocation());
				}
				
			}
		}
		ListIterator<ConstructorObject> constructorIt = sourceClass.getConstructorIterator();
		while(constructorIt.hasNext()){
			ConstructorObject co = constructorIt.next();
			for(MethodInvocationObject mio : co.getInvokedStaticMethods()) {
				if(mio.getOriginClassName().equals(targetClass.getName())) {
					numOfSMI ++;
					smiToInline.add(targetClass.getMethod(mio));
					methodInvocations2Replace.add(mio.getMethodInvocation());
				}
			}
		}
		ListIterator<FieldObject> fieldIt = sourceClass.getFieldIterator();
		while(fieldIt.hasNext()) {
			FieldObject f = fieldIt.next();
			VariableDeclarationFragment fr = f.getVariableDeclarationFragment();
			if (fr.getInitializer() != null){
				if(fr.getInitializer() instanceof MethodInvocation) {
					MethodInvocation mi = (MethodInvocation) fr.getInitializer();
					if(mi.resolveTypeBinding().getName().equals(targetClass.getSimpleName())) {
						numOfSMI ++;
						smiToInline.add(getMethod(targetClass,mi.getName().toString()));
						methodInvocations2Replace.add(mi);
					}
				}
			}
		}
		return numOfSMI;
	}
	
	private MethodObject getMethod(ClassObject c, String name) {
		ListIterator<MethodObject> methodIt = c.getMethodIterator();
		while(methodIt.hasNext()) {
			MethodObject m = methodIt.next();
			if(m.getName().equals(name)) return m;
		}
		return null;
	}

	private int getNumOfCI() {
		int numOfCI = 0;
		ListIterator<MethodObject> methodIt = sourceClass.getMethodIterator();
		while(methodIt.hasNext()) {
			MethodObject m = methodIt.next();
			for(CreationObject co : m.getCreations()) {
				if(co.getType().getClassType().equals(targetClass.getName())) numOfCI ++;
			}
		}
		ListIterator<ConstructorObject> constructorIt = sourceClass.getConstructorIterator();
		while(constructorIt.hasNext()){
			ConstructorObject co = constructorIt.next();
			for(CreationObject creation : co.getCreations()) {
				if(creation.getType().getClassType().equals(targetClass.getName())) numOfCI ++;
			}
		}
		ListIterator<FieldObject> fieldIt = sourceClass.getFieldIterator();
		while(fieldIt.hasNext()) {
			FieldObject f = fieldIt.next();
			VariableDeclarationFragment fr = f.getVariableDeclarationFragment();
			if (fr.getInitializer() != null && fr.getInitializer() instanceof ClassInstanceCreation) {
				if (fr.getInitializer().resolveTypeBinding().getJavaElement() != null){
					if (fr.getInitializer().resolveTypeBinding().getName().equals(targetClass.getSimpleName())){
						numOfCI ++;
					}
				}
			}
		}
		return numOfCI;
	}

	private int getNumOfMPT() {
		int numOfMPT = 0;
		ListIterator<MethodObject> methodIt = sourceClass.getMethodIterator();
		while(methodIt.hasNext()) {
			MethodObject m = methodIt.next();
			for(TypeObject paramType : m.getParameterTypeList()) {
				if(paramType.getClassType().equals(targetClass.getName())) numOfMPT ++;
			}
		}
		ListIterator<ConstructorObject> constructorIt = sourceClass.getConstructorIterator();
		while(constructorIt.hasNext()){
			ConstructorObject co = constructorIt.next();
			for(TypeObject paramType : co.getParameterTypeList()) {
				if(paramType.getClassType().equals(targetClass.getName())) numOfMPT ++;
			}
		}
		return numOfMPT;
	}
	
	private int getNumOfMRT() {
		int numOfMRT = 0;
		ListIterator<MethodObject> methodIt = sourceClass.getMethodIterator();
		while(methodIt.hasNext()) {
			MethodObject m = methodIt.next();
			if(m.getReturnType().getClassType().equals(targetClass.getName())) numOfMRT ++;
		}
		return numOfMRT;
	}
	
	private int getNumOfMET() {
		int numOfMET = 0;
		ListIterator<MethodObject> methodIt = sourceClass.getMethodIterator();
		while(methodIt.hasNext()) {
			MethodObject m = methodIt.next();
			ITypeBinding[] exceptions = m.getMethodDeclaration().resolveBinding().getExceptionTypes();
			for(ITypeBinding exceptionType : exceptions) {
				if(exceptionType.getName().equals(targetClass.getSimpleName()))
					numOfMET ++;
			}
		}
		return numOfMET;
	}

	public int getNoOfCICases() {
		return getNumOfCI();
		
	}
	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException,
			OperationCanceledException {
		CompositeChange change = new CompositeChange("Object Inlining");
		change.add(r.createChange(pm));
		return change;
	}
}
