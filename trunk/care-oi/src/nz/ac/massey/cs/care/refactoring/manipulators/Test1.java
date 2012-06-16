package nz.ac.massey.cs.care.refactoring.manipulators;

import org.eclipse.ant.core.AntRunner;
import org.eclipse.ant.core.TargetInfo;
import org.eclipse.core.runtime.CoreException;
import org.junit.Test;

public class Test1 {

	@Test
	public void main() {

		try {
			// AntRunner runner = new AntRunner();
			// runner.setBuildFileLocation("/Volumes/Data2/PhD/workspaces/corpus2010/test1/build.xml");
			// runner.setArguments("-Dmessage=Building -verbose");
			// for(TargetInfo t : runner.getAvailableTargets()) {
			// if(t.getName().equals("clean")) {
			// runner.run();
			// }
			// }
			// // runner.run();
			// }catch (CoreException e) {
			// e.printStackTrace();
			// System.out.println(e);
			// }
			AntRunner1 antRunner = new AntRunner1("/Volumes/Data2/PhD/workspaces/corpus2010/test1/build.xml");
			BuildResult r = antRunner.run();
			System.out.println(r.isCompilationPassed());
			System.out.println(r.isTestPassed());
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
