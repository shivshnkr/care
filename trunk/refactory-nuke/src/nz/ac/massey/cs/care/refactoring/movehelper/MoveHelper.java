package nz.ac.massey.cs.care.refactoring.movehelper;

import static nz.ac.massey.cs.care.refactoring.movehelper.Utils.log;
import static nz.ac.massey.cs.care.refactoring.scripts.Utils.*;
import static nz.ac.massey.cs.care.refactoring.scripts.Utils.prepare;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import nz.ac.massey.cs.care.refactoring.manipulators.MyCompiler;
import nz.ac.massey.cs.care.refactoring.views.CareView;
import nz.ac.massey.cs.gql4jung.Edge;
import nz.ac.massey.cs.gql4jung.ResultCounter;
import nz.ac.massey.cs.gql4jung.Vertex;
import nz.ac.massey.cs.guery.ComputationMode;
import nz.ac.massey.cs.guery.Motif;
import nz.ac.massey.cs.guery.PathFinder;
import nz.ac.massey.cs.guery.adapters.jung.JungAdapter;
import nz.ac.massey.cs.guery.impl.BreadthFirstPathFinder;
import nz.ac.massey.cs.guery.impl.MultiThreadedGQLImpl;

import org.apache.commons.collections15.Transformer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.corext.refactoring.reorg.IReorgDestination;
import org.eclipse.jdt.internal.corext.refactoring.reorg.JavaMoveProcessor;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgDestinationFactory;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgPolicyFactory;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgUtils;
import org.eclipse.jdt.internal.corext.refactoring.reorg.IReorgPolicy.IMovePolicy;
import org.eclipse.jdt.internal.ui.refactoring.reorg.CreateTargetQueries;
import org.eclipse.jdt.internal.ui.refactoring.reorg.ReorgQueries;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.participants.MoveRefactoring;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchSite;

import edu.uci.ics.jung.algorithms.scoring.BetweennessCentrality;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.Graph;
import gr.uom.java.ast.ASTReader;

/**
 * Utility to batch process input files. Run with -Xmx1024M
 * 
 * @author Jens, Ali
 */
public class MoveHelper {
	private static final String SEP = ","; // used in csv files
	private static final String NL = System.getProperty("line.separator");
	private static Map<String, String> refactoring = new HashMap<String, String>();
	private static boolean isRenameRequired = false;
	private static int additionalRefacRequired = 0;
	private static MoveCandidateRefactoring candidate = new MoveCandidateRefactoring();
	private static IWorkbenchPartSite iWorkbenchPartSite;
	static Transformer<Vertex,String> componentMembership = new Transformer<Vertex,String>() {
		@Override
		public String transform(Vertex s) {
			return s.getNamespace();//s.substring(0,s.indexOf('.')); // component is first token in name
		}
	};
	public static class VertexComparator implements Comparator<Vertex> {
		@Override
		public int compare(Vertex o1, Vertex o2) {
			// note that the container is ignored for performance reasons - this
			// is ok, there is only one container per program
			return o1.getFullname().compareTo(o2.getFullname());
			
		}
	}

	public static boolean applyMoveRefactoring(Edge winner,
			DirectedGraph<Vertex, Edge> g, List<Motif<Vertex, Edge>> motifs, int totalInstances, IWorkbenchPartSite iwps) {
		iWorkbenchPartSite = iwps;
		boolean succeeded = executeWinner(winner, totalInstances, motifs, g); 
		if(succeeded){
			String classToMove = candidate.getClassToMove();
			candidate.setClassObjectToMove(ASTReader.getSystemObject().getClassObject(classToMove));
			executeMoveRefactoring(candidate, motifs, totalInstances);
		}
		return succeeded;
	}
	private static boolean executeWinner(Edge winner, int initialInstances,
			List<Motif<Vertex, Edge>> motifs, DirectedGraph<Vertex, Edge> g) {
		Vertex s = winner.getStart();
		Vertex t = winner.getEnd();
		// Step1: MoveSrc. try moving s to t package and recompute instances
		String oldNS = s.getNamespace();
		//calculate number of refactorings required if we move source to target package
		int moveSrcRefacRequired = countRefacRequired(g,s,t);
		List<Vertex> classesMoved = moveRefactoring(s, t, g);
		int moveSrcInstances = CareView.countAllInstances(g, motifs).getNumberOfInstances();//getResults(g, motifs).getNumberOfInstances();
		// reset graph
		resetGraph(classesMoved, oldNS);
		classesMoved.clear();
		// Step2: MoveTgt. try move t to s package and recompute instances
		oldNS = t.getNamespace();
		int moveTgtRefacRequired = countRefacRequired(g,t,s);
		classesMoved = moveRefactoring(t, s, g);
		int moveTgtInstances = CareView.countAllInstances(g, motifs).getNumberOfInstances();//getResults(g, motifs).getNumberOfInstances();
		// reset graph
		resetGraph(classesMoved, oldNS);
		classesMoved.clear();
		return chooseRightRefactoring(s, t, g, initialInstances, moveSrcInstances, moveSrcRefacRequired,
				moveTgtInstances, moveTgtRefacRequired);
	}

	
	protected static void executeMoveRefactoring(MoveCandidateRefactoring candidate, List<Motif<Vertex, Edge>> motifs, int totalInstances ) {
		IJavaProject p = ASTReader.getExaminedProject();
		IFile sourceFile = candidate.getClassObjectToMove().getIFile();
		if(sourceFile == null) return;
		applyMove(sourceFile, candidate.getTargetPackage());
		
//		ICompilationUnit source = p.findType(winner.getStart().getFullname()).getCompilationUnit();
		MyCompiler compiler = new MyCompiler(p.getProject());
		IStatus status1 = compiler.build(p.getProject());
//		IPath wp = p.getProject().getWorkspace().getRoot().getLocation();
//		String binFolder = wp.toOSString() + p.getProject().getFullPath().toOSString() + "/bin/";
//		File outputPath = new File(binFolder);
//		DirectedGraph<Vertex, Edge> g = null;
//		int instancesAfter = Integer.MAX_VALUE;
//		try {
//			g = loadGraph(outputPath.getAbsolutePath());
//			prepare(g);
//			instancesAfter = CareView.countAllInstances(g, motifs).getNumberOfInstances();//getResults(g, motifs).getNumberOfInstances();
//			
//		} catch (Exception e1) {
//			e1.printStackTrace();
//		}
		if(!status1.isOK() ) {//|| totalInstances < instancesAfter) {
			//rollback move refactoring
			String classname = candidate.getClassToMove();
			if(classname.contains(".")) classname = classname.substring(classname.lastIndexOf(".")+1);
			classname = candidate.getTargetPackage() + "." + classname;
			try {
				IFile source = (IFile)p.findType(classname).getCompilationUnit().getResource();
				applyMove(source, candidate.getSourcePackage());
				
			} catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
	}
	@SuppressWarnings({ "restriction", "unchecked" })
	private static void applyMove(IFile sourceFile, String targetPackage) {
		// TODO Auto-generated method stub
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
				IReorgDestination dest = getContainer(targetPackage);
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
		}catch (OperationCanceledException e) {
			e.printStackTrace();
		} catch (CoreException e) {
			e.printStackTrace();
		}
		
	}
	private static IReorgDestination getContainer(String targetPackage) {
		try {
			IPackageFragment[] packages = ASTReader.getExaminedProject().getPackageFragments();
			for (IPackageFragment mypackage : packages) {
				if(mypackage.getElementName().equals(targetPackage)){
					return ReorgDestinationFactory.createDestination(mypackage);
				}
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	private static int countRefacRequired(DirectedGraph<Vertex, Edge> g, Vertex s, Vertex t) {
		int counter = 0;
		String className = null;
		if(s.getFullname().contains("$")) {
			String fullClassName = s.getFullname();
			className = fullClassName.substring(0,fullClassName.lastIndexOf("$"));
			
		} else className = s.getFullname();
		//We do not want to count inner classes as additional refactorings. 
		String nameToCompare = className + "$";
		for(Edge e : s.getOutEdges()) {
			Vertex end = e.getEnd();
			String endName = end.getFullname();
			if(end.getNamespace().equals(s.getNamespace()) && 
					!endName.startsWith(nameToCompare)) {
				if(!end.isPublic()) counter++;
			}
		}
		//check whether a rename refactoring is required. 
		if(!passPreconditions(t.getNamespace() + "." + s.getName(), g)) counter++;
		return counter;
	}

	private static void resetGraph(List<Vertex> classesMoved, String oldNS) {
		for (Vertex v : classesMoved) {
			v.setNamespace(oldNS);
		}
	}

	// Move(s,t.namespace)
	public static List<Vertex> moveRefactoring(Vertex s, Vertex t,
			DirectedGraph<Vertex, Edge> g) {
		String className = s.getFullname();
		if (!className.contains("$")) {
			List<Vertex> classesToMove = getInnerClasses(className, g);
			classesToMove.add(s);
			for (Vertex toMove : classesToMove) {
				toMove.setNamespace(t.getNamespace());
			}
			return classesToMove;
		} else {
			className = className.substring(0, className.lastIndexOf("$"));
			Vertex outerClass = getOuterClass(className, g);
			if(outerClass!=null){
				List<Vertex> classesToMove = getInnerClasses(
						outerClass.getFullname(), g);
				classesToMove.add(s);
				for (Vertex toMove : classesToMove) {
					toMove.setNamespace(t.getNamespace());
				}
				return classesToMove;
			} else return new ArrayList<Vertex>();
			
		}
	}

	private static Vertex getOuterClass(String className,
			DirectedGraph<Vertex, Edge> g) {
		for (Vertex v : g.getVertices()) {
			if (v.getFullname().equals(className))
				return v;
		}
		return null;
	}

	private static List<Vertex> getInnerClasses(String className,
			DirectedGraph<Vertex, Edge> g) {
		String classToCompare = className + "$"; //inner classes have $ in their name. 
		List<Vertex> innerClasses = new ArrayList<Vertex>();
		for (Vertex v : g.getVertices()) {
			if (v.getFullname().startsWith(classToCompare))
				innerClasses.add(v);
		}
		return innerClasses;
	}

	private static boolean chooseRightRefactoring(Vertex s, Vertex t,
			DirectedGraph<Vertex, Edge> g, int instances, int moveSrcInstances,
			int moveSrcRefacRequired, int moveTgtInstances, int moveTgtRefacRequired) {

		int refactoring = Criteria.choose(instances, moveSrcInstances, moveSrcRefacRequired, moveTgtInstances, moveTgtRefacRequired);
		if(refactoring == 0) {
			log("None of the refactoring was performed.");
			return false;
		}
		else if(refactoring == 12){
			log("Refactoring performed: Move the Source class to the Target class's Package.");
			candidate.setClassToMove(s.getFullname());
			candidate.setSourcePackage(s.getNamespace());
			candidate.setTargetPackage(t.getNamespace());
			recordAdditionalInfo(g,s,t,moveSrcRefacRequired);
			moveRefactoring(s, t, g);
			return true;
		}
		else if(refactoring == 21){
			candidate.setClassToMove(t.getFullname());
			candidate.setSourcePackage(t.getNamespace());
			candidate.setTargetPackage(s.getNamespace());
			log("Refactoring performed: Move the Target class to the Source class's Package.");
			recordAdditionalInfo(g,t,s,moveTgtRefacRequired);
			moveRefactoring(t, s, g);
			return true;
		}
		else return false;
	}

	private static void recordAdditionalInfo(DirectedGraph<Vertex, Edge> g,
			Vertex s, Vertex t, int refacRequired) {
		refactoring.put(s.getFullname(), t.getNamespace());
		isRenameRequired = !passPreconditions(t.getNamespace() + "." + s.getName(), g);
		additionalRefacRequired = isRenameRequired ? refacRequired -1 : refacRequired;
		
	}

	private static boolean passPreconditions(String targetVertexName,
			DirectedGraph<Vertex, Edge> g) {
		for (Vertex v : g.getVertices()) {
			if (v.getFullname().equals(targetVertexName))
				return false;
		}
		return true;
	}

	private static ResultCounter getResults(DirectedGraph<Vertex, Edge> g,
			List<Motif<Vertex, Edge>> motifs) {
		MultiThreadedGQLImpl<Vertex, Edge> engine = new MultiThreadedGQLImpl<Vertex, Edge>();
		PathFinder<Vertex, Edge> pFinder = new BreadthFirstPathFinder<Vertex, Edge>(
				true);

		final ResultCounter registry = new ResultCounter();

		for (Motif<Vertex, Edge> motif : motifs) {
			engine.query(new JungAdapter<Vertex, Edge>(g), motif, registry,
					ComputationMode.CLASSES_NOT_REDUCED, pFinder);
		}
		return registry;
	}

	private static Double round(Double val) {
		if ("NaN".equals(val.toString()))
			return new Double(-1);
		int decimalPlace = 2;
		try{
			BigDecimal bd = new BigDecimal(val.doubleValue());
			bd = bd.setScale(decimalPlace, BigDecimal.ROUND_UP);
			return bd.doubleValue();
		}catch(Exception e){
			return val;
		}
		
	}

	static Map<Edge, Double> getSortedBetwValues(Graph<Vertex, Edge> g) {
		final BetweennessCentrality<Vertex, Edge> edgeBetweenness = new BetweennessCentrality<Vertex, Edge>(
				g);
		final Map<Edge, Double> betwMap = new LinkedHashMap<Edge, Double>();
		// computing edge betweenness.
		for (Edge e : g.getEdges()) {
			betwMap.put(e, edgeBetweenness.getEdgeScore(e));
		}
		// sort betweenness map by values
		List<Map.Entry<Edge, Double>> list = new LinkedList<Map.Entry<Edge, Double>>(
				betwMap.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<Edge, Double>>() {
			@Override
			public int compare(Entry<Edge, Double> o1, Entry<Edge, Double> o2) {
				return -o1.getValue().compareTo(o2.getValue());
			}
		});
		Map<Edge, Double> result = new LinkedHashMap<Edge, Double>();
		for (Iterator<Map.Entry<Edge, Double>> it = list.iterator(); it
				.hasNext();) {
			Map.Entry<Edge, Double> entry = it.next();
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}

	// rank according to betweenness position in the map
	static Map<Edge, Integer> rankBetwValues(Map<Edge, Double> map) {
		Integer count = 0;
		Double previous = 0.0;
		Map<Edge, Integer> result = new LinkedHashMap<Edge, Integer>();
		for (Map.Entry<Edge, Double> entry : map.entrySet()) {
			if (!entry.getValue().equals(previous)) {
				result.put(entry.getKey(), ++count);
				previous = entry.getValue();
			} else {
				result.put(entry.getKey(), count);
				previous = entry.getValue();
			}
		}
		return result;
	}
	public static void printSCCInfoHeader(String filename) throws IOException {
		FileWriter out = new FileWriter(filename, true);
		StringBuffer b = new StringBuffer()
				.append("compression before").append(SEP)
				.append("compression after")
				.append(SEP).append("Max SCC size before").append(SEP)
				.append("Max SCC size after")
				.append(SEP).append("density before")
				.append(SEP).append("density after")
				.append(SEP).append("spl before")
				.append(SEP).append("spl after")
				.append(SEP).append("count before")
				.append(SEP).append("count after")
				.append(NL);
		out.write(b.toString());
		out.close();
		// log("result summary added to ",filename);
	}
	public static void printSCCInfo(String filename, SCCMetrics before, SCCMetrics after) throws IOException {
		FileWriter out = new FileWriter(filename, true);
		StringBuffer b = new StringBuffer()
				.append(before.getCompressionRatio()).append(SEP)
				.append(after.getCompressionRatio())
				.append(SEP).append(before.getMaxSccSize()).append(SEP)
				.append(after.getMaxSccSize())
				.append(SEP).append(before.getDensity())
				.append(SEP).append(after.getDensity())
				.append(SEP).append(before.getTangledness())
				.append(SEP).append(after.getTangledness())
				.append(SEP).append(before.getCount())
				.append(SEP).append(after.getCount())
				.append(NL);
		out.write(b.toString());
		out.close();
		// log("result summary added to ",filename);
	}
	
	/**
	 * Normalize the graph to make the outcomes of algorithms search for paths
	 * satisfying certain conditions predictable. I.e., this means sorting
	 * incoming/outgoing edges.
	 * 
	 * @param g
	 * @return
	 * @throws Exception
	 */
	public static void prepare(DirectedGraph<Vertex, Edge> g) throws Exception {
		long before = System.currentTimeMillis();
		log("Start normalising graph - sort incoming/outgoing edges");

		final BetweennessCentrality<Vertex, Edge> edgeBetweenness = new BetweennessCentrality<Vertex, Edge>(
				g);
		Comparator<Edge> comp = new Comparator<Edge>() {
			@Override
			public int compare(Edge e1, Edge e2) {
				// compare betweenness scores first
				double d = edgeBetweenness.getEdgeScore(e1)
						- edgeBetweenness.getEdgeScore(e2);
				if (d != 0) {
					return d < 0 ? -1 : 1;
				}
				// compare fully qualified names
				String n1 = getFullName(e1);
				String n2 = getFullName(e2);
				return n1.compareTo(n2);
			}

			private String getFullName(Edge e) {
				return new StringBuffer().append(e.getStart().getFullname())
						.append(">").append(e.getEnd().getFullname())
						.toString();
			}
		};

		for (Vertex v : g.getVertices()) {
			v.sortEdges(comp);
		}

		log("Graph normalised, this took "
				+ (System.currentTimeMillis() - before), "ms");
	}

}
