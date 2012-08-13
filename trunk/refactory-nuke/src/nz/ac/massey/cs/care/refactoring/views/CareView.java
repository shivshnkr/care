package nz.ac.massey.cs.care.refactoring.views;

import static nz.ac.massey.cs.care.refactoring.scripts.Utils.findLargestByIntRanking;
import static nz.ac.massey.cs.care.refactoring.scripts.Utils.getExcludeHiddenFilesFilter;
import static nz.ac.massey.cs.care.refactoring.scripts.Utils.getOrAddFolder;
import static nz.ac.massey.cs.care.refactoring.scripts.Utils.loadGraph;
import static nz.ac.massey.cs.care.refactoring.scripts.Utils.loadPowerGraph;
import static nz.ac.massey.cs.care.refactoring.scripts.Utils.prepare;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.Graph;
import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.ClassObject;
import gr.uom.java.ast.CompilationUnitCache;
import gr.uom.java.jdeodorant.refactoring.views.ElementChangedListener;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import nz.ac.massey.cs.care.ast.ASTUtils;
import nz.ac.massey.cs.care.ast.DependencyFinder;
import nz.ac.massey.cs.care.refactoring.manipulators.AntRunner1;
import nz.ac.massey.cs.care.refactoring.manipulators.MyCompiler;
import nz.ac.massey.cs.care.refactoring.manipulators.Postconditions;
import nz.ac.massey.cs.care.refactoring.metrics.PackageMetrics;
import nz.ac.massey.cs.care.refactoring.metrics.SCCMetrics;
import nz.ac.massey.cs.care.refactoring.movehelper.MoveHelper;
import nz.ac.massey.cs.care.refactoring.scripts.Analyser;
import nz.ac.massey.cs.care.refactoring.executers.GeneralizeRefactoring;
import nz.ac.massey.cs.care.refactoring.executers.OIRefactoring;
import nz.ac.massey.cs.care.refactoring.executers.SLPlusOIRefactoring;
import nz.ac.massey.cs.care.refactoring.executers.SLRefactoring;
import nz.ac.massey.cs.care.refactoring.executers.TGPlusOIRefactoring;
import nz.ac.massey.cs.care.refactoring.executers.TGPlusSLPlusOIRefactoring;
import nz.ac.massey.cs.care.refactoring.executers.TGPlusSLRefactoring;
import nz.ac.massey.cs.gql4jung.DefaultScoringFunction;
import nz.ac.massey.cs.gql4jung.E;
import nz.ac.massey.cs.gql4jung.Edge;
import nz.ac.massey.cs.gql4jung.ResultCounter;
import nz.ac.massey.cs.gql4jung.ScoringFunction;
import nz.ac.massey.cs.gql4jung.V;
import nz.ac.massey.cs.gql4jung.Vertex;
import nz.ac.massey.cs.guery.ComputationMode;
import nz.ac.massey.cs.guery.Motif;
import nz.ac.massey.cs.guery.MotifReader;
import nz.ac.massey.cs.guery.PathFinder;
import nz.ac.massey.cs.guery.adapters.jung.JungAdapter;
import nz.ac.massey.cs.guery.impl.BreadthFirstPathFinder;
import nz.ac.massey.cs.guery.impl.MultiThreadedGQLImpl;
import nz.ac.massey.cs.guery.io.dsl.DefaultMotifReader;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.IProgressService;

import au.com.bytecode.opencsv.CSVReader;

import com.google.common.base.Function;

public class CareView extends ViewPart{
	
	private TableViewer tableViewer;
	private Action startIntegratedRefactoringsAction;
	private Action testRefactoringAction;
	private static String PROJECT_NAME = null;
	private static IJavaProject selectedProject;
	private static Logger LOGGER = Logger.getLogger("batch-script");
	private static final String WORKSPACE_PATH = "/Volumes/Data2/PhD/workspaces/CARE/refactory-nuke/";
	private static final String RESULTS_FOLDER = WORKSPACE_PATH + "results";
	private static final String PROJECTS_TODO =  WORKSPACE_PATH + "projects-todo";
	private static final String PROJECTS_DONE =  WORKSPACE_PATH + "projects-done";
	private static String PROJECT_RESULT_DIR = null;
	private static final String QUERY_FOLDER = WORKSPACE_PATH + "queries";
	private static String PROJECT_REFCODE_DIR = null;
	private static String PROJECT_OUTPUT_DIR = null;
	private String DEPEND_DIR_PATH;
	private String DETEAIL_RESULT_PATH;
	private static List<Edge> edgesSucceeded = new ArrayList<Edge>();
	private static final String SEP = ",";
	private static final String NL = System.getProperty("line.separator");
	public static boolean MOVE_DONE = true;
	private static final int MAX_ITERATIONS = 50; // stop after this number of edges have been removed
	private static String OverviewFilename = null;
	public static ScoringFunction scoringfunction = new DefaultScoringFunction();
	private static List<Edge> useLessEdges = new ArrayList<Edge>();
	private static int totalInstancesBefore = 0;//at the beginning of the program
	private static int totalInstancesAfter = 0;//at the end of the program
	private int counter1 = 0; //total no.of declaration elements generalized
	private int counter2 = 0; //total no. of DIs used
	private int iterationCounter1 = 0; //counter1 for one iteration
	private int iterationCounter2 = 0; //counter2 for one iteration
	private List<Motif<Vertex, Edge>> motifs = null;
	private int totalAttempted = 0;
	private static String refactoringTypeApplied = null;
	@Override
	public void createPartControl(Composite parent) {
		tableViewer = new TableViewer(parent, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
		tableViewer.setContentProvider(new ViewContentProvider());
		tableViewer.setLabelProvider(new ViewLabelProvider());
		tableViewer.setSorter(null);
		tableViewer.setInput(getViewSite());
		TableLayout layout = new TableLayout();
		layout.addColumnData(new ColumnWeightData(20, true));
		layout.addColumnData(new ColumnWeightData(60, true));
		layout.addColumnData(new ColumnWeightData(30, true));
		layout.addColumnData(new ColumnWeightData(30, true));
		tableViewer.getTable().setLayout(layout);
		tableViewer.getTable().setLinesVisible(true);
		tableViewer.getTable().setHeaderVisible(true);
		TableColumn column0 = new TableColumn(tableViewer.getTable(),SWT.LEFT);
		column0.setText("Refactoring Type");
		column0.setResizable(true);
		column0.pack();
		TableColumn column1 = new TableColumn(tableViewer.getTable(),SWT.LEFT);
		column1.setText("Source Class");
		column1.setResizable(true);
		column1.pack();
		TableColumn column2 = new TableColumn(tableViewer.getTable(),SWT.LEFT);
		column2.setText("Target Class");
		column2.setResizable(true);
		column2.pack();
		
		tableViewer.setColumnProperties(new String[] {"type", "source", "target"});
		tableViewer.setCellEditors(new CellEditor[] {
				new TextCellEditor(), new TextCellEditor(), new TextCellEditor(), new TextCellEditor(),
				new MyComboBoxCellEditor(tableViewer.getTable(), new String[] {"0", "1", "2"}, SWT.READ_ONLY)
		});
		makeActions();
		contributeToActionBars(); 
		JavaCore.addElementChangedListener(new ElementChangedListener(),ElementChangedEvent.POST_CHANGE);
	}

	private void executeJustAnalyser(IProgressMonitor monitor) {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		File output = getOrAddFolder("output/");
		File[] projectFiles = getProjectFiles(monitor);
		for (File projectFile : projectFiles) {
			IProject project = root.getProject(projectFile.getName());
			String name = project.getName();
			System.out.println(name);
			selectedProject = JavaCore.create(project);
			try {
				selectedProject.getProject().build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
				IPath wp = project.getWorkspace().getRoot().getLocation();
				String binFolder = wp.toOSString() + project.getFullPath().toOSString() + "/bin/";
				File outputPath = new File(binFolder);
				DirectedGraph<Vertex, Edge> g = null;
				loadMotifs();
				g = loadGraph(outputPath.getAbsolutePath());
				prepare(g);
				Analyser.analyse(g, name, 0, motifs);
				
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			if (MOVE_DONE) {
				File usedDatefile = new File(PROJECTS_DONE + "/" + project.getName());
				try {
					org.apache.commons.io.FileUtils.moveFile(projectFile,usedDatefile);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			monitor.worked(1);
			System.gc();
		}
		if (monitor != null)
			monitor.done();
	}
	private void makeActions() {
		testRefactoringAction = new Action() {
			public void run() {
				try {
					IWorkbench wb = PlatformUI.getWorkbench();
					IProgressService ps = wb.getProgressService();
						ps.busyCursorWhile(new IRunnableWithProgress() {
							public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
								executeJustAnalyser(monitor);
							}
						});
						MessageBox mb = new MessageBox(getSite().getWorkbenchWindow().getShell(),SWT.ICON_INFORMATION);
						mb.setMessage("Analysed all projects");
						mb.setText("Status");
						mb.open();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		};
		testRefactoringAction.setToolTipText("Test a Refactorings");
		testRefactoringAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
			getImageDescriptor(ISharedImages.IMG_OBJ_ADD));
		testRefactoringAction.setEnabled(true);
		
		startIntegratedRefactoringsAction = new Action() {
			public void run() {
				try {
					IWorkbench wb = PlatformUI.getWorkbench();
					IProgressService ps = wb.getProgressService();
						ps.busyCursorWhile(new IRunnableWithProgress() {
							public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
								executeAllRefactorings(monitor);
							}
						});
						MessageBox mb = new MessageBox(getSite().getWorkbenchWindow().getShell(),SWT.ICON_INFORMATION);
						mb.setMessage("All possible refactorings applied");
						mb.setText("Status");
						mb.open();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		};
		startIntegratedRefactoringsAction.setToolTipText("Apply All Refactorings");
		startIntegratedRefactoringsAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
			getImageDescriptor(ISharedImages.IMG_DEF_VIEW));
		startIntegratedRefactoringsAction.setEnabled(true);
	}
	private File[] getProjectFiles(IProgressMonitor monitor) {
		File projectsDir = new File(PROJECTS_TODO);
		if (monitor != null)
			monitor.beginTask("Started performing refactorings",projectsDir.listFiles().length);
		File[] projectFiles = projectsDir.listFiles(new FileFilter(){

			@Override
			public boolean accept(File file) {
				return !file.getName().endsWith(".svn");
			}
		});
		return projectFiles;
	}
	private void loadMotifs(){
		File qFolder = new File(QUERY_FOLDER);
		File[] queryFiles = qFolder
				.listFiles(getExcludeHiddenFilesFilter());
		List<Motif<Vertex, Edge>> motifs = new ArrayList<Motif<Vertex, Edge>>();
		for (int i = 0; i < queryFiles.length; i++) {
			File f = queryFiles[i];
			Motif<Vertex, Edge> m;
			try {
				m = loadMotif(QUERY_FOLDER+"/"+f.getName());
				if (m != null)
					motifs.add(m);
				this.motifs = motifs;
			} catch (Exception e) {
				System.out.println("could not load motif files");
				
			}
		}
		
	}
	protected void executeAllRefactorings(IProgressMonitor monitor) {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		File[] projectFiles = getProjectFiles(monitor);
		for (File projectFile : projectFiles) {
			long t1 = System.currentTimeMillis();
			IProject project = root.getProject(projectFile.getName());
			String name = project.getName();
			System.out.println(name);
			try {
				configureLoggingFolders(project.getName());
			} catch (IOException e) {
				e.printStackTrace();
			}
			selectedProject = JavaCore.create(project);
			try {
				selectedProject.getProject().build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
				IPath wp = project.getWorkspace().getRoot().getLocation();
				String binFolder = wp.toOSString() + project.getFullPath().toOSString() + "/bin/";
				File outputPath = new File(binFolder);
				DirectedGraph<Vertex, Edge> g = null;
				ASTUtils.createFactoryDeclaration(selectedProject);
				loadMotifs();
				g = loadGraph(outputPath.getAbsolutePath());
				prepare(g);
				analyse(g,outputPath.getAbsolutePath(),0,motifs);
				long t2 = System.currentTimeMillis();
				printOverview(OverviewFilename, totalAttempted, edgesSucceeded.size(), totalInstancesBefore, totalInstancesAfter, t2-t1);
				
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			if (MOVE_DONE) {
				File usedDatefile = new File(PROJECTS_DONE + "/" + project.getName());
				try {
					org.apache.commons.io.FileUtils.moveFile(projectFile,usedDatefile);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			useLessEdges.clear();
			edgesSucceeded.clear();
			totalAttempted = 0;
			counter1 = 0;
			counter2 = 0;
			monitor.worked(1);
			System.gc();
		}
		if (monitor != null)
			monitor.done();
	}
	private void printOverview(String overviewFilename2, int attempted, int successful,
			int totalInstancesBefore2, int totalInstancesAfter2, long l) {
		try {
			FileWriter out = new FileWriter(overviewFilename2, true);
			Double percentRemoved = 100.0 - (double) ((totalInstancesAfter2 * 100) / totalInstancesBefore2);
			StringBuffer b = new StringBuffer()
					.append(attempted).append(SEP)
					.append(successful).append(SEP)
					.append(totalInstancesBefore2).append(SEP)
					.append(totalInstancesAfter2).append(SEP)
					.append(percentRemoved).append(SEP)
					.append(l).append(NL);
			out.write(b.toString());
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static Motif<Vertex, Edge> loadMotif(String name) throws Exception {
		MotifReader<Vertex, Edge> motifReader = new DefaultMotifReader<Vertex, Edge>();
		InputStream in = new FileInputStream(name);
		Motif<Vertex, Edge> motif = motifReader.read(in);
		in.close();
		return motif;
	}


	private void printDistanceInfo(String filename, double before,
			double after) throws IOException {
		FileWriter out = new FileWriter(filename, true);
		StringBuffer b = new StringBuffer()
				.append(before).append(SEP)
				.append(after)
				.append(NL);
		out.write(b.toString());
		out.close();
	}
	
	private void printModularityInfoHeader(String filename) throws IOException {
		FileWriter out = new FileWriter(filename, true);
		StringBuffer b = new StringBuffer()
				.append("modularity before").append(SEP)
				.append("modularity after").append(SEP)
				.append("types generalised").append(SEP)
				.append("DI used")
				.append(NL);
		out.write(b.toString());
		out.close();
		
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
	
	public static void printDistanceInfoHeader(String filename) throws IOException {
		FileWriter out = new FileWriter(filename, true);
		StringBuffer b = new StringBuffer()
				.append("D before").append(SEP)
				.append("D after")
				.append(NL);
		out.write(b.toString());
		out.close();
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
	
	private static double getDistance(DirectedGraph<Vertex, Edge> g) throws Exception {
		Graph<V,E> pg = loadPowerGraph(g);
		PackageMetrics pm = new PackageMetrics();
		Map<String, PackageMetrics.Metrics> values = pm.compute(g, "");
		double avgDistance = 0.0;
		for(V p : pg.getVertices()) {
			PackageMetrics.Metrics pMetrics = values.get(p.getName());
			avgDistance = avgDistance + pMetrics.getD();
		}
		
		return avgDistance/pg.getVertices().size();
	}
	
	private static boolean alreadyChecked(Edge winner) {
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
	
	public void analyse(DirectedGraph<Vertex, Edge> g,
			String graphSource, int i, List<Motif<Vertex, Edge>> motifs)
			throws Exception {

		long ts1 = System.currentTimeMillis();

		if (i > MAX_ITERATIONS)
			return; // only check the first 50 iterations
		g = loadGraph(graphSource);
		prepare(g);
		//remove MyFactory incoming and outgoing edges.
		removeFactoryEdges(g);
		final ResultCounter registry = countAllInstances(g, motifs); 
		// find edge with highest rank
		int instances = registry.getNumberOfInstances();
		File outputFolder = new File(RESULTS_FOLDER + "/" + selectedProject.getElementName() + "/output");
		if (i == 0) {
			totalInstancesBefore = instances;
		}
		totalInstancesAfter = instances;
		Double instPercent = (double) ((instances * 100) / totalInstancesBefore);
		logg("Total Instances = " + registry.getNumberOfInstances()+ ", " + round(instPercent) +"%");
		String[] outfiles = getOutputFiles(graphSource, outputFolder);
		OverviewFilename = outfiles[4]; 
		if (registry.getNumberOfInstances() == 0) {
			log("No more instances found at step ", i);
			printRemovalStepStats(outfiles[0], i, instances,
					round(instPercent));
			return;
		}
		List<Edge> edgesWithHighestRank = findLargestByIntRanking(g.getEdges(),
				new Function<Edge, Integer>() {
					@Override
					public Integer apply(Edge e) {
						return registry.getCount(e);
					}
				});
		int edgeCounter = 0;
		for (Edge winner : edgesWithHighestRank) {
			
//			winner = setupTestEdge();
			
			if(alreadyChecked(winner)){
				edgeCounter++;
				if(!hasHeader(outfiles[2])) println(outfiles[2],"iteration,source,type,target");
				printSkippedEdges(outfiles[2],i,winner);
				continue;
			}
			totalAttempted  ++;
			int topScore = registry.getCount(winner);
			logg("Iteration " + i + ". Attempting " + winner.getStart().getFullname() + " > "
					+ winner.getEnd().getFullname() +": Top Score is: " + topScore);
			if (topScore == 0) {
				logg("only edges with score==0 found, will not remove edges"); break;
			} else {
				DETEAIL_RESULT_PATH = PROJECT_OUTPUT_DIR + "/"	+ selectedProject.getProject().getName() + ".csv";
				boolean succeeded = executeIntegratedRefactoring(winner,g,motifs, registry.getNumberOfInstances());
				if(succeeded) {
					selectedProject.getProject().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, new NullProgressMonitor());
					edgesSucceeded.add(winner);
					long ts2 = System.currentTimeMillis();

//					logg("Iteration "+ i+ ", instances "+ instances+ ", instances "+
//							round(instPercent)+ "%, detection took "+
//							(ts2 - ts1) / 1000+ "s");

					if (i == 0) {
						// first line with table headers
						println(outfiles[0],
								"counter,instances,instances(%)");
						println(outfiles[1],
								"iteration,source,type,target, scd removed, stk removed, awd removed, deginh removed, edges skipped, TG, SL, SMI, Move, time (ms)");
						if(!hasHeader(outfiles[2])) println(outfiles[2],"iteration,source,type,target");
						printHeader(DETEAIL_RESULT_PATH);
						println(outfiles[3],"iteration, refactoring");
						println(outfiles[4],"attempted, successful, instances before, instances after, percent removed, time(ms)");
						
					}
					printRemovalStepStats(outfiles[0], i, instances,round(instPercent));
					printRefactoredEdgeDetails(outfiles[1], i + 1, winner, registry.getCount("cd", winner), registry.getCount("stk", winner), registry.getCount("awd", winner), registry.getCount("deginh", winner), edgeCounter, 0,0,0,0, ts2 - ts1);
					if(refactoringTypeApplied != null) printAppliedRefactoringType(outfiles[3], i+1);
					// release tmp variables before recursing
					refactoringTypeApplied = null;
					analyse(g,graphSource,i+1, motifs);
					refreshProject();
					break;
				}
				else {
					edgeCounter++;
					useLessEdges.add(winner);
					if(!hasHeader(outfiles[2])) println(outfiles[2],"iteration,source,type,target");
					printSkippedEdges(outfiles[2],i,winner);
					refreshProject();
					continue;
					
				}
			}
		}
	}
	private Edge setupTestEdge() {
		Vertex s = new Vertex();
		s.setName("A");
		s.setNamespace("a");
		Vertex t = new Vertex();
		t.setName("B$Inner");
		t.setNamespace("b");
		Edge e = new Edge("e-new", s, t);
		e.setType("extends");
		return e;
	}

	private static void printAppliedRefactoringType(String filename, int i) throws IOException {
		FileWriter out = new FileWriter(filename, true);
		StringBuffer b = new StringBuffer().append(i).append(SEP)
				.append(refactoringTypeApplied)
				.append(NL);
		out.write(b.toString());
		out.close();
		// log("result summary added to ",filename);
	}
	private static void printRefactoredEdgeDetails(String filename, int i, Edge winner, int scdRemoved, int stkRemoved, int awdRemoved, int deginhRemoved, int edgeCounter, int tg, int sl, int smi, int move, long time)
			throws IOException {
		FileWriter out = new FileWriter(filename, true);

		StringBuffer b = new StringBuffer().append(i).append(SEP)
				.append(winner.getStart().getFullname()).append(SEP).append(winner.getType())
				.append(SEP).append(winner.getEnd().getFullname()).append(SEP)
				.append(scdRemoved)
				.append(SEP)
				.append(stkRemoved)
				.append(SEP)
				.append(awdRemoved)
				.append(SEP)
				.append(deginhRemoved)
				.append(SEP)
				.append(edgeCounter)
				.append(SEP)
				.append(tg)
				.append(SEP)
				.append(sl)
				.append(SEP)
				.append(smi)
				.append(SEP)
				.append(move)
				.append(SEP)
				.append(time)
				.append(NL);
		out.write(b.toString());
		out.close();
	}
	private static boolean hasHeader(String filename) {
		try {
			File f = new File(filename);
			CSVReader reader = new CSVReader(new FileReader(f));
			String[] nextLine= reader.readNext();
			if(nextLine[0].equals("iteration")) return true; 
			else return false;
		} catch (FileNotFoundException e) {
			return false;
		} catch (IOException e) {
			return false;
		}
	}
	private static void printSkippedEdges(String filename, int i,Edge winner) throws IOException {
		FileWriter out = new FileWriter(filename, true);
		StringBuffer b = new StringBuffer().append(i).append(SEP)
				.append(winner.getStart().getFullname())
				.append(SEP).append(winner.getType()).append(SEP)
				.append(winner.getEnd().getFullname())
				.append(NL);
		out.write(b.toString());
		out.close();
		// log("result summary added to ",filename);
	}
	private static void println(String filename, String text)
			throws IOException {
		FileWriter out = new FileWriter(filename, true);
		out.write(text);
		out.write(NL);
		out.close();
	}
	private static void printRemovalStepStats(String filename, int i,
			int instances, double instPercent) throws IOException {
		FileWriter out = new FileWriter(filename, true);
		StringBuffer b = new StringBuffer().append(i).append(SEP)
				.append(instances).append(SEP).append(instPercent)
				.append(NL);
		out.write(b.toString());
		out.close();
		// log("result summary added to ",filename);
	}
	private static String[] getOutputFiles(String graphSource, File outputFolder) {
		String graphfile = selectedProject.getElementName() ;
		String[] filenames = new String[5];
		String f1 = outputFolder.getPath();
		f1 = f1 + "/" + graphfile + "_instances.csv";
		String f2 = outputFolder.getPath();
		f2 = f2 + "/" + graphfile + "_details.csv";
		String f3 = outputFolder.getPath();
		f3 = f3 + "/" + graphfile + "_skipped_edges.csv";
		String f4 = outputFolder.getPath();
		f4 = f4 + "/" + graphfile + "_refactorings_applied.csv";
		String f5 = outputFolder.getPath();
		f5 = f5 + "/" + graphfile + "_overview.csv";
		filenames[0] = f1;
		filenames[1] = f2;
		filenames[2] = f3;
		filenames[3] = f4;
		filenames[4] = f5;
		return filenames;
	}
	private void removeFactoryEdges(DirectedGraph<Vertex, Edge> g) {
		Vertex factory = null;
		for(Vertex v: g.getVertices()) {
			if(v.getFullname().equals("registry.ServiceLocator"))
			{
				factory = v;
				break;
			}
		}
		if(factory == null) return;
		Collection<Edge> inEdges = new ArrayList<Edge>();
		Collection<Edge> outEdges = new ArrayList<Edge>();
		for(Edge e : factory.getInEdges()) {
			inEdges.add(e);
		}
		for(Edge e : factory.getOutEdges()) {
			outEdges.add(e);
		}
		for(Edge winner : inEdges) {
			g.removeEdge(winner);
		}
		for(Edge winner : outEdges) {
			g.removeEdge(winner);
		}
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

	public static ResultCounter countAllInstances(DirectedGraph<Vertex, Edge> g,
			List<Motif<Vertex, Edge>> motifs) {
		String outfolder = "";
		MultiThreadedGQLImpl<Vertex, Edge> engine = new MultiThreadedGQLImpl<Vertex, Edge>();
		PathFinder<Vertex, Edge> pFinder = new BreadthFirstPathFinder<Vertex, Edge>(
				true);

		final ResultCounter registry = new ResultCounter();

		for (Motif<Vertex, Edge> motif : motifs) {
			outfolder = outfolder + motif.getName() + "_";
			engine.query(new JungAdapter<Vertex, Edge>(g), motif, registry,
					ComputationMode.ALL_INSTANCES, pFinder);
		}
		return registry;
	}

	private boolean executeIntegratedRefactoring(Edge winner, DirectedGraph<Vertex, Edge> g, 
			List<Motif<Vertex, Edge>> motifs, int instances) throws IOException, JavaModelException {
		boolean result = false;
		Vertex source = winner.getStart();
		Vertex target = winner.getEnd();
		CompilationUnitCache.getInstance().clearCache();
		boolean loadSucceeded = loadRequiredASTs(winner);
		if(!loadSucceeded) {
			System.out.println("Couldn't load classes");
			return result;
		}
		if(!winner.getType().equals("uses")) {
			//we apply move refactoring
			boolean succeeded = MoveHelper.applyMoveRefactoring(winner,g,motifs,instances,getSite());
			if(succeeded) {
				refactoringTypeApplied = "Move";
				logg("Refactoring applied = MOVE");
			}
			return succeeded;
		}
		//if source class is interface we apply move refactoring
		if(source.isInterface()){
			boolean succeeded = MoveHelper.applyMoveRefactoring(winner,g,motifs,instances,getSite());
			if(succeeded) {
				refactoringTypeApplied = "Move";
				logg("Refactoring applied = MOVE");
			}
			return succeeded;
		}
		//if anonymous or inner class, we apply move refactoring
		if(source.isInnerClass() || source.isAnonymousClass() || target.isInnerClass() ||
				target.isAnonymousClass()) {
			boolean succeeded = MoveHelper.applyMoveRefactoring(winner,g,motifs,instances,getSite());
			if(succeeded) {
				refactoringTypeApplied = "Move";
				logg("Refactoring applied = MOVE");
			}
			return succeeded;
		}
		ClassObject sourceObject = ASTReader.getSystemObject().getClassObject(source.getFullname());
		ClassObject targetObject = ASTReader.getSystemObject().getClassObject(target.getFullname());
		String dependencyType = new DependencyFinder(sourceObject, targetObject).compute();
		String buildPath = "/Volumes/Data2/PhD/workspaces/corpus2010/test1/build.xml";
		if(dependencyType.equals("CI")) {
			SLRefactoring refac = new SLRefactoring(winner);
			if(attemptRefactoring(refac, winner, buildPath, instances)){
				refactoringTypeApplied = "CI";
				logg("Refactoring applied = SL");
				return true;
			} else {
				boolean succeeded = MoveHelper.applyMoveRefactoring(winner,g,motifs,instances,getSite());
				if(succeeded) {
					refactoringTypeApplied = "Move";
					logg("Refactoring applied = MOVE");
				}
				return succeeded;
			}
		}
		if(dependencyType.equals("VD/MPT/MRT/MET")) {
			GeneralizeRefactoring refac = new GeneralizeRefactoring(winner);
			if(attemptRefactoring(refac, winner, buildPath, instances)){
				refactoringTypeApplied = "VD/MPT/MRT/MET";
				logg("Refactoring applied = TG");
				return true;
			} else {
				boolean succeeded = MoveHelper.applyMoveRefactoring(winner,g,motifs,instances,getSite());
				if(succeeded) {
					refactoringTypeApplied = "Move";
					logg("Refactoring applied = MOVE");
				}
				return succeeded;
			}
		}
		if(dependencyType.equals("SMI")){
			OIRefactoring refac = new OIRefactoring(winner);
			if(attemptRefactoring(refac, winner, buildPath, instances)){
				refactoringTypeApplied = "SMI";
				logg("Refactoring applied = SMI");
				return true;
			} else {
				boolean succeeded = MoveHelper.applyMoveRefactoring(winner,g,motifs,instances,getSite());
				if(succeeded) {
					refactoringTypeApplied = "Move";
					logg("Refactoring applied = MOVE");
				}
				return succeeded;
			}
		}
		if(dependencyType.equals("CI+VD/MPT/MRT/MET")){
			TGPlusSLRefactoring compRefac = new TGPlusSLRefactoring(winner);
			if(attemptRefactoring(compRefac,winner,buildPath, instances)){
				refactoringTypeApplied = "CI+VD/MPT/MRT/MET";
				logg("Refactoring applied = SL + TG");
				return true;
			} else {
				boolean succeeded = MoveHelper.applyMoveRefactoring(winner,g,motifs,instances,getSite());
				if(succeeded) {
					refactoringTypeApplied = "Move";
					logg("Refactoring applied = MOVE");
				}
				return succeeded;
			}
		}
		if(dependencyType.equals("CI+SMI")) {
			SLPlusOIRefactoring compRefac = new SLPlusOIRefactoring(winner);
			if(attemptRefactoring(compRefac,winner,buildPath,instances)){
				refactoringTypeApplied = "CI+SMI";
				logg("Refactoring applied = SL + SMI");
				return true;
			} else {
				boolean succeeded = MoveHelper.applyMoveRefactoring(winner,g,motifs,instances,getSite());
				if(succeeded) {
					refactoringTypeApplied = "Move";
					logg("Refactoring applied = MOVE");
				}
				return succeeded;
			}
		}
		if(dependencyType.equals("VD/MPT/MRT/MET+SMI")) {
			TGPlusOIRefactoring compRefac = new TGPlusOIRefactoring(winner);
			if(attemptRefactoring(compRefac,winner,buildPath, instances)){
				refactoringTypeApplied = "VD/MPT/MRT/MET+SMI";
				logg("Refactoring applied = TG + SMI");
				return true;
			} else {
				boolean succeeded = MoveHelper.applyMoveRefactoring(winner,g,motifs,instances,getSite());
				if(succeeded) {
					refactoringTypeApplied = "Move";
					logg("Refactoring applied = MOVE");
				}
				return succeeded;
			}
		}
		if(dependencyType.equals("CI+VD/MPT/MRT/MET+SMI")) {
			TGPlusSLPlusOIRefactoring compRefac = new TGPlusSLPlusOIRefactoring(winner);
			if(attemptRefactoring(compRefac,winner,buildPath, instances)){
				refactoringTypeApplied = "CI+VD/MPT/MRT/MET+SMI";
				logg("Refactoring applied = SL + TG + SMI");
				return true;
			} else {
				boolean succeeded = MoveHelper.applyMoveRefactoring(winner,g,motifs,instances,getSite());
				if(succeeded) {
					refactoringTypeApplied = "Move";
					logg("Refactoring applied = MOVE");
				}
				return succeeded;
			}
		}
		return result;
	}
	
	private boolean attemptRefactoring(Refactoring refac, Edge winner, String buildPath, int instances) {
		boolean result = false;
		try {
			RefactoringStatus status = refac.checkInitialConditions(new NullProgressMonitor());
			status = refac.checkFinalConditions(new NullProgressMonitor());
			boolean postconditionsFailed = false; 
			if(!status.hasError()){
				configureLogger(winner);
				addOldCodeAppender(winner);
				CompositeChange change = (CompositeChange) refac.createChange(new NullProgressMonitor());
				if(change != null) {
					Change undo = change.perform(new NullProgressMonitor());
					addRefacCodeAppender(winner);
					MyCompiler compiler = new MyCompiler(selectedProject.getProject());
//					AntRunner1 antRunner = new AntRunner1(buildPath);
//					Postconditions post = new Postconditions(antRunner);
//					boolean passed = true;//OIRefactoring.checkOCLConstraints(post,"care-oi-post.ocl");
					IStatus status1 = compiler.build(selectedProject.getProject());
					if(status1.isOK()){
						//check post condition
						boolean r = isPostconditionSafe(instances);
						if(!r) {
							postconditionsFailed = true;
							rollback(undo);
							undo.dispose();
						}
					}
					else {
						postconditionsFailed = true;
						addErrorAppender(status1.getMessage());
						rollback(undo);
						undo.dispose();
						compiler.build(selectedProject.getProject());
						CompilationUnitCache.getInstance().clearAffectedCompilationUnits();
					}
					if(!postconditionsFailed) {
						refreshProject();
						result = true;
						iterationCounter2 = 0;
						counter1 = counter1 + iterationCounter1;
						counter2 = counter2 + iterationCounter2;
					}
					writeResult(DETEAIL_RESULT_PATH, status, postconditionsFailed, winner);
				}
			} else {
				//it means preconditions failed. 
				writeResult(DETEAIL_RESULT_PATH, status, postconditionsFailed, winner);
			}
			
		} catch (OperationCanceledException e) {
			e.printStackTrace();
		} catch (CoreException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
		return result;
	}
	

	
	private boolean isPostconditionSafe(int instances) {
		// TODO Auto-generated method stub
		refreshProject();
		IPath wp = selectedProject.getProject().getWorkspace().getRoot().getLocation();
		String binFolder = wp.toOSString() + selectedProject.getProject().getFullPath().toOSString() + "/bin/";
		File outputPath = new File(binFolder);
		DirectedGraph<Vertex, Edge> g = null;
		try {
			g = loadGraph(outputPath.getAbsolutePath());
			prepare(g);
			removeFactoryEdges(g);
			int instancesAfter = countAllInstances(g, motifs).getNumberOfInstances();
			if(instancesAfter <= instances) return true;
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		return false;
	}
	public void run() {
		String PROJECT_TO_TEST = "";
		Edge toTest = new Edge();
		toTest.setStart(new Vertex(""));
		toTest.setEnd(new Vertex(""));
		toTest.setType("");
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		// Get a project in the workspace
		File projectFile = new File(PROJECT_TO_TEST);
		File qFolder = new File(QUERY_FOLDER);
		IProject project = root.getProject(projectFile.getName());
		String name = project.getName();
		System.out.println(name);
		try {
			CareView.configureLoggingFolders(project.getName());
		} catch (IOException e) {
			e.printStackTrace();
		}
		selectedProject = JavaCore.create(project);
		try {
			selectedProject.getProject().build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
			IPath wp = project.getWorkspace().getRoot().getLocation();
			String binFolder = wp.toOSString() + project.getFullPath().toOSString() + "/bin/";
			File outputPath = new File(binFolder);
			DirectedGraph<Vertex, Edge> g = null;
			ASTUtils.createFactoryDeclaration(selectedProject);
			File[] queryFiles = qFolder
					.listFiles(getExcludeHiddenFilesFilter());
			List<Motif<Vertex, Edge>> motifs = new ArrayList<Motif<Vertex, Edge>>();
			for (int i = 0; i < queryFiles.length; i++) {
				File f = queryFiles[i];
				Motif<Vertex, Edge> m = loadMotif(QUERY_FOLDER+"/"+f.getName());
				if (m != null)
					motifs.add(m);
			}
			this.motifs = motifs;
			g = loadGraph(outputPath.getAbsolutePath());
			prepare(g);
			final ResultCounter registry = CareView.countAllInstances(g, motifs); 
			executeIntegratedRefactoring(toTest,g,motifs, registry.getNumberOfInstances());
			
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		System.gc();
	}
	public static String read(String filename){
		StringBuffer b = new StringBuffer();
		  try{
			  // Open the file that is the first 
			  // command line parameter
			  FileInputStream fstream = new FileInputStream(filename);
			  // Get the object of DataInputStream
			  DataInputStream in = new DataInputStream(fstream);
			  BufferedReader br = new BufferedReader(new InputStreamReader(in));
			  String strLine;
			  //Read File Line By Line
			  
			  while ((strLine = br.readLine()) != null)   {
			  // Print the content on the console
			  
			  b.append(strLine);
			  b.append("\n");
			  }
			  System.out.println (b.toString());
			  //Close the input stream
			  in.close();
			    }catch (Exception e){//Catch exception if any
			  System.err.println("Error: " + e.getMessage());
			  }
			  return b.toString();
			  
	}

	private boolean loadRequiredASTs(Edge winner) throws JavaModelException {
		// TODO Auto-generated method stub
//		CompilationUnitCache.getInstance().clearCache();
//		CompilationUnitCache.getInstance().clearAffectedCompilationUnits();
		new ASTReader(selectedProject);
		IJavaProject p = ASTReader.getExaminedProject();
		p.open(new NullProgressMonitor());
		String filename = winner.getStart().getFullname();
		IType s = p.findType(filename);
		if(s == null) return false;
		ICompilationUnit source = s.getCompilationUnit();
		IType target = p.findType(winner.getEnd().getFullname());
		if(source==null || target==null) return false;
		ICompilationUnit serviceLocator = p.findType("registry.ServiceLocator").getCompilationUnit();
		try{
			ASTReader.getSystemObject().addClasses(ASTReader.parseAST(source));
			ASTReader.getSystemObject().addClasses(ASTReader.parseAST(serviceLocator));
			ASTReader.getSystemObject().addClasses(ASTReader.parseAST(target.getCompilationUnit()));
			if(target != null) {
				ITypeHierarchy hierarchy = target.newTypeHierarchy(null);
				IType[] supertypes = hierarchy.getAllSupertypes(target);
				for(IType t : supertypes) {
					if(!t.isBinary())
						ASTReader.getSystemObject().addClasses(ASTReader.parseAST(t.getCompilationUnit()));
				}
			}
		}catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public static void refreshProject() {
		try {
			selectedProject.getProject().refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		} catch (CoreException e1) {
			e1.printStackTrace();
		}
		System.gc();
	}
	private void addErrorAppender(String errors) throws IOException {
		String filename = "error.log";
		String fullpath = DEPEND_DIR_PATH + "/" + filename;
		//configure the appender
		FileAppender appender = new FileAppender(new PatternLayout(),fullpath);
		appender.setName(filename);
		if(LOGGER.getAppender(filename) == null) LOGGER.addAppender(appender);
		log(errors);
		LOGGER.removeAllAppenders();
	}

	private void rollback(Change undo) {
		try {
			undo.perform(new NullProgressMonitor());
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
	private void configureLogger(Edge winner) throws IOException {
		String dirname = getSimpleName(winner.getStart().getFullname())+"_TO_"+getSimpleName(winner.getEnd().getFullname());
		String dependDirPath = PROJECT_RESULT_DIR + "/refactored_code/" + dirname;
		DEPEND_DIR_PATH = dependDirPath;
		File dependDir = new File(dependDirPath);
		if(!dependDir.exists()) dependDir.mkdirs();
	}
	private void addOldCodeAppender(Edge winner) throws IOException {
		String filename = getSimpleName(winner.getStart().getFullname())+"_old.txt";
		String fullpath = DEPEND_DIR_PATH  + "/" + filename;
		//configure the appender
		FileAppender appender = new FileAppender(new PatternLayout(),fullpath);
		appender.setName(filename);
		if(LOGGER.getAppender(filename) == null) LOGGER.addAppender(appender);
		//add old source code
		String classname = winner.getStart().getFullname();
		classname = classname.replace("$", ".");
		ClassObject object = ASTReader.getSystemObject().getClassObject(classname);
		if(object == null) return;
//		if(dependency.getSourceClassObject().getTypeDeclaration() == null) return;
		CompilationUnit cu = (CompilationUnit) object.getCompilationUnit();//getTypeDeclaration().getRoot();
		ICompilationUnit icu =  (ICompilationUnit) cu.getJavaElement();
		try {
			final String oldSource = icu.getSource();
			log(oldSource);
			LOGGER.removeAllAppenders();
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		
	}
	private void addRefacCodeAppender(Edge winner) throws IOException {
		String filename = getSimpleName(winner.getStart().getFullname())+".txt";
		String fullpath = DEPEND_DIR_PATH + "/" + filename;
		//configure the appender
		FileAppender appender = new FileAppender(new PatternLayout(),fullpath);
		appender.setName(filename);
		if(LOGGER.getAppender(filename) == null) {
			LOGGER.addAppender(appender);
		}
		//add new code
		String classname = winner.getStart().getFullname();
		classname = classname.replace("$", ".");
		ClassObject object = ASTReader.getSystemObject().getClassObject(classname);
		if(object == null) return;
		if(object.getTypeDeclaration()==null) return;
		CompilationUnit cu = (CompilationUnit) object.getCompilationUnit();
		ICompilationUnit icu =  (ICompilationUnit) cu.getJavaElement();
		try {
			final String newSource = icu.getSource();
			log(newSource);
			log("//*********************REFACTORED CODE ****************");
			LOGGER.removeAllAppenders();
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		
		
	}
	private void writeResult(String dependencyOutfile, RefactoringStatus status,
			boolean isCompilationFailed, Edge winner) {
		
		String message = null;
		RefactoringStatusEntry[] entries = status.getEntries();
		for(RefactoringStatusEntry entry : entries) {
			message = entry.getMessage();
			if(message == null) message = "";
			break;
		}
		if(message == null) message = "";
		FileWriter out;
		try {
			out = new FileWriter(dependencyOutfile,true);
			StringBuffer b = new StringBuffer();
			b.append(winner.getStart().getFullname())
			.append(SEP)
			.append(winner.getType())
			.append(SEP)
			.append(winner.getEnd().getFullname())
			.append(SEP);
			if(message.equals("isInstantiationFailed")) b.append("1");
			else b.append("0");
			b.append(SEP);
			if(status.hasError()) b.append("1");
			else b.append("0");
			b.append(SEP);
			if(isCompilationFailed) b.append("1");
			else b.append("0");
			b.append(SEP);
			if(!status.hasError() && !isCompilationFailed) b.append("Pass");
			else b.append("Fail");
			b.append(NL);
			
			out.write(b.toString());
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	private void printHeader(String dependencyOutfile) {
		FileWriter out;
		try {
			out = new FileWriter(dependencyOutfile,true);
			StringBuffer b = new StringBuffer();
			b.append("source")
			.append(SEP)
			.append("type")
			.append(SEP)
			.append("target")
			.append(SEP)
			.append("isInstiationFailed")
			.append(SEP)
			.append("preconditionsFailed")
			.append(SEP)
			.append("postconditionsFailed")
			.append(SEP)
			.append("result")
			.append(NL);
			
			out.write(b.toString());
			out.close();
		} catch (IOException e) {
			//do nothing
		}
	}

	public String getSimpleName(String fullname){
		String simpleName = fullname.substring(fullname.lastIndexOf(".")+1);
		return simpleName;
	}
	static void configureLoggingFolders(String project) throws IOException {
		PROJECT_NAME = project;
		File resultsFolder = new File(RESULTS_FOLDER);
		if (!resultsFolder.exists()) {
			log("create results folder " + resultsFolder);
			resultsFolder.mkdirs();
		}
		String projectPath = RESULTS_FOLDER + "/" + PROJECT_NAME;
		PROJECT_RESULT_DIR = projectPath;
		File projectFolder = new File(projectPath);
		if(!projectFolder.exists()){
			log("created project folder " + projectFolder);
			projectFolder.mkdir();
		}
		String refactoredCodePath = projectPath + "/refactored_code";
		PROJECT_REFCODE_DIR = refactoredCodePath;
		File refactoredCodeFolder = new File(PROJECT_REFCODE_DIR);
		if(!refactoredCodeFolder.exists()){
			log("created refactored code folder "+ refactoredCodeFolder);
			refactoredCodeFolder.mkdir();
		}
		String outputPath = projectPath + "/output";
		PROJECT_OUTPUT_DIR = outputPath;
		File outputFolder = new File(outputPath);
		if(!outputFolder.exists()){
			log("created output folder "+ outputFolder);
			outputFolder.mkdir();
		}
	}
	
	private static void log(Object... s) {
		StringBuffer b = new StringBuffer();
		for (Object t:s) {
			b.append(t);
		}
		LOGGER.warn(b.toString());
//		System.out.println(b.toString());
	}
	private static void logg(String s) {
		System.out.println(s);
	}

	
	@Override
	public void setFocus() {
		tableViewer.getControl().setFocus();
		
	}
	
	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalToolBar(bars.getToolBarManager());
	}
	
	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(startIntegratedRefactoringsAction);
		manager.add(testRefactoringAction);
	}
	
	class ViewContentProvider implements IStructuredContentProvider {
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		
		}
		public void dispose() {
		}
		public Object[] getElements(Object parent) {
				return new OIRefactoring[] {};
		}
	}

	class ViewLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			//AbstractionCandidateRefactoring entry = (AbstractionCandidateRefactoring)obj;
			
			switch(index){
			case 0:
				return "Object Inlining";
			case 1:
				return "";//entry.getSourceClass();
			case 2:
				return "";//entry.getTargetClass();
			case 3:
				return String.valueOf(false);
			default:
				return "";
			}
			
		}
		public Image getColumnImage(Object obj, int index) {
			return null;
		}
		public Image getImage(Object obj) {
			return null;
		}
	}
}

		