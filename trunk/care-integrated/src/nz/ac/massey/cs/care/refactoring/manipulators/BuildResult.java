package nz.ac.massey.cs.care.refactoring.manipulators;

public class BuildResult {

	private boolean isCompilationPassed = false;
	private int isTestPassed = 0;
	
	public boolean isCompilationPassed() {
		return isCompilationPassed;
	}
	public void setCompilationPassed(boolean isCompilationPassed) {
		this.isCompilationPassed = isCompilationPassed;
	}
	public int isTestPassed() {
		return isTestPassed;
	}
	/**
	 * Sets the result of execution of "test" target
	 * @param i (1=passed, 0=failed, -1=test target doesn't exist
	 */
	public void setTestPassed(int i) {
		this.isTestPassed = i;
	}
}
