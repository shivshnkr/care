package nz.ac.massey.cs.care.refactoring.views;

import static nz.ac.massey.cs.care.util.MovePrintery.printMetrics;
import static nz.ac.massey.cs.care.util.MovePrintery.printPackageMetrics;
import static nz.ac.massey.cs.care.util.Utils.loadGraph;
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
import nz.ac.massey.cs.care.ast.ASTUtils;
import nz.ac.massey.cs.care.metrics.MetricsResult;
import nz.ac.massey.cs.care.metrics.ModularityMetrics;
import nz.ac.massey.cs.care.metrics.PackageMetrics;
import nz.ac.massey.cs.care.metrics.SCCMetrics;
import nz.ac.massey.cs.care.scripts.CompositeExecuter;
import nz.ac.massey.cs.care.scripts.MetricsComputer;
import nz.ac.massey.cs.guery.Motif;

import org.apache.commons.collections15.Transformer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
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
import gr.uom.java.ast.ASTReader;

@SuppressWarnings("restriction")
public class CompositeRefactoringView extends ViewPart{
	
	private TableViewer tableViewer;
	private Action startAllProjectsAnalysisAction;
	private Action startIndividualProjectAnalysisAction;
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
				CompositeExecuter.setMaxSteps(val.toString());
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
	class MyContentProvider implements IStructuredContentProvider {

		public Object[] getElements(Object inputElement) {
			MyModel mm = new MyModel("Maximum Number of Refactorings", CompositeExecuter.getMaxSteps());
			return new MyModel[]{mm};
		}

		public void dispose() {
			
		}
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			
		}
		
	}
	@SuppressWarnings("unchecked")
	private void executeRefactorings(IProgressMonitor monitor, String projectName, boolean analyseAll) {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		loadMotifs();
		Motif<Vertex, Edge>[] motifArray = new Motif[4];
		int i = 0;
		for (Motif<Vertex, Edge> motif : motifs) {
			motifArray[i++] = motif;
		}
		CompositeExecuter.setIPart(getSite());//constant
		CompositeExecuter.setMotifs(motifs);//constant
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
		MessageConsole myConsole = findConsole("CARE-CONSOLE");
		MessageConsoleStream out = myConsole.newMessageStream();
		out.println(name);
		System.out.println(name);
		selectedProject = JavaCore.create(iProject);
		try {
			selectedProject.getProject().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, new NullProgressMonitor());
			IPath wp = iProject.getWorkspace().getRoot().getLocation();
			String binFolder = wp.toOSString() + iProject.getFullPath().toOSString() + "/bin/";
			String projectPath = new File(binFolder).getAbsolutePath();
			DirectedGraph<Vertex, Edge> g = loadGraph(projectPath);
			new ASTReader(selectedProject);
			selectedProject.open(new NullProgressMonitor());
			//calculate metrics before refactoring
			MetricsResult before = computeMetrics(g);
			//start the refactoring process
			CompositeExecuter executer = new CompositeExecuter();
			try {
				if(selectedProject.findType("registry.ServiceLocator") == null) {
					ASTUtils.createFactoryDeclaration(selectedProject);
				}
				executer.setIProject(iProject);
				executer.setOutputFiles(projectPath);
				executer.setConsoleStream(out);
				executer.execute(g, projectPath, 0, monitor);
			} catch (Exception e) {
				e.printStackTrace();
			}
			//recompute metrics after refactoring
			g = loadGraph(projectPath);
			MetricsResult after = computeMetrics(g);
			String[] metricsFiles = executer.getMetricsOutputFiles(projectPath);
			printMetrics(selectedProject.getProject(), metricsFiles[0], before.getSccMetrics(), after.getSccMetrics(), 
					before.getModularity(), after.getModularity(), before.getDistance(), after.getDistance(), after.getTime()-before.getTime());
			printPackageMetrics(selectedProject.getProject(),metricsFiles[1], before.getPackageMetrics(), after.getPackageMetrics());
			
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}
	
	public static MetricsResult computeMetrics(DirectedGraph<Vertex, Edge> g) throws Exception {
		MetricsResult r = new MetricsResult();
		SCCMetrics sccMetrics = MetricsComputer.computeSCC(loadPowerGraph(g));
		double modularity = ModularityMetrics.computeScaledModularity(g, componentMembership);
		double distance = MetricsComputer.getDistance(g);
		Map<String, PackageMetrics.Metrics> packageMetrics = new PackageMetrics().compute(g, "");
		long time = System.currentTimeMillis();
		
		r.setSccMetrics(sccMetrics);
		r.setModularity(modularity);
		r.setDistance(distance);
		r.setPackageMetrics(packageMetrics);
		r.setTime(time);
		
		return r;
	}

	private void makeActions() {
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
		org.osgi.framework.Bundle b = Platform.getBundle("nz.ac.massey.cs.care");
		URL fullPathString1= BundleUtility.find(b, "icons/single1.png");
	    ImageDescriptor image1= ImageDescriptor.createFromURL(fullPathString1);
	    startIndividualProjectAnalysisAction.setImageDescriptor(image1);
		startIndividualProjectAnalysisAction.setEnabled(true);
		
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
	    URL fullPathString = BundleUtility.find(b, "icons/multiple1.png");
	    ImageDescriptor image= ImageDescriptor.createFromURL(fullPathString);
	    startAllProjectsAnalysisAction.setImageDescriptor(image);
		startAllProjectsAnalysisAction.setEnabled(true);
	}
	private void loadMotifs(){
		Bundle bundle = Platform.getBundle("nz.ac.massey.cs.care");
		URL fileURL = BundleUtility.find(bundle,"queries/scd.guery");
		URL fileURL2 = BundleUtility.find(bundle,"queries/stk.guery");
		URL fileURL3 = BundleUtility.find(bundle,"queries/awd.guery");
		URL fileURL4 = BundleUtility.find(bundle,"queries/deginh.guery");
		String uri = null;
		String uri2 = null;
		String uri3 = null;
		String uri4 = null;
		try {
				uri = FileLocator.resolve(fileURL).getFile();
		        uri2 = FileLocator.resolve(fileURL2).getFile();
		        uri3 = FileLocator.resolve(fileURL3).getFile();
		        uri4 = FileLocator.resolve(fileURL4).getFile();
		        
		    } catch (IOException e1) {
		        e1.printStackTrace();
		    }
		
		File[] queryFiles = new File[4];
		queryFiles[0] = new File(uri);
		queryFiles[1] = new File(uri2);
		queryFiles[2] = new File(uri3);
		queryFiles[3] = new File(uri4);
		
		List<Motif<Vertex, Edge>> motifs = new ArrayList<Motif<Vertex, Edge>>();
		for (int i = 0; i < queryFiles.length; i++) {
			File f = queryFiles[i];
			Motif<Vertex, Edge> m;
			try {
				m = loadMotif(f.getAbsolutePath());
				if (m != null)
					motifs.add(m);
				this.motifs = motifs;
			} catch (Exception e) {
				System.out.println("could not load motif files");
				
			}
		}
		
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

		