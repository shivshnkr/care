package nz.ac.massey.cs.care.ast;

import gr.uom.java.ast.ClassObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
	private List<MethodInvocation> smiToInline = new ArrayList<MethodInvocation>();
	private List<MethodInvocation> miToInline = new ArrayList<MethodInvocation>();
	private List<QualifiedName> sfiToInline = new ArrayList<QualifiedName>();
	private List<FieldDeclaration> fieldsToRemove = new ArrayList<FieldDeclaration>();
	private List<VariableDeclarationStatement> variablesToRemove = new ArrayList<VariableDeclarationStatement>();
	private Map<IVariableBinding, VariableBindingManager> localVariableManagers = new HashMap<IVariableBinding, VariableBindingManager>();
	private Map<IVariableBinding, VariableBindingManager> fieldVariableManagers = new HashMap<IVariableBinding, VariableBindingManager>();

	private int numOfMPT;
	private Checker checker;
	private ICompilationUnit sourceICUnit;
	private CompilationUnit sourceCUnit;
	private IResource resource;
	private boolean action;
	private int numberOfDeclarationElements;
	private int numOfCI;
	private int numOfMRT;
	private int numOfMET;
	private int numOfSMI;
	private int numOfSFI;
	
	
	public CheckerASTVisitor(Checker checker, ClassObject sourceClass, ClassObject targetClass, boolean action) {
		numberOfDeclarationElements = 0;
		numOfMPT = 0; //number of Method Parameter Type Dependencies + Catch clause declarations (SingleVariableDeclaration)
		numOfMRT = 0; //number of Method Return Type Dependencies
		numOfMET = 0; //number of Method Exception Type Dependencies
		numOfSMI = 0; //number of Static Method Invocation dependencies
		numOfSFI = 0; //number of Static Field Invocation dependencies
		numOfCI = 0;
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
			VariableDeclarationFragment fragment = (VariableDeclarationFragment) node.fragments().iterator().next();
			IVariableBinding binding = fragment.resolveBinding();
			VariableBindingManager manager = new VariableBindingManager(fragment); // create the manager fro the fragment
			fieldVariableManagers.put(binding, manager);
			numberOfDeclarationElements++;
			fieldsToRemove.add(node);
			if (action)
				checker.handleField(node, sourceICUnit, resource, sourceCUnit, targetClass);
		}
		return true;
	}

	public boolean visit(MethodDeclaration node) {
		if(node.isConstructor()) return true;
		String returnTypeName = node.getReturnType2().toString();
		if(returnTypeName.equals(targetClass.getSimpleName())) 
			numOfMRT ++;
		for(ITypeBinding exceptionType : node.resolveBinding().getExceptionTypes()) {
			if(exceptionType.getName().equals(targetClass.getSimpleName()))
				numOfMET ++;
		}
		if (action)
			checker.handleMethod(node, sourceICUnit, resource, sourceCUnit, targetClass);
		return true;
	}
	
	public boolean visit(SingleVariableDeclaration node){ 
		String parameterTypeName = node.getType().toString();
		if (parameterTypeName.equals(targetClass.getSimpleName())) {
			numOfMPT++;
		}
		
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
			VariableDeclarationFragment fragment = (VariableDeclarationFragment) node.fragments().iterator().next();
			IVariableBinding binding = fragment.resolveBinding();
			VariableBindingManager manager = new VariableBindingManager(fragment); // create the manager fro the fragment
			localVariableManagers.put(binding, manager);
			numberOfDeclarationElements++;
			variablesToRemove.add(node);
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
				numOfSFI ++;
				sfiToInline.add(node);
			}
		}
		return super.visit(node);
	}
	/**
	 * Visits {@link SimpleName} AST nodes. Resolves the binding of the simple
	 * name and looks for it in the {@link #localVariableManagers} map. If the
	 * binding is found, this is a reference to a local variable. The variable
	 * binding manager of this local variable then has to be informed about that
	 * reference.
	 *
	 * @param node
	 *            the node to visit
	 */
	public boolean visit(SimpleName node) {
		IBinding binding = node.resolveBinding();
		if (localVariableManagers.containsKey(binding)) {
			VariableBindingManager manager = localVariableManagers.get(binding);
			manager.variableRefereneced(node);
		} else if(fieldVariableManagers.containsKey(binding)) {
			VariableBindingManager manager = fieldVariableManagers.get(binding);
			manager.variableRefereneced(node);
		}
		return true;
	}
	/**
	 * Visits {@link Assignment} AST nodes (e.g. {@code x = 7 + 8} ). Resolves
	 * the binding of the left hand side (in the example: {@code x}). If the
	 * binding is found in the {@link #localVariableManagers} map, we have an
	 * assignment of a local variable. The variable binding manager of this
	 * local variable then has to be informed about this assignment.
	 *
	 * @param node
	 *            the node to visit
	 */
	public boolean visit(Assignment node) {
		if (node.getLeftHandSide() instanceof SimpleName) {
			IBinding binding = ((SimpleName) node.getLeftHandSide()).resolveBinding();
			if (localVariableManagers.containsKey(binding)) {
				// contains key -> it is an assignment ot a local variable
				VariableBindingManager manager = localVariableManagers.get(binding);
				manager.variableInitialized(node);
			}
			else if(fieldVariableManagers.containsKey(binding)){
				VariableBindingManager manager = fieldVariableManagers.get(binding);
				manager.variableInitialized(node);
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
			numOfCI ++;
			this.constructorInvocationsToReplace.add(node);
		}
		return super.visit(node);
	}

	public int getNumOfMPT () {
		return numOfMPT;
	}
	public int getNumOfMRT () {
		return numOfMRT;
	}
	public int getNumOfMET () {
		return numOfMET;
	}
	public int getNumOfCI() {
		return numOfCI;
	}
	public int getNumOfSFI() {
		return numOfSFI;
	}
	public int getNumOfSMI() {
		return numOfSMI;
	}
	
	@Override
	public boolean visit(MethodInvocation node){
		
		if(node!=null && node.getExpression()!=null) {
//			if(Modifier.isStatic(binding.getModifiers())) {
//				if(node.getExpression() == null) return true;
//				String typeName = node.getExpression().toString();
//				if(typeName.contains(".")) typeName = typeName.substring(typeName.lastIndexOf(".")+1);
//				if(typeName.equals(targetClass.getSimpleName())){
//					numOfSMI ++;
//					smiToInline.add(node);
//				}
//			} else {
				if (node.getExpression().resolveTypeBinding().getName().equals(targetClass.getSimpleName())) {
					numOfSMI ++;
					miToInline.add(node);
				}
//			}
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
	

	public List<MethodInvocation> getMiToInline() {
		return miToInline;
	}
	public List<MethodInvocation> getSmiToInline() {
		return smiToInline;
	}
	public List<QualifiedName> getSfiToInline() {
		return sfiToInline;
	}
	public List<FieldDeclaration> getFieldsToRemove() {
		return fieldsToRemove;
	}
	public List<VariableDeclarationStatement> getVariableDeclarationsToRemove() {
		return variablesToRemove;
	}
	public List<VariableDeclarationFragment> getVariableDeclarationFragmentsToRemove() {
		return new ArrayList<VariableDeclarationFragment>();
	}
	public Collection<VariableBindingManager> getVariableManagers() {
		return localVariableManagers.values();
	}
	public Collection<VariableBindingManager> getFieldManagers() {
		return fieldVariableManagers.values();
	}
}
