package nz.ac.massey.cs.care.ast;

import java.util.*;

import gr.uom.java.ast.*;

import org.eclipse.jdt.core.dom.*;

public class TargetClassASTVisitor extends ASTVisitor {
	private ClassObject targetClass;
	private MethodInvocation smi;
	private boolean hasError = false;
	private Set<MethodDeclaration> methodsToInline = new HashSet<MethodDeclaration>();
	
	public TargetClassASTVisitor(ClassObject targetClass, MethodInvocation smi) {
		super();
		this.targetClass = targetClass;
		this.smi = smi;
	}

	@Override
	public boolean visit(MethodInvocation node) {
		if(node.resolveMethodBinding().getDeclaringClass().getName().equals(targetClass.getSimpleName())) {
			//check if this method is recursive
			if(node.resolveMethodBinding().isEqualTo(smi.resolveMethodBinding())) {
				hasError = true;
				return false;
			}
			methodsToInline.add(getMethodDeclaration(node));
		}
		return super.visit(node);
	}
	
	@Override
	public boolean visit(SimpleName node) {
		if(node.resolveTypeBinding().getName().equals(targetClass.getSimpleName())){
//			fieldsToInline.add(getFieldDeclaration(node));
		}
		return super.visit(node);
	}
	
	@Override
	public boolean visit(ClassInstanceCreation node){
		String typeName = node.getType().toString();
		if (typeName.contains("."))
			typeName = typeName.substring(typeName.lastIndexOf(".") + 1);
		if (typeName.equals(targetClass.getSimpleName())) {
			//precondition fails if we have a new instance creation of the target type within the method
			hasError = true;
			return false;
		}
		return super.visit(node);
	}
	
	private MethodDeclaration getMethodDeclaration(MethodInvocation invocation) {
		ASTNode node = invocation;
		while (node != null & !(node instanceof MethodDeclaration)) {
			node = node.getParent();
		}
		if(node == null) return null;
		else return (MethodDeclaration) node;
	}
	
	
	
	public void process() {
		MethodDeclaration md = null;//getMethodDeclaration(smi);
		MethodObject invokedMethod = null;
		while(targetClass.getMethodIterator().hasNext()) {
			MethodObject m = targetClass.getMethodIterator().next();
			if(m.getName().equals(smi.getName().toString())) {
				invokedMethod = m;
//				md = m.getMethodDeclaration();
				break;
			}
		}
		if(invokedMethod != null) {
//			md.accept(this);
			for(MethodInvocationObject mio : invokedMethod.getMethodInvocations()) {
				
			}
			
		}
	}
	
	public boolean hasError() {
		return hasError;
	}
}
