package nz.ac.massey.cs.care.ast;

import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

public class VariableASTInfo extends ASTInfo {

	private VariableDeclarationStatement variable;

	public VariableDeclarationStatement getVariable() {
		return variable;
	}

	public void setVariable(VariableDeclarationStatement variable) {
		this.variable = variable;
	}

	public VariableASTInfo(VariableDeclarationStatement node, int startPosition, int length) {
		super(startPosition, length);
		this.variable = node;
		
	}
	
	
}
