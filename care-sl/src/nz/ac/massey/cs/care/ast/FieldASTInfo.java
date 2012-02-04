package nz.ac.massey.cs.care.ast;

import org.eclipse.jdt.core.dom.FieldDeclaration;

public class FieldASTInfo extends ASTInfo {
	
	private FieldDeclaration node = null;
	
	public FieldASTInfo(FieldDeclaration node2, int startPosition2, int length2) {
		super(startPosition2, length2);
		this.node = node2;
	}
	public FieldDeclaration getFieldDeclaration() {
		return node;
	}
	public void setFieldDeclaration(FieldDeclaration node) {
		this.node = node;
	}
	

}
