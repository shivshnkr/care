package nz.ac.massey.cs.care.refactoring.constraints;

import static nz.ac.massey.cs.care.util.MoveUtils.countAllInstances;
import nz.ac.massey.cs.care.Edge;
import nz.ac.massey.cs.care.Postcondition;
import nz.ac.massey.cs.care.Vertex;
import nz.ac.massey.cs.care.refactoring.movehelper.Candidate;
import edu.uci.ics.jung.graph.DirectedGraph;

public class CheckInstanceCountPostcondition implements Postcondition {

	@Override
	public boolean isFailed(Candidate candidate) {
		DirectedGraph<Vertex, Edge> g = candidate.getGraph();
		candidate.setInstancesAfter(countAllInstances(g, candidate.getMotifs()).getNumberOfInstances());
		if(candidate.getInstancesAfter() <= candidate.getInstancesBefore()) return false;
		else return true;
	}

	@Override
	public boolean isGraphLevel() {
		return true;
	}

	@Override
	public String getName() {
		return "Instance Count";
	}

}
