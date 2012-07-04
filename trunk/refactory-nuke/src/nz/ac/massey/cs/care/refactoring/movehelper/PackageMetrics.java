package nz.ac.massey.cs.care.refactoring.movehelper;

import java.util.HashMap;
import java.util.Map;
import nz.ac.massey.cs.gql4jung.Edge;
import nz.ac.massey.cs.gql4jung.Vertex;
import edu.uci.ics.jung.graph.DirectedGraph;

public class PackageMetrics {
	public class Metrics {
		// Afferent Couplings (Ca): The number of other packages that depend upon classes within the package is an indicator of the package's responsibility.
		public int getCA() {
			return CA;
		}
		// Efferent Couplings (Ce): The number of other packages that the classes in the package depend upon is an indicator of the package's independence.
		public int getCE() {
			return CE;
		}
		// Abstractness (A): The ratio of the number of abstract classes (and interfaces) in the analyzed package to the total number of classes in the analyzed package.
		public double getA() {
			double total=nonabstractcount+abstractcount;
			return total==0?0:(double)abstractcount/total;
		}
		// Instability (I): The ratio of efferent coupling (Ce) to total coupling (Ce + Ca) such that I = Ce / (Ce + Ca). This metric is an indicator of the package's resilience to change.
		public double getI() {
			if (CE+CA==0) return 0;
			return (double)CE/(double)(CA+CE);
		}
		// Distance from the Main Sequence (D): The perpendicular distance of a package from the idealized line A + I = 1. This metric is an indicator of the package's balance between abstractness and stability
		public double getD() {
			double v = getA() + getI();
			return Math.abs(1-v);
		}
		private int CA = 0; 
		private int CE = 0; 
		private int abstractcount = 0;
		private int nonabstractcount = 0;
	}
	
	/**
	 * Computes the metrics for the packages in a set of containers.
	 * @param graph
	 * @param containers
	 * @return
	 * @throws Exception
	 */
	public Map<String,Metrics> compute( DirectedGraph<Vertex, Edge> graph,String... containers) throws Exception {
		Map<String,Metrics> metrics = new HashMap<String,Metrics>();
		// compute CE
		for (Vertex v:graph.getVertices()) {
			if (includeRefs(v,containers)) {
				String ns = v.getNamespace();
				Metrics m = getOrAdd(metrics,ns);
				for (Edge e:v.getOutEdges()) {
					if (includeRefs(e.getEnd(),containers) && !ns.equals(e.getEnd().getNamespace())) {
						m.CE = m.CE+1;
					} 
				}
			}
		}
		// compute CA
		for (Vertex v:graph.getVertices()) {
			if (includeRefs(v,containers)) {
				String ns = v.getNamespace();
				Metrics m = getOrAdd(metrics,ns);
				for (Edge e:v.getInEdges()) {
					if (includeRefs(e.getStart(),containers) && !ns.equals(e.getStart().getNamespace())) {
						m.CA = m.CA+1;
					} 
				}
			}
		}
		// compute A
		for (Vertex v:graph.getVertices()) {
			if (includeRefs(v,containers)) {
				String ns = v.getNamespace();
				Metrics m = getOrAdd(metrics,ns);
				if (v.isAbstract() || v.getType().equals("interface")) {
					m.abstractcount = m.abstractcount+1;
				}
				else {
					m.nonabstractcount = m.nonabstractcount+1;
				}
			}
		}	
		
		
		return metrics;
	}
	
	private Metrics getOrAdd(Map<String,Metrics> metrics,String namespace) {
		Metrics m = metrics.get(namespace);
		if (m==null) {
			m = new Metrics();
			metrics.put(namespace, m);
		}
		return m;
	}
	/**
	 * Filter which references to include. By default, only consider types within a given set of containers (jars).
	 * @param v
	 * @param containers
	 * @return
	 */
	public boolean includeRefs(Vertex v,String... containers) {
		/*
		String container = v.getContainer();
		for (String c:containers) {
			if (c.equals(container)) return true;
		}
		return false;
		*/
		return true;
	}
}
