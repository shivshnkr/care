package nz.ac.massey.cs.care.move.metrics;

import nz.ac.massey.cs.care.move.*;
import nz.ac.massey.cs.guery.MotifInstance;
import nz.ac.massey.cs.guery.ResultListener;

public class Counter implements ResultListener<Vertex,Edge> {
	public int count = 0;
	@Override
	public synchronized boolean found(MotifInstance<Vertex,Edge> instance) {
		count = count+1;
		return true;
	}

	@Override
	public void progressMade(int progress, int total) {}

	@Override
	public void done() {}
	
}