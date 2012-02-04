package nz.ac.massey.cs.care.ast;

public class ASTInfo {

	protected int startPosition;
	protected int length;

	public ASTInfo() {
		super();
	}

	public ASTInfo(int startPosition2, int length2) {
		this.startPosition = startPosition2;
		this.length = length2;
	}

	public int getStartPosition() {
		return startPosition;
	}

	public void setStartPosition(int startPosition) {
		this.startPosition = startPosition;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

}