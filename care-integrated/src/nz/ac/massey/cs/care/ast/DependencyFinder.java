package nz.ac.massey.cs.care.ast;

import java.util.Iterator;
import java.util.List;

import gr.uom.java.ast.ClassObject;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

public class DependencyFinder extends ASTVisitor{
	private ClassObject sourceClass;
	private ClassObject targetClass;
	private int declarationElements = 0; //VD/MPT/MRT/MET
	private int constructorInvocations = 0;
	private int instanceOfs = 0;
	private int castExpressions = 0;
	private int staticMembers = 0;
	
	public String compute() {
		sourceClass.getCompilationUnit().accept(this);
		if(constructorInvocations > 0 && declarationElements == 0 && 
				staticMembers == 0) {
			return "CI";
		} else if(constructorInvocations == 0 && declarationElements > 0 && 
				staticMembers == 0) {
			return "VD/MPT/MRT/MET";
		} else if(constructorInvocations == 0 && declarationElements == 0 && 
				staticMembers > 0) {
			return "SMI";
		} else if(constructorInvocations > 0 && declarationElements > 0 && 
				staticMembers == 0) {
			return "CI+VD/MPT/MRT/MET";
		} else if(constructorInvocations > 0 && declarationElements == 0 && 
				staticMembers > 0) {
			return "CI+SMI";
		} else if(constructorInvocations == 0 && declarationElements > 0 && 
				staticMembers > 0) {
			return "VD/MPT/MRT/MET+SMI";
		} else if(constructorInvocations > 0 && declarationElements > 0 && 
				staticMembers > 0) {
			return "CI+VD/MPT/MRT/MET+SMI";
		} else {
			return "";
		}
		
	}
	public DependencyFinder(ClassObject sourceClass, ClassObject targetClass) {
		super();
		this.sourceClass = sourceClass;
		this.targetClass = targetClass;
	}

	@Override
	public boolean visit(FieldDeclaration node) {
		String typeName = node.getType().toString();
		if (typeName.contains("."))
			typeName = typeName.substring(typeName.lastIndexOf(".") + 1);
		if (typeName.equals(targetClass.getSimpleName())) {
			declarationElements ++;
		}
		return true;
	}
	
	@Override
	public boolean visit(MethodDeclaration node) {
		if(node.isConstructor()) return true;
		String returnTypeName = node.getReturnType2().toString();
		if(returnTypeName.equals(targetClass.getSimpleName())) 
			declarationElements++;
		List methodParameters = node.parameters();
		Iterator methodParameterIterator = methodParameters.iterator();
		while (methodParameterIterator.hasNext()) {
			SingleVariableDeclaration methodParameter = (SingleVariableDeclaration) methodParameterIterator
					.next();
			String parameterTypeName = methodParameter.getType().toString();
			if (parameterTypeName.equals(targetClass.getSimpleName())) declarationElements++;
		}
		return true;
	}
	
	@Override
	public boolean visit(VariableDeclarationStatement node) {
		String typeName = node.getType().toString();
		if (typeName.contains("."))
			typeName = typeName.substring(typeName.lastIndexOf(".") + 1);
		if (typeName.equals(targetClass.getSimpleName())) {
			declarationElements++;
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
				staticMembers ++;
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
			constructorInvocations ++;
		}
		return super.visit(node);
	}
	
	@Override
	public boolean visit(CastExpression node) {
		String typeName = node.getType().toString();
		if (typeName.contains("."))
			typeName = typeName.substring(typeName.lastIndexOf(".") + 1);
		if (typeName.equals(targetClass.getSimpleName())) {
			castExpressions ++;
		}
		return true;
	}
	
	@Override
	public boolean visit(InstanceofExpression node) {
		String typeName = node.getRightOperand().toString();
		if (typeName.contains("."))
			typeName = typeName.substring(typeName.lastIndexOf(".") + 1);
		if (typeName.equals(targetClass.getSimpleName())) {
			instanceOfs ++;
		}
		return true;
	}
	
	
}
