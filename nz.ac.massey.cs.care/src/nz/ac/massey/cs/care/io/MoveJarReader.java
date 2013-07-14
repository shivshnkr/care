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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


import org.apache.log4j.Logger;

import com.jeantessier.classreader.Attribute_info;
import com.jeantessier.classreader.Classfile;
import com.jeantessier.classreader.ClassfileLoader;
import com.jeantessier.classreader.LoadEvent;
import com.jeantessier.classreader.LoadListener;
import com.jeantessier.classreader.Method_info;
import com.jeantessier.classreader.SourceFile_attribute;
import com.jeantessier.classreader.TransientClassfileLoader;
import com.jeantessier.classreader.Field_info;
import com.jeantessier.dependency.ClassNode;
import com.jeantessier.dependency.CodeDependencyCollector;
import com.jeantessier.dependency.DependencyEvent;
import com.jeantessier.dependency.DependencyListener;
import com.jeantessier.dependency.FeatureNode;
import com.jeantessier.dependency.Node;
import com.jeantessier.dependency.NodeFactory;

import nz.ac.massey.cs.care.Edge;
import nz.ac.massey.cs.care.Vertex;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.io.GraphIOException;

/**
 * Reads the graph from jar files, used depfind
 * (http://depfind.sourceforge.net/).
 * This also checks several features for move class refactoring, 
 * e.g. does a move require rename refactoring? 
 * or does it requires a change accessibility refactoring?
 * 
 * @author jens dietrich, Ali Shah
 */
public class MoveJarReader {

	private List<File> jars = null;
	private boolean removeDuplicateEdges = true;
	private boolean removeSelfRefs = true;
	private int jarCounter = 0;
	private static Logger LOG = Logger.getLogger(MoveJarReader.class);
	private static ClassfileLoader loader = null;
	private static DirectedGraph<Vertex, Edge> graph = null;
	private static Set<Classfile> classfiles = null;
	private static Map<String,Classfile> classfilesMapping = null;
	private static Map<String,ClassNode> classnodesMapping = null; //used to get dependencies info

	public MoveJarReader(List<File> jars) {
		super();
		this.jars = jars;
	}
	public MoveJarReader(File... files) {
		super();
		jars = new ArrayList<File>(files.length);
		for (File f:files) jars.add(f);
	}
	
	public boolean isRemoveDuplicateEdges() {
		return removeDuplicateEdges;
	}
	public void setRemoveDuplicateEdges(boolean removeDuplicateEdges) {
		this.removeDuplicateEdges = removeDuplicateEdges;
	}
	public boolean isRemoveSelfRefs() {
		return removeSelfRefs;
	}
	public void setRemoveSelfRefs(boolean removeSelfRefs) {
		this.removeSelfRefs = removeSelfRefs;
	}

	
	public synchronized DirectedGraph<Vertex, Edge> readGraph()	throws GraphIOException {
		// TODO - for now we remove all appenders
		// need to add proper log4j initialisation later
		// comprehensive logging leads to memory problems
//		Logger.getRootLogger().removeAllAppenders();
//		BasicConfigurator.configure();
		
		if (jars.size()==0) {
			//this.fireProgressListener(0,0);
			return new DirectedSparseGraph<Vertex, Edge>();
		}
		
		final int TOTAL = 100;
		final int TOTAL1 = 50;
		final int TOTAL2 = TOTAL-TOTAL1;
		final int PART1 = TOTAL1/jars.size();
		
		//this.fireProgressListener(0,TOTAL);
		
		NodeFactory factory = new NodeFactory();
//		SelectionCriteria filter = new ComprehensiveSelectionCriteria();
		CodeDependencyCollector collector = new CodeDependencyCollector(factory);
		
		List<String> list = new ArrayList<String>();
		for (File f:jars) {
			list.add(f.getAbsolutePath());
		}
		classfiles = new HashSet<Classfile>();
		graph = new DirectedSparseGraph<Vertex, Edge> ();
		final Map<String,Vertex> vertices = new HashMap<String,Vertex>();
		final Map<Classfile,String> containerMapping = new HashMap<Classfile,String>();
		classfilesMapping = new HashMap<String,Classfile>();
		//to notify changes only.
//		DeletingVisitor deletingVisitor = new DeletingVisitor(factory);
//	    ClassfileLoaderDispatcher dispatcher = new ModifiedOnlyDispatcher(ClassfileLoaderEventSource.DEFAULT_DISPATCHER);
//		Monitor monitor    = new Monitor(collector, deletingVisitor);
//	    monitor.setClosedSession(true);
	    loader = new TransientClassfileLoader();
//		loader.addLoadListener(monitor);
		loader.addLoadListener(new LoadListener() {
			String container = null;
			@Override
			public void beginClassfile(LoadEvent event) {}
			@Override
			public void beginFile(LoadEvent event) {}
			@Override
			public void beginGroup(LoadEvent event) {
				String name = event.getGroupName();
				try {
					File f = new File(name);
					if (f.exists() && hasClasses(f) && !name.equals(container)) {
						container = f.getName();
						jarCounter = jarCounter+1;
						//fireProgressListener(jarCounter*PART1,TOTAL);
						LOG.info("analyse file: "+container);
					}
				}
				catch (Exception x){}
			}
			private boolean hasClasses(File f) {
				if (f.isDirectory()) return true;
				else {
					String n = f.getName();
					if (n.endsWith(".jar")) return true;
					else if (n.endsWith(".zip")) return true;
					else if (n.endsWith(".war")) return true;
					else if (n.endsWith(".ear")) return true;
				}
				return false;
			}
			@Override
			public void beginSession(LoadEvent event) {
				//System.out.println("start depfind session ");
			}
			@Override
			public void endClassfile(LoadEvent event) {
				Classfile cf = event.getClassfile();
				classfiles.add(cf);
				classfilesMapping.put(cf.getClassName(), cf);
				containerMapping.put(cf,container);
			}
			@Override
			public void endFile(LoadEvent event) {}
			@Override
			public void endGroup(LoadEvent event) {}
			@Override
			public void endSession(LoadEvent event) {}
		});
		loader.load(list);
		NodeFactory dependencyGraph = collector.getFactory();
		classnodesMapping = dependencyGraph.getClasses();
		int counter = 1;
		for (Classfile classfile:classfiles) {
			addVertex(graph,classfile,counter,vertices,containerMapping);
			counter = counter+1;
		}
		
		//fireProgressListener(TOTAL1,TOTAL);
		
		collector.addDependencyListener(new DependencyListener() {
			int counter = 0;
			@Override
			public void beginClass(DependencyEvent event) {}

			@Override
			public void beginSession(DependencyEvent event) {}

			@Override
			public void dependency(DependencyEvent event) {
				Node start = event.getDependent();
				Node end = event.getDependable();
				if (start instanceof ClassNode && end instanceof ClassNode) {
					addEdge(graph,start,end,false,counter,vertices);
					counter = counter+1;
				}
				else {
					addEdge(graph,start,end,true,counter,vertices);
					counter = counter+1;
				}
			}

			@Override
			public void endClass(DependencyEvent event) {}

			@Override
			public void endSession(DependencyEvent event) {}
		});
		//System.out.println("class loaded: " + loader.getAllClassfiles().size());
		int i = 0;
		int bucket = classfiles.size()/TOTAL2;
		if (bucket==0) bucket=1;
		int j = 0;
		for (Classfile cf : classfiles) {
			collector.visitClassfile(cf);
			i=i+1;
			if (i%bucket==0) {
				j=j+1;
				//fireProgressListener(TOTAL1+j,TOTAL);
			}
		}
		//fireProgressListener(TOTAL,TOTAL);
		return graph;

	}
	
	public static Classfile getClassfile(String name) {
		return classfilesMapping.get(name);
	}
	public static ClassNode getClassnode(String name) {
		return classnodesMapping.get(name);
	}
	private void addVertex(DirectedGraph<Vertex, Edge> graph,Classfile classfile,int id,Map<String,Vertex> vertices,Map<Classfile,String> containerMapping) {
		Vertex v = new Vertex();
		v.setId(String.valueOf(id));
		v.setName(classfile.getSimpleName());
		v.setAbstract(classfile.isAbstract());
		v.setInterface(classfile.isInterface());
		v.setInnerClass(classfile.isInnerClass());
		v.setAnonymousClass(classfile.isAnonymousClass());
		v.setPublic(classfile.isPublic());
		int sep = classfile.getClassName().lastIndexOf('.');
		if (sep==-1){
			v.setNamespace("");
		}
		else {
			v.setNamespace(classfile.getClassName().substring(0,sep));
		}
		v.setType(getType(classfile));
		v.setContainer(containerMapping.get(classfile)); // not yet supported
		graph.addVertex(v);
		vertices.put(classfile.getClassName(),v);
		
		//System.out.println("Adding vertex " + v);
		
		
		
	}
	
	private void addEdge(DirectedGraph<Vertex, Edge> graph, Node start,Node end, boolean isUses, int counter, Map<String, Vertex> vertices) {
		Vertex source = findVertex(start,vertices);
		Vertex target = findVertex(end,vertices);
		if (target!=null) { // this is possible - reference to external class
			String type = null;
			if (isUses) {
				type = "uses";
			}
			else {
				if ("class".equals(source.getType()) && "interface".equals(target.getType())) {
					type = "implements";
				}
				else {
					type = "extends";
				}
			}

			boolean addEdge = true;
			if (this.removeSelfRefs && source==target) {
				addEdge = false;
			}
			if (addEdge && this.removeDuplicateEdges) {
				for (Edge e:graph.getOutEdges(source)) {
					// TODO, FIXME
					// note that jung will not allow to add another edge with the same sourec or target
					// this means, we cannot have two edges of different types (extends and uses)
					// however, this is sometimes interesting, e.g. in the composite pattern
					// solution: use flags instead of a type attribute in Vertex
					if (e.getEnd()==target) {
						addEdge=false;
						break;
					}
				}
			}
			if (addEdge) {
				Edge edge = new Edge();
				edge.setId("e-"+counter);
				edge.setStart(source);
				edge.setEnd(target);
				edge.setType(type);
				boolean added = graph.addEdge(edge,source,target);
				/** log for debugging
				if (start.toString().indexOf("org.apache.log4j.jdbc.JDBCAppender")>-1) {
					System.out.println("Adding edge " + edge + (added?"success":"failed"));
					System.out.println("  Logging outgoing edges: ");
					for (Edge e:source.getOutEdges()) {
						System.out.println("  - "+e);
					}

				}
				if (!added) {
					System.out.println("Rejected edge " + edge);
				}
				*/

			}
		}
	}

	private Vertex findVertex(Node n, Map<String, Vertex> vertices) {
		ClassNode cNode = null;
		if (n instanceof ClassNode) {
			cNode = (ClassNode)n;
		}
		else if (n instanceof FeatureNode) {
			FeatureNode fNode = (FeatureNode)n;
			cNode = fNode.getClassNode();
		}
		if (cNode!=null) {
			return vertices.get(cNode.getName());
		}
		else {
			return null;
		}
	}

	private String getType(Classfile classfile) {
		if (classfile.isInterface()) return "interface";
		else if (classfile.isEnum()) return "enum";
		else if (classfile.isAnnotation()) return "annotation";
		return "class";
	}

	public static void close() {
		loader = null;
		graph = null;
		classfiles = null;
		classfilesMapping = null;
		classnodesMapping = null; 
	}
	
	/**
	 * Non-public fields that this class uses inside a package. In case of 
	 * moving this class to another package, these fields must be made public.  
	 * @param classname
	 * @return
	 */
	public static Set<Field_info> getNonPublicIntraPackageOutboundFields(String classname) {
		ClassNode classnode = getClassnode(classname);
		if(classnode == null) return new HashSet<Field_info>();
		String packagename = classnode.getPackageNode().getName();
		ClassNode outer = getClassnode(getOuterClass(classname));
		Set<Field_info> nonPublicOutboundFields = new HashSet<Field_info>();
		for(FeatureNode fn: classnode.getFeatures()) { 
			for(Node n : fn.getOutboundDependencies()){
				if(n.isConfirmed() && n instanceof FeatureNode) {
					FeatureNode fn1 = (FeatureNode) n;
					Classfile cf = getClassfile(fn1.getClassNode().getName());
					//if dependency is from outside the package don't consider it or
					//dependency should not be on the class itself (static blocks).
					if(!fn1.getClassNode().getPackageNode().getName().equals(packagename) ||
							isDependencyFromSameCompilationUnit(fn1, outer) ) continue;
					if (isFieldNode(fn1.getName())) {
						Field_info f = cf.getField(fn1.getSimpleName());
						if (!f.isPublic()) nonPublicOutboundFields.add(f);
					}
				}
			}
		}
		return nonPublicOutboundFields;
	}
	
	/**
	 * Non-public methods that this class uses inside a package. In case of 
	 * moving this class to another package, these methods must be made public.  
	 * @param classname
	 * @return
	 */
	public static Set<Method_info> getNonPublicIntraPackageOutboundMethods(String classname) {
		ClassNode classnode = getClassnode(classname);
		Set<Method_info> nonPublicOutboundMethods = new HashSet<Method_info>();
		String packagename = classnode.getPackageNode().getName();
		ClassNode outer = getClassnode(getOuterClass(classname));
		for(FeatureNode fn: classnode.getFeatures()) { 
			for(Node n : fn.getOutboundDependencies()){
				if(n.isConfirmed() && n instanceof FeatureNode) {
					FeatureNode dependency = (FeatureNode) n;
					Classfile cf = getClassfile(dependency.getClassNode().getName());
					//if dependency is from outside the package don't consider it.
					//dependency should not be on the class itself (static blocks).
					if(!dependency.getClassNode().getPackageNode().getName().equals(packagename)|| 
							isDependencyFromSameCompilationUnit(dependency, outer)) continue;
					if(isMethodNode(dependency.getName())) {
						Method_info m = cf.getMethod(dependency.getSimpleName());
						if(!m.isPublic()) nonPublicOutboundMethods.add(m);
					}
				}
			}
		}
		return nonPublicOutboundMethods;
	}
//	private static boolean isFeatureOfInnerClass(FeatureNode feature,
//			ClassNode outerclass) {
//		ClassNode featureClass = feature.getClassNode();
//		for(ClassNode innerClassnode : getInnerClassNodes(outerclass.getName())) {
//			if(featureClass.equals(innerClassnode)) return true;
//		}
//		return false;
//	}
	/**
	 * Non-public fields of this class that are used by other classes inside a package. 
	 * In case of moving this class to another package, these fields must be made public.  
	 * @param classname
	 * @return
	 */
	public static Set<Field_info> getNonPublicIntraPackageInboundFields(String classname) {
		ClassNode classnode = getClassnode(classname);
		Classfile classfile = getClassfile(classname);
		if(classnode == null) return new HashSet<Field_info>();
		Set<Field_info> nonPublicInboundFields = new HashSet<Field_info>();
		ClassNode outer = getClassnode(getOuterClass(classname));
		for(FeatureNode fn: classnode.getFeatures()) {
			int inboundcount = 0;
			for(Node d : fn.getInboundDependencies()) {
				if(d instanceof FeatureNode) {
					FeatureNode incomingDependency = (FeatureNode) d;
					if(isDependencyFromSameCompilationUnit(incomingDependency, outer)) continue;
				} else {
					inboundcount ++;
				}
			}
			if(inboundcount > 0) {
				if(isFieldNode(fn.getName())){
					Field_info f = classfile.getField(fn.getSimpleName());
					if(!f.isPublic()) {
						nonPublicInboundFields.add(f);
					}
				}
			}
		}
		return nonPublicInboundFields;
	}
	
	private static boolean isDependencyFromSameCompilationUnit(
			FeatureNode dependency, ClassNode outer) {
		Collection<ClassNode> inners = getInnerClassNodes(outer.getName());
		if(dependency.getClassNode().equals(outer)) return true;
		for(ClassNode inner : inners) {
			if(dependency.getClassNode().equals(inner)) return true;
		}
		return false;
	}
	/**
	 * Non-public methods of this class uses that are used by other classes inside a package.  
	 * In case of moving this class to another package, these methods must be made public.  
	 * @param classname
	 * @return
	 */
	public static Set<Method_info> getNonPublicIntraPackageInboundMethods(String classname) {
		ClassNode classnode = getClassnode(classname);
		Classfile classfile = getClassfile(classname);
		if(classnode == null) return new HashSet<Method_info>();
		Set<Method_info> nonPublicInboundMethods = new HashSet<Method_info>();
		ClassNode outer = getClassnode(getOuterClass(classname));
		for(FeatureNode fn: classnode.getFeatures()) {
			int inboundcount = 0;
			for(Node d : fn.getInboundDependencies()) {
				if(d instanceof FeatureNode) {
					FeatureNode dependency = (FeatureNode) d;
					if(isDependencyFromSameCompilationUnit(dependency, outer)) continue;
					else inboundcount ++;
				} 
			}
			if(inboundcount > 0) {
				if(isMethodNode(fn.getName())){
					Method_info m = classfile.getMethod(fn.getSimpleName());
					if(m == null) continue;
					if(!m.isPublic()) {
						nonPublicInboundMethods.add(m);
					}
				}
			}
		}
		
		return nonPublicInboundMethods;
	}
	/**
	 * Non-public classes inside the package used by this class. In case of move refactoring
	 * these classes must be made public
	 * @param classname
	 * @return
	 */
	public static Set<Classfile> getIntraPackageNonPublicClasses(String classname) {
		ClassNode classnode = getClassnode(classname);
		if(classnode == null) return new HashSet<Classfile>();
		String packagename = classnode.getPackageNode().getName();
		Set<Classfile> nonPublicClasses = new HashSet<Classfile>();
		for(FeatureNode fn: classnode.getFeatures()) { 
			for(Node n : fn.getOutboundDependencies()){
				if(n.isConfirmed() && n instanceof FeatureNode) {
					FeatureNode dependableFeature = (FeatureNode) n;
					ClassNode dependable = dependableFeature.getClassNode();
					Classfile dependableCF = getClassfile(dependable.getName());
					//dependable class should be in the same package
					//it should not be public
					//dependable class is not the dependent class (self-dependency)
					if(dependable.getPackageNode().getName().equals(packagename) &&
						!dependable.equals(classnode) && !dependableCF.isPublic()) {
						if(!isInnerClass(classnode, dependable))
							nonPublicClasses.add(dependableCF);
					}
				}
			}
		}
		return nonPublicClasses;
	}

	private static boolean isInnerClass(ClassNode classnode,
			ClassNode dependable) {
		for(ClassNode inner: getInnerClassNodes(classnode.getName())) {
			if(dependable.equals(inner)) return true;
		}
		return false;
	}
	public static int getChangeAccessabilityRefactoringsCount(String classname) {
		//compute refactorings for this class.
		Set<Field_info> f1 = getIntraPackageNonPublicFields(classname);
		Set<Method_info> m1 = getIntraPackageNonPublicMethods(classname);
		Set<Classfile> cfs = getIntraPackageNonPublicClasses(classname);
		int thisClass = f1.size() + m1.size() + cfs.size();
		return thisClass;
	}
	
	public static Set<Field_info> getIntraPackageNonPublicFields(String classname) {
		Set<Field_info> fields = new HashSet<Field_info>();
		Set<Field_info> f1 = getNonPublicIntraPackageOutboundFields(classname);
		Set<Field_info> f2 = getNonPublicIntraPackageInboundFields(classname);
		fields.addAll(f1);
		fields.addAll(f2);
		return fields;
	}
	
	public static Set<Method_info> getIntraPackageNonPublicMethods(String classname) {
		Set<Method_info> methods = new HashSet<Method_info>();
		Set<Method_info> m1 = getNonPublicIntraPackageOutboundMethods(classname);
		Set<Method_info> m2 = getNonPublicIntraPackageInboundMethods(classname);
		methods.addAll(m1);
		methods.addAll(m2);
		return methods;
	}
	
	public static boolean isFieldNode(String node) {
		if(node.contains("(") || node.contains("{")) return false;
		else return true;
	}
	
	public static boolean isMethodNode(String node) {
		if(node.contains("(") || node.contains("{")) return true;
		else return false;
	}
	
	public static boolean isRenameRequired(String simpleClassnameToMove, String targetPackageName) {
		String newClassname = targetPackageName + "." + simpleClassnameToMove;
		if(getClassfile(newClassname)==null) return false;
		else return true;
	}
	public static List<String> getInnerClasses(String classname) {
		Classfile cf = getClassfile(classname);
		if(classname == null) return new ArrayList<String>();
		List<String> classes = new ArrayList<String>();
//		for (Attribute_info attribute : cf.getAttributes()) {
//            if (attribute instanceof InnerClasses_attribute) {
//                for (InnerClass innerClass : ((InnerClasses_attribute) attribute).getInnerClasses()) {
//                	String innername = innerClass.toString();
//                	if(innername.startsWith(classname)){
//                		classes.add(innerClass.getInnerClassInfo());
//                	}
//                		
//                }
//            }
//        }
		//get other inner classes in the same package
		String packageName = cf.getPackageName();
		List<Classfile> classesInPackage = new ArrayList<Classfile>();
		for(Classfile cf1 : classfiles){
			if(cf1.getPackageName().equals(packageName)) classesInPackage.add(cf1);
		}
		for(Classfile cf1 : classesInPackage) {
			ClassNode cn1 = getClassnode(getOuterClass(cf1.getClassName()));
			if(cn1.getName().equals(classname)) classes.add(cf1.getClassName());
		}
		classes.remove(classname);
		return classes;
	}
	
	public static String getOuterClass(String classname) {
		Classfile cf = getClassfile(classname);
		ClassNode cn = getClassnode(classname);
		if(cn == null || cf == null) return null;
		String packagename = cn.getPackageNode().getName();
		if (classname == null)
			return null;
		for (Attribute_info attribute : cf.getAttributes()) {
			if (attribute instanceof SourceFile_attribute) {
				String owner = ((SourceFile_attribute) attribute)
						.getSourceFile();
				String ownername = owner.substring(0, owner.lastIndexOf("."));
				return packagename + "." + ownername;
			}
		}
		return "";
	}
	public static Collection<ClassNode> getInnerClassNodes(String outerclass) {
		List<String> innerclasses = getInnerClasses(outerclass);
		Collection<ClassNode> result = new ArrayList<ClassNode>();
		for(String inner : innerclasses) {
			ClassNode cn = getClassnode(inner);
			if(cn == null) continue;
			result.add(cn);
		}
		return result;
	}
	
	public static boolean isOuterClass(String classname) {
		ClassNode cn = getClassnode(classname);
		Classfile cf = getClassfile(classname);
		if(cn.isConfirmed() & (cf.isAnonymousClass() || cf.isMemberClass() || cf.isLocalClass())){
			return false;
		}
		return true;
	}
}
