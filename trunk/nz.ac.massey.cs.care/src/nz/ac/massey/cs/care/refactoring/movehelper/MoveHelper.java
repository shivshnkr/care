package nz.ac.massey.cs.care.refactoring.movehelper;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.corext.refactoring.reorg.IReorgDestination;
import org.eclipse.jdt.internal.corext.refactoring.reorg.IReorgPolicy.IMovePolicy;
import org.eclipse.jdt.internal.corext.refactoring.reorg.JavaMoveProcessor;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgDestinationFactory;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgPolicyFactory;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgUtils;
import org.eclipse.jdt.internal.ui.refactoring.reorg.CreateTargetQueries;
import org.eclipse.jdt.internal.ui.refactoring.reorg.ReorgQueries;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.participants.MoveRefactoring;
import org.eclipse.ui.IWorkbenchPartSite;

/**
 * Helper class for Move Class Refactoring
 * @author Ali
 */
@SuppressWarnings("restriction")
public class MoveHelper {
	private static IWorkbenchPartSite iWorkbenchPartSite;
	
	private static IJavaProject iJavaProject;
	
	public static void setPart(IWorkbenchPartSite iwp) {
		iWorkbenchPartSite = iwp;
	}
	
	@SuppressWarnings({"unchecked" })
	public
	static void applyMove(IFile sourceFile, String targetPackage, boolean succeeded) {
		List elements  = new ArrayList();
		elements.add(JavaCore.create(sourceFile));
		IResource[] resources= ReorgUtils.getResources(elements);
		IJavaElement[] javaElements= ReorgUtils.getJavaElements(elements);
		IMovePolicy policy;
		try {
			policy = ReorgPolicyFactory.createMovePolicy(resources, javaElements);
			policy.setDestinationCheck(true);
			policy.setUpdateReferences(true);
			if (policy.canEnable()) {
				JavaMoveProcessor processor= new JavaMoveProcessor(policy);
				IReorgDestination dest = getContainer(targetPackage, iJavaProject);
				processor.setUpdateReferences(true);
				processor.setDestination(dest);
				processor.setCreateTargetQueries(new CreateTargetQueries(iWorkbenchPartSite.getShell()));
				processor.setReorgQueries(new ReorgQueries(iWorkbenchPartSite.getShell()));
				MoveRefactoring refactoring = new MoveRefactoring(processor);
				refactoring.checkAllConditions(new NullProgressMonitor());
				Change change = refactoring.createChange(new NullProgressMonitor());
				change.perform(new NullProgressMonitor());
			}
		} catch (JavaModelException e1) {
			e1.printStackTrace();
			succeeded = false;
		}catch (OperationCanceledException e) {
			e.printStackTrace();
			succeeded = false;
		} catch (CoreException e) {
			e.printStackTrace();
			succeeded = false;
		} catch(NullPointerException e){
			e.printStackTrace();
			succeeded = false;
		}
		
	}
	private static IReorgDestination getContainer(String targetPackage, IJavaProject ijp) {
		try {
			IPackageFragment[] packages = ijp.getPackageFragments();
			for (IPackageFragment mypackage : packages) {
				if(!(mypackage.getKind() == IPackageFragmentRoot.K_SOURCE)) continue;
				if(mypackage.getElementName().equals(targetPackage)){
					return ReorgDestinationFactory.createDestination(mypackage);
				}
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	public static void setIJavaProject(IJavaProject p) {
		iJavaProject = p;
	}	

}
