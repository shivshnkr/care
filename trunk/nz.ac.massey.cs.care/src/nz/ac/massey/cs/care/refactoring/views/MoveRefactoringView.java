package nz.ac.massey.cs.care.refactoring.views;

import static nz.ac.massey.cs.care.util.MovePrintery.printMetrics;
import static nz.ac.massey.cs.care.util.MovePrintery.printPackageMetrics;
import static nz.ac.massey.cs.care.util.MovePrintery.println;
import static nz.ac.massey.cs.care.util.MoveUtils.loadGraph;
import static nz.ac.massey.cs.care.util.Utils.loadMotif;
import static nz.ac.massey.cs.care.util.Utils.loadPowerGraph;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import nz.ac.massey.cs.care.Edge;
import nz.ac.massey.cs.care.Vertex;
import nz.ac.massey.cs.care.metrics.ModularityMetrics;
import nz.ac.massey.cs.care.metrics.PackageMetrics;
import nz.ac.massey.cs.care.metrics.SCCMetrics;
import nz.ac.massey.cs.care.scripts.MetricsComputer;
import nz.ac.massey.cs.care.scripts.MoveExecuter;
import nz.ac.massey.cs.guery.Motif;

import org.apache.commons.collections15.Transformer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.eclipse.ui.internal.util.BundleUtility;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.IProgressService;
import org.osgi.framework.Bundle;

import edu.uci.ics.jung.graph.DirectedGraph;


@SuppressWarnings("restriction")
public class MoveRefactoringView extends ViewPart{
	
	private TableViewer tableViewer = null;
	private Action startIndividualProjectAnalysisAction;
	private Action startAllProjectsAnalysisAction;
	private static IJavaProject selectedProject = null;
	private List<Motif<Vertex, Edge>> motifs = null;
	static Transformer<Vertex,String> componentMembership = new Transformer<Vertex,String>() {
		@Override
		public String transform(Vertex s) {
			return s.getNamespace();
		}
	};
	@Override
	public void createPartControl(Composite parent) {
		tableViewer = new TableViewer(parent,SWT.BORDER|SWT.FULL_SELECTION);
		tableViewer.setLabelProvider(new ViewLabelProvider());
		tableViewer.setContentProvider(new MyContentProvider());
		tableViewer.setCellModifier(new ICellModifier() {

			public boolean canModify(Object element, String property) {
				return property.equals("value");
			}
			public Object getValue(Object element, String property) {
				return ((MyModel)element).value + "";
			}
			public void modify(Object element, String property, Object val) {
				TableItem item = (TableItem)element;
				int newVal = Integer.parseInt(val.toString());
				((MyModel)item.getData()).value = newVal;
				MoveExecuter.setMaxSteps(val.toString());
				tableViewer.update(item.getData(), null);
			}
			
		});
		tableViewer.setCellEditors(new CellEditor[] { new TextCellEditor(tableViewer.getTable()),
				 new TextCellEditor(tableViewer.getTable()), new TextCellEditor(tableViewer.getTable())});
		TableLayout layout = new TableLayout();
		layout.addColumnData(new ColumnWeightData(30, true));
		layout.addColumnData(new ColumnWeightData(30, true));
		layout.addColumnData(new ColumnWeightData(50, true));
		tableViewer.getTable().setLayout(layout);
		tableViewer.getTable().setLinesVisible(true);
		tableViewer.getTable().setHeaderVisible(true);
		TableColumn column0 = new TableColumn(tableViewer.getTable(),SWT.LEFT);
		column0.setText("Property");
		column0.setResizable(true);
		column0.pack();
		TableColumn column1 = new TableColumn(tableViewer.getTable(),SWT.LEFT);
		column1.setText("Value");
		column1.setResizable(true);
		column1.pack();
		TableColumn column2 = new TableColumn(tableViewer.getTable(),SWT.LEFT);
		column2.setText("Notes");
		column2.setResizable(true);
		column2.pack();
		
		tableViewer.setColumnProperties(new String[] {"property", "value", "notes"});
		
		MyModel[] model = createModel();
		tableViewer.setInput(model);
		tableViewer.getTable().setLinesVisible(true);
		
		makeActions();
		contributeToActionBars();

	}
	
	class MyContentProvider implements IStructuredContentProvider {

		public Object[] getElements(Object inputElement) {
			MyModel mm = new MyModel("Maximum Number of Refactorings", 10);
			return new MyModel[]{mm};
		}
		public void dispose() {
			
		}
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			
		}
		
	}
	
	private MyModel[] createModel() {
		MyModel[] elements = new MyModel[1];
		elements[0] = new MyModel("max",10);
		return elements;
	}
	
	class MyModel {
		public int counter;
		public String property;
		public int value;
		
		public MyModel(int counter) {
			this.counter = counter;
		}
		
		public MyModel(String prop, int val) {
			property = prop;
			value = val;
		}

		public String toString() {
			return "Item " + this.value;
		}
	}
	
	@SuppressWarnings("unchecked")
	private void executeRefactorings(IProgressMonitor monitor, String projectName, boolean analyseAll) {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		loadMotifs();
		Motif<Vertex, Edge>[] motifArray = new Motif[2];
		for (Motif<Vertex, Edge> motif : motifs) {
			if(motif.getName().equals("scd")) motifArray[0] = motif;
			else motifArray[1] = motif;
		}
		MoveExecuter.setIPart(getSite());//constant
		MoveExecuter.setMotifs(motifArray);//constant
		if(analyseAll) {
			monitor.beginTask("Started performing refactorings. It may take a while.",root.getProjects().length);
			for(IProject iProject : root.getProjects()) {
				if(!iProject.isOpen()) continue;
				processProject(iProject, monitor);
				monitor.worked(1);
				System.gc();
			}
		} else {
			if(projectName == null) return;
			monitor.beginTask("Started performing refactorings. It may take a while.",1);
			IProject iProject = root.getProject(projectName);
			processProject(iProject, monitor);
			monitor.worked(1);
		}
		monitor.done();
			
	}
	
	private void processProject(IProject iProject, IProgressMonitor monitor) {
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
			double modularityBefore = ModularityMetrics.computeScaledModularity(g, componentMembership);
			double distanceBefore = MetricsComputer.getDistance(g);
			Map<String, PackageMetrics.Metrics> pmBefore = new PackageMetrics().compute(g, "");
			long timeBefore = System.currentTimeMillis();
			//start the refactoring process
			try {
//				MoveModelExecuter executer = new MoveModelExecuter();
//				executer.setIProject(iProject);
//				executer.setOutputFiles(projectPath);
				MoveExecuter executer = initializeExecuter(projectPath, iProject);
				executer.execute(g, projectPath, 0, monitor);
			} catch (Exception e) {
				e.printStackTrace();
			}
			//recompute metrics after refactoring
			g = loadGraph(projectPath);
			SCCMetrics sccAfter = MetricsComputer.computeSCC(loadPowerGraph(g));
			double modularityAfter = ModularityMetrics.computeScaledModularity(g, componentMembership);
			double distanceAfter = MetricsComputer.getDistance(g);
			Map<String, PackageMetrics.Metrics> pmAfter = new PackageMetrics().compute(g, "");
			long timeAfter = System.currentTimeMillis();
			printMetrics(selectedProject.getProject(),metricsFiles[0], sccBefore, sccAfter, modularityBefore, modularityAfter,
					distanceBefore, distanceAfter, timeAfter - timeBefore);
			printPackageMetrics(selectedProject.getProject(),metricsFiles[1], pmBefore, pmAfter);
			
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
	}

	private MoveExecuter initializeExecuter(
			String projectPath, IProject iProject) {
		MoveExecuter e = new MoveExecuter();
		MessageConsole myConsole = findConsole("CARE-CONSOLE");
		MessageConsoleStream out = myConsole.newMessageStream();
		out.println(iProject.getName());
		try{
			e.setIProject(iProject);
			e.setOutputFiles(projectPath);
			e.setConsoleStream(out);
			return e;
		} catch (Exception ex) {
			
		}
		return e;
	}

	private static String[] getOutputFiles(String graphfile) throws IOException, CoreException {
		//the path looks like this /Volumes/Data2/PhD/workspaces/corpus2010/marauroa-3.8.1/bin
		String separator = System.getProperty("file.separator");
		//first remove bin from the graph file name
		graphfile = graphfile.substring(0, graphfile.lastIndexOf(separator));
		//now remove the first part to just get the name of the project
		graphfile = graphfile.substring(graphfile.lastIndexOf(separator)+1);
		IFolder f = selectedProject.getProject().getFolder("output-move-refactoring");
		if(!f.exists()) f.create(IResource.NONE, true, null);
		String[] filenames = new String[2];
		String f1 = f.getName();
		f1 = f1 + separator + graphfile + "_metrics.csv";
		//print header of _metrics file
		println(selectedProject.getProject(),f1,"compression before,compression after,max scc size before,"+
				"max scc size after,density before,density after,tangledness before," +
				"tangledness after,count before,count after,"+
				"modularity before,modularity after,distance before,distance after," +
				"total time");
		String f2 = f.getName();
		f2 = f2 + separator + graphfile + "_package_metrics.csv";
		//print header of _distance file
		println(selectedProject.getProject(),f2,"package,CE before,CE after,CA before,CA after,A before,A after," +
				"I before,I after,D before,D after");
		filenames[0] = f1;
		filenames[1] = f2;
		
		return filenames;
	}
	

	private void makeActions() {
		
		startAllProjectsAnalysisAction = new Action() {
			public void run() {
				try {
					IWorkbench wb = PlatformUI.getWorkbench();
					IProgressService ps = wb.getProgressService();
						ps.busyCursorWhile(new IRunnableWithProgress() {
							public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
								executeRefactorings(monitor, null, true);
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
		startAllProjectsAnalysisAction.setToolTipText("Analyse All Projects");
		
	    org.osgi.framework.Bundle b = Platform.getBundle("nz.ac.massey.cs.care");
	    URL fullPathString = BundleUtility.find(b, "icons/multiple1.png");
	    ImageDescriptor image= ImageDescriptor.createFromURL(fullPathString);
	    startAllProjectsAnalysisAction.setImageDescriptor(image);
		startAllProjectsAnalysisAction.setEnabled(true);
		
		startIndividualProjectAnalysisAction = new Action() {
			public void run() {
				Shell shell = getSite().getWorkbenchWindow().getShell();
				IWorkspaceRoot r = ResourcesPlugin.getWorkspace().getRoot();
				ContainerSelectionDialog dialog =
						 new ContainerSelectionDialog(shell, r, false, "Select the Project");
				dialog.setTitle("Project Selection");		 
				String projectName = null;
				if (dialog.open() == Window.OK) {
					Object[] result = dialog.getResult();
					for (int i = 0; i < result.length; i++) {
						Path path = (Path) result[i];
						projectName = path.toString();
						System.out.println(path);
					}
				}
				final String name = projectName;
				try {
					IWorkbench wb = PlatformUI.getWorkbench();
					IProgressService ps = wb.getProgressService();
						ps.busyCursorWhile(new IRunnableWithProgress() {
							public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
								executeRefactorings(monitor, name, false);
							}
						});
						MessageBox mb = new MessageBox(getSite().getWorkbenchWindow().getShell(),SWT.ICON_INFORMATION);
						mb.setMessage("Finished Analysis of " + selectedProject.getElementName());
						mb.setText("Status");
						mb.open();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		};
		startIndividualProjectAnalysisAction.setToolTipText("Analyse Individual Project");
	    URL fullPathString1= BundleUtility.find(b, "icons/single1.png");
	    ImageDescriptor image1= ImageDescriptor.createFromURL(fullPathString1);
	    startIndividualProjectAnalysisAction.setImageDescriptor(image1);
		startIndividualProjectAnalysisAction.setEnabled(true);
	}
	
	private void loadMotifs(){
		
		List<Motif<Vertex, Edge>> motifs = new ArrayList<Motif<Vertex, Edge>>();
		Bundle bundle = Platform.getBundle("nz.ac.massey.cs.care");
		URL fileURL = BundleUtility.find(bundle,"queries/scd.guery");
		URL fileURL2 = BundleUtility.find(bundle,"queries/wcd.guery");
		System.out.println(fileURL.getFile());
		System.out.println(fileURL2.getPath());
		String uri = null;
		String uri2 = null;
		try {
				uri = FileLocator.resolve(fileURL).getFile();
		        uri2 = FileLocator.resolve(fileURL2).getFile();
		        
		    } catch (IOException e1) {
		        e1.printStackTrace();
		    }
		File[] queryFiles = new File[2];
		queryFiles[0] = new File(uri);
		queryFiles[1] = new File(uri2);
		
		for (int i = 0; i < 2; i++) {
			File f = queryFiles[i];
			
			Motif<Vertex, Edge> m;
			try {
				m = loadMotif(f.getAbsolutePath());
				if (m != null)
					motifs.add(m);
				this.motifs = motifs;
			} catch (Exception e) {
				System.out.println("could not load motif files");
				e.printStackTrace();
				
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
		manager.add(startIndividualProjectAnalysisAction);
		manager.add(startAllProjectsAnalysisAction);
	}
	
	class ViewLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			MyModel prop = (MyModel) obj;
			switch(index){
			case 0:
				return prop.property;
			case 1:
				return String.valueOf(prop.value);
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
	 private MessageConsole findConsole(String name) {
	      ConsolePlugin plugin = ConsolePlugin.getDefault();
	      IConsoleManager conMan = plugin.getConsoleManager();
	      MessageConsole myConsole = new MessageConsole(name, null);
	      conMan.addConsoles(new IConsole[]{myConsole});
	      return myConsole;
	 }
}

		