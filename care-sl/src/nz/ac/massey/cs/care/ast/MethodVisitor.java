package nz.ac.massey.cs.care.ast;

import org.eclipse.jdt.core.dom.*;

import java.util.*;

public class MethodVisitor extends ASTVisitor {
	private List<MethodDeclaration> methods = new ArrayList<MethodDeclaration>();
	
	@Override
	public boolean visit(MethodDeclaration node){
		methods.add(node);
		return super.visit(node);
	}

	public List<MethodDeclaration> getMethods() {
		return methods;
	}
	
	public void process(CompilationUnit sourceTypeDeclaration) {
		sourceTypeDeclaration.accept(this);
	}
}
