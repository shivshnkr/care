package nz.ac.massey.cs.care.ast;

import gr.uom.java.ast.ClassObject;

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.refactoring.structure.ChangeTypeRefactoring;

@SuppressWarnings("restriction")
public class Checker {

	private Set<ITypeBinding> possibleTypes = new HashSet<ITypeBinding>();
	private ClassObject targetClass = null;
	private List<FieldASTInfo> fieldsTobeReplaced = new ArrayList<FieldASTInfo>();
	private List<VariableASTInfo> variablesTobeReplaced = new ArrayList<VariableASTInfo>();
	private List<ReturnASTInfo> returnTypesToBeReplaced = new ArrayList<ReturnASTInfo>();
	private List<ParameterASTInfo> parametersToBeReplaced = new ArrayList<ParameterASTInfo>();
	private boolean isRefactorable = true;
	
	
	public boolean isRefactorable() {
		return isRefactorable;
	}
	public Checker(IProject project, ClassObject targetClass) throws CoreException {
		this.project = project;
		javaProject = JavaCore.create(project);
		this.targetClass = targetClass;
	}
	

	public List<FieldASTInfo> getFieldsTobeReplacedByNull() {
		return fieldsTobeReplaced;
	}


	public List<VariableASTInfo> getVariablesTobeReplacedByNull() {
		return variablesTobeReplaced;
	}

	protected void handleField(FieldDeclaration node, ICompilationUnit unit,
			IResource resource, CompilationUnit cUnit, ClassObject targetClass) {
		if (!node.getType().isPrimitiveType()) {
			if (node.getType().isArrayType()
					&& ((ArrayType) node.getType()).getComponentType()
							.isPrimitiveType())
				return;
			if(!isRefactorable) return;
			String typeName = node.getType().toString();
			if(typeName.contains(".")) typeName = typeName.substring(typeName.lastIndexOf(".")+1);
			if(!typeName.equals(targetClass.getSimpleName())) return;
			int typeStart = node.getType().getStartPosition();
			FieldASTInfo field = new FieldASTInfo(node, node.getStartPosition(), node.getLength());
			this.fieldsTobeReplaced.add(field);
//			VariableDeclarationFragment fragment = (VariableDeclarationFragment)node.fragments().iterator().next();
//			 Expression initializer = fragment.getInitializer();
//			 if(initializer != null && initializer instanceof ClassInstanceCreation) {
//				ClassInstanceCreation creation = (ClassInstanceCreation) initializer;
//				if(creation.getType().resolveBinding().getQualifiedName().equals(targetClass.getName())){
//					FieldASTInfo field = new FieldASTInfo(node, node.getStartPosition(), node.getLength());
//					this.fieldsTobeReplaced.add(field);
//				}
//			 }
			try {
				Collection validTypes = checkType(unit, typeName, typeStart, new NullProgressMonitor());
				
				if(validTypes.size()==0) {
					isRefactorable = false;
//				} else if((node.getModifiers() & Modifier.PUBLIC) != 0){ //this means the node is public
//					isRefactorable = false;
				} else if(validTypes.size()==1) {
					ITypeBinding validType = (ITypeBinding) validTypes.iterator().next();
					possibleTypes.add(validType);
				} else if(validTypes.size() > 1){
					//find the most general one
					possibleTypes.add(getMostGeneralType(validTypes));
				}
				
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
	}
	
	protected void handleMethod(MethodDeclaration node, ICompilationUnit unit,
			IResource resource, CompilationUnit cUnit, ClassObject targetClass) {
		if (!node.isConstructor() && !node.getReturnType2().isPrimitiveType()) {
			if (node.getReturnType2().isArrayType()
					&& ((ArrayType) node.getReturnType2()).getComponentType()
							.isPrimitiveType())
				return;
			if(!isRefactorable) return;
			Type returnTypeNode = node.getReturnType2();
			String returnTypeName = returnTypeNode.toString();
			if(returnTypeName.equals(targetClass.getSimpleName())) {
				int returnTypeStart = returnTypeNode.getStartPosition();
				ReturnASTInfo returnType = new ReturnASTInfo(returnTypeNode, returnTypeNode.getStartPosition(), returnTypeNode.getLength());
				this.returnTypesToBeReplaced.add(returnType);
				try {
					Collection validTypes = checkType(unit, returnTypeName, returnTypeStart, new NullProgressMonitor());
					if(validTypes.size()==0) {
						isRefactorable = false;
//					} else if((node.getModifiers() & Modifier.PUBLIC) != 0){
//						isRefactorable = false;
					} else if(validTypes.size()==1) {
						ITypeBinding validType = (ITypeBinding) validTypes.iterator().next();
						possibleTypes.add(validType);
					} else if(validTypes.size() > 1){
						//find the most general one
						possibleTypes.add(getMostGeneralType(validTypes));
					}
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
			
		}
		List methodParameters = node.parameters();
		Iterator methodParameterIterator = methodParameters.iterator();
		while (methodParameterIterator.hasNext()) {
			SingleVariableDeclaration methodParameter = (SingleVariableDeclaration) methodParameterIterator
					.next();
			if (!methodParameter.getType().isPrimitiveType()) {
				if (methodParameter.getType().isArrayType()
						&& ((ArrayType) methodParameter.getType())
								.getComponentType().isPrimitiveType())
					return;
				String parameterTypeName = methodParameter.getType().toString();
				if (parameterTypeName.equals(targetClass.getSimpleName())) {
					int parameterTypeStart = methodParameter.getType()
							.getStartPosition();
					int length = methodParameter.getType().getLength();
					ParameterASTInfo param = new ParameterASTInfo(methodParameter, parameterTypeStart, length );
					this.parametersToBeReplaced.add(param);
					try {
						Collection validTypes = checkType(unit,
								parameterTypeName, parameterTypeStart,
								new NullProgressMonitor());
						if(validTypes.size()==0) {
							isRefactorable = false;
						} else if (validTypes.size() == 1) {
							ITypeBinding validType = (ITypeBinding) validTypes
									.iterator().next();
							possibleTypes.add(validType);
						} else if(validTypes.size() > 1){
							//find the most general one
							possibleTypes.add(getMostGeneralType(validTypes));
						}
					} catch (CoreException e) {
						e.printStackTrace();
					}
				}
				
			}
		}
	}

	protected void handleVariable(VariableDeclarationStatement node,
			ICompilationUnit unit, IResource resource, CompilationUnit cUnit, ClassObject targetClass) {
		if (!node.getType().isPrimitiveType()) {
			if (node.getType().isArrayType()
					&& ((ArrayType) node.getType()).getComponentType()
							.isPrimitiveType())
				return;
			if(!isRefactorable) return;
			String typeName = node.getType().toString();
			if(typeName.contains(".")) typeName = typeName.substring(typeName.lastIndexOf(".")+1);
			if(!typeName.equals(targetClass.getSimpleName())) return;
			 VariableASTInfo variable = new VariableASTInfo(node, node.getType().getStartPosition(), node.getType().getLength());
			 this.variablesTobeReplaced.add(variable);
//			VariableDeclarationFragment fragment = (VariableDeclarationFragment)node.fragments().iterator().next();
//			 Expression initializer = fragment.getInitializer();
//			 if(initializer != null && initializer instanceof ClassInstanceCreation) {
//				 VariableASTInfo variable = new VariableASTInfo(node, node.getType().getStartPosition(), node.getType().getLength());
//				 this.variablesTobeReplaced.add(variable);
//			 }
			int typeStart = node.getType().getStartPosition();
			try {
				Collection validTypes = checkType(unit, typeName, typeStart, new NullProgressMonitor());
				if(validTypes.size()==0) {
					isRefactorable = false;
//				} else if((node.getModifiers() & Modifier.PUBLIC) != 0){ //precondition. 
//					isRefactorable = false;
				} else if(validTypes.size()==1) {
					ITypeBinding validType = (ITypeBinding) validTypes.iterator().next();
					possibleTypes.add(validType);
				} else if(validTypes.size() > 1){
					//find the most general one
					possibleTypes.add(getMostGeneralType(validTypes));
				}
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
	}

	public boolean isCheckCancelled() {
		return false;
	}

	public IProject getProject(){
		return project;
	}

	public static IJavaProject getJavaProject() {
		return javaProject;
	}
	@SuppressWarnings("rawtypes")
	public Collection checkType(ICompilationUnit unit, String selectedType,
			int selectedTypeStart,
			IProgressMonitor monitor) throws CoreException {
		Collection validTypes = new ArrayList();
//		System.out.println("computing valid super types of " + selectedType);
		ChangeTypeRefactoring refactoring = new ChangeTypeRefactoring(unit,
				selectedTypeStart, selectedType.length(), selectedType);
		if (refactoring.checkInitialConditions(monitor).getSeverity() == Status.OK)
		try{
			validTypes = refactoring.computeValidTypes(monitor);
		} catch (Exception e) {
			return validTypes;
		}
		return validTypes;
	}

	private IProject project = null;
	private static IJavaProject javaProject;


	public Set<ITypeBinding> getPossibleTypes() {
		return possibleTypes;
	}
	private ITypeBinding getMostGeneralType(Collection/*<ITypeBinding>*/ types) {

		// first, find a most general valid type (there may be more than one)
		ITypeBinding type= (ITypeBinding)types.iterator().next();
		for (Iterator it= types.iterator(); it.hasNext(); ){
			ITypeBinding other= (ITypeBinding)it.next();
			if (isSubTypeOf(type, other)){
				type= other;
			}
		}
		return type;
	}
	public boolean isSubTypeOf(ITypeBinding type1, ITypeBinding type2){

		// to ensure that, e.g., Comparable<String> is considered a subtype of raw Comparable
		if (type1.isParameterizedType() && type1.getTypeDeclaration().isEqualTo(type2.getTypeDeclaration())){
			return true;
		}
		Set superTypes= getAllSuperTypes(type1);
		return contains(superTypes, type2);
	}
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Set/*<ITypeBinding>*/ getAllSuperTypes(ITypeBinding type){
		
		ITypeBinding fObject= targetClass.getTypeDeclaration().getAST().resolveWellKnownType("java.lang.Object");
		Set/*<ITypeBinding>*/ result= new HashSet();
		result.add(type);
		if (type.getSuperclass() != null){
			result.addAll(getAllSuperTypes(type.getSuperclass()));
		}
		ITypeBinding[] interfaces= type.getInterfaces();
		for (int i=0; i < interfaces.length; i++){
			result.addAll(getAllSuperTypes(interfaces[i]));
		}
		if ((type != fObject) && !contains(result, fObject)){
			result.add(fObject);
		}
		return result;
	}
	private static boolean contains(Collection/*<ITypeBinding>*/ c, ITypeBinding binding){
		for (Iterator/*<ITypeBinding>*/ it=c.iterator(); it.hasNext(); ){
			ITypeBinding b = (ITypeBinding)it.next();
			if (Bindings.equals(b, binding)) return true;
		}
		return false;
	}


	public List<FieldASTInfo> getFieldsTobeReplaced() {
		return fieldsTobeReplaced;
	}


	public List<VariableASTInfo> getVariablesTobeReplaced() {
		return variablesTobeReplaced;
	}


	public List<ReturnASTInfo> getReturnTypesToBeReplaced() {
		return returnTypesToBeReplaced;
	}


	public List<ParameterASTInfo> getParametersToBeReplaced() {
		return parametersToBeReplaced;
	}
	public void setRefactorable(boolean b) {
		this.isRefactorable = b;
		
	}
}

