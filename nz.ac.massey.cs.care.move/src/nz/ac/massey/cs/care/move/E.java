package nz.ac.massey.cs.care.move;

public class E extends nz.ac.massey.cs.guery.adapters.jungalt.Edge<V>{
	
	private String ident = "";
	
	public E(String id, V end, V start) {
		super(id,end,start);
	}
	public E() {
		super();
	}
	public E(String edge) {
		super();
		ident = edge; 
	}
	public String getIdent() {
		return ident;
	}
	public void setIdent(String ident) {
		this.ident = ident;
	}
	public String toString(){
		return super.toString();
	}
}
