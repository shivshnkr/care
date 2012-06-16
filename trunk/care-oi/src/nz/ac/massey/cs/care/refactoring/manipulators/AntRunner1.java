package nz.ac.massey.cs.care.refactoring.manipulators;

import java.io.File;
import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.Target;
import org.eclipse.ant.core.AntRunner;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

public class AntRunner1 {
	
	private String buildPath = null;
	
	public AntRunner1(String buildPath) {
		this.buildPath = buildPath;
	}
	public BuildResult run(){
		
		BuildListener l = new BuildListener(){

			@Override
			public void buildStarted(BuildEvent buildevent) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void buildFinished(BuildEvent buildevent) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void targetStarted(BuildEvent buildevent) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void targetFinished(BuildEvent buildevent) {
				
				// TODO Auto-generated method stub
				if (buildevent.getPriority() == Project.MSG_ERR) {
					System.out.println("FAILED FAILED");
					System.out.println(buildevent.getTarget().getName());
					System.out.println(buildevent.getMessage());
				}
				
			}

			@Override
			public void taskStarted(BuildEvent buildevent) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void taskFinished(BuildEvent buildevent) {
				
				if (buildevent.getPriority() == Project.MSG_ERR) {
					System.out.println("FAILED");
					System.out.println(buildevent.getTarget().getName());
					System.out.println(buildevent.getMessage());
				}
			}

			@Override
			public void messageLogged(BuildEvent buildevent) {
				// TODO Auto-generated method stub
				
			}
			
		};

		File buildFile = new File(buildPath);
		
		Project antProject = new Project();
		antProject.setUserProperty("ant.file", buildFile.getAbsolutePath());
		BuildResult result = new BuildResult();
		antProject.addBuildListener(l);
		try {
			antProject.fireBuildStarted();
			antProject.init();
			ProjectHelper helper = ProjectHelper.getProjectHelper();
			antProject.addReference("ant.projectHelper", helper);
			helper.parse(antProject, buildFile);
			Hashtable<String, Target> targets = antProject.getTargets();
			
			Target clean = targets.get("clean");
			try {
				if(clean!=null){
					runDependentTargets(clean, targets);
				}
			} catch(BuildException e){
				antProject.log(e.getMessage());
			}
			Target compile = targets.get("compile");
			try {
				if(compile!=null){
					runDependentTargets(compile, targets);
					result.setCompilationPassed(true);
				}
			} catch(BuildException e){
				antProject.log(e.getMessage());
			}
			Target test = targets.get("test");
			try {
				if(test!=null){
					runDependentTargets(test, targets);
					result.setTestPassed(1);
				} else result.setTestPassed(-1);
			} catch(BuildException e){
				e.printStackTrace();
				antProject.log(e.getMessage());
			}
			antProject.fireBuildFinished(null);
			antProject.log("********************build finished******************");
		} catch (BuildException e) {
			e.printStackTrace();
			antProject.fireBuildFinished(null);
		}
		return result;
	}
	
	public void runBuild() {
		IProgressMonitor monitor = new NullProgressMonitor();
		AntRunner runner = new AntRunner();
		runner.setBuildFileLocation("c:/buildfiles/build.xml");
		runner.setArguments("-Dmessage=Building -verbose");
		try {
			runner.run();
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}


	private void runDependentTargets(Target target, Hashtable<String, Target> targets){
		Enumeration dependents = target.getDependencies();
		while(dependents.hasMoreElements()){
			String dependentName = (String) dependents.nextElement();
			Target dependent = targets.get(dependentName);
			runDependentTargets(dependent, targets);
		}
		target.performTasks();
		
	}
}
