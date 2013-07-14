package nz.ac.massey.cs.care.dependency;

import java.util.*;

import nz.ac.massey.cs.care.refactoring.executers.CareRefactoring;
import nz.ac.massey.cs.care.refactoring.executers.CompositeRefactoring;
import nz.ac.massey.cs.care.refactoring.movehelper.Candidate;

public class CompositeDependency extends Dependency {
	public CompositeDependency(Candidate c) {
		super(c);
	}

	private List<Dependency> dependencies = new ArrayList<Dependency>();
	@Override
	public String getName() {
		StringBuffer b = new StringBuffer();
		for(Dependency cr : dependencies) {
			b.append(cr.getName()).append("+");
		}
		String name = b.toString();
		return name.substring(0, name.lastIndexOf("+"));
	}

	@Override
	public CareRefactoring getRefactoring() {
		CompositeRefactoring cr = new CompositeRefactoring(candidate);
		for(Dependency d : dependencies) {
			cr.addRefactoring(d.getRefactoring());
		}
		return cr;
	}

	public void addDependency(Dependency d) {
		this.dependencies.add(d);
	}
}
