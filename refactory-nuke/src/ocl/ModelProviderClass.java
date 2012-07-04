package ocl;

import nz.ac.massey.cs.care.refactoring.manipulators.AntRunner1;
import nz.ac.massey.cs.care.refactoring.manipulators.BuildResult;
import nz.ac.massey.cs.care.refactoring.manipulators.Postconditions;
import nz.ac.massey.cs.care.refactoring.manipulators.Preconditions;
import gr.uom.java.ast.*;
import gr.uom.java.ast.decomposition.*;

public class ModelProviderClass {

	protected ClassObject clazz;
	protected MethodObject method;
	protected ConstructorObject constructor;
	protected FieldObject field;
	protected ParameterObject parameter;
	protected AbstractStatement abstractStatement;
	protected AbstractExpression abstractExpression;
	protected LocalVariableDeclarationObject localVariableDeclaration;
	protected MethodInvocationObject methodInvocation;
	protected SuperMethodInvocationObject superMethodInvocation;
	protected FieldInstructionObject fieldInstructionObject;
	protected LocalVariableInstructionObject localVariableInstruction;
	protected ClassInstanceCreationObject classInstanceCreation;
	protected ArrayCreationObject arrayCreationObject;
	protected Access access;
	protected CreationObject creation;
	protected TypeObject type;
	protected VariableDeclarationObject variableDeclaration;
	protected Preconditions preconditions;
	protected Postconditions postconditions;
	protected AntRunner1 antRunner;
	protected BuildResult buildResult;
}
