package tests.nz.ac.massey.cs.care.move;

import static nz.ac.massey.cs.care.move.util.Utils.*;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nz.ac.massey.cs.care.move.*;
import nz.ac.massey.cs.care.move.io.JarReader;
import nz.ac.massey.cs.care.move.util.ResultCounter;
import nz.ac.massey.cs.care.move.util.Utils;
import nz.ac.massey.cs.guery.Motif;

import org.junit.After;
import org.junit.Test;

import com.google.common.base.Function;

import edu.uci.ics.jung.graph.DirectedGraph;

public class GraphTests {

	@After
	public void cleanup() throws Exception {
		JarReader.close();
	}
	@Test
	public void test() throws Exception {
		String filename = "test-data/jmoney-0.4.4.jar";
        DirectedGraph<Vertex,Edge> g = loadGraph(filename);
        print(g.getVertexCount());
        Motif<Vertex,Edge> scdMotif = loadMotif("queries/scd.guery");
        final ResultCounter registry = countInstances(g,scdMotif);
		int scdInstances = registry.getNumberOfInstances();
        Set<Edge> edgesWithHighestRank = findLargestByIntRanking(g.getEdges(),new Function<Edge,Integer>() {
			@Override
			public Integer apply(Edge e) {
				return registry.getCount(e);
			}});
        Comparator<Edge> comp = new Comparator<Edge>() {
			@Override
			public int compare(Edge e1, Edge e2) {
				String s1 = e1.getStart().getFullname() + ">"+e1.getEnd().getFullname();
				String s2 = e2.getStart().getFullname() + ">"+e2.getEnd().getFullname();
				return s1.compareTo(s2);
			}
		};
		List<Integer> ranks = new ArrayList<Integer>();
		for (Edge e : edgesWithHighestRank) {
			 ranks.add(registry.getCount(e)); 
		}
//		Collections.sort(edgesWithHighestRank,comp);
		assertEquals(177, edgesWithHighestRank.size());
	}
//	@Test
//	public void test1() throws Exception {
//		String filename = "test-data/ant-1.8.1.jar";
//        DirectedGraph<Vertex,Edge> g = loadGraph(filename);
//        String classname = "org.apache.tools.ant.util.FileUtils";
//        int n = JarReader.getChangeAccessabilityRefactoringsCount(classname);
//		assertEquals(0, n);
//	}
	
	
	
	protected DirectedGraph<Vertex, Edge> loadGraph(String name) throws Exception {
		File in = new File(name);
		JarReader reader = new JarReader(in);
        DirectedGraph<Vertex, Edge> g = reader.readGraph();
        return g;
	}
	
	private void print(Object... message) {
		for(Object o : message){
			System.out.print(o);
		}
		System.out.println();
	}
}
