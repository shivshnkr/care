package nz.ac.massey.cs.care.move.metrics;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nz.ac.massey.cs.care.move.*;
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.DirectedGraph;

public class SCCMetrics {
	
	private double graphSpl = 0.0; 
	private double compressionRatio = 0.0;
	private int maxSccSize = 0; 
	private double density = 0.0;
	private int count = 0;
	
	public void compute(DirectedGraph<Set<V>, Integer> sccGraph, DirectedGraph<V,E> powergraph){
		computeCompressionRatio(sccGraph, powergraph);
		computeMaxSCCSize(sccGraph, powergraph);
		computeDensity(sccGraph, powergraph);
		computeTangledness(sccGraph, powergraph);
		computeCount(sccGraph);
	}

	private void computeCount(DirectedGraph<Set<V>, Integer> sccGraph) {
		count = sccGraph.getVertexCount();
		
	}

	private void computeTangledness(DirectedGraph<Set<V>, Integer> sccGraph,
			DirectedGraph<V, E> powergraph) {
		DijkstraShortestPath<V, E> sp = new DijkstraShortestPath<V, E>(powergraph);
//		System.out.println(sccGraph.getVertexCount());
		Set<E> edgesInSCC = new HashSet<E>();
		double sccSPLAvg = 0.0;
		double sscSPLSum = 0.0;
		double graphSPLAvg = 0.0;
		int maxPossibleSPL = 0;
		double sccSPL = 0.0;
		for(Set<V> scc : sccGraph.getVertices()) {
			if(scc.size() == 1) continue;
			for(V start : scc){
				for(E e : start.getOutEdges()) {
					if(contains(scc, e.getEnd())) {
						//this means that the next vertex is in the scc
						addEdge(edgesInSCC, e);
					} 
				}
			}
			for(E edge : edgesInSCC) {
				List<E> spl = sp.getPath(edge.getEnd(), edge.getStart());
//				System.out.println(spl.size());
				sscSPLSum = sscSPLSum + spl.size();
			}
			maxPossibleSPL = scc.size() - 1;
			sccSPLAvg = sscSPLSum/edgesInSCC.size();
			if(maxPossibleSPL == 1) sccSPL = 0.0;
			else sccSPL = (maxPossibleSPL - sccSPLAvg) / (maxPossibleSPL - 1);
			graphSPLAvg = graphSPLAvg + sccSPL;
//			System.out.println("Average is: " + sccSplAvg);
			sccSPL = 0.0;
			sccSPLAvg = 0.0;
			sscSPLSum = 0.0;
			edgesInSCC.clear();
		}
		
		graphSpl = graphSPLAvg/sccGraph.getVertexCount();
//		System.out.println("overall avg is: " + graphSpl);
		
	}

	public double getTangledness() {
		return round(graphSpl);
	}
	private void computeMaxSCCSize(DirectedGraph<Set<V>, Integer> sccGraph,
			DirectedGraph<V, E> powergraph) {
		List<Integer> numsList = new ArrayList<Integer>();
		for(Set<V> set : sccGraph.getVertices()) {
			numsList.add(set.size());
		}
		Object[] numsArray =  numsList.toArray();
		Arrays.sort(numsArray);
		maxSccSize = (Integer) numsArray[numsArray.length-1];
	}

	private void computeCompressionRatio(
			DirectedGraph<Set<V>, Integer> sccGraph,
			DirectedGraph<V, E> powergraph) {
		compressionRatio = ((double)sccGraph.getVertexCount())/((double)powergraph.getVertexCount());
	}

	private void computeDensity(DirectedGraph<Set<V>, Integer> sccg,
			DirectedGraph<V, E> pg) {
		Set<E> edgesInSCC = new HashSet<E>();
		double densityPerScc = 0.0;
		double totalDensity = 0.0;
		for(Set<V> scc : sccg.getVertices()) {
			if(scc.size() == 1) continue;
			for(V start : scc){
				for(E e: start.getOutEdges()) {
					if(contains(scc, e.getEnd())) {
						addEdge(edgesInSCC, e);
					}
				}
			}
//		System.out.println("Component has :" + edgesInSCC.size() + " edges");
		int n = scc.size();
		if(n == 2) {
			densityPerScc = 0.0;
		} else {
			densityPerScc = (double) (edgesInSCC.size() - n) / ((n * n) - (2 * n));	
		}
		
		totalDensity = totalDensity + densityPerScc/sccg.getVertexCount();
		edgesInSCC.clear();
		}
		density = totalDensity; 
//		System.out.println("Total Density is :" + totalDensity);
	}

	public static boolean addEdge(Set<E> edgesInSCC, E e) {
		if(e.getStart().getName().equals(e.getEnd().getName())) return false;
		boolean add = true;
		for(E edge : edgesInSCC) {
			if(e.getStart().getName().equals(edge.getStart().getName()) && 
					e.getEnd().getName().equals(edge.getEnd().getName())){
				//this means this edge exists
				add = false;
			}
		}
		if(add) edgesInSCC.add(e);
		return add;
		
	}

	public static boolean contains(Set<V> vertices, V toMatch) {
		boolean result = false;
		for(V v : vertices) {
			if(v.getName().equals(toMatch.getName())) return true;
		}
		return result;
	}

	public double getCompressionRatio() {
		return round(compressionRatio);
	}

	public int getMaxSccSize() {
		return maxSccSize;
	}
	
	private static Double round(Double val) {
		if ("NaN".equals(val.toString()))
			return new Double(-1);
		int decimalPlace = 2;
		try{
			BigDecimal bd = new BigDecimal(val.doubleValue());
			bd = bd.setScale(decimalPlace, BigDecimal.ROUND_UP);
			return bd.doubleValue();
		}catch(Exception e){
			return val;
		}
		
	}

	public double getDensity() {
		return round(density);
	}

	public Object getCount() {
		return count;
	}
}
