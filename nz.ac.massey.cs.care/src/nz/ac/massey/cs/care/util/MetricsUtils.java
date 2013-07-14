package nz.ac.massey.cs.care.util;

import static nz.ac.massey.cs.care.util.Utils.loadPowerGraph;

import java.util.Map;

import nz.ac.massey.cs.care.E;
import nz.ac.massey.cs.care.Edge;
import nz.ac.massey.cs.care.V;
import nz.ac.massey.cs.care.Vertex;
import nz.ac.massey.cs.care.metrics.PackageMetrics;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.Graph;

public class MetricsUtils {
	public static double getDistance(DirectedGraph<Vertex, Edge> g) throws Exception {
		Graph<V,E> pg = loadPowerGraph(g);
		PackageMetrics pm = new PackageMetrics();
		Map<String, PackageMetrics.Metrics> values = pm.compute(g, "");
		double avgDistance = 0.0;
		for(V p : pg.getVertices()) {
			PackageMetrics.Metrics pMetrics = values.get(p.getName());
			avgDistance = avgDistance + pMetrics.getD();
		}
		
		return avgDistance/pg.getVertices().size();
	}
}
