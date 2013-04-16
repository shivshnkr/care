package nz.ac.massey.cs.care.move.refactoring.views;

import static nz.ac.massey.cs.care.move.util.Printery.*;
import static nz.ac.massey.cs.care.move.util.Utils.getExcludeHiddenFilesFilter;
import static nz.ac.massey.cs.care.move.util.Utils.getOrAddFolder;
import static nz.ac.massey.cs.care.move.util.Utils.loadGraph;
import static nz.ac.massey.cs.care.move.util.Utils.loadMotif;
import static nz.ac.massey.cs.care.move.util.Utils.loadPowerGraph;
import edu.uci.ics.jung.graph.DirectedGraph;
import gr.uom.java.jdeodorant.refactoring.views.ElementChangedListener;

import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import nz.ac.massey.cs.care.move.Edge;
import nz.ac.massey.cs.care.move.Vertex;
import nz.ac.massey.cs.care.move.metrics.Modularity;
import nz.ac.massey.cs.care.move.metrics.PackageMetrics;
import nz.ac.massey.cs.care.move.metrics.SCCMetrics;
import nz.ac.massey.cs.care.move.scripts.MoveExecuter;
import nz.ac.massey.cs.care.move.scripts.MetricsComputer;
import nz.ac.massey.cs.care.move.scripts.MoveModelExecuter;
import nz.ac.massey.cs.guery.Motif;

import org.apache.commons.collections15.Transformer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
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
import org.eclipse.ltk.core.refactoring.Refactoring;
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


public class CareView extends ViewPart{
	
	private TableViewer tableViewer;
	private Action startIntegratedRefactoringsAction;
	private Action testRefactoringAction;
	private static IJavaProject selectedProject = null;
	private static final String WORKSPACE_PATH = "/Volumes/Data2/PhD/workspaces/CARE/nz.ac.massey.cs.care.move/";
	private static final String PROJECTS_TODO =  WORKSPACE_PATH + "projects-todo";
	private static final String PROJECTS_DONE =  WORKSPACE_PATH + "projects-done";
	private static final String QUERY_FOLDER = WORKSPACE_PATH + "queries";
	private static boolean MOVE_DONE = true;
	private List<Motif<Vertex, Edge>> motifs = null;
	static Transformer<Vertex,String> componentMembership = new Transformer<Vertex,String>() {
		@Override
		public String transform(Vertex s) {
			return s.getNamespace();
		}
	};
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

	@SuppressWarnings("unchecked")
	private void executeRefactorings(IProgressMonitor monitor) {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		getOrAddFolder("output/cd");
		loadMotifs();
		Motif<Vertex, Edge>[] motifArray = new Motif[2];
		for (Motif<Vertex, Edge> motif : motifs) {
			if(motif.getName().equals("scd")) motifArray[0] = motif;
			else motifArray[1] = motif;
		}
		MoveExecuter.setIPart(getSite());//constant
		MoveExecuter.setWorkspace(WORKSPACE_PATH);//constant
		MoveExecuter.setMotifs(motifArray);//constant
		
		File[] projectFiles = getProjectFiles(monitor);
		
		for (File projectFile : projectFiles) {
			IProject iProject = root.getProject(projectFile.getName());
			String name = iProject.getName();
			System.out.println(name);
			selectedProject = JavaCore.create(iProject);
			try {
				selectedProject.getProject().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, new NullProgressMonitor());
				IPath wp = iProject.getWorkspace().getRoot().getLocation();
				String binFolder = wp.toOSString() + iProject.getFullPath().toOSString() + "/bin/";
				String projectPath = new File(binFolder).getAbsolutePath();
				DirectedGraph<Vertex, Edge> g = loadGraph(projectPath);
				//calculate metrics before refactoring
				String[] metricsFiles = getOutputFiles(projectPath);
				SCCMetrics sccBefore = MetricsComputer.computeSCC(loadPowerGraph(g));
				double modularityBefore = Modularity.computeScaledModularity(g, componentMembership);
				double distanceBefore = MetricsComputer.getDistance(g);
				Map<String, PackageMetrics.Metrics> pmBefore = new PackageMetrics().compute(g, "");
				long timeBefore = System.currentTimeMillis();
				//start the refactoring process
				try {
					MoveExecuter executer = initializeExecuter(projectFile, projectPath, iProject);
					executer.setIProject(iProject);
					executer.setOutputFiles(projectPath);
					executer.execute(g, projectPath, 0);
				} catch (Exception e) {
					e.printStackTrace();
				}
				//recompute metrics after refactoring
				g = loadGraph(projectPath);
				SCCMetrics sccAfter = MetricsComputer.computeSCC(loadPowerGraph(g));
				double modularityAfter = Modularity.computeScaledModularity(g, componentMembership);
				double distanceAfter = MetricsComputer.getDistance(g);
				Map<String, PackageMetrics.Metrics> pmAfter = new PackageMetrics().compute(g, "");
				long timeAfter = System.currentTimeMillis();
				printMetrics(metricsFiles[0], sccBefore, sccAfter, modularityBefore, modularityAfter,
						distanceBefore, distanceAfter, timeAfter - timeBefore);
				printPackageMetrics(metricsFiles[1], pmBefore, pmAfter);
//				computeGraphProperties(name, g);
				
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			if (MOVE_DONE) {
				File usedDatefile = new File(PROJECTS_DONE + "/" + iProject.getName());
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
	
	public void computeGraphProperties(String name, DirectedGraph<Vertex, Edge> g) throws IOException {
		Properties properties = new Properties();
		properties.put("vertexcount",""+g.getVertexCount());
		properties.put("edgecount",""+g.getEdgeCount());
		String graphPropPath = WORKSPACE_PATH + "/output/graph-properties/" + name +".properties";
		File target= new File(graphPropPath);
		Writer writer = new FileWriter(target);
		properties.store(writer, "generated by " + CareView.class);
		writer.close();
		
	}

	private MoveExecuter initializeExecuter(File projectFile,
			String projectPath, IProject iProject) throws IOException {
		MoveExecuter e = new MoveExecuter();
		e.addUnMovableClasses(getFixedClasses(projectFile));
//		e.addSkippedEdges(getSkippedEdges(projectFile.getName()));
		e.setIProject(iProject);
		e.setOutputFiles(projectPath);
		return e;
	}

	protected Set<Edge> getSkippedEdges(String name) {
		File f = new File(WORKSPACE_PATH + "output/cd/" + name + "_skipped_edges.csv");
		Set<Edge> output = new HashSet<Edge>();
		if(!f.exists()) return output;
		try {
			CSVReader r = new CSVReader(new FileReader(f));
			String[] nextLine = null;
			while ((nextLine = r.readNext()) != null) {
				if(nextLine[0].equals("iteration")) continue;
				String sourcefullname = null;
				try{
					sourcefullname = nextLine[1];
				} catch (Exception e) {
					continue;
				}
				String sourceSimpleName = MoveExecuter.getSimplename(sourcefullname);
				String sourcePackageName = sourcefullname.substring(0,sourcefullname.lastIndexOf("."));
				String type = nextLine[2];
				String target = nextLine[3];
				String targetSimpleName = MoveExecuter.getSimplename(target);
				String targetPackageName = target.substring(0,target.lastIndexOf("."));
				
				Vertex start = new Vertex();
				start.setName(sourceSimpleName);
				start.setNamespace(sourcePackageName);
				
				Vertex end = new Vertex();
				end.setName(targetSimpleName);
				end.setNamespace(targetPackageName);
				
				Edge e = new Edge();
				e.setStart(start);
				e.setEnd(end);
				e.setType(type);
				output.add(e);
			}
			r.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return output;
	}

	private static String[] getOutputFiles(String graphfile) throws IOException {
		//the path looks like this /Volumes/Data2/PhD/workspaces/corpus2010/marauroa-3.8.1/bin
		String separator = System.getProperty("file.separator");
		//first remove bin from the graph file name
		graphfile = graphfile.substring(0, graphfile.lastIndexOf(separator));
		//now remove the first part to just get the name of the project
		graphfile = graphfile.substring(graphfile.lastIndexOf(separator)+1);
		File outputFolder = new File(WORKSPACE_PATH + "output" + separator + "cd");
		String[] filenames = new String[2];
		String f1 = outputFolder.getPath();
		f1 = f1 + separator + graphfile + "_metrics.csv";
		//print header of _metrics file
		println(f1,"compression before,compression after,max scc size before,"+
				"max scc size after,density before,density after,tangledness before," +
				"tangledness after,count before,count after,"+
				"modularity before,modularity after,distance before,distance after," +
				"total time");
		String f2 = outputFolder.getPath();
		f2 = f2 + separator + graphfile + "_package_metrics.csv";
		//print header of _distance file
		println(f2,"package,CE before,CE after,CA before,CA after,A before,A after," +
				"I before,I after,D before,D after");
		filenames[0] = f1;
		filenames[1] = f2;
		
		return filenames;
	}
	public static List<String> getFixedClasses(File f) {
		List<String> classes = new ArrayList<String>();
		try {
			CSVReader r = new CSVReader(new FileReader(f));
			String[] nextLine = null;
			while ((nextLine = r.readNext()) != null) {
				classes.add(nextLine[0]);
			}
			r.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return classes;
	}

	private void makeActions() {
		testRefactoringAction = new Action() {
			public void run() {
				try {
					IWorkbench wb = PlatformUI.getWorkbench();
					IProgressService ps = wb.getProgressService();
						ps.busyCursorWhile(new IRunnableWithProgress() {
							public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
								executeRefactorings(monitor);
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
//								executeAllRefactorings(monitor);
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
	public static File[] getProjectFiles(IProgressMonitor monitor) {
		File projectsDir = new File(PROJECTS_TODO);
		if (monitor != null)
			monitor.beginTask("Started performing refactorings",projectsDir.listFiles().length);
		File[] projectFiles = projectsDir.listFiles(new FileFilter(){

			@Override
			public boolean accept(File file) {
				String name = file.getName();
				return !name.endsWith(".svn") && !name.endsWith(".DS_Store");
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

	protected Edge setupTestEdge() {
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
				return new Refactoring[] {};
		}
	}

	class ViewLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			//AbstractionCandidateRefactoring entry = (AbstractionCandidateRefactoring)obj;
			
			switch(index){
			case 0:
				return "Move Refactoring";
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

		