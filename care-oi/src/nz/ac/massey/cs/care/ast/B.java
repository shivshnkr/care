package nz.ac.massey.cs.care.ast;

import java.util.Random;


public class B extends Exception{

	public static int field1;
	private Object aObject = new A();

	public static void m1() {
		A a = new A();
	}
	public static Object m2() {
	Object c = null;
	try {
		c = Class.forName("test").newInstance();
	} catch (InstantiationException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (IllegalAccessException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (ClassNotFoundException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
		return c;
	}
	public void m3() throws B {
		// TODO Auto-generated method stub
		
	}
}
