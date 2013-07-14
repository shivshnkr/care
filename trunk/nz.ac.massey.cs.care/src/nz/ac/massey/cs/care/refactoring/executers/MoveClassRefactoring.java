package nz.ac.massey.cs.care.refactoring.executers;

import static nz.ac.massey.cs.care.util.MoveUtils.*;

import java.util.ArrayList;
import java.util.List;

import nz.ac.massey.cs.care.Edge;
import nz.ac.massey.cs.care.Postcondition;
import nz.ac.massey.cs.care.Precondition;
import nz.ac.massey.cs.care.Vertex;
import nz.ac.massey.cs.care.refactoring.constraints.CheckAccessabilityCountPrecondition;
import nz.ac.massey.cs.care.refactoring.constraints.CheckBlacklistedPrecondition;
import nz.ac.massey.cs.care.refactoring.constraints.CheckRenamePrecondition;
import nz.ac.massey.cs.care.refactoring.constraints.CheckSamePackagePrecondition;
import nz.ac.massey.cs.care.refactoring.movehelper.Candidate;
import nz.ac.massey.cs.care.refactoring.movehelper.ConstraintsResult;
import nz.ac.massey.cs.care.refactoring.movehelper.RefactoringResult;
import nz.ac.massey.cs.care.scripts.Criteria;
import nz.ac.massey.cs.care.scripts.MoveExecuter;
import nz.ac.massey.cs.care.scripts.CompositeExecuter;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
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
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.MoveRefactoring;

import edu.uci.ics.jung.graph.DirectedGraph;
import gr.uom.java.ast.ASTReader;

@SuppressWarnings("restriction")
public class MoveClassRefactoring extends CareRefactoring {

	private Candidate finalCandidate;
	private MoveExecuter executer;
	private DirectedGraph<Vertex, Edge> g;
	private Candidate[] candidates;

	public MoveClassRefactoring(Candidate candidate) {
		super(candidate, new Precondition[]{new CheckRenamePrecondition(), 
				new CheckBlacklistedPrecondition(),
				new CheckAccessabilityCountPrecondition(), new CheckSamePackagePrecondition() });
		try {
			g =  loadGraphUsingMoveReader(candidate.getGraphSource());
			candidate.setGraph(g);
		} catch (Exception e) {
		}
		candidates = initializeMoveCandidates(candidate.getEdge(), g);
	}

	@Override
	public String getName() {
		return "Move";
	}

	@Override
	public void checkPreconditions(RefactoringResult result) {
		finalCandidate = chooseFinalCandidate(candidates, g); 
		if(finalCandidate == null) {
			result.setError(true);
			ConstraintsResult r1 = candidates[0].getConstraintsResult();
			candidate.setConstraintsResult(r1);
//			result.setFailedPreconditions(failedPreconditions);
		}
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException,
			OperationCanceledException {
		try {
			if(finalCandidate == null) return null;
			String class2Move = finalCandidate.getClassToMove();
//			System.out.println(class2Move);
			IType t = ASTReader.getExaminedProject().findType(class2Move);
			if(t == null) { 
				return new NullChange();
			}
			ICompilationUnit icu = t.getCompilationUnit();
			if(icu == null) {
				return new NullChange();
			}
			IFile source = (IFile)icu.getResource();
			applyMove(source, finalCandidate.getTargetPackage());
			candidate.setConstraintsResult(finalCandidate.getConstraintsResult());
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void applyMove(IFile source, String targetPackage) {
		List elements  = new ArrayList();
		elements.add(JavaCore.create(source));
		IResource[] resources= ReorgUtils.getResources(elements);
		IJavaElement[] javaElements= ReorgUtils.getJavaElements(elements);
		IMovePolicy policy;
		try {
			policy = ReorgPolicyFactory.createMovePolicy(resources, javaElements);
			policy.setDestinationCheck(true);
			policy.setUpdateReferences(true);
			if (policy.canEnable()) {
				JavaMoveProcessor processor= new JavaMoveProcessor(policy);
				IReorgDestination dest = getContainer(targetPackage);
				processor.setUpdateReferences(true);
				processor.setDestination(dest);
				processor.setCreateTargetQueries(new CreateTargetQueries(candidate.getWorkBench().getShell()));
				processor.setReorgQueries(new ReorgQueries(candidate.getWorkBench().getShell()));
				MoveRefactoring refactoring = new MoveRefactoring(processor);
				refactoring.checkAllConditions(new NullProgressMonitor());
				Change change = refactoring.createChange(new NullProgressMonitor());
				undoList.add(change.perform(new NullProgressMonitor()));
			}
		} catch (JavaModelException e1) {
			e1.printStackTrace();
		}catch (OperationCanceledException e) {
			e.printStackTrace();
		} catch (CoreException e) {
			e.printStackTrace();
		} catch(NullPointerException e){
			e.printStackTrace();
		}
		
	}
	private static IReorgDestination getContainer(String targetPackage) {
		try {
			IPackageFragment[] packages = ASTReader.getExaminedProject().getPackageFragments();
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
	public void rollback() {
		if(finalCandidate == null) return;
		String classname = finalCandidate.getClassToMove();
		if(classname.contains(".")) classname = classname.substring(classname.lastIndexOf(".")+1);
		classname = finalCandidate.getTargetPackage() + "." + classname;
		try {
			IType t = ASTReader.getExaminedProject().findType(classname);
			if(t == null) { return;}
			ICompilationUnit icu = t.getCompilationUnit();
			if(icu == null) {return;}
			IFile source = (IFile)icu.getResource();
			applyMove(source, finalCandidate.getSourcePackage());
			
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
	}
	
	public Candidate chooseFinalCandidate(Candidate[] candidates, DirectedGraph<Vertex, Edge> g) {
		
		Candidate c1 = candidates[0];
		Candidate c2 = candidates[1]; 
		
		boolean r1 = isApplicableOnGraph(c1, pres, posts, g);
		boolean r2 = isApplicableOnGraph(c2, pres, posts, g);
		
		Candidate finalCandidate = new Candidate();
		if(r1 && r2) {
			finalCandidate = Criteria.choose(c1, c2);
		} else if(r1 && !r2) {
			finalCandidate = c1;
		} else if(!r1 && r2) {
			finalCandidate = c2;
		} else {
			finalCandidate = null;
		}
		return finalCandidate;
		
	}
	/**
	 * This methods returns true only if a candidate for move passes all pre and postconditions.
	 * These pre and postconditions are checked on the graph level
	 * @param candidate
	 * @param pres
	 * @param posts
	 * @param g
	 * @return
	 */
	private boolean isApplicableOnGraph(Candidate candidate, Precondition[] pres, Postcondition[] posts,
			DirectedGraph<Vertex, Edge> g) {
		
		executer = new MoveExecuter();
		ConstraintsResult result = candidate.getConstraintsResult();
		for(Precondition pre : pres) {
			if(pre.isGraphLevel()) {
				String name = pre.getName();
				boolean failed = pre.isFailed(candidate);
				if(failed) {
					if(name.equals("Rename")) result.setRename(false);
					else if(name.equals("Change Accessability")) result.setAccessability(false);
					else if(name.equals("same package")) result.setSamePackage(false);
					else result.setBlacklisted(false);
				} else {
					//precondition passed
					if(name.equals("Rename")) result.setRename(true);
					else if(name.equals("Change Accessability")) result.setAccessability(true);
					else if(name.equals("same package")) result.setSamePackage(true);
					else result.setBlacklisted(true);
				}
			}
		}
		//we have one graph level post condition
		boolean postsSuccessful = true;
		if(result.areMovePresPassed()) {
			List<Vertex> classesMoved = executer.performMoveOnGraph(g, candidate);
			candidate.setInstancesAfter(countAllInstances(g, CompositeExecuter.getMotifs()).getNumberOfInstances());
			candidate.setClassesMoved(classesMoved);
			for(Postcondition post : posts) {
				if(post.isGraphLevel()){
					boolean failed = post.isFailed(candidate);
					if(failed) {
						if(post.getName().equals("Instance Count")) result.setInstanceCount(false);
						postsSuccessful = false;
					} else {
						if(post.getName().equals("Instance Count")) result.setInstanceCount(true);
					}
				}
			}
		}
		executer.resetGraph(candidate, g);
		if(result.areMovePresPassed() && postsSuccessful) return true; else return false;
	}
	public Candidate[] initializeMoveCandidates(Edge winner, DirectedGraph<Vertex, Edge> g) {
		Candidate[] candidates = new Candidate[2];
		Candidate c1 = initializeCandidte(winner.getStart(), winner.getEnd(), g);
		Candidate c2 = initializeCandidte(winner.getEnd(), winner.getStart(), g);
		candidates[0] = c1;
		candidates[1] = c2;
		return candidates;
	}
	private Candidate initializeCandidte(Vertex start,
			Vertex end, DirectedGraph<Vertex, Edge> g) {
		Candidate c = new Candidate();
		c.setSourceClass(start.getFullname());
		c.setSourcePackage(start.getNamespace());
		c.setTargetPackage(end.getNamespace());
		c.setGraphSource(candidate.getGraphSource());
		c.setGraph(candidate.getGraph());
		int instancesBefore = countAllInstances(g, candidate.getMotifs()).getNumberOfInstances();
		c.setInstancesBefore(instancesBefore);
		c.setProject(c.getProject());
		c.setMotifs(candidate.getMotifs());
		return c;
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		return null;
	}
}
