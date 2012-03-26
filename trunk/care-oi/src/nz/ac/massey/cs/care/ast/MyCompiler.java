package nz.ac.massey.cs.care.ast;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IProblemRequestor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.core.builder.JavaBuilder;
import org.eclipse.ltk.internal.core.refactoring.BasicElementLabels;
import org.eclipse.ltk.internal.core.refactoring.Messages;


@SuppressWarnings("restriction")
public class MyCompiler {
	
	private boolean projectHasError = false;
	
	static StringBuffer message = new StringBuffer();
	

	
	public IStatus build(IProject project){
		try {
			project.touch(new NullProgressMonitor());
			project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
			project.build(JavaBuilder.CLEAN_BUILD, new NullProgressMonitor());
			project.build(JavaBuilder.FULL_BUILD, new NullProgressMonitor());
		} catch (CoreException e) {
			return e.getStatus();
		}
		if(!existsProblems(project)){
			return Status.OK_STATUS;
		} else {
				String errorMessages = findProblems(project);
				IStatus s = null;
				try{
					s = new Status(IStatus.ERROR, ResourcesPlugin.PI_RESOURCES,IResourceStatus.BUILD_FAILED, Messages.format(errorMessages, BasicElementLabels.getPathLabel(project.getFullPath(), false)), null);
				} catch(Exception e) {
					return new Status(IStatus.ERROR, ResourcesPlugin.PI_RESOURCES, errorMessages);
				}
			return s;
		}
		
	}

	private String findProblems(IProject project) {
		IMarker[] markers;
		try {
			markers = project.findMarkers(IMarker.PROBLEM, true,
					IResource.DEPTH_INFINITE);

			if (markers.length > 0) {
				Set<IFile> filesWithErrors = new HashSet<IFile>();
				for (int i = 0; i < markers.length; i++) {
					if (isLaunchProblem(markers[i])) {
						IMarker error = markers[i];
						IResource cause = error.getResource();
						if(cause instanceof IFile) {
							IFile file = (IFile) cause;
							filesWithErrors.add(file);
						}
					}
				}
				message = new StringBuffer();
				for(IFile file : filesWithErrors) {
					org.eclipse.jdt.core.ICompilationUnit causeCu = JavaCore.createCompilationUnitFrom(file);
					// create requestor for accumulating discovered problems
					IProblemRequestor problemRequestor = new IProblemRequestor() {
						public void acceptProblem(IProblem problem) {
//							System.out.println(problem.getID() + ": "
//									+ problem.getMessage());
							if(problem.isError()){
								message.append(CharOperation.charToString(problem.getOriginatingFileName()) + ": ");
								message.append("at Line " + problem.getSourceLineNumber() + ": ");
								message.append(problem.getID() + ": " + problem.getMessage());
								message.append("\n");
							}
						}
						public void beginReporting() {	}
						public void endReporting() { }
						public boolean isActive() {	return true;
						} // will detect problems if active
					};
					org.eclipse.jdt.core.ICompilationUnit workingCopy = causeCu.getWorkingCopy(new WorkingCopyOwner() {	}, problemRequestor, null);
//					// trigger reconciliation
					workingCopy.reconcile();
//					workingCopy.reconcile(org.eclipse.jdt.core.ICompilationUnit.NO_AST, true, null, null);
					System.out.println(message);
				}
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return message.toString();
		
	}
	
	/**
	 * Returns whether the given project contains any problem markers of the
	 * specified severity.
	 * 
	 * @param proj the project to search
	 * @return whether the given project contains any problems that should
	 *  stop it from launching
	 * @throws CoreException if an error occurs while searching for
	 *  problem markers
	 */
	protected boolean existsProblems(IProject proj)  {
		IMarker[] markers;
		try {
			markers = proj.findMarkers(IMarker.PROBLEM, true,
					IResource.DEPTH_INFINITE);

			if (markers.length > 0) {
				for (int i = 0; i < markers.length; i++) {
					if (isLaunchProblem(markers[i])) {
						return true;
					}
				}
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return false;
	}
	/**
	 * Returns whether the given problem should potentially abort the launch.
	 * By default if the problem has an error severity, the problem is considered
	 * a potential launch problem. Subclasses may override to specialize error
	 * detection.
	 * 
	 * @param problemMarker candidate problem
	 * @return whether the given problem should potentially abort the launch
	 * @throws CoreException if any exceptions occur while accessing marker attributes
	 */
	protected boolean isLaunchProblem(IMarker problemMarker) throws CoreException {
		Integer severity = (Integer)problemMarker.getAttribute(IMarker.SEVERITY);
		if (severity != null) {
			return severity.intValue() >= IMarker.SEVERITY_ERROR;
		} 
		
		return false;
	}
	
	public boolean hasErrors() {
		
		return this.projectHasError;
	}

	
}
