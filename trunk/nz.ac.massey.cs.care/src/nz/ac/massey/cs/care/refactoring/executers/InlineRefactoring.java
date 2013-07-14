package nz.ac.massey.cs.care.refactoring.executers;

import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.ClassObject;
import gr.uom.java.ast.ConstructorObject;
import gr.uom.java.ast.CreationObject;
import gr.uom.java.ast.FieldInstructionObject;
import gr.uom.java.ast.FieldObject;
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

import nz.ac.massey.cs.care.Precondition;
import nz.ac.massey.cs.care.refactoring.movehelper.Candidate;
import nz.ac.massey.cs.care.refactoring.movehelper.ConstraintsResult;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveStaticMembersProcessor;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.MoveRefactoring;

@SuppressWarnings("restriction")
public class InlineRefactoring extends CareRefactoring {
	private ClassObject sourceClass = null;
	private ClassObject targetClass = null;
	private Set<MethodObject> staticMethodsToInline = new LinkedHashSet<MethodObject>();
	private Set<FieldObject> fieldsToInline = new LinkedHashSet<FieldObject>();
	private Set<MethodObject> tempSMIToInline = new LinkedHashSet<MethodObject>();
	private ConstraintsResult result = candidate.getConstraintsResult();
	
	private MoveRefactoring move;
	public InlineRefactoring(Candidate candidate) {
		super(candidate, new Precondition[]{});
		sourceClass = candidate.getSourceClassObject();
		targetClass = candidate.getTargetClassObject();
	}

	@Override
	public String getName() {
		return "Inlining";
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
		try {
			int sum = getNumOfSFI() + getNumOfSMI();;
			if(sum == 0) {
				status.addError("No target type dependency found");
				return;
			}
			//we reach here because we found either SMI or SFI type dependency.
			//in the case of SMI we need to recursively check internal dependencies for SMI in the source class.
			//i.e. other static methods and fields in the target class that have been used by the original SMI.
			computeAllStaticMembersToInline(status);
			if(status.hasError()) {
				if(status.getEntries()[0].getMessage().equals("Supertype Accessed")) {
					result.setClassBoundryViolation(false);
				} 
				return;
			}
			boolean hasSelfInstantiation = false; //this is the case when any of the target class's static method
			//to be in-lined has a new instance creation with the target type.
			boolean isSupertypeAccessed = false; //when members of supertype are accessed from the target class.
			for(MethodObject m : staticMethodsToInline) {
				for(CreationObject co : m.getCreations()) {
					if(co.getType().getClassType().equals(targetClass.getSimpleName())) hasSelfInstantiation = true;
				}
				for (MethodInvocationObject mio : m.getInvokedStaticMethods()) {
					String methodOwnerClass = mio.getOriginClassName();
					if (containsInHierarchy(methodOwnerClass, targetClass)) {
						//this means the method is part of super class
						isSupertypeAccessed = true;
					}
				}
				List<FieldInstructionObject> fields = m.getFieldInstructions();
				for(FieldInstructionObject field : fields) {
					String fieldOwnerClass = field.getOwnerClass();
					//this is to check if any field accessed in this method belongs to super class.
					if(containsInHierarchy(fieldOwnerClass, targetClass)) isSupertypeAccessed = true;
				}
				
			}
			
			for(FieldObject fo : fieldsToInline){
				VariableDeclarationFragment vdf = fo.getVariableDeclarationFragment();
				
				if(vdf != null) {
					if(vdf.getInitializer() != null && vdf.getInitializer() instanceof ClassInstanceCreation){
						if (vdf.getInitializer().resolveTypeBinding().getJavaElement() != null){
							if (vdf.getInitializer().resolveTypeBinding().getName().equals(targetClass.getSimpleName())){
								hasSelfInstantiation = true; //means that field is instantiated with target type.
							}
						}
					}
					if(vdf.getInitializer() != null && vdf.getInitializer() instanceof MethodInvocation){
						//Do Nothing
					}
				}
				String fieldOwnerClass = fo.getClassName();
				if(!fieldOwnerClass.equals(targetClass.getName())) hasSelfInstantiation = true;
			}
			if(hasSelfInstantiation) {
				status.addError("Self Instantiation");
				result.setSelfInstanceCreation(false);
			} else {
				result.setSelfInstanceCreation(true);
			}
			if(isSupertypeAccessed) {
				status.addError("Supertype Accessed");
				result.setClassBoundryViolation(false);
			} else {
				result.setClassBoundryViolation(true);
			}
			if(hasSelfInstantiation || isSupertypeAccessed) return;
            int size = staticMethodsToInline.size() + fieldsToInline.size();
            IMember[] staticMembers = new IMember[size];
			int count = 0;
            for(MethodObject m : staticMethodsToInline) {
				IMethod resolvedMethod = (IMethod)m.getMethodDeclaration().resolveBinding().getJavaElement();
				staticMembers[count++] = resolvedMethod;
			}
			for(FieldObject fo : fieldsToInline) {
				IField field = (IField) fo.getVariableDeclaration().resolveBinding().getJavaElement();
				staticMembers[count++] = field;
			}
			CodeGenerationSettings settings = JavaPreferencesSettings.getCodeGenerationSettings(ASTReader.getExaminedProject());
			MoveStaticMembersProcessor smp = new MoveStaticMembersProcessor(staticMembers, settings);
			smp.setDelegateUpdating(true);
			smp.setDeprecateDelegates(false);
			smp.setDestinationTypeFullyQualifiedName(sourceClass.getName());
			move =  new MoveRefactoring(smp);
			status.merge(move.checkAllConditions(new NullProgressMonitor()));
			
			
		}catch (JavaModelException e) {
			e.printStackTrace();
		} catch (CoreException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException,
			OperationCanceledException {
		try {
			pm.beginTask("Creating change...", 1);
			Change undo = new CompositeChange("Undo Inline");
			Change change = move.createChange(pm);
			undo = change.perform(pm);
			undoList.add(undo);
			return null;
		} catch(Exception e) {
			IStatus status1 = new Status(IStatus.ERROR, ResourcesPlugin.PI_RESOURCES, "Inline Change Creation Error");
			throw new CoreException(status1);
		} finally {
			pm.done();
		}
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
	private int getNumOfSMI() {
		int numOfSMI = 0;
		ListIterator<MethodObject> methodIt = sourceClass.getMethodIterator();
		while(methodIt.hasNext()) {
			MethodObject m = methodIt.next();
			for(MethodInvocationObject mio : m.getInvokedStaticMethods()) {
				if(mio.getOriginClassName().equals(targetClass.getName())) {
					numOfSMI ++;
					tempSMIToInline.add(targetClass.getMethod(mio));
				}
				
			}
		}
		ListIterator<ConstructorObject> constructorIt = sourceClass.getConstructorIterator();
		while(constructorIt.hasNext()){
			ConstructorObject co = constructorIt.next();
			for(MethodInvocationObject mio : co.getInvokedStaticMethods()) {
				if(mio.getOriginClassName().equals(targetClass.getName())) {
					numOfSMI ++;
					tempSMIToInline.add(targetClass.getMethod(mio));
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
						tempSMIToInline.add(getMethod(targetClass,mi.getName().toString()));
					}
				}
			}
		}
		return numOfSMI;
	}
	private void computeAllStaticMembersToInline(RefactoringStatus status) {
		Queue<MethodObject> q = new LinkedBlockingQueue<MethodObject>();
		for(MethodObject m : tempSMIToInline) {
			try{
				q.add(m);
				staticMethodsToInline.add(m);
			} catch(Exception e) {
				continue;
			}
		}
		processQueue(q);
		
		for(FieldObject fo : fieldsToInline) {
			VariableDeclarationFragment vdf = fo.getVariableDeclarationFragment();
			if(vdf != null) {
				if(vdf.getInitializer()!= null && vdf.getInitializer() instanceof MethodInvocation) {
					MethodInvocation mi = (MethodInvocation) vdf.getInitializer();
					String methodName = mi.getName().toString();
					MethodObject mo  = getMethod(targetClass, methodName);
					if(mo != null) {
						q.add(mo);
						staticMethodsToInline.add(mo);
					} else {
						// initializer is not from target class. Now check if it is
						// from superclass. 
						String initTypeName = mi.resolveTypeBinding().getBinaryName();
						if(containsInHierarchy(initTypeName, targetClass)) {
							status.addError("Supertype Accessed");
						}
					}
				} 
			}
		}
		//process it again.
		processQueue(q);
	}
	
	private void processQueue(Queue<MethodObject> q) {
		while (!q.isEmpty()) {
			MethodObject m = q.poll();
			addFieldsToBeInlined(m);
			addMethodsToBeInlined(m, q);
		}
		
	}

	private void addFieldsToBeInlined(MethodObject m) {
		List<FieldInstructionObject> fieldInstructions = m.getFieldInstructions();
		for(FieldInstructionObject fieldInstruction : fieldInstructions){
			if(fieldInstruction.getOwnerClass().equals(targetClass.getName())) {
				FieldObject fo = getField(fieldInstruction);
				if(fo != null){
					fieldsToInline.add(fo);
				}
			}
		}
	}
	private void addMethodsToBeInlined(MethodObject m, Queue<MethodObject> q) {
		for (MethodInvocationObject mio : m.getInvokedStaticMethods()) {
			// here we take care of two things: recursive methods and
			// cycles between methods.
			//mio should be from the target class. 
			if(m.equals(mio)) continue;
			if(isAlreadyAdded(mio)) continue; //to check cycles between methods
			String methodOwnerClass = mio.getOriginClassName();
			ClassObject ownerClassObject = null;
			try{
				ownerClassObject = ASTReader.getSystemObject().getClassObject(methodOwnerClass);
			} catch(Exception e) {
				continue;
			}
			if(ownerClassObject != null) {
				//invoked method should only be part of this class.
				if(targetClass.getName().equals(methodOwnerClass)) {
					q.add(ownerClassObject.getMethod(mio));
					staticMethodsToInline.add(ownerClassObject.getMethod(mio));
				}
			}
		}
	}
	private boolean isAlreadyAdded(MethodInvocationObject mio) {
		for(MethodObject mo : staticMethodsToInline) {
			if(mo.equals(mio)) return true;
		}
		return false;
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
//		does not include the class itself.
		for(Iterator<TypeObject> iter = clazz.getSuperclassIterator(); iter.hasNext();) {
			TypeObject supertype = iter.next();
			if(supertype != null){
				String supername = supertype.toString();
				if(supername.equals(nameToCheck)) return true;
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
	private MethodObject getMethod(ClassObject c, String name) {
		ListIterator<MethodObject> methodIt = c.getMethodIterator();
		while(methodIt.hasNext()) {
			MethodObject m = methodIt.next();
			if(m.getName().equals(name)) return m;
		}
		return null;
	}

}
