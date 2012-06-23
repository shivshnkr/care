package nz.ac.massey.cs.care.ast;

import org.eclipse.jdt.core.dom.Type;

public class ReturnASTInfo extends ASTInfo {

	private Type returnType;

	public Type getReturnType() {
		return returnType;
	}

	public void setReturnType(Type returnType) {
		this.returnType = returnType;
	}

	public ReturnASTInfo(Type type, int startPosition2, int length2) {
		super(startPosition2, length2);
		this.returnType = type;
	}
	
}
