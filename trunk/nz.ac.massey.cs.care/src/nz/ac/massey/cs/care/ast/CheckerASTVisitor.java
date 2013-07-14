package nz.ac.massey.cs.care.ast;

import gr.uom.java.ast.ClassObject;
import gr.uom.java.ast.ConstructorObject;
import gr.uom.java.ast.FieldObject;
import gr.uom.java.ast.MethodObject;
import gr.uom.java.ast.util.StatementExtractor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import nz.ac.massey.cs.care.refactoring.executers.Helper;

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
	private Checker checker;
	private ICompilationUnit sourceICUnit;
	private CompilationUnit sourceCUnit;
	private IResource resource;
	private ClassObject sourceClass;

	/**
	 * This class is used by the Generalize Refactoring to compute the relevant dependencies. 
	 * @param checker
	 * @param sourceClass
	 * @param targetClass
	 * @param action
	 */
	public CheckerASTVisitor(Checker checker, ClassObject sourceClass, ClassObject targetClass, boolean action) {
		this.checker = checker;
		if(targetClass.getTypeDeclaration()==null) {
			System.out.println(targetClass.getName());
			setRefactorable(false);
		}
		if(sourceClass.getTypeDeclaration()==null) {
			System.out.println(sourceClass.getName());
			setRefactorable(false);
		}
		if(sourceClass.getCompilationUnit() == null) {
			System.out.println(sourceClass.getName());
			setRefactorable(false);
		}
		this.sourceClass = sourceClass;
		this.sourceICUnit = (ICompilationUnit) sourceClass.getCompilationUnit().getJavaElement();
		this.sourceCUnit = sourceClass.getCompilationUnit();
		try {
			this.resource = this.sourceICUnit.getUnderlyingResource();
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		this.targetCUnit  = targetClass.getCompilationUnit();
		this.targetClass = targetClass;
		
	}
	private void setRefactorable(boolean b) {
		checker.setRefactorable(b);
		
	}
	
	public boolean isRefactorable() {
		return checker.isRefactorable();
	}
	public void visitme() {
		visitFields();
		visitMethods();
		visitConstructors();
		if(Helper.supertypeToUse == null) {
			ITypeBinding supToUse = getSupertypeToUse();
			if(supToUse == null) setRefactorable(false);
			else Helper.supertypeToUse = supToUse.getQualifiedName();
		}
	}
	private void visitFields() {
		Iterator<FieldObject> fieldIter = sourceClass.getFieldIterator();
		while(fieldIter.hasNext()) {
			FieldObject fo = fieldIter.next();
			VariableDeclarationFragment fragment = fo.getVariableDeclarationFragment();
			if(fragment == null) continue;
			FieldDeclaration node = (FieldDeclaration) fragment.getParent();
			checker.handleField(node, sourceICUnit, sourceCUnit, targetClass);
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
		MethodDeclaration node = mo.getMethodDeclaration();
		checker.handleMethod(node, sourceICUnit, resource, sourceCUnit, targetClass);
		StatementExtractor statementExtractor = new StatementExtractor();
		List<Statement> sourceVariableDeclarationStatements = statementExtractor.getVariableDeclarationStatements(node.getBody());
		for(Statement statement : sourceVariableDeclarationStatements) {
			VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement)statement;
			checker.handleVariable(variableDeclarationStatement, sourceICUnit, resource, sourceCUnit, targetClass);
		}
	}
	private void visitConstructors() {
		Iterator<ConstructorObject> ico = sourceClass.getConstructorIterator();
		while(ico.hasNext()) {
			ConstructorObject co = ico.next();
			visitMethodObject(new MethodObject(co));
		}
	}
	
	
	public int getNumberOfDeclarationElements() {
		int num = checker.getFieldsTobeReplaced().size() + checker.getParametersToBeReplaced().size() + 
		checker.getVariablesTobeReplaced().size() + checker.getReturnTypesToBeReplaced().size();
		return num;
	}
	
	public ITypeBinding getSupertypeToUse() {
		try {
			Set<ITypeBinding> possibleTypes = checker.getPossibleTypes();
			IType[] allTypes = getSupertypes();
			for(IType type : allTypes) {
				for(ITypeBinding possibleType : possibleTypes){
					if(type.getElementName().equals(possibleType.getName())) return possibleType;
				}
			}
		} catch(Exception e) {
			return null;
		}
		return null;
	}
	public IType[] getSupertypes(){
		IType[] supertypes= new IType[]{};
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
	
}
