/**
 * Copyright 2009 Jens Dietrich Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package nz.ac.massey.cs.care.refactoring.movehelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import com.google.common.base.Function;

import nz.ac.massey.cs.gql4jung.E;
import nz.ac.massey.cs.gql4jung.Edge;
import nz.ac.massey.cs.gql4jung.V;
import nz.ac.massey.cs.gql4jung.Vertex;
import nz.ac.massey.cs.gql4jung.io.JarReader;
import nz.ac.massey.cs.guery.Motif;
import nz.ac.massey.cs.guery.MotifInstance;
import nz.ac.massey.cs.guery.MotifReaderException;
import nz.ac.massey.cs.guery.Path;
import nz.ac.massey.cs.guery.io.xml.XMLMotifReader;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseGraph;

/**
 * Utility methods.
 * @author jens dietrich, modified by Ali
 */
public class Utils {
	
	public final static String SEP = ",";
	
	public static Motif<Vertex,Edge> loadQuery(File file) throws IOException,MotifReaderException {
		if (file.isDirectory()) return null;
		InputStream in = new FileInputStream(file);
		return new XMLMotifReader().read(in);
	}
	public static DirectedGraph<Vertex, Edge> loadGraph(String name) throws Exception {

		File in = new File(name);
		JarReader reader = new JarReader(in);
		return reader.readGraph();
	}
	public static DirectedGraph<Vertex, Edge> loadGraph(List<File> jars) throws Exception {
		JarReader reader = new JarReader(jars);
		return reader.readGraph();
	}
	
	public static void log(Object... s) {
		for (Object t:s) {
			System.out.print(t);
		}
		System.out.println();
	}
	// filter to allow only files wit a certain extension
	public static FileFilter getExtensionFileFilter(final String extension) {
		return new FileFilter(){
			@Override
			public boolean accept(File f) {
				return f.getName().endsWith(extension);
			}
		};
	}
	// filter to exclude hidden files
	public static FileFilter getExcludeHiddenFilesFilter() {
		return new FileFilter(){
			@Override
			public boolean accept(File f) {
				return !f.getName().startsWith(".");
			}
		};
	}
	
	// get or add a folder
	public static File getOrAddFolder(String name) { 
		File f = new File(name);
		if (!f.exists()) {
			f.mkdirs();
		}
		return f;
	}
	
	public static String replaceExtension(String name,String oldExtension,String newExtension) {
		String n = name.endsWith(oldExtension)?name.substring(0,name.length()-oldExtension.length()):name;
		return n+newExtension;
	}
	
	public static String removeExtension(String name,String extension) {
		return name.endsWith(extension)?name.substring(0,name.length()-extension.length()):name;
	}
	
	public static String readValueFromCSV(File file,int col, int row) throws Exception {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line = null;
		int c = -1;
		while ((line=reader.readLine())!=null) {
			c=c+1;
			if (c==row) break;
		}
		reader.close();
			
		if (line==null) {
			return null;
		}
		else {
			// scan line
			c = -1;
			String token = null;
			for (StringTokenizer tok=new StringTokenizer(line,SEP);tok.hasMoreTokens();) {
				c=c+1;
				token = tok.nextToken();
				if (c==col) return token;			
			}
		}
		return null;
	}
	
	public static int readIntValueFromCSV(File file,int col, int row) throws Exception {
		
		return Integer.valueOf(readValueFromCSV(file,col,row));
	}
	
	public static String getPackageName (String fullClassName) {
		return fullClassName.substring(0,fullClassName.lastIndexOf('.'));
	}
	public static String getClassName (String fullClassName) {
		return fullClassName.substring(fullClassName.lastIndexOf('.'));
	}
	// assume that ranks assigns only non-negative ints
	public static List<Edge> findLargestByIntRanking(Collection<Edge> coll,Function<Edge,Integer> ranks) {
		Map<Edge,Integer> edgeRanks = new HashMap<Edge, Integer>();
		for(Edge e : coll){
			int r = ranks.apply(e);
			edgeRanks.put(e, r);
		}
		Map<Edge,Double> sortedEdgeRanks = sortByValue(edgeRanks);
//		double rank = 0;
		List<Edge> largest = new ArrayList<Edge>();
		int count = 1;
		for(Entry<Edge, Double> e : sortedEdgeRanks.entrySet()){
			if(count < 200){
				largest.add(e.getKey());
				count++ ; 
			} else break;
		}
//		int rank = 0;
//		List<Edge> largest = new ArrayList<Edge>();
//		for (Edge edge:coll) {
//			//if(!edge.getStart().getNamespace().equals(edge.getEnd().getName())) continue;
//			int r = ranks.apply(edge);
//			if (r>rank) {
//				String srcNS = edge.getStart().getNamespace();
//				String tgtNS = edge.getEnd().getNamespace();
//				if(!srcNS.equals(tgtNS)){
//					rank=r;
//					largest.clear();
//					largest.add(edge);
//				}
//			}
//			else if (r==rank) {
//				String srcNS = edge.getStart().getNamespace();
//				String tgtNS = edge.getEnd().getNamespace();
//				if(!srcNS.equals(tgtNS)){
//					largest.add(edge);
//				}
//			}
//		}
		return largest;
	}
	
	public static List<Edge> findLargestByDoubleRanking(Collection<Edge> coll,Function<Edge,Double> ranks) {
//		Map<Edge, Integer> edgeRanks = new HashMap<Edge, Integer>();
//		for(Edge e : coll){
//			double r = ranks.apply(e);
//			edgeRanks.put(e, r);
//		}
//		Map<Edge,Integer> sortedEdgeRanks = sortByValue(edgeRanks);
////		double rank = 0;
//		List<Edge> largest = new ArrayList<Edge>();
//		int count = 1;
//		for(Entry<Edge, Integer> e : sortedEdgeRanks.entrySet()){
//			if(count < 100){
//				largest.add(e.getKey());
//				count++ ; 
//			} else break;
//		}
//		for (Edge element:coll) {
//			double r = ranks.apply(element);
//			if (r>rank) {
//				rank=r;
//				largest.clear();
//				largest.add(element);
//			}
//			else if (r==rank) {
//				largest.add(element);
//			}
//		}
//		return largest;
		return null;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	static Map<Edge, Double> sortByValue(Map<Edge, Integer> edgeRanks) {
	     List list = new LinkedList(edgeRanks.entrySet());
	     Collections.sort(list, new Comparator() {
	          public int compare(Object o1, Object o2) {
	               return - ((Comparable) ((Map.Entry) (o1)).getValue())
	              .compareTo(((Map.Entry) (o2)).getValue());
	          }
	     });

	    Map result = new LinkedHashMap();
	    for (Iterator it = list.iterator(); it.hasNext();) {
	        Map.Entry entry = (Map.Entry)it.next();
	        result.put(entry.getKey(), entry.getValue());
	    }
	    return result;
	}
	
	public static String stringify (MotifInstance<Vertex,Edge> instance) {
		StringBuffer b = new StringBuffer();
		Motif<Vertex,Edge> motif = instance.getMotif();
		char SEP = '\n';
		for (String role:motif.getRoles()) {
			b.append(role)
			.append(": ")
			.append(stringify(instance.getVertex(role)))
			.append(SEP);
		}
		for (String role:motif.getPathRoles()) {
			b.append(role)
			.append(": ")
			.append(stringify(instance.getPath(role)))
			.append(SEP);
		}
		return b.toString();
	}
	public static String stringify (Vertex vertex) {
		return vertex.getFullname();
	}
	public static String stringify (Path<Vertex,Edge> path) {
		char SEP = '\n';
		StringBuffer b = new StringBuffer()
		.append(stringify(path.getStart()));
		for (Edge edge:path.getEdges()) {
			b.append(" -> ");
			b.append(stringify(edge.getEnd()));
		}
		b.append(SEP);
		return b.toString();
	}
	/**
	 * loads the powergraph (namespace,link) from the original graph
	 * @param g
	 * @return
	 */
	public static DirectedGraph<V, E> loadPowerGraph(
			DirectedGraph<Vertex, Edge> g) {
		DirectedGraph<V,E> pg = new DirectedSparseGraph<V,E>();
		Set<String> namespaces = new HashSet<String>();
		
		for(Vertex v : g.getVertices()){
			String namespace = v.getNamespace();
			namespaces.add(namespace);
		}
		
		for(String namespace : namespaces){
			V newV = new V(namespace);
			newV.setName(namespace);
			pg.addVertex(newV);
		}
		
		for(Edge e : g.getEdges()){
			String srcNamespace = e.getStart().getNamespace();
			String tarNamespace = e.getEnd().getNamespace();
			if(srcNamespace.equals(tarNamespace)) continue;
			V src = null;
			V tar = null;
			for(V ns : pg.getVertices()){
				if(ns.getName().equals(srcNamespace)) src = ns;
			}
			for(V ns : pg.getVertices()){
				if(ns.getName().equals(tarNamespace)) tar = ns;
			}
			String edge = srcNamespace + tarNamespace;
			E newE = new E(edge,src,tar);
			if(!pg.containsEdge(newE)) pg.addEdge(newE,src,tar);
		}
		return pg;
	}
}