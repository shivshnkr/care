package nz.ac.massey.cs.care.ast;

import gr.uom.java.ast.ClassObject;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

public class ConstructorInvocationVisitor extends ASTVisitor {

	private ClassObject targetClass = null;
	private List<ClassInstanceCreation> constructorInvocationsToReplace = new ArrayList<ClassInstanceCreation>();

	public ConstructorInvocationVisitor(ClassObject targetClass2) {
		this.targetClass = targetClass2;
	}

	public boolean visit(ClassInstanceCreation node) {
		if(node.getType().resolveBinding().getQualifiedName().equals(targetClass.getName())){
			if(!isPartOfReturnStatement(node)){
				this.constructorInvocationsToReplace.add(node);
			}
		}
		return true;
	}
	
	private boolean isPartOfReturnStatement(ClassInstanceCreation e) {
			ASTNode node = e.getParent();
			while (node!=null && !(node instanceof ReturnStatement)) {
				node = node.getParent();
			}
			if(node != null && node instanceof ReturnStatement) return true;
			else return false;
	}

	
	
	public List<ClassInstanceCreation> getConstructorInvocationsToReplace() {
		return constructorInvocationsToReplace;
	}
}
