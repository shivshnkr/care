package nz.ac.massey.cs.care.ast;

import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

public class ParameterASTInfo extends ASTInfo {

	private SingleVariableDeclaration parameter;

	public ParameterASTInfo(SingleVariableDeclaration param, int startPosition2, int length2) {
		super(startPosition2, length2);
		this.parameter = param;
	}

	public SingleVariableDeclaration getParameter() {
		return parameter;
	}

	public void setParameter(SingleVariableDeclaration parameter) {
		this.parameter = parameter;
	}
	
}
