package nz.ac.massey.cs.care.scripts;

import static nz.ac.massey.cs.care.util.MovePrintery.printConstraints;
import static nz.ac.massey.cs.care.util.MovePrintery.printFailedEdge;
import static nz.ac.massey.cs.care.util.MovePrintery.printRefactoredEdgeDetails;
import static nz.ac.massey.cs.care.util.MovePrintery.printRefactoringApplied;
import static nz.ac.massey.cs.care.util.MovePrintery.printRemovalStepStats;
import static nz.ac.massey.cs.care.util.MovePrintery.printSkippedEdges;
import static nz.ac.massey.cs.care.util.MovePrintery.println;
import static nz.ac.massey.cs.care.util.Utils.findLargestByIntRanking;
import static nz.ac.massey.cs.care.util.Utils.loadGraph;
import static nz.ac.massey.cs.care.util.Utils.round;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nz.ac.massey.cs.care.Edge;
import nz.ac.massey.cs.care.Vertex;
import nz.ac.massey.cs.care.ast.DependencyFinder;
import nz.ac.massey.cs.care.dependency.Dependency;
import nz.ac.massey.cs.care.io.GraphMLWriter;
import nz.ac.massey.cs.care.metrics.PackageMetrics;
import nz.ac.massey.cs.care.refactoring.constraints.CheckInnerClass;
import nz.ac.massey.cs.care.refactoring.constraints.MyCompiler;
import nz.ac.massey.cs.care.refactoring.executers.CareRefactoring;
import nz.ac.massey.cs.care.refactoring.executers.MoveClassRefactoring;
import nz.ac.massey.cs.care.refactoring.movehelper.Candidate;
import nz.ac.massey.cs.care.refactoring.movehelper.RefactoringResult;
import nz.ac.massey.cs.care.scoring.DefaultScoringFunction;
import nz.ac.massey.cs.care.scoring.ScoringFunction;
import nz.ac.massey.cs.care.util.MoveUtils;
import nz.ac.massey.cs.care.util.ResultCounter;
import nz.ac.massey.cs.guery.Motif;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.console.MessageConsoleStream;

import com.google.common.base.Function;

import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.io.GraphIOException;
import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.ClassObject;
import gr.uom.java.ast.CompilationUnitCache;

/**
 * Composite refactoring execution process. 
 * @author Ali Shah
 */
public class CompositeExecuter {
	private static int MAX_ITERATIONS = 10; // stop after this number of refactorings have been performed
	private double initialTotalInstances = 0;
	private Set<Edge> useLessEdges = new HashSet<Edge>();
	private Set<Edge> edgesSucceeded = new HashSet<Edge>();
	private static boolean DO_NOT_REMOVE_EDGES_WITH_NULL_SCORE = true;
	public static ScoringFunction scoringfunction = new DefaultScoringFunction();
	private IProject iProject = null;
	private static IWorkbenchPartSite iwps;
	private static List<String> blacklisted = new ArrayList<String>();
	private boolean abort;
	private static List<Motif<Vertex, Edge>> motifs;
	private static String[] outfiles;
	private static String PROJECT_RESULT_DIR = null;
	private static String PROJECT_NAME = null;
	private String DEPEND_DIR_PATH;

	private int instancesBefore;
	private MessageConsoleStream out = null;

	public static class VertexComparator implements Comparator<Vertex> {
		@Override
		public int compare(Vertex o1, Vertex o2) {
			return o1.getFullname().compareTo(o2.getFullname());
		}
	}

	public void execute(DirectedGraph<Vertex, Edge> g, String graphSource, int i, IProgressMonitor monitor)
			throws Exception {

		if(monitor.isCanceled()) {
			return;
		}
		
		long ts1 = System.currentTimeMillis();

		if (i > MAX_ITERATIONS) return; 
		final ResultCounter registry = MoveUtils.countAllInstances(g, getMotifs());
		int allInstances = registry.getNumberOfInstances();
		if (i == 0) {
			initialTotalInstances = allInstances;
		}
		instancesBefore = allInstances;
		Double instancesPercent = (allInstances * 100) / initialTotalInstances;
		if (allInstances == 0) {
			logg("No more instances found at step ", i);
			printRemovalStepStats(iProject, getOutputFiles()[0], i, allInstances, round(instancesPercent));
			return;
		}
		long ts2 = System.currentTimeMillis();
		logg("[Iteration ", i, "] Antipattern Instances ", allInstances, ", Instances Percent ",
				round(instancesPercent), "%, Detection took ",
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
		int edgeCounter = 0;
		for (Edge winner : edgesWithHighestRank) {
			if(monitor.isCanceled()) {
				return;
			}
//		 winner = setupTestEdge();
			System.gc();
			if(alreadyChecked(winner)){
				edgeCounter++;
				continue;
			}
			logg("[Iteration " + i + "] Analyzing Dependency " + winner.getStart().getFullname() + " -> "
					+ winner.getEnd().getFullname());
			int topScore = registry.getCount(winner);
			
			if (topScore == 0 && DO_NOT_REMOVE_EDGES_WITH_NULL_SCORE) {
				logg("only edges with score==0 found, will not remove edges");
				return;
			} else {
				String source = winner.getStart().getFullname();
				String target = winner.getEnd().getFullname();
				// calculate distance from main path before refactoring
				double srcDistanceBefore = round(getDistance(g, winner.getStart().getNamespace()));
				double tarDistanceBefore = round(getDistance(g, winner.getEnd().getNamespace()));
				boolean succeeded = false; 
				try{
					succeeded = performRefactorings(winner, g, graphSource);
				} catch (Exception e) {
					e.printStackTrace();
					continue;
				}
				if(abort) return;
				if (succeeded) {
					g = loadGraph(graphSource);
//					dumpGraph(g,i+1);
					edgesSucceeded.add(winner);
					// recompute distance
					double srcDistanceAfter = round(getDistance(g, winner.getStart().getNamespace()));
					double tarDistanceAfter = round(getDistance(g, winner.getEnd().getNamespace()));

					printRemovalStepStats(iProject, getOutputFiles()[0], i, allInstances,round(instancesPercent));
					printRefactoredEdgeDetails(iProject, getOutputFiles()[1], i + 1, winner, source,target,
							registry.getCount("scd",winner),registry.getCount("awd",winner),registry.getCount("stk", winner),
							registry.getCount("deginh",winner), srcDistanceBefore,srcDistanceAfter, tarDistanceBefore,
							tarDistanceAfter, edgeCounter, ts2 - ts1);
				
					// release tmp variables before recursion
					map.clear();
					refreshProject();
					execute(g, graphSource, i + 1, monitor);
					break;
				} else {
					edgeCounter++;
					useLessEdges.add(winner);
					printSkippedEdges(iProject, getOutputFiles()[2],i,winner);
					continue;
				}
			}
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

	protected Edge setupTestEdge() {
		Vertex s = new Vertex();
		s.setName("TextFigure");
		s.setNamespace("org.jhotdraw.draw");
		Vertex t = new Vertex();
		t.setName("TextEditingTool");
		t.setNamespace("org.jhotdraw.draw.tool");
		Edge e = new Edge("e-new", s, t);
		e.setType("uses");
		return e;
	}
	/**
	 * <pre>
	 * Motifs order is as follows:
	 * SCD, STK, AWD, DEGINH
	 * </pre>
	 * @return
	 */
	public static List<Motif<Vertex, Edge>>getMotifs() {
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
	public boolean performRefactorings(Edge winner,
			DirectedGraph<Vertex, Edge> g, String graphSource) throws Exception {
		boolean succeeded = false;
		
		RefactoringResult result = new RefactoringResult();
		Candidate c = initializeCandidate(winner, g, graphSource);
		CareRefactoring refac = null;
		//First, check if source or target type is anonymous inner class. 
		//If yes, perform the move class refactoring. 
		if(!new CheckInnerClass().isFailed(c)) {
			refac = getMoveRefactoring(c);
			performRefactoring(refac, c, result, g, graphSource, winner);
			printConstraints(iProject, getOutputFiles()[5], refac.getName(), c.getEdgeType(), c);
			printRefactoringApplied(iProject, getOutputFiles()[6], result, c, c.getEdgeType());
			refac.clear();
			succeeded = !result.hasError();
			return succeeded;
		}
		Dependency d = new DependencyFinder(c).compute();
		if(d == null) return false;
		//Second, check if dependency category is one of the following. 
		//If yes, perform the move refactoring
		if(d.getName().equals("Extends") || d.getName().equals("Implements") ||
				d.getName().equals("Other") || d.getName().equals("MET")) {
			refac = d.getRefactoring();
			performRefactoring(refac, c, result, g, graphSource, winner);
			printConstraints(iProject, getOutputFiles()[5], refac.getName(), d.getName(), c);
			printRefactoringApplied(iProject, getOutputFiles()[6], result, c, d.getName());
			refac.clear();
			succeeded = !result.hasError();
			return succeeded; 
		}
		//Third, try other refactorings based on the dependency category like inlining, generalize etc.
		//If they fail, try move refactoring as an alternative.
		refac = d.getRefactoring();
		performRefactoring(refac, c, result, g, graphSource, winner);
		printConstraints(iProject, getOutputFiles()[5], refac.getName(), d.getName(), c);
		if(result.hasError()) {
			//try move
			result.clear();
			refac = getMoveRefactoring(c);
			performRefactoring(refac, c, result, g, graphSource, winner);
			printConstraints(iProject, getOutputFiles()[5], refac.getName(), d.getName(), c);
			printRefactoringApplied(iProject, getOutputFiles()[6], result, c, d.getName());
		} else {
			printRefactoringApplied(iProject, getOutputFiles()[6], result, c, d.getName());
		}
		refac.clear();
		succeeded = !result.hasError();
		return succeeded;
	}
	private MoveClassRefactoring getMoveRefactoring(Candidate c) {
		return new MoveClassRefactoring(c);
	}
	private void performRefactoring(CareRefactoring refac, Candidate c,
			RefactoringResult result, DirectedGraph<Vertex, Edge> g, String graphSource, Edge winner) throws Exception {
		//Check if we are attempting a refactoring for the first time or second time for 
		//one particular dependency. 
		if(result.getFirstAttempted() == null) {
			result.setFirstAttempted(refac);
		} else {
			result.setSecondAttepted(refac);
		}
		//record the source code, if not performing move refactoring
		String sourceBefore = null;
		if (!(refac instanceof MoveClassRefactoring)) {
			 sourceBefore = getSource(c.getSourceClass());
		}
		//First, check refactoring preconditions
		refac.checkPreconditions(result);
		if(!result.hasError()) {
			//Second, perform refactoring tentatively
			refac.perform(result);
			if(!result.hasError()) {
				//Third, check refactoring postconditions
				refac.checkPostconditions(result);
				if (result.hasError()) {
					refac.rollback();
					if (!new MyCompiler(c.getProject()).build().isOK()) {
						// If the rollback doesn't work, we have to abort the project
						abort = true;
						printFailedEdge(iProject,winner, c, getOutputFiles()[3], "");
					}
					refac.clear();
				} else {
					refac.clear();
					result.setFinalRefactoringApplied(refac);
					if (!(refac instanceof MoveClassRefactoring)) {
						configureLogger(winner);
						addOldCodeAppender(winner, sourceBefore);
						addRefacCodeAppender(winner);
					}
				}
			}
		}
	}
	
	private String getSource(String sourceClass) {
		String source = null;
		try {
			source = ASTReader.getExaminedProject().findType(sourceClass).getSource();
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		return source;
	}
	private void configureLogger(Edge winner) throws IOException {
		String separator = System.getProperty("file.separator");
		String dirname = getSimpleName(winner.getStart().getFullname())+"_TO_"+getSimpleName(winner.getEnd().getFullname());
		IFolder outputFolder = getOrAddOutputFolder("output-composite-refactoring");
		IFolder outputSubFolder = getOrAddOutputFolder(outputFolder.getName() + separator + "refactored-code");
		IFolder dependencyFolder = getOrAddOutputFolder(outputFolder.getName() + separator + outputSubFolder.getName() + separator + dirname);
		String dependDirPath =  outputFolder.getName() + separator + outputSubFolder.getName() +
				separator + dependencyFolder.getName();
		DEPEND_DIR_PATH = dependDirPath;
	}
	private IFolder getOrAddOutputFolder(String name) {
		IFolder f = iProject.getFolder(name);
		if(!f.exists())
			try {
				f.create(IResource.NONE, true, null);
			} catch (CoreException e) {
				e.printStackTrace();
			}
		return f;
	}
	public String getSimpleName(String fullname){
		String simpleName = fullname.substring(fullname.lastIndexOf(".")+1);
		return simpleName;
	}
	private void addOldCodeAppender(Edge winner, String sourceBefore) throws IOException {
		String filename = getSimpleName(winner.getStart().getFullname())+"_old.txt";
		String fullpath = DEPEND_DIR_PATH  + "/" + filename;
		println(iProject, fullpath, sourceBefore);
		
	}
	private void addRefacCodeAppender(Edge winner) throws IOException {
		String filename = getSimpleName(winner.getStart().getFullname())+".txt";
		String fullpath = DEPEND_DIR_PATH + "/" + filename;
		//add new code
		String classname = winner.getStart().getFullname();
		classname = classname.replace("$", ".");
		String afterSource = getSource(classname);
		if(afterSource == null) return;
		afterSource = afterSource + "\n" + "//*********************REFACTORED CODE ****************";
		println(iProject, fullpath, afterSource);
	}

	private Candidate initializeCandidate(Edge winner, DirectedGraph<Vertex, Edge> g, String graphSource) {
		System.gc();
		CompilationUnitCache.getInstance().clearCache();
		CompilationUnitCache.getInstance().clearAffectedCompilationUnits();
		Vertex start = winner.getStart();
		Vertex end = winner.getEnd();
		Candidate candidate = new Candidate();
		candidate.setSourceClass(start.getFullname());
		candidate.setTargetClass(end.getFullname());
		candidate.setEdgeType(winner.getType());
		candidate.setEdge(winner);
		candidate.setSourcePackage(start.getNamespace());
		candidate.setTargetPackage(end.getNamespace());
		candidate.setInstancesBefore(instancesBefore);
		candidate.setProject(iProject);
		candidate.setWorkbench(iwps);
		candidate.setMotifs(motifs);
		candidate.setGraph(g);
		candidate.setGraphSource(graphSource);
		loadRequiredASTs(candidate);
		return candidate;
		
	}
	
	private boolean loadRequiredASTs(Candidate c) {
		try {
			ASTReader.getSystemObject().clear();
			IJavaProject p = ASTReader.getExaminedProject();
			String sourceName = c.getSourceClass();
			if(sourceName.contains("$")) {
				sourceName = sourceName.replace("$", ".");
			}
			IType s = p.findType(sourceName);
			if (s == null)
				return false;
			ICompilationUnit source = s.getCompilationUnit();
			String targetName = c.getTargetClass();
			if(targetName.contains("$")) targetName = targetName.replace("$", ".");
			IType target = p.findType(targetName);
			if (source == null || target == null)
				return false;
			ICompilationUnit serviceLocator = p.findType(
					"registry.ServiceLocator").getCompilationUnit();
			
			ASTReader.getSystemObject().addClasses(ASTReader.parseAST(source));
			ASTReader.getSystemObject().addClasses(
					ASTReader.parseAST(serviceLocator));
			ASTReader.getSystemObject().addClasses(
					ASTReader.parseAST(target.getCompilationUnit()));
			ClassObject sourceObject = ASTReader.getSystemObject().getClassObject(sourceName);
			ClassObject targetObject = ASTReader.getSystemObject().getClassObject(targetName);
			c.setSourceClassObject(sourceObject);
			c.setTargetClassObject(targetObject);

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	private  void logg(Object... s) {
		StringBuffer b = new StringBuffer();
		for (Object t:s) {
			b.append(t);
		}
		out.println(b.toString());
		System.out.println(b.toString());
	}

	public static String getSimplename(String cname) {
		return cname.substring(cname.lastIndexOf(".") + 1);
	}
	public void setIProject(IProject project) {
		iProject = project;
	}
	
	private static double getDistance(DirectedGraph<Vertex, Edge> g,
			String sPackage) throws Exception {
		PackageMetrics pm = new PackageMetrics();
		Map<String, PackageMetrics.Metrics> values = pm.compute(g, "");
		PackageMetrics.Metrics pMetrics = values.get(sPackage);
		if(pMetrics == null) return -1.00;
		return pMetrics.getD();
	}
	
	/**<pre>
	 * Files are in this order:
	 * 0. instances.csv
	 * 1. details.csv
	 * 2. skipped_edges.csv
	 * 3. error_edge.csv
	 * 4. g2c_success.csv
	 * 5. constraints.csv
	 * 6. refactorings-applied.csv
	 * </pre>
	 * @return outfiles
	 */
	public static String[] getOutputFiles() {
		return outfiles; 
	}
	public void setOutputFiles(String graphfile) throws IOException {
		try {
			outfiles = getOutputFiles(graphfile);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
	public String[] getOutputFiles(String graphfile) throws IOException, CoreException {
		//the path looks like this /Volumes/Data2/PhD/workspaces/corpus2010/marauroa-3.8.1/bin
		String separator = System.getProperty("file.separator");
		//first remove bin from the graph file name
		graphfile = graphfile.substring(0, graphfile.lastIndexOf(separator));
		//now remove the first part to just get the name of the project
		graphfile = graphfile.substring(graphfile.lastIndexOf(separator)+1);
		IFolder f = getOrAddOutputFolder("output-composite-refactoring");
		IFolder subFolder = getOrAddOutputFolder(f.getName() + separator +"output");
		String outputFolder = f.getName() + separator + subFolder.getName();
		String[] filenames = new String[7];
		String f1 = outputFolder;//.getPath();
		f1 = f1 + separator + graphfile + "_instances.csv";
		//print header of _instances file
		println(iProject, f1,"iteration,instances,instances(%)");
		String f2 = outputFolder;//.getPath();
		f2 = f2 + separator + graphfile + "_details.csv";
		//print header of _details file
		println(iProject, f2,"iteration,source,type,target,cd,awd,stk,deginh,src D before," +
								 "src D after,tar D before,tar D after,edges skipped,time (ms)");
		String f3 = outputFolder;//.getPath();
		f3 = f3 + separator + graphfile + "_skipped_edges.csv";
		//print header of _skipped edges
		println(iProject, f3,"iteration,source,type,target");
		String f4 = outputFolder;//.getPath();
		f4 = f4 + separator + graphfile + "_error_edge.csv";
		//print header of _error_edge file
		println(iProject,f4,"source,type,target,class2move,to package,message");
		String f5 = outputFolder;//;.getPath();
		f5 = f5 + separator + graphfile + "_g2c_success.csv";//graph to code level success
		//print header of _g2c_success file
		String f6 = outputFolder;//.getPath();
		f6 = f6 + separator + graphfile + "_refactoring_constraints.csv";
		//print header of constraints file
		println(iProject, f6, "source,target,dependency,attempted refac,blacklist,accessability,rename,same package,no valid supertype,self instance creation,class boundary violation,compilation,instance count");
		String f7 = outputFolder;
		f7 = f7 + separator + graphfile + "_refactorings_applied.csv";
		println(iProject, f7,"source,target,dependency,1st attempted refac,2nd attempted refac,final refactoring applied,result");
		filenames[0] = f1;
		filenames[1] = f2;
		filenames[2] = f3;
		filenames[3] = f4;
		filenames[4] = f5;
		filenames[5] = f6;
		filenames[6] = f7;
		return filenames;
	}
	public String[] getMetricsOutputFiles(String graphfile) throws IOException, CoreException {
		//the path looks like this /Volumes/Data2/PhD/workspaces/corpus2010/marauroa-3.8.1/bin
		String separator = System.getProperty("file.separator");
		//first remove bin from the graph file name
		graphfile = graphfile.substring(0, graphfile.lastIndexOf(separator));
		//now remove the first part to just get the name of the project
		graphfile = graphfile.substring(graphfile.lastIndexOf(separator)+1);
		IFolder f = getOrAddOutputFolder("output-composite-refactoring");
		IFolder subFolder = getOrAddOutputFolder(f.getName() + separator + "output");
		String outputFolder = f.getName() + separator + subFolder.getName();
		String[] filenames = new String[2];
		String f1 = outputFolder;//outputFolder.getPath();
		f1 = f1 + separator + graphfile + "_metrics.csv";
		//print header of _metrics file
		println(iProject,f1,"compression before,compression after,max scc size before,"+
				"max scc size after,density before,density after,tangledness before," +
				"tangledness after,count before,count after,"+
				"modularity before,modularity after,distance before,distance after," +
				"total time");
		String f2 = outputFolder;//.getPath();
		f2 = f2 + separator + graphfile + "_package_metrics.csv";
		//print header of _distance file
		println(iProject, f2,"package,CE before,CE after,CA before,CA after,A before,A after," +
				"I before,I after,D before,D after");
		filenames[0] = f1;
		filenames[1] = f2;
		return filenames;
	}
	public static void setIPart(IWorkbenchPartSite site) {
		iwps = site;
	}
	
	public static void setMotifs(List<Motif<Vertex, Edge>> motifs2) {
		motifs = motifs2;
		
	}
	public void dumpGraph(DirectedGraph<Vertex, Edge> g, String suffix) {
		try {
			String path = PROJECT_RESULT_DIR + "/graphs";
			File nextOut = new File(path + File.separator + PROJECT_NAME + "_" + suffix + ".graphml");
			Writer out = new BufferedWriter(new FileWriter(nextOut));
			GraphMLWriter w = new GraphMLWriter(out);
			w.writeGraph(g);
			w.close();
		} catch(IOException e) {
			e.printStackTrace();
		} catch (GraphIOException e) {
			e.printStackTrace();
		}
	}

	public static void setMaxSteps(String steps) {
		try{
			MAX_ITERATIONS = Integer.valueOf(steps);
		} catch(NumberFormatException e) {
			MAX_ITERATIONS = 10;
		}
	}
	public void setConsoleStream(MessageConsoleStream out) {
		this.out  = out;
		
	}
	public static int getMaxSteps() {
		return MAX_ITERATIONS;
	}
	
}
