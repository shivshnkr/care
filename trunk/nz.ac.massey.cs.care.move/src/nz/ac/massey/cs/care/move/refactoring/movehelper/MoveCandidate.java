package nz.ac.massey.cs.care.move.refactoring.movehelper;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;

import nz.ac.massey.cs.care.move.Vertex;


public class MoveCandidate {
	private String classToMove;
	private String targetPackage;
	private String sourcePackage;
	private IProject project;
	private int instancesBefore;
	private int instancesAfter;
	private ConstraintsResult constraintsResult = new ConstraintsResult();
	private List<Vertex> classesMoved = new ArrayList<Vertex>();
	
	public ConstraintsResult getConstraintsResult() {
		return constraintsResult;
	}
	
	public String getClassToMove() {
		return classToMove;
	}

	public String getTargetPackage() {
		return targetPackage;
	}

	public void setClassToMove(String classToMove) {
		this.classToMove = classToMove;
		
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
	
}
