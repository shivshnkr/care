package nz.ac.massey.cs.care.ast;

import gr.uom.java.ast.ClassObject;
import gr.uom.java.ast.ConstructorObject;
import gr.uom.java.ast.CreationObject;
import gr.uom.java.ast.FieldInstructionObject;
import gr.uom.java.ast.FieldObject;
import gr.uom.java.ast.MethodInvocationObject;
import gr.uom.java.ast.MethodObject;
import gr.uom.java.ast.decomposition.cfg.PlainVariable;
import gr.uom.java.ast.util.ExpressionExtractor;

import java.util.Iterator;
import java.util.List;

import nz.ac.massey.cs.care.dependency.CIDependency;
import nz.ac.massey.cs.care.dependency.CompositeDependency;
import nz.ac.massey.cs.care.dependency.Dependency;
import nz.ac.massey.cs.care.dependency.EXDependency;
import nz.ac.massey.cs.care.dependency.IMDependency;
import nz.ac.massey.cs.care.dependency.METDependency;
import nz.ac.massey.cs.care.dependency.OtherDependency;
import nz.ac.massey.cs.care.dependency.SMIDependency;
import nz.ac.massey.cs.care.dependency.VDDependency;
import nz.ac.massey.cs.care.refactoring.movehelper.Candidate;
import nz.ac.massey.cs.care.util.Utils;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeLiteral;

public class DependencyFinder {
	private ClassObject sourceClass;
	private ClassObject targetClass;
	private int declarationElements = 0; //VD/MPT/MRT/MET
	private int constructorInvocations = 0;
	private int instanceOfs = 0;
	private int castExpressions = 0;
	private int staticMembers = 0;
	private int methodExceptions = 0;
	private Candidate candidate;
	private int typeLiteralsCount = 0;
	
	public void visitSourceClass() {
		visitFields();
		visitMethods();
		visitConstructors();
	}
	private void visitConstructors() {
		Iterator<ConstructorObject> ico = sourceClass.getConstructorIterator();
		while(ico.hasNext()) {
			ConstructorObject co = ico.next();
			visitMethodObject(new MethodObject(co));
		}
	}
	private void visitMethods() {
		Iterator<MethodObject> imo = sourceClass.getMethodIterator();
		while(imo.hasNext()) {
			MethodObject mo = imo.next();
			visitMethodObject(mo);
		}
	}
	private void visitMethodObject(MethodObject mo) {
		visitConstructorInvocations(mo);
		visitStaticMembersInvocations(mo);
		visitDeclarationElements(mo);
		visitInstanceofExpressions(mo);
		visitCastExpressions(mo);
		visitTypeLiterals(mo);
	}
	
	private void visitDeclarationElements(MethodObject mo) {
		for(PlainVariable pv : mo.getDeclaredLocalVariables()) {
			if(!pv.isField() && !pv.isParameter()) {
				String name = pv.getVariableType();
				if(name.contains(".")) name = Utils.getSimpleName(name); 
				if(name.equals(targetClass.getSimpleName())) declarationElements ++;
			}
		}
		MethodDeclaration node = mo.getMethodDeclaration();
		vistMethod(node);
	}
	private void visitStaticMembersInvocations(MethodObject mo) {
		for(MethodInvocationObject mio : mo.getInvokedStaticMethods()) {
			MethodInvocation node = mio.getMethodInvocation();
			if(node.getExpression() == null) continue;
			String typeName = node.getExpression().toString();
			if(typeName.contains(".")) typeName = typeName.substring(typeName.lastIndexOf(".")+1);
			if(typeName.equals(targetClass.getSimpleName())){
				staticMembers ++;
			}
		}
		List<FieldInstructionObject> fields = mo.getFieldInstructions();
		for(FieldInstructionObject fieldInvoked : fields) {
			if(fieldInvoked.isStatic()) {
				String name = fieldInvoked.getOwnerClass();
				if(name.contains(".")) name = Utils.getSimpleName(name); 
				if(name.equals(targetClass.getSimpleName())) staticMembers ++;	
			}
		}
		
	}
	private void visitConstructorInvocations(MethodObject mo) {
		for(CreationObject co : mo.getCreations()) {
			String name = co.getType().getClassType();
			if(name.contains(".")) name = Utils.getSimpleName(name);
			if(name.equals(targetClass.getSimpleName()))
				constructorInvocations ++;
		}
	}
	private void visitTypeLiterals(MethodObject mo) {
		if(mo.isAbstract()) return;
		ExpressionExtractor extractor = new ExpressionExtractor();
		MethodDeclaration md = mo.getMethodDeclaration();
		Block body = md.getBody();
		if(body == null) return;
//		System.out.println(mo.getClassName() + " method: " + mo.getName() );
		List<Statement> statements = body.statements();
		for(Statement s : statements) {
			List<Expression> typeLiterals = extractor.getTypeLiterals(s);
			for(Expression e : typeLiterals) {
				TypeLiteral tl = (TypeLiteral) e;
				String name = tl.getType().toString();
				if(name.contains(".")) name = Utils.getSimpleName(name);
				if(name.equals(targetClass.getSimpleName())) typeLiteralsCount  ++;
			}
		}
		
	}
	private void visitCastExpressions(MethodObject mo) {
		ExpressionExtractor extractor = new ExpressionExtractor();
		List<Expression> castExpressionsList = extractor.getCastExpressions(mo.getMethodDeclaration().getBody());
		for(Expression expression : castExpressionsList) {
			CastExpression castExpression = (CastExpression)expression;
			String typeName = castExpression.getType().toString();
			if (typeName.contains("."))
				typeName = Utils.getSimpleName(typeName);
			if (typeName.equals(targetClass.getSimpleName())) {
				castExpressions ++;
			}
		}
		
	}
	private void visitInstanceofExpressions(MethodObject mo) {
		ExpressionExtractor extractor = new ExpressionExtractor();
		List<Expression> instanceofExpressions = extractor.getInstanceofExpressions(mo.getMethodDeclaration().getBody());
		for(Expression expression : instanceofExpressions) {
			InstanceofExpression instanceofExpression = (InstanceofExpression)expression;
			Type name = instanceofExpression.getRightOperand();
			String typeName = name.toString();
			if (typeName.contains("."))
				typeName = Utils.getSimpleName(typeName);
			if (typeName.equals(targetClass.getSimpleName())) {
				instanceOfs ++;
			}
		}
	}
	private void visitFields() {
		if(sourceClass == null) return;
		Iterator<FieldObject> fieldIter = sourceClass.getFieldIterator();
		while(fieldIter.hasNext()) {
			try {
				FieldObject fo = fieldIter.next();
				String declarationClass = fo.getType().getClassType();
				String initializerClass = "";
				if(fo.isClassInstanceCreation()){
					initializerClass = fo.getInitializerClassName();
				}
				if(declarationClass.contains(".")) declarationClass = Utils.getSimpleName(declarationClass); 
				if(declarationClass.equals(targetClass.getSimpleName())) declarationElements ++;
				if(initializerClass.equals(targetClass.getSimpleName())) constructorInvocations ++;
			} catch (Exception e) {
				continue;
			}
		}
	}
	@SuppressWarnings("rawtypes")
	private void vistMethod(MethodDeclaration node) {
		String returnTypeName = null;
		if(node.isConstructor()) returnTypeName = ""; 
		else returnTypeName = node.getReturnType2().toString(); 
		if(targetClass == null) return;
		if(returnTypeName == null) returnTypeName = "";
		if(returnTypeName.contains(".")) returnTypeName = Utils.getSimpleName(returnTypeName);
		if(returnTypeName.equals(targetClass.getSimpleName())) 
			declarationElements++;
		List methodParameters = node.parameters();
		Iterator methodParameterIterator = methodParameters.iterator();
		while (methodParameterIterator.hasNext()) {
			SingleVariableDeclaration methodParameter = (SingleVariableDeclaration) methodParameterIterator
					.next();
			String parameterTypeName = methodParameter.getType().toString();
			if (parameterTypeName.equals(targetClass.getSimpleName())) declarationElements++;
		}
		List exceptions = node.thrownExceptions();
		for(Object exception : exceptions) {
			ASTNode e = (ASTNode) exception;
			String name = e.toString();
			if(name.contains(".")) name = Utils.getSimpleName(name);
			if(name.equals(targetClass.getSimpleName())) methodExceptions ++;
		}
	}
	public Dependency compute() {
		visitSourceClass();
		if(candidate.getEdgeType().equals("extends")) {
			return new EXDependency(candidate);
		} else if(candidate.getEdgeType().equals("implements")) {
			return new IMDependency(candidate);
		} else if(constructorInvocations == 0 && declarationElements == 0 && staticMembers > 0 && !hasOther()) {
			return new SMIDependency(candidate);//works fine
		} else if(constructorInvocations == 0 && declarationElements > 0 && staticMembers == 0 && !hasOther()) {
			return new VDDependency(candidate); //works fine
		} else if(constructorInvocations > 0 && declarationElements == 0 && staticMembers == 0 && !hasOther()) {
			return new CIDependency(candidate); //works fine
		} else if(constructorInvocations > 0 && declarationElements > 0 && staticMembers == 0 && !hasOther()) {
			CompositeDependency cd = new CompositeDependency(candidate);
			cd.addDependency(new VDDependency(candidate));
			cd.addDependency(new CIDependency(candidate));
			return cd; //works fine
		} else if(constructorInvocations > 0 && declarationElements == 0 && staticMembers > 0 && !hasOther()) {
			CompositeDependency cd = new CompositeDependency(candidate);
			cd.addDependency(new SMIDependency(candidate));
			cd.addDependency(new CIDependency(candidate));
			return cd;
		} else if(constructorInvocations == 0 && declarationElements > 0 && staticMembers > 0 && !hasOther()) {
			CompositeDependency cd = new CompositeDependency(candidate);
			cd.addDependency(new SMIDependency(candidate)); //works fine
			cd.addDependency(new VDDependency(candidate));
			return cd;
		} else if(constructorInvocations > 0 && declarationElements > 0 && staticMembers > 0 && !hasOther()) {
			CompositeDependency cd = new CompositeDependency(candidate);
			cd.addDependency(new SMIDependency(candidate));
			cd.addDependency(new VDDependency(candidate));
			cd.addDependency(new CIDependency(candidate));
			return cd;
		} else if(hasOther()) {
			return new OtherDependency(candidate);
		} else if(methodExceptions > 0) {
			return new METDependency(candidate);
		} else {
			return null;
		}
	}
	/**
	 * We check this with other dependency categories because we are not 
	 * refactoring these two types of dependencies.
	 * @return
	 */
	private boolean hasOther() {
		if(instanceOfs > 0 || castExpressions > 0 || typeLiteralsCount > 0) {
			return true;
		} else {
			return false;
		}
	}
	public DependencyFinder(ClassObject sourceClass, ClassObject targetClass) {
		super();
		this.sourceClass = sourceClass;
		this.targetClass = targetClass;
	}
	
	public DependencyFinder(Candidate c) {
		super();
		this.sourceClass = c.getSourceClassObject();
		this.targetClass = c.getTargetClassObject();
		this.candidate = c;
	}
}
