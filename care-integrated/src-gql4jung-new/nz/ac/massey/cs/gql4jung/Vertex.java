/**
 * Copyright 2009 Jens Dietrich Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 
 * Unless required by applicable law or agreed to in writing, software distributed under the 
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied. See the License for the specific language governing permissions 
 * and limitations under the License.
 */


package nz.ac.massey.cs.gql4jung;


/**
 * Custom vertex class.
 * @author jens dietrich
 */
public class Vertex extends nz.ac.massey.cs.guery.adapters.jungalt.Vertex<Edge> {

	// properties
	// when making this a general purpose query language, we need to remove this
	private String namespace = "";
	private String name = "";
	private boolean isAbstract = false;
	private boolean isInterface = false;
	private String type = null;
	private String container = null;
	private String cluster = null;
	private String fullName = null;
	private boolean isPublic;
	private boolean isInnerClass;
	private boolean isAnonymousClass;
	
	public Vertex(String id) {
		super(id);
	}
	public Vertex() {
		super();
	}
	
	public boolean isAbstract() {
		return isAbstract;
	}
	public String getType() {
		return type;
	}
	public void setAbstract(boolean isAbstract) {
		this.isAbstract = isAbstract;
	}
	public void setType(String type) {
		this.type = type;
	}

	
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
		this.fullName = null;
	}
	public String getNamespace() {
		return namespace;
	}
	public void setNamespace(String namespace) {
		this.namespace = namespace;
		this.fullName = null;
	}
	public String getFullname() {
		if (this.fullName==null) {
			if (this.namespace==null||this.namespace.length()==0) {
				this.fullName = name;
			}
			else {
				this.fullName = namespace+'.'+name;
			}
		}
		return fullName;
	}
	
	public String toString() {
		return new StringBuffer() 
			.append(this.getId())
			.append(':')
			.append(this.namespace)
			.append('.')
			.append(this.name)
			.toString();
	}
	
	public String getContainer() {
		return container;
	}
	public String getCluster() {
		return cluster;
	}
	public void setContainer(String container) {
		this.container = container;
	}
	public void setCluster(String cluster) {
		this.cluster = cluster;
	}
	// checks for inner class relationships
	public boolean isPartOf(Vertex v) {
		if (!this.namespace.equals(v.namespace)) return false;
		else return (this.name.startsWith(v.name+'$'));
	}
	public void copyValuesTo(Vertex v) {
		v.setName(name);
		v.setNamespace(namespace);
		v.setContainer(container);
		v.setType(type);
		v.setCluster(cluster);
		v.setAbstract(isAbstract);
	}
	public void setPublic(boolean public1) {
		this.isPublic  = public1;
		
	}
	public boolean isPublic(){
		return isPublic;
	}
	public void setInterface(boolean interface1) {
		this.isInterface = interface1;
		
	}
	public boolean isInterface() {
		return isInterface;
	}
	public void setInnerClass(boolean innerClass) {
		this.isInnerClass = innerClass;
		
	}
	public boolean isInnerClass() {
		return isInnerClass;
	}
	public void setAnonymousClass(boolean anonymousClass) {
		this.isAnonymousClass = anonymousClass;
		
	}
	public boolean isAnonymousClass() {
		return isAnonymousClass;
	}
	

}
