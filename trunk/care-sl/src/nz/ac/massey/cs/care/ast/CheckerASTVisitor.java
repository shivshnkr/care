package nz.ac.massey.cs.care.ast;

import gr.uom.java.ast.ClassObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.internal.corext.dom.Bindings;

@SuppressWarnings("restriction")
public class CheckerASTVisitor extends ASTVisitor {

	private CompilationUnit targetCUnit = null;
	private ClassObject targetClass = null;
	private List<ClassInstanceCreation> constructorInvocationsToReplace = new ArrayList<ClassInstanceCreation>();
	private List<MethodInvocation> smiToReplace = new ArrayList<MethodInvocation>();
	private List<QualifiedName> sfiToReplace = new ArrayList<QualifiedName>();
	
	
	public CheckerASTVisitor(Checker checker, ClassObject sourceClass, ClassObject targetClass, boolean action) {
		numberOfDeclarationElements = 0;
		numberOfOtherUses = 0;
		this.checker = checker;
		this.sourceICUnit = (ICompilationUnit) sourceClass.getCompilationUnit().getJavaElement();
		this.sourceCUnit = sourceClass.getCompilationUnit();
		try {
			this.resource = this.sourceICUnit.getUnderlyingResource();
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		this.targetCUnit  = targetClass.getCompilationUnit();
		this.action = action;
		this.targetClass = targetClass;
		if(targetClass.getTypeDeclaration()==null) {
			System.out.println(targetClass.getName());
			setRefactorable(false);
		}
		if(sourceClass.getTypeDeclaration()==null) {
			System.out.println(sourceClass.getName());
			setRefactorable(false);
		}
	}
	private void setRefactorable(boolean b) {
		checker.setRefactorable(b);
		
	}
	public List<ClassInstanceCreation> getConstructorInvocationsToReplace() {
		return constructorInvocationsToReplace;
	}
	
	public boolean isRefactorable() {
		return checker.isRefactorable();
	}
	
	public boolean visit(FieldDeclaration node) {
		String typeName = node.getType().toString();
		if (typeName.contains("."))
			typeName = typeName.substring(typeName.lastIndexOf(".") + 1);
		if (typeName.equals(targetClass.getSimpleName())) {
			numberOfDeclarationElements++;
			if (action)
				checker.handleField(node, sourceICUnit, resource, sourceCUnit, targetClass);
		}
		return true;
	}

	public boolean visit(MethodDeclaration node) {
		if(node.isConstructor()) return true;
		String returnTypeName = node.getReturnType2().toString();
		if(returnTypeName.equals(targetClass.getSimpleName())) 
			numberOfDeclarationElements++;
		List methodParameters = node.parameters();
		Iterator methodParameterIterator = methodParameters.iterator();
		while (methodParameterIterator.hasNext()) {
			SingleVariableDeclaration methodParameter = (SingleVariableDeclaration) methodParameterIterator
					.next();
			String parameterTypeName = methodParameter.getType().toString();
			if (parameterTypeName.equals(targetClass.getSimpleName())) numberOfDeclarationElements++;
		}
		if (action)
			checker.handleMethod(node, sourceICUnit, resource, sourceCUnit, targetClass);
		return true;
	}
	
	public boolean visit(CastExpression node) {
		String typeName = node.getType().toString();
		if (typeName.contains("."))
			typeName = typeName.substring(typeName.lastIndexOf(".") + 1);
		if (typeName.equals(targetClass.getSimpleName())) {
			setRefactorable(false);
		}
		return true;
	}
	
	public boolean visit(VariableDeclarationStatement node) {
		String typeName = node.getType().toString();
		if (typeName.contains("."))
			typeName = typeName.substring(typeName.lastIndexOf(".") + 1);
		if (typeName.equals(targetClass.getSimpleName())) {
			numberOfDeclarationElements++;
			if (action)
				checker.handleVariable(node, sourceICUnit, resource, sourceCUnit, targetClass);
		}
		return true;
	}
	
	@Override
	public boolean visit(QualifiedName node){
		IBinding b = node.resolveBinding();
		if(b.getKind() == IBinding.VARIABLE){
			String qualifier = node.getQualifier().toString();
			if(qualifier.contains(".")) qualifier = qualifier.substring(qualifier.lastIndexOf(".")+1);
			if(qualifier.equals(targetClass.getSimpleName())){
				numberOfOtherUses ++;
				sfiToReplace.add(node);
			}
		}
		return super.visit(node);
	}
	
	@Override
	public boolean visit(ClassInstanceCreation node){
		String typeName = node.getType().toString();
		if (typeName.contains("."))
			typeName = typeName.substring(typeName.lastIndexOf(".") + 1);
		if (typeName.equals(targetClass.getSimpleName())) {
			numberOfOtherUses ++;
			this.constructorInvocationsToReplace.add(node);
		}
		return super.visit(node);
	}

	public int getNumberOfDeclarationElements() {
		return numberOfDeclarationElements;
	}
	
	@Override
	public boolean visit(MethodInvocation node){
		if(node.resolveMethodBinding()!=null) {
			IMethodBinding binding = node.resolveMethodBinding();
			int modifiers = binding.getModifiers() & Modifier.STATIC;
			if(modifiers != 0) {
				if(node.getExpression() == null) return true;
				String typeName = node.getExpression().toString();
				if(typeName.contains(".")) typeName = typeName.substring(typeName.lastIndexOf(".")+1);
				if(typeName.equals(targetClass.getSimpleName())){
					numberOfOtherUses ++;
					smiToReplace.add(node);
				}
			}
		}
		return super.visit(node);
	}
	
	public ITypeBinding getSupertypeToUse() {
		Set<ITypeBinding> possibleTypes = checker.getPossibleTypes();
		IType[] allTypes = getSupertypes();
		for(IType type : allTypes) {
			for(ITypeBinding possibleType : possibleTypes){
				if(type.getElementName().equals(possibleType.getName())) return possibleType;
			}
		}
		return null;
	}
	public IType[] getSupertypes(){
		IType[] supertypes=null;
		IType targetType = ((ICompilationUnit)targetClass.getCompilationUnit().getJavaElement()).getType(targetClass.getSimpleName());
		if(targetType==null) return null; //anony types
		try {
			ITypeHierarchy hierarchy = targetType.newTypeHierarchy(null);
			supertypes = hierarchy.getAllSupertypes(targetType);
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		//returns in bottom-up order
		return supertypes;
	}
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Set/*<ITypeBinding>*/ getAllSuperTypes(ITypeBinding type){
		ITypeBinding fObject= targetCUnit.getAST().resolveWellKnownType("java.lang.Object");
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
	@SuppressWarnings({ "rawtypes" })
	private static boolean contains(Collection/*<ITypeBinding>*/ c, ITypeBinding binding){
		for (Iterator/*<ITypeBinding>*/ it=c.iterator(); it.hasNext(); ){
			ITypeBinding b = (ITypeBinding)it.next();
			if (Bindings.equals(b, binding)) return true;
		}
		return false;
	}
	public List<FieldASTInfo> getFieldsTobeReplaced() {
		return checker.getFieldsTobeReplacedByNull();
	}

	public List<VariableASTInfo> getVariablesTobeReplaced() {
		return checker.getVariablesTobeReplacedByNull();
	}
	
	public List<ParameterASTInfo> getParametersTobeReplaced() {
		return checker.getParametersToBeReplaced();
	}
	
	public List<ReturnASTInfo> getReturnTypesTobeReplaced() {
		return checker.getReturnTypesToBeReplaced();
	}
	
	public int getNumberOfOtherUses() {
		return numberOfOtherUses;
	}

	private Checker checker;
	private ICompilationUnit sourceICUnit;
	private CompilationUnit sourceCUnit;
	private IResource resource;
	private boolean action;
	private int numberOfDeclarationElements;
	private int numberOfOtherUses;


	public List<MethodInvocation> getSmiToReplace() {
		return smiToReplace;
	}
	public List<QualifiedName> getSfiToReplace() {
		return sfiToReplace;
	}
}
