package nz.ac.massey.cs.care.refactoring.manipulators;


public class Postconditions {
	
	public Postconditions(MyCompiler compiler2, MyTestRunner testRunner2) {
		compiler = compiler2;
		testRunner = testRunner2;
	}
	public Postconditions(AntRunner1 antRunner) {
		this.antRunner = antRunner;
	}
	MyCompiler compiler;
	MyTestRunner testRunner;
	AntRunner1 antRunner;

}
