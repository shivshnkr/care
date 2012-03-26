package nz.ac.massey.cs.gql4jung;

import nz.ac.massey.cs.gql4jung.Edge;
import nz.ac.massey.cs.gql4jung.Vertex;
import nz.ac.massey.cs.guery.Motif;
import nz.ac.massey.cs.guery.Path;

/**
 * Function used to compute the score for an edge.
 * This can be used to associates weights with certain motifs and path roles within motifs.
 * @author jens dietrich
 *
 */
public interface ScoringFunction {

	int getEdgeScore(Motif<Vertex,Edge> motif,String pathRole,Path<Vertex,Edge> path,Edge e);
}
