package nz.ac.massey.cs.care.move;

public class V extends nz.ac.massey.cs.guery.adapters.jungalt.Vertex<E> {

	private String name = "";
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public V(String id){
		super(id);
	}
	public V(){
		super();
	}
	
	public String toString(){
		return this.name;
	}
}
