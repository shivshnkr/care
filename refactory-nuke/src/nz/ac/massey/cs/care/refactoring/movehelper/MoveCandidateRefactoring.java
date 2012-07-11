package nz.ac.massey.cs.care.refactoring.movehelper;

import gr.uom.java.ast.ClassObject;

public class MoveCandidateRefactoring {
	private ClassObject classObjectToMove;
	private String classToMove;
	private String targetPackage;
	private String sourcePackage;
	private boolean isAdditionalRefactoringsRequired;
	
	public ClassObject getClassObjectToMove() {
		return classObjectToMove;
	}

	public void setClassObjectToMove(ClassObject classObjectToMove) {
		this.classObjectToMove = classObjectToMove;
	}

	public String getClassToMove() {
		return classToMove;
	}

	public String getTargetPackage() {
		return targetPackage;
	}

	public void setClassToMove(String classToMove) {
		this.classToMove = classToMove;
		
	}

	public void setTargetPackage(String targetPackage) {
		this.targetPackage = targetPackage;
		
	}

	public void setAdditionalRefactoringsRequired(boolean isAddRefacRequired) {
		this.isAdditionalRefactoringsRequired = isAddRefacRequired;
		
	}
	
	public boolean isAdditionalRefactoringsRequired() {
		return isAdditionalRefactoringsRequired;
		
	}

	public String getSourcePackage() {
		return sourcePackage;
	}

	public void setSourcePackage(String sourcePackage) {
		this.sourcePackage = sourcePackage;
	}
}
