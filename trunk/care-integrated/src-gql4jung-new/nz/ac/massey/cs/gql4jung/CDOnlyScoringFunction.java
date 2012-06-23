package nz.ac.massey.cs.gql4jung;

import nz.ac.massey.cs.gql4jung.*;
import nz.ac.massey.cs.guery.Motif;
import nz.ac.massey.cs.guery.Path;

public class CDOnlyScoringFunction implements ScoringFunction {

	@Override
	public int getEdgeScore(Motif<Vertex,Edge> motif, String pathRole,Path<Vertex,Edge> path,Edge e) {
		return ("cd".equals(motif.getName()))?1:0; 
	}
	
	@Override
	public String toString() {
		return "CD only scoring function";
	}

}
