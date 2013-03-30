package nz.ac.massey.cs.care.move.scoring;

import nz.ac.massey.cs.care.move.*;
import nz.ac.massey.cs.guery.Motif;
import nz.ac.massey.cs.guery.Path;

public class DefaultScoringFunction implements ScoringFunction {

	@Override
	public int getEdgeScore(Motif<Vertex,Edge> motif, String pathRole,Path<Vertex,Edge> path,Edge e) {
		return 1;
	}
	
	@Override
	public String toString() {
		return "simple scoring function (score is always 1)";
	}

}
