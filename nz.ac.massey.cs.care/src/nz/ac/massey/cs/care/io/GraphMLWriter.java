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
import java.io.PrintWriter;
import java.io.Writer;

import nz.ac.massey.cs.care.Edge;
import nz.ac.massey.cs.care.Vertex;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.io.GraphIOException;

/**
 * Simple utility to write graphml files. 
 * memory.
 * @author jens dietrich
 */
public class GraphMLWriter {

	private Writer writer = null;

	public GraphMLWriter(Writer writer) {
		super();
		this.writer = writer;
	}
	public synchronized void writeGraph(DirectedGraph<Vertex, Edge> g) throws GraphIOException {
		PrintWriter out = new PrintWriter(writer);
		out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		out.println("<graphml xmlns=\"http://graphml.graphdrawing.org/xmlns/graphml\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://graphml.graphdrawing.org/xmlns/graphml\">");
		out.println("<graph edgedefault=\"directed\" file=\"\">");
		for (Vertex v:g.getVertices()) {
			write(out,v);
		}
		for (Edge e:g.getEdges()) {
			write(out,e);
		}
		out.println("</graph>");
		out.println("</graphml>");
	}

	private void write(PrintWriter out, Vertex v) {
		out.print("<node ");
		printAttr(out,"id",v.getId());
		printAttr(out,"container",v.getContainer());
		printAttr(out,"namespace",v.getNamespace());
		printAttr(out,"name",v.getName());
		printAttr(out,"cluster",v.getCluster());
		printAttr(out,"type",v.getType());
		printAttr(out,"isAbstract",String.valueOf(v.isAbstract()));
		out.println(" />");
	}
	
	private void printAttr(PrintWriter out, String name, String value) {
		out.print(name);
		out.print("=\"");
		out.print(value);
		out.print("\" ");
	}
	private void write(PrintWriter out, Edge e) {
		out.print("<edge ");
		printAttr(out,"id",e.getId());
		printAttr(out,"source",e.getStart().getId());
		printAttr(out,"target",e.getEnd().getId());
		printAttr(out,"type",e.getType());
		out.println(" />");		
	}
	
	public synchronized void close() throws IOException {
		if (this.writer!=null) {
			writer.close();
		}
	}

}
