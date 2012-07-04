package nz.ac.massey.cs.care.refactoring.manipulators;

import org.junit.runner.Result;

public class MyTestRunner {
	private String testClass = null;
	
	public boolean executeTests() {
		Result r = null;
		try {
			Class c = Class.forName(testClass);
			r = org.junit.runner.JUnitCore.runClasses(c);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return false;
		}
		if(r.getFailureCount()==0) return true; else return false;
	}

	public MyTestRunner() {
		super();
		this.testClass = "";
	}
	public MyTestRunner(String testClass) {
		super();
		this.testClass = testClass;
	}

}
