package nz.ac.massey.cs.care.refactoring.views;

import gr.uom.java.ast.CompilationUnitCache;

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;

public class MyElementChangedListener implements IElementChangedListener {

	public void elementChanged(ElementChangedEvent event) {
		IJavaElementDelta javaElementDelta = event.getDelta();
		processDelta(javaElementDelta);
	}

	private void processDelta(IJavaElementDelta delta) {
		IJavaElement javaElement = delta.getElement();
		switch(javaElement.getElementType()) {
		case IJavaElement.JAVA_MODEL:
		case IJavaElement.JAVA_PROJECT:
		case IJavaElement.PACKAGE_FRAGMENT_ROOT:
		case IJavaElement.PACKAGE_FRAGMENT:
			IJavaElementDelta[] affectedChildren = delta.getAffectedChildren();
			for(IJavaElementDelta affectedChild : affectedChildren) {
				processDelta(affectedChild);
			}
			break;
		case IJavaElement.COMPILATION_UNIT:
			ICompilationUnit compilationUnit = (ICompilationUnit)javaElement;
			if(delta.getKind() == IJavaElementDelta.ADDED) {
				CompilationUnitCache.getInstance().compilationUnitAdded(compilationUnit);
			}
			else if(delta.getKind() == IJavaElementDelta.REMOVED) {
				CompilationUnitCache.getInstance().compilationUnitRemoved(compilationUnit);
			}
			else if(delta.getKind() == IJavaElementDelta.CHANGED) {
				if((delta.getFlags() & IJavaElementDelta.F_FINE_GRAINED) != 0) {
					CompilationUnitCache.getInstance().compilationUnitChanged(compilationUnit);
				}
			}
		}
	}
}
