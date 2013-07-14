package nz.ac.massey.cs.care.refactoring.movehelper;

import edu.uci.ics.jung.graph.DirectedGraph;
import gr.uom.java.ast.ClassObject;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.ui.IWorkbenchPartSite;

import nz.ac.massey.cs.care.Edge;
import nz.ac.massey.cs.care.Vertex;
import nz.ac.massey.cs.guery.Motif;


public class Candidate {
	private String sourceClass;
	private String targetClass;
	private String edgeType;
	private String targetPackage;
	private String sourcePackage;
	private IProject project;
	private int instancesBefore;
	private int instancesAfter;
	private ConstraintsResult constraintsResult = new ConstraintsResult();
	private List<Vertex> classesMoved = new ArrayList<Vertex>();
	private Edge edge;
	private ClassObject sourceClassObject;
	private ClassObject targetClassObject;
	private IWorkbenchPartSite iwps;
	private ClassObject classObjectToMove;
	private List<Motif<Vertex, Edge>> motifs;
	private DirectedGraph<Vertex, Edge> g;
	private Candidate reservedMoveCandidate = null;
	private String graphSource;
	
	public ConstraintsResult getConstraintsResult() {
		return constraintsResult;
	}
	
	public void setConstraintsResult(ConstraintsResult cr) {
		this.constraintsResult = cr;
	}
	
	public String getClassToMove() {
		return sourceClass;
	}

	public String getTargetPackage() {
		return targetPackage;
	}

	public void setSourceClass(String classToMove) {
		this.sourceClass = classToMove;
		
	}

	public void setTargetPackage(String targetPackage) {
		this.targetPackage = targetPackage;
		
	}

	public String getSourcePackage() {
		return sourcePackage;
	}

	public void setSourcePackage(String sourcePackage) {
		this.sourcePackage = sourcePackage;
	}
	
	/**
	 * this method is used to set classes that were moved on the graph
	 * this can be used to reset the graph
	 */
	public void setClassesMoved(List<Vertex> classes) {
		this.classesMoved  = classes;
	}
	public List<Vertex> getClassesMoved() {
		return classesMoved;
	}

	public void setProject(IProject iProject) {
		this.project = iProject;
		
	}
	public IProject getProject() {
		return project;
	}

	public void setInstancesBefore(int numberOfInstances) {
		this.instancesBefore = numberOfInstances;
		
	}

	public void setInstancesAfter(int numberOfInstances) {
		this.instancesAfter = numberOfInstances;
		
	}

	public int getInstancesBefore() {
		return instancesBefore;
	}

	public int getInstancesAfter() {
		return instancesAfter;
	}

	public String getTargetClass() {
		return targetClass;
	}

	public void setTargetClass(String targetClass) {
		this.targetClass = targetClass;
	}

	public String getEdgeType() {
		return edgeType;
	}

	public void setEdgeType(String edgeType) {
		this.edgeType = edgeType;
	}

	public String getSourceClass() {
		return sourceClass;
	}

	public void setEdge(Edge winner) {
		this.edge = winner;
	}
	
	public Edge getEdge() {
		return edge;
	}

	public void setSourceClassObject(ClassObject sourceClassObject) {
		this.sourceClassObject = sourceClassObject;
		
	}
	public ClassObject getSourceClassObject() {
		return sourceClassObject;
	}
	public void setTargetClassObject(ClassObject targetClassObject) {
		this.targetClassObject = targetClassObject;
		
	}
	public ClassObject getTargetClassObject() {
		return targetClassObject;
	}

	public IWorkbenchPartSite getWorkBench() {
		return iwps;
	}

	public void setWorkbench(IWorkbenchPartSite iwps) {
		this.iwps = iwps;
		
	}

	public void setClassObjectToMove(ClassObject classObject) {
		this.classObjectToMove = classObject;
		
	}

	public ClassObject getClassObjectToMove() {
		return classObjectToMove;
	}

	public List<Motif<Vertex,Edge>> getMotifs() {
		return motifs;
	}

	public DirectedGraph<Vertex, Edge> getGraph() {
		return g;
	}

	public void setMotifs(List<Motif<Vertex, Edge>> motifs) {
		this.motifs = motifs;
		
	}

	public void setGraph(DirectedGraph<Vertex, Edge> g) {
		this.g = g;
		
	}

	public Candidate getReservedMoveCandidate() {
		return reservedMoveCandidate;
	}

	public void setReservedMoveCandidate(Candidate getReservedMoveCandidate) {
		this.reservedMoveCandidate = getReservedMoveCandidate;
	}

	public void setGraphSource(String graphSource) {
		this.graphSource = graphSource;
		
	}

	public String getGraphSource() {
		return graphSource;
	}
	
}
