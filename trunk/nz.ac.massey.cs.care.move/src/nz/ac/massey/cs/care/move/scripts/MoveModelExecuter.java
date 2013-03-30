package nz.ac.massey.cs.care.move.scripts;

import static nz.ac.massey.cs.care.move.util.Printery.*;
import static nz.ac.massey.cs.care.move.util.Utils.*;
import static nz.ac.massey.cs.care.move.io.JarReader.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.ui.IWorkbenchPartSite;

import nz.ac.massey.cs.care.move.*;
import nz.ac.massey.cs.guery.Motif;
import nz.ac.massey.cs.care.move.io.JarReader;
import nz.ac.massey.cs.care.move.metrics.*;
import nz.ac.massey.cs.care.move.refactoring.constraints.CheckAccessabilityCountPrecondition;
import nz.ac.massey.cs.care.move.refactoring.constraints.CheckBlacklistedPrecondition;
import nz.ac.massey.cs.care.move.refactoring.constraints.CheckCompilationPostcondition;
import nz.ac.massey.cs.care.move.refactoring.constraints.CheckInstanceCountPostcondition;
import nz.ac.massey.cs.care.move.refactoring.constraints.CheckRenamePrecondition;
import nz.ac.massey.cs.care.move.refactoring.movehelper.ConstraintsResult;
import nz.ac.massey.cs.care.move.refactoring.movehelper.MoveCandidate;
import nz.ac.massey.cs.care.move.refactoring.movehelper.MoveHelper;
import nz.ac.massey.cs.care.move.scoring.*;
import nz.ac.massey.cs.care.move.util.ResultCounter;

import com.google.common.base.Function;
import edu.uci.ics.jung.graph.DirectedGraph;

/**
 * Move class refactoring execution process. 
 * 
 * @author Ali, Jens
 */
public class MoveModelExecuter {
	private static String WORKSPACE_PATH = null;//"/Volumes/Data2/PhD/workspaces/CARE/nz.ac.massey.cs.care.move/";
	private static final int MAX_ITERATIONS = 50; // stop after this number of edges have been removed
	private double scdInstancesAtStart = 0;
	private double wcdInstancesAtStart = 0;
	private Set<Edge> useLessEdges = new HashSet<Edge>();
	private Set<Edge> edgesSucceeded = new HashSet<Edge>();
	private static boolean DO_NOT_REMOVE_EDGES_WITH_NULL_SCORE = true;
	public static ScoringFunction scoringfunction = new CDOnlyScoringFunction();
	private  MoveCandidate candidate = new MoveCandidate();
	private IProject iProject = null;
	private IJavaProject iJavaProject = null;
	private static IWorkbenchPartSite iwps;
	private static List<String> blacklisted = new ArrayList<String>();
	private boolean abort;
	private static Motif<Vertex, Edge>[] motifs;
	private static String[] outfiles;
	private List<MoveCandidate> candidates = new ArrayList<MoveCandidate>();
	private Precondition[] pres = {	new CheckRenamePrecondition(), 
			new CheckBlacklistedPrecondition(),
			new CheckAccessabilityCountPrecondition() };

	private Postcondition[] posts = {	new CheckInstanceCountPostcondition(), 
				new CheckCompilationPostcondition()  };

	public static class VertexComparator implements Comparator<Vertex> {
		@Override
		public int compare(Vertex o1, Vertex o2) {
			return o1.getFullname().compareTo(o2.getFullname());
		}
	}

	public void execute(DirectedGraph<Vertex, Edge> g, String graphSource, int i)
			throws Exception {

		long ts1 = System.currentTimeMillis();

		if (i > MAX_ITERATIONS) return; // only check the first 50 iterations
		final ResultCounter registry = countInstances(g, getMotifs()[0]);
		int scdInstances = registry.getNumberOfInstances();
		// record weak CD in the graph
		int wcdInstances = countWeakInstances(g, getMotifs()[1]);
		
		if (i == 0) {
			log("analysing cd queries");
			scdInstancesAtStart = scdInstances;
			wcdInstancesAtStart = wcdInstances;
		}
		Double scdPercent = (scdInstances * 100) / scdInstancesAtStart;
		Double wcdPercent = (wcdInstances * 100) / wcdInstancesAtStart;
		if (scdInstances == 0) {
			log("No more instances found at step ", i);
			printRemovalStepStats(getOutputFiles()[0], i, scdInstances,
					round(scdPercent), wcdInstances, round(wcdPercent));
			return;
		}
		
		long ts2 = System.currentTimeMillis();
		log("Iteration ", i, ", instances ", scdInstances, ", instances ",
				round(scdPercent), "%, detection took ",
				(ts2 - ts1) / 1000, "s");
		// find edges with highest rank
		final Map<Edge, Integer> map = new HashMap<Edge, Integer>();

		Set<Edge> edgesWithHighestRank = findLargestByIntRanking(g.getEdges(),
				new Function<Edge, Integer>() {
					@Override
					public Integer apply(Edge e) {
						return registry.getCount(e);
					}
				});
		Edge winner = edgesWithHighestRank.iterator().next();
//		log("Iteration " + i + ". Attempting " + winner.getStart().getFullname() + " > "
//					+ winner.getEnd().getFullname());
		int topScore = registry.getCount(winner);
		
		if (topScore == 0 && DO_NOT_REMOVE_EDGES_WITH_NULL_SCORE) {
			System.out.println("only edges with score==0 found, will not remove edges");
			return;
		} else {
			Vertex s = winner.getStart();
			Vertex t = winner.getEnd();
			// remove the edge
			g.removeEdge(winner);
			s.getOutEdges().remove(winner);
			t.getInEdges().remove(winner);
			printRemovalStepStats(getOutputFiles()[0], i, scdInstances,round(scdPercent), wcdInstances,round(wcdPercent));
			map.clear();
			refreshProject();
			execute(g, graphSource, i + 1);
		}
		
	}
	public void refreshProject() {
		try {
			iProject.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		} catch (CoreException e1) {
			e1.printStackTrace();
		}
		System.gc();
	}
	private MoveCandidate getFinalCandidate() {
		return candidate;
	}
	private void setFinalCandidate(MoveCandidate c) {
		this.candidate = c;
	}
	public static void setMotifs(Motif<Vertex, Edge>[] motifss) {
		motifs = motifss;
	}
	/**
	 * <pre>
	 * Motifs order is as follows:
	 * 0. Strong CD
	 * 1. Weak CD
	 * </pre>
	 * @return
	 */
	public static Motif<Vertex, Edge>[] getMotifs() {
		return motifs;
	}
	
	/**
	 * Classes that either should not be moved in the process or
	 * those classes that create error if they are moved outside
	 * their original package.
	 * @param classes
	 */
	public void addUnMovableClasses(List<String> classes) {
		blacklisted.addAll(classes);
	}
	
	public void addSkippedEdges(Set<Edge> edges) {
		useLessEdges.addAll(edges);
	}
	
	private boolean alreadyChecked(Edge winner) {
		for(Edge e : useLessEdges) {
			String oldStart = e.getStart().getName();
			String oldEnd = e.getEnd().getName();
			String newStart = winner.getStart().getName();
			String newEnd = winner.getEnd().getName();
			if(oldStart.equals(newStart) && oldEnd.equals(newEnd)) return true;
		}
		for(Edge e : edgesSucceeded) {
			String oldStart = e.getStart().getName();
			String oldEnd = e.getEnd().getName();
			String newStart = winner.getStart().getName();
			String newEnd = winner.getEnd().getName();
			if(oldStart.equals(newStart) && oldEnd.equals(newEnd)) return true;
		}
		return false;
	}
	private boolean applyMove(Edge winner, int initialInstances,
			DirectedGraph<Vertex, Edge> g) {
		
		boolean succeeded = false;
		
		MoveCandidate c1 = initializeCandidte(winner.getStart(), winner.getEnd(), g);
		MoveCandidate c2 = initializeCandidte(winner.getEnd(), winner.getStart(), g); 
		
		boolean r1 = isApplicableOnGraph(c1, pres, posts, g);
		boolean r2 = isApplicableOnGraph(c2, pres, posts, g);
		
		MoveCandidate finalCandidate = new MoveCandidate();
		MoveCandidate reserveCandidate = new MoveCandidate();
		if(r1 && r2) {
			MoveCandidate[] candidates = Criteria.choose(c1, c2);
			finalCandidate = candidates[0];
			reserveCandidate = candidates[1];
		} else if(r1 && !r2) {
			finalCandidate = c1;
			reserveCandidate = null;
		} else if(!r1 && r2) {
			finalCandidate = c2;
			reserveCandidate = null;
		} else {
			finalCandidate = null;
			reserveCandidate = null;
		}
		if(finalCandidate != null) {
			//apply move on the source code level
			succeeded = applyOnCode(finalCandidate, posts, g, winner);
			if(!succeeded && reserveCandidate != null) {
				succeeded = applyOnCode(reserveCandidate, posts, g, winner);
			}
		}
		//record constraints information
		printConstraintsInfo(getOutputFiles()[5], c1);
		printConstraintsInfo(getOutputFiles()[5], c2);
		
		return succeeded;
	}
	
	private MoveCandidate initializeCandidte(Vertex start,
			Vertex end, DirectedGraph<Vertex, Edge> g) {
		MoveCandidate candidate = new MoveCandidate();
		candidate.setClassToMove(start.getFullname());
		candidate.setSourcePackage(start.getNamespace());
		candidate.setTargetPackage(end.getNamespace());
		int instancesBefore = countInstances(g, getMotifs()[0]).getNumberOfInstances();
		candidate.setInstancesBefore(instancesBefore);
		candidate.setProject(iProject);
		return candidate;
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
	private boolean isApplicableOnGraph(MoveCandidate candidate, Precondition[] pres, Postcondition[] posts,
			DirectedGraph<Vertex, Edge> g) {
		
		if(isAlreadyChecked(candidate)) {
			//if we already tried this candidate and it failed, no need to retry it.
			Boolean checked = candidate.getConstraintsResult().areGraphConstraintsPassed();
			if(checked == null) {
				//Do Nothing
			}
			else if(checked == false) return false;
			else return true;
		}
		
		ConstraintsResult result = candidate.getConstraintsResult();
		for(Precondition pre : pres) {
			if(pre.isGraphLevel()) {
				String name = pre.getName();
				boolean failed = pre.check(candidate);
				if(failed) {
					if(name.equals("Rename")) result.setRename(false);
					else if(name.equals("Change Accessability")) result.setAccessability(false);
					else result.setBlacklisted(false);
				} else {
					//precondition passed
					if(name.equals("Rename")) result.setRename(true);
					else if(name.equals("Change Accessability")) result.setAccessability(true);
					else result.setBlacklisted(true);
				}
			}
		}
		//we have one graph level post condition
		boolean postsSuccessful = true;
		if(result.arePresPassed()) {
			List<Vertex> classesMoved = performMoveOnGraph(g, candidate);
			candidate.setInstancesAfter(countInstances(g, getMotifs()[0]).getNumberOfInstances());
			candidate.setClassesMoved(classesMoved);
			for(Postcondition post : posts) {
				if(post.isGraphLevel()){
					boolean failed = post.check(candidate);
					if(failed) {
						if(post.getName().equals("Instance Count")) result.setInstanceCount(false);
						postsSuccessful = false;
					} else {
						if(post.getName().equals("Instance Count")) result.setInstanceCount(true);
					}
				}
			}
		}
		resetGraph(candidate, g);
		candidates.add(candidate);
		if(result.arePresPassed() && postsSuccessful) return true; else return false;
	}

	private boolean isAlreadyChecked(MoveCandidate candidate) {
		boolean r = false;
		for(MoveCandidate c : candidates) {
			if(c.getClassToMove().equals(candidate.getClassToMove()) && 
					c.getTargetPackage().equals(candidate.getTargetPackage())) {
				candidate = c;
				r = true;
				break;
			}
		}
		return r;
	}
	public static boolean isBlacklisted(String classToMove) {
		for(String classBlacklisted : blacklisted){
			
			if(classBlacklisted.equals(classToMove)) return true;
			String outerclass = JarReader.getOuterClass(classToMove);
			if(outerclass != null && outerclass.equals(classBlacklisted)) return true;
		}
		return false;
	}

	public static String getSimplename(String cname) {
		return cname.substring(cname.lastIndexOf(".") + 1);
	}
	private boolean applyOnCode(MoveCandidate candidate, Postcondition[] posts, DirectedGraph<Vertex, Edge> g, Edge winner) {
		boolean succeeded = false;
		ConstraintsResult result = candidate.getConstraintsResult();
		performMoveOnCode(candidate.getClassToMove(), candidate.getTargetPackage());
		//check postconditions
		boolean postSuccessful = true;
		for(Postcondition post : posts) {
			if(!post.isGraphLevel()) {
				postSuccessful = post.check(candidate);
				break; //as there is only postcondition
			}
		}
		if(!postSuccessful) {
			result.setCompilation(false);
			//this means the suggested refactoring on graph could not succeed on code.
			printG2CSuccess(getOutputFiles()[4],candidate,false);
			//rollback move refactoring
			String classname = candidate.getClassToMove();
			blacklisted.add(classname);
			if(classname.contains(".")) classname = classname.substring(classname.lastIndexOf(".")+1);
			classname = candidate.getTargetPackage() + "." + classname;
			performMoveOnCode(classname, candidate.getSourcePackage());
			for(Postcondition post : posts) {
				if(!post.isGraphLevel()) {
					postSuccessful = post.check(candidate);
					break; //as there is only postcondition
				}
			}
			if(postSuccessful)
				//this means rollback was successful.
				succeeded = false;
			else {
				//abort the project.
				abort = true;
				printFailedEdge(winner, candidate, getOutputFiles()[3], ((CheckCompilationPostcondition) posts[1]).getErrorMessage());
			}
		} else {
			//postconditions succeeded and refactoring was performed on code successfully.
			result.setCompilation(true);
			printG2CSuccess(getOutputFiles()[4],candidate,true);
			succeeded = true;
		}
		if(succeeded) setFinalCandidate(candidate);
		return succeeded;
	}
	
	private boolean performMoveOnCode(String classToMove, String targetPackage) {
		boolean result = false;
		try {
			IType t = iJavaProject.findType(classToMove);
			if(t == null) { return false;}
			ICompilationUnit icu = t.getCompilationUnit();
			if(icu == null) {return false;}
			IFile source = (IFile)icu.getResource();
			MoveHelper.setIJavaProject(iJavaProject);
			MoveHelper.setPart(iwps);
			MoveHelper.applyMove(source, targetPackage, result);
			
		} catch (JavaModelException e) {
			e.printStackTrace();
			result = false;
		}
		return result;
	}

	public void setIProject(IProject project) {
		iProject = project;
		iJavaProject = JavaCore.create(iProject);
		
	}
	
	private static double getDistance(DirectedGraph<Vertex, Edge> g,
			String sPackage) throws Exception {
		PackageMetrics pm = new PackageMetrics();
		Map<String, PackageMetrics.Metrics> values = pm.compute(g, "");
		PackageMetrics.Metrics pMetrics = values.get(sPackage);
		if(pMetrics == null) return -1.00;
		return pMetrics.getD();
	}
	
	private static void resetGraph(MoveCandidate candidate,
			DirectedGraph<Vertex, Edge> g) {
		resetGraph(candidate.getClassesMoved(),candidate.getSourcePackage());
	}
	private static void resetGraph(List<Vertex> classesMoved, String oldNS) {
		for (Vertex v : classesMoved) {
			v.setNamespace(oldNS);
		}
	}

	public static List<Vertex> performMoveOnGraph(DirectedGraph<Vertex, Edge>g, MoveCandidate c) {
		List<String> classes2Move = new ArrayList<String>();
		String class2Move = c.getClassToMove();
		if(isOuterClass(class2Move)){
			classes2Move.add(class2Move);
			for(String inner : getInnerClasses(class2Move)) {
				classes2Move.add(inner);
			}
		} else {
			//get the outer class and all its inner classes
			String outerclass = getOuterClass(class2Move);
			classes2Move.add(outerclass);
			for(String inner : getInnerClasses(outerclass)) {
				classes2Move.add(inner);
			}
		}
		List<Vertex> verticesToMove = new ArrayList<Vertex>();
		for(String clazz : classes2Move) {
			verticesToMove.add(getVertex(g,clazz));
		}
		for (Vertex toMove : verticesToMove) {
			toMove.setNamespace(c.getTargetPackage());
		}
		return verticesToMove;
	}

	private static Vertex getVertex(DirectedGraph<Vertex, Edge> g, String clazz) {
		for(Vertex v : g.getVertices()) {
			if(v.getFullname().equals(clazz)) return v;
		}
		return null;
	}

	
	/**<pre>
	 * Files are in this order:
	 * 0. instances.csv
	 * 1. details.csv
	 * 2. skipped_edges.csv
	 * 3. error_edge.csv
	 * 4. g2c_success.csv
	 * 5. constraints.csv
	 * </pre>
	 * @return outfiles
	 */
	public static String[] getOutputFiles() {
		return outfiles; 
	}
	public void setOutputFiles(String graphfile) throws IOException {
		outfiles = getOutputFiles(graphfile);
	}
	public String[] getOutputFiles(String graphfile) throws IOException {
		//the path looks like this /Volumes/Data2/PhD/workspaces/corpus2010/marauroa-3.8.1/bin
		String separator = System.getProperty("file.separator");
		//first remove bin from the graph file name
		graphfile = graphfile.substring(0, graphfile.lastIndexOf(separator));
		//now remove the first part to just get the name of the project
		graphfile = graphfile.substring(graphfile.lastIndexOf(separator)+1);
		File outputFolder = new File(WORKSPACE_PATH + "output" + separator + "cd");
		String[] filenames = new String[6];
		String f1 = outputFolder.getPath();
		f1 = f1 + separator + graphfile + "_instances.csv";
		//print header of _instances file
		println(f1,"iteration,instances,instances(%),weakerInstances,weakerInstances(%)");
//		String f2 = outputFolder.getPath();
//		f2 = f2 + separator + graphfile + "_details.csv";
//		//print header of _details file
//		println(f2,"iteration,source,type,target,move,to,cd,src D before," +
//								 "src D after,tar D before,tar D after,edges skipped,time (ms)");
//		String f3 = outputFolder.getPath();
//		f3 = f3 + separator + graphfile + "_skipped_edges.csv";
//		//print header of _skipped edges
//		println(f3,System.getProperty("line.separator"));
//		println(f3,"iteration,source,type,target");
//		String f4 = outputFolder.getPath();
//		f4 = f4 + separator + graphfile + "_error_edge.csv";
//		//print header of _error_edge file
//		println(f4,"source,type,target,class2move,to package,error message");
//		String f5 = outputFolder.getPath();
//		f5 = f5 + separator + graphfile + "_g2c_success.csv";//graph to code level success
//		//print header of _g2c_success file
//		println(f5,"class2move,topackage,result");
//		String f6 = outputFolder.getPath();
//		f6 = f6 + separator + graphfile + "_constraints.csv";
//		//print header of constraints file
//		println(f6, "class2move,topackage,rename,blacklisted,accessability,instance count,compilation,result");
		filenames[0] = f1;
//		filenames[1] = f2;
//		filenames[2] = f3;
//		filenames[3] = f4;
//		filenames[4] = f5;
//		filenames[5] = f6;
		return filenames;
	}

	public static void setIPart(IWorkbenchPartSite site) {
		iwps = site;
	}
	
	public static void setWorkspace(String name) {
		WORKSPACE_PATH = name;
	}

}
