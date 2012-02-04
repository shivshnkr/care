package nz.ac.massey.cs.care.refactoring.manipulators;

import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.ClassObject;

public class AbstractionCandidateRefactoring {
	private String sourceClass;
	private String targetClass;
	private String relationship;
	private ClassObject sourceClassObject;
	private ClassObject targetClassObject;
	
	public String getSourceClass() {
		return sourceClass;
	}
	public void setSourceClass(String sourceClass) {
		this.sourceClass = sourceClass;
	}
	public String getTargetClass() {
		return targetClass;
	}
	public void setTargetClass(String targetClass) {
		this.targetClass = targetClass;
	}
	public String getRelationship() {
		return relationship;
	}
	public void setRelationship(String relationship) {
		this.relationship = relationship;
	}
	public ClassObject getSourceClassObject() {
		return sourceClassObject;
	}
	public void setSourceClassObject(ClassObject sourceClassObject) {
		this.sourceClassObject = sourceClassObject;
	}
	public ClassObject getTargetClassObject() {
		return targetClassObject;
	}
	public void setTargetClassObject(ClassObject targetClassObject) {
		this.targetClassObject = targetClassObject;
	}
	
	public boolean instantiateParticipants(){
		boolean succeed = false;
		String sourceClass = this.getSourceClass();
		String targetClass = this.getTargetClass();
		if(sourceClass.contains("$")) sourceClass = sourceClass.replace("$", ".");
		if(targetClass.contains("$")) targetClass = targetClass.replace("$", ".");
		ClassObject sourceClassObject = ASTReader.getSystemObject()
		.getClassObject(sourceClass);
		if(sourceClassObject == null) return succeed;
		else this.setSourceClassObject(sourceClassObject);
		
		ClassObject targetClassObject = ASTReader.getSystemObject()
		.getClassObject(targetClass);
		if(targetClassObject == null) return succeed; 
		else this.setTargetClassObject(targetClassObject);
		
		return true;
	}
	public String toString(){
		return "Abstraction Dependency: " + getSourceClass() + "_TO_" + getTargetClass();
	}
}
