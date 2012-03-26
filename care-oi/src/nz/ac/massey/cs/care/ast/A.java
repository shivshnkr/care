package nz.ac.massey.cs.care.ast;

import java.io.IOException;

public class A {
	public static final Object bfield = new B();
	private int x = 550;
	private B bfield1 = new B();
	public A() {
//		B bObject = new B();
	}
	public A(int i) {
//		int x = i;
	}
//	public A(String s) {
//		
//	}
	
	public B m(B b) throws B{
		B.m1();
		Object test = B.m2();
//		b.m2();
		B b1 = new B();
		return null;
	}
	public void m2(B i){
		B.m1();
		try {
			i.m3();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//			bfield.m2();
	}
}
