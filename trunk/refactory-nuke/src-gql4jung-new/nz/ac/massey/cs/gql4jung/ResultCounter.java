/**
 * 
 */
package nz.ac.massey.cs.gql4jung;

import java.util.HashMap;
import java.util.Map;

import nz.ac.massey.cs.care.refactoring.views.CareView;
import nz.ac.massey.cs.gql4jung.Edge;
import nz.ac.massey.cs.gql4jung.Vertex;
import nz.ac.massey.cs.guery.Motif;
import nz.ac.massey.cs.guery.MotifInstance;
import nz.ac.massey.cs.guery.Path;
import nz.ac.massey.cs.guery.ResultListener;

/**
 * Result listener to keep track of scores.
 * @author jens dietrich
 *
 */
public class ResultCounter implements ResultListener<Vertex,Edge> {

	private Map<String,Map<Edge,Integer>> edgeOccByMotif = new HashMap<String,Map<Edge,Integer>>();
	private int counter = 0;

	@Override
	public void done() {
		
	}
	@Override
	public synchronized boolean found(MotifInstance<Vertex,Edge> instance) {
		counter=counter+1;
		//if (counter%100000==0) log(counter," instances found");
		Motif<Vertex,Edge> motif = instance.getMotif();
		for (String pathRole:motif.getPathRoles()) {
			Path<Vertex,Edge> path = instance.getPath(pathRole);
			for (Edge edge:path.getEdges()) {
				int score = CareView.scoringfunction.getEdgeScore(motif,pathRole,path,edge);
				register(edge,motif.getName(),score);
			}
		}
		return true;
	}
	private synchronized void register(Edge edge, String motif,int score) {
//		String srcNamespace = edge.getStart().getNamespace();
//		String tarNamespace = edge.getEnd().getNamespace();
//		if(srcNamespace.equals(tarNamespace)) return;
		Map<Edge,Integer> map = edgeOccByMotif.get(motif);
		if (map==null) {
			map = new HashMap<Edge,Integer>();
			edgeOccByMotif.put(motif,map);
		}
		Integer counter = map.get(edge);
		int c = counter==null?0:counter.intValue();
		map.put(edge,c+score);
		
		
	}
	@Override
	public synchronized void progressMade(int progress, int total) {
		/*
		if (total>5000) {
			log("Done ",progress,"/",total," looking for instances of motif ",this.currentMotif.getName()," ",this.motifCounter,"/",this.totalMotifCount);
		}
		*/
	}
	
	public synchronized int getCount(String motif,Edge edge) {
		Map<Edge,Integer> map = edgeOccByMotif.get(motif);
		if (map==null) {
			return 0; // no counts available for this motif
		}
		Integer counter = map.get(edge);
		return counter==null?0:counter.intValue();
	}
			
	public synchronized int getCount(Edge edge) {
		int total = 0;
		for (String motif:edgeOccByMotif.keySet()) {
			total = total+getCount(motif,edge);
		}
		
		// double check whether this and the sum for the counts for all patterns are consistent
		/*
		int checksum = getCount("awd",edge)+getCount("cd",edge)+getCount("deginh",edge)+getCount("stk",edge);
		if (total!=checksum) {
			System.err.println("Edge ranks do not match for " + edge);
			System.err.println("Total is " + total + " but sum of pattern ranks is " + checksum);
		}
		*/
		
		return total;
	}

	public synchronized Map<String,Integer> getEdgeParticipation(Edge e) {
		Map<String,Integer> map = new HashMap<String,Integer>();
		for (String motif:edgeOccByMotif.keySet()) {
			map.put(motif,this.getCount(motif,e));
		}	
		return map;
	}
	
	public synchronized int getNumberOfInstances() {
		return this.counter;
	}
}