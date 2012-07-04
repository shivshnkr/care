package nz.ac.massey.cs.care.refactoring.manipulators;

import java.util.ListIterator;

import gr.uom.java.ast.ClassObject;
import gr.uom.java.ast.ConstructorObject;
import gr.uom.java.ast.CreationObject;
import gr.uom.java.ast.MethodObject;
import gr.uom.java.ast.TypeObject;

public class Preconditions {
	private ClassObject source;
	private ClassObject target;
	
	public Preconditions(){
		
	}
	public Preconditions(ClassObject source, ClassObject target) {
		this.source = source;
		this.target = target;
	}
	
	/**
	 * Precondition fails if target class is used as a return type for any method in source class
	 * @return false when precondition fails
	 */
	public boolean precondition1(){
		int numOfMRT = 0;
		ListIterator<MethodObject> methodIt = source.getMethodIterator();
		while(methodIt.hasNext()) {
			MethodObject m = methodIt.next();
			if(m.getReturnType().getClassType().equals(target.getName())) numOfMRT ++;
		}
		return (numOfMRT > 0) ?  false : true;
	}
	
	public boolean precondition2(){
		int numOfMPT = 0;
		ListIterator<MethodObject> methodIt = source.getMethodIterator();
		while(methodIt.hasNext()) {
			MethodObject m = methodIt.next();
			for(TypeObject paramType : m.getParameterTypeList()) {
				if(paramType.getClassType().equals(target.getName())) numOfMPT ++;
			}
		}
		ListIterator<ConstructorObject> constructorIt = source.getConstructorIterator();
		while(constructorIt.hasNext()){
			ConstructorObject co = constructorIt.next();
			for(TypeObject paramType : co.getParameterTypeList()) {
				if(paramType.getClassType().equals(target.getName())) numOfMPT ++;
			}
		}
		return  (numOfMPT > 0) ?  false : true;
	}
	
	
	/**
	 * This precondition checks that any static method of target class should not contain 
	 * a new instance creation of target class.  
	 * @return
	 */
	public boolean preconditionn() {
		boolean result = true;
		ListIterator<MethodObject> methodIt = target.getMethodIterator();
		while(methodIt.hasNext()) {
			MethodObject m = methodIt.next();
			for(CreationObject co : m.getCreations()) {
				if(co.getType().getClassType().equals(target.getName())) result = false;
			}
		}
		return result;
	}

}
