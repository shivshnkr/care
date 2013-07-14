/**
 * Copyright 2009 Jens Dietrich Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 
 * Unless required by applicable law or agreed to in writing, software distributed under the 
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied. See the License for the specific language governing permissions 
 * and limitations under the License.
 */


package nz.ac.massey.cs.care.io;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import nz.ac.massey.cs.care.Edge;
import nz.ac.massey.cs.care.Vertex;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.io.GraphIOException;
import edu.uci.ics.jung.io.GraphReader;

/**
 * Simple utility to read graphml files. Not very effective, stores graph and xml at the same time in
 * memory.
 * @author jens dietrich
 */
public class GraphMLReader implements GraphReader<DirectedGraph<Vertex,Edge>, Vertex, Edge> {

	private Reader reader = null;
	private int idCounter = 0;

	public GraphMLReader(Reader reader) {
		super();
		this.reader = reader;
	}
	@Override
	public synchronized DirectedGraph<Vertex, Edge> readGraph() throws GraphIOException {
		
		Map<String,Vertex> vertices = new HashMap<String,Vertex>();	
		Map<String,Edge> edges = new HashMap<String,Edge>();
		// parse
		SAXBuilder builder = new SAXBuilder();
		try {
			Namespace NS_GRAPHML = Namespace.getNamespace("http://graphml.graphdrawing.org/xmlns/graphml");
			Document doc = builder.build(reader);
			Element root = doc.getRootElement();
			assert "graphml".equals(root.getName());
			Element eGraph = root.getChild("graph",NS_GRAPHML); 
			for (Object o:eGraph.getChildren("node",NS_GRAPHML)) {
				if (o instanceof Element) {
					Vertex v = buildVertex(vertices,(Element)o);
				}
			}
			for (Object o:eGraph.getChildren("edge",NS_GRAPHML)) {
				if (o instanceof Element) {
					Edge e = buildEdge(vertices,(Element)o);
					if (edges.containsKey(e.getId())) {
						throw new GraphIOException("There are two edges with the same id " + e.getId() + " in the graph");
					}
					edges.put(e.getId(),e);
				}
			}
			// TODO: at this stage both the xml doc and the graph are in memory
			// we could gc the doc before we continue
			// build graph
			
		} catch (Exception e) {
			throw new GraphIOException(e);
		}
		DirectedGraph<Vertex, Edge> graph = new DirectedSparseGraph<Vertex, Edge> ();
		for (Vertex v:vertices.values()) {
			graph.addVertex(v);
		}
		for (Edge e:edges.values()) {
			graph.addEdge(e,e.getStart(),e.getEnd());
		}
		return graph;
		
	}
	private String createNextId(String prefix) {
		this.idCounter = idCounter+1;
		return prefix+idCounter;
	}
	private Edge buildEdge(Map<String, Vertex> vertices, Element e) throws GraphIOException {
		Edge edge = new Edge();
		String id = e.getAttributeValue("id");
		if (id==null) id = createNextId("e");
		edge.setId(id);
		
		String source = e.getAttributeValue("source");
		if (source==null) throw new GraphIOException("Source attribute missing in edge " + id);
		Vertex v1 = vertices.get(source);
		if (v1 == null) throw new GraphIOException("No vertex found for id " + source);
		edge.setStart(v1);
		
		String target = e.getAttributeValue("target");
		if (target==null) throw new GraphIOException("Target attribute missing in edge " + id);
		Vertex v2 = vertices.get(target);
		if (v2 == null) throw new GraphIOException("No vertex found for id " + source);
		edge.setEnd(v2);
		
		String type = e.getAttributeValue("type");
		if (type==null) throw new GraphIOException("Type attribute missing in edge " + id);
		edge.setType(type);
		
		return edge;
	}
	private Vertex buildVertex(Map<String, Vertex> vertices, Element e) throws GraphIOException {
		Vertex v = new Vertex();
		String id = e.getAttributeValue("id");
		if (id==null) id = this.createNextId("v");
		v.setId(id);
		v.setName(e.getAttributeValue("name"));
		v.setAbstract("true".equals(e.getAttributeValue("isAbstract")));
		v.setNamespace(e.getAttributeValue("namespace"));
		v.setType(e.getAttributeValue("type"));
		v.setCluster(e.getAttributeValue("cluster"));
		v.setContainer(e.getAttributeValue("container"));
		// register
		if (vertices.containsKey(v.getId())) {
			throw new GraphIOException("There are two nodes with the same id " + v.getId() + " in the graph");
		}
		vertices.put(v.getId(),v);
		return v;
	}
	@Override
	public synchronized void close() throws GraphIOException {
		if (this.reader!=null) {
			try {
				this.reader.close();
			} catch (IOException e) {
				throw new GraphIOException(e);
			}
		}
	}

}
