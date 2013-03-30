package tests.nz.ac.massey.cs.care.move;

import static org.junit.Assert.*;

import java.io.File;
import java.util.*;

import static nz.ac.massey.cs.care.move.io.JarReader.*;
import nz.ac.massey.cs.care.move.*;
import nz.ac.massey.cs.care.move.io.JarReader;

import org.junit.After;
import org.junit.Test;

import com.jeantessier.classreader.Attribute_info;
import com.jeantessier.classreader.Classfile;
import com.jeantessier.classreader.ConstantPoolEntry;
import com.jeantessier.classreader.Field_info;
import com.jeantessier.classreader.Method_info;
import com.jeantessier.classreader.impl.InnerClass;
import com.jeantessier.classreader.impl.InnerClasses_attribute;
import com.jeantessier.dependency.ClassNode;
import com.jeantessier.dependency.FeatureNode;
import com.jeantessier.dependency.MetricsGatherer;
import com.jeantessier.dependency.Node;
import com.jeantessier.metrics.Metrics;

import edu.uci.ics.jung.graph.DirectedGraph;

public class JarReaderTests {

	@After
	public void cleanup() throws Exception {
		JarReader.close();
	}
	@Test
	public void test() throws Exception {
		String filename = "test-data/test1.jar";
        DirectedGraph<Vertex,Edge> g = loadGraph(filename);
        print(g.getVertexCount());
        for(Vertex v : g.getVertices()) {
        	print(v.getFullname());
        }
        for(Edge e : g.getEdges()){
        	print(e.getStart().getFullname() + "->" + e.getEnd().getFullname());
        }
        String classname = "a.C";
		Set<Field_info> nonPublicOutboundFields = getNonPublicIntraPackageOutboundFields(classname);
		Set<Method_info> nonPublicOutboundMethods = getNonPublicIntraPackageOutboundMethods(classname);
		int numOfRefactoringsRequired = nonPublicOutboundFields.size() + nonPublicOutboundMethods.size();
//		System.out.println("printing outgoing dependencies on " + classname);
//		print("Non-PUblic methods are: ");
//		for(Method_info m : nonPublicOutboundMethods) {
//			print(m.getFullSignature());
//		}
//		print("Non-public fields are: ");
//		for(Field_info f : nonPublicOutboundFields) {
//			print (f.getFullSignature());
//		}
		assertEquals(2, numOfRefactoringsRequired);
	}

	@Test
	public void test2() throws Exception {
		String filename = "test-data/test1.jar";
        loadGraph(filename);
        String classname = "a.C";
        Set<Field_info> nonPublicInboundFields = JarReader.getNonPublicIntraPackageInboundFields(classname);
		Set<Method_info> nonPublicInboundMethods = JarReader.getNonPublicIntraPackageInboundMethods(classname);
		int numOfRefactoringsRequired = nonPublicInboundFields.size() + nonPublicInboundMethods.size();
//		print("Non-PUblic methods are: ");
//		for(Method_info m : nonPublicInboundMethods) {
//			print(m.getFullSignature());
//		}
//		print("Non-public fields are: ");
//		for(Field_info f : nonPublicInboundFields) {
//			print (f.getFullSignature());
//		}
		assertEquals(1, numOfRefactoringsRequired);
	}
	@Test
	public void testBytecode() throws Exception {
		String filename = "test-data/test-bytecode.jar";
        DirectedGraph<Vertex, Edge> g = loadGraph(filename);
        for(Edge e : g.getEdges()) {
        	System.out.println(e.toString());
        }
        assertEquals(3,g.getEdgeCount());
	}
	@Test
	public void test3() throws Exception {
		String filename = "test-data/test1.jar";
        loadGraph(filename);
        String classname = "a.C";
        int n = getChangeAccessabilityRefactoringsCount(classname);
        assertEquals(3, n);
	}
	
	@Test
	public void test4() throws Exception {
		String filename = "test-data/test1.jar";
        loadGraph(filename);
        String classname = "a.C";
        Set<Field_info> fields = getIntraPackageNonPublicFields(classname);
        assertEquals(1, fields.size());
	}
	
	@Test
	public void test5() throws Exception {
		String filename = "test-data/test1.jar";
        loadGraph(filename);
        String classname = "a.C";
        Set<Method_info> methods = JarReader.getIntraPackageNonPublicMethods(classname);
        assertEquals(2,methods.size());
	}
	
	
	@Test
	public void test8() throws Exception {
		String filename = "test-data/test2.jar";
        loadGraph(filename);
        String classname = "a.D";
        int n = JarReader.getChangeAccessabilityRefactoringsCount(classname);
        Set<Field_info> fields = JarReader.getIntraPackageNonPublicFields(classname);
        Set<Method_info> methods = JarReader.getIntraPackageNonPublicMethods(classname);
        Set<Classfile> classes = JarReader.getIntraPackageNonPublicClasses(classname);
        for(Field_info f : fields) {
        	print(f.getFullSignature());
        }
        for(Method_info m : methods) {
        	print(m.getFullSignature());
        }
        for(Classfile cf : classes) {
        	print(cf.getClassName());
        }
        assertEquals(2, n);
	}
	
	@Test
	public void test9() throws Exception {
		String filename = "test-data/test2.jar";
        loadGraph(filename);
        boolean result = JarReader.isRenameRequired("D", "b");
        assertEquals(false, result);
	}
	@Test
	public void test10() throws Exception {
		String filename = "test-data/test2.jar";
        loadGraph(filename);
        boolean result = JarReader.isRenameRequired("B", "b");
        assertEquals(true, result);
	}
	
	
	@Test
	public void test12() throws Exception{
		//test for checking the outgoing links for inner classes
		String filename = "test-data/test4.jar";
		loadGraph(filename);
		List<String> innerClasses = JarReader.getInnerClasses("a.C");
		int fieldscount = 0, methodscount = 0;
		for(String classname : innerClasses){
			Set<Field_info> fields = JarReader.getNonPublicIntraPackageOutboundFields(classname);
			Set<Method_info> methods = JarReader.getNonPublicIntraPackageOutboundMethods(classname);
			for (Field_info f : fields) {
				print(f.getFullSignature());
				fieldscount++;
			}
			for (Method_info m : methods) {
				print(m.getFullSignature());
				methodscount ++;
			}
		}
		assertEquals(1,fieldscount + methodscount);
	}
	
	@Test
	public void test13() throws Exception{
		//test for checking the incoming links for inner classes.
		//for inner class don't compute incoming fields references. 
		String filename = "test-data/test4.jar";
		loadGraph(filename);
		List<String> innerClasses = JarReader.getInnerClasses("a.C");
		int methodscount = 0;
		for(String classname : innerClasses){
			print("--------");
			print(classname);
			print("--------");
			Set<Field_info> fields = getNonPublicIntraPackageInboundFields(classname);
			for(Field_info f: fields){
				print(f.getFullName());
			}
			Set<Method_info> methods = JarReader.getNonPublicIntraPackageInboundMethods(classname);
			for(Method_info m : methods){
				print(m.getFullName());
			}
			methodscount += methods.size();
		}
		assertEquals(2,methodscount);
	}
	
	@Test
	public void test14() throws Exception{
		String filename = "test-data/test4.jar";
		loadGraph(filename);
		String classname = "a.C$1";
		assertEquals("a.C",getOuterClass(classname));
	}
	
	@Test
	public void test15() throws Exception{
		String filename = "test-data/test4.jar";
		loadGraph(filename);
		String classname = "a.C$1";
		assertEquals("a.C",getOuterClass(classname));
	}
	@Test
	public void test16() throws Exception{
		String filename = "test-data/test5.jar";
		loadGraph(filename);
		String classname = "a.C";
		List<String> inners = getInnerClasses(classname);
		int outer=0, inner =0;
		Set<Method_info> ms2 = getIntraPackageNonPublicMethods(classname);
		outer += ms2.size();
		for(String inner1 : inners) {
			Set<Method_info> ms3 = getIntraPackageNonPublicMethods(inner1);
			inner+=ms3.size();
		}
		Set<Classfile> cfs = getIntraPackageNonPublicClasses(classname);
		assertEquals(3,outer+inner+cfs.size());
	}
	@Test
	public void test17() throws Exception {
		String filename = "test-data/test5.jar";
		loadGraph(filename);
		String classname = "a.C$Member";
		int n = getChangeAccessabilityRefactoringsCount(classname);
		assertEquals(3,n);
	}
	@Test
	public void test18() throws Exception {
		String filename = "test-data/test5.jar";
		loadGraph(filename);
		String classname = "b.B";
		int n = getChangeAccessabilityRefactoringsCount(classname);
		assertEquals(0,n);
	}
	@Test
	public void test19() throws Exception {
		String filename = "test-data/test5.jar";
		loadGraph(filename);
		String classname = "a.D";
		int n = getChangeAccessabilityRefactoringsCount(classname);
		assertEquals(4,n);
	}
	@Test
	public void test20() throws Exception {
		String filename = "test-data/test6.jar";
		loadGraph(filename);
		String classname = "a.Inner";
		ClassNode outer = getClassnode(getOuterClass(classname));
		assertEquals("a.C",outer.getName());
	}
	@Test
	public void test21() throws Exception {
		String filename = "test-data/test6.jar";
		loadGraph(filename);
		List<String> inners = getInnerClasses("a.C");
		assertEquals(4,inners.size());
	}
	@Test
	public void test22() throws Exception {
		String filename = "test-data/test6.jar";
		loadGraph(filename);
		String classname = "a.C";
		int n = getChangeAccessabilityRefactoringsCount(classname);
		for(String inner : getInnerClasses(classname)) {
			n += getChangeAccessabilityRefactoringsCount(inner);
		}
		assertEquals(4,n);
	}
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
