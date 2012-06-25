package nz.ac.massey.cs.care.refactoring.slhelper;

import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.ClassObject;

import java.util.ArrayList;
import java.util.List;

import nz.ac.massey.cs.care.ast.CheckerASTVisitor;
import nz.ac.massey.cs.care.ast.MethodVisitor;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

public class ReplaceByFactoryRefactoring extends Refactoring {

	private ClassObject sourceClass = null;
	private ClassObject targetClass = null;
	private String targetSuperclassName = null;
	private MultiTextEdit sourceMultiTextEdit;
	private MultiTextEdit multiTextEdit = new MultiTextEdit();
	private CompilationUnitChange compilationUnitChange;
	private CompilationUnitChange factoryCompilationUnitChange;
	private CompilationUnit sourceCompilationUnit;
	private CheckerASTVisitor visitor;
	private CompilationUnit factoryCompilationUnit;
	private List<String> methodsCreated = new ArrayList<String>();
	
	
	public ReplaceByFactoryRefactoring(ClassObject sourceClass2,
			ClassObject targetClass2, String targetSuperClass, CheckerASTVisitor visitor) {
		this.sourceClass = sourceClass2;
		this.targetClass = targetClass2;
		this.targetSuperclassName = targetSuperClass;
		this.visitor = visitor;
		
		if(!sourceClass.isAnony()){
			this.sourceCompilationUnit = (CompilationUnit) sourceClass.getTypeDeclaration().getRoot();
		} else {
			this.sourceCompilationUnit = (CompilationUnit) sourceClass.getAnonymousClassDeclaration().getRoot();
		}
		ICompilationUnit sourceICompilationUnit = (ICompilationUnit)sourceCompilationUnit.getJavaElement();
		this.compilationUnitChange = new CompilationUnitChange("", sourceICompilationUnit);
		this.sourceMultiTextEdit = new MultiTextEdit();
		this.compilationUnitChange.setEdit(sourceMultiTextEdit);
		ClassObject factory = ASTReader.getSystemObject().getClassObject("registry.ServiceLocator"); 
//		assert(factory!=null);
		this.factoryCompilationUnit = factory.getCompilationUnit();
		ICompilationUnit factoryICompilationUnit = (ICompilationUnit) factoryCompilationUnit.getJavaElement();
		this.factoryCompilationUnitChange = new CompilationUnitChange("", factoryICompilationUnit);
		this.factoryCompilationUnitChange.setEdit(multiTextEdit);
	}

	@Override
	public String getName() {
		return "Replace by Factory";
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		RefactoringStatus status= new RefactoringStatus();
		try {
			pm.beginTask("Checking preconditions...", 1);
		} finally {
			pm.done();
		}
		return status;
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		final RefactoringStatus status= new RefactoringStatus();
		try {
			pm.beginTask("Checking preconditions...", 2);
			apply(status);
		} finally {
			pm.done();
		}
		return status;
	}

	private void apply(RefactoringStatus status) {
		AST ast = null;
		if(sourceClass.isAnony()) {
			ast = sourceClass.getAnonymousClassDeclaration().getAST();
		} else {
			ast = sourceClass.getTypeDeclaration().getAST();
		}
		ASTRewrite sourceRewriter = ASTRewrite.create(ast);
		for(ClassInstanceCreation instance : visitor.getConstructorInvocationsToReplace()) {
			String methodName = "init_" + instance.getType().resolveBinding().getQualifiedName();
			methodName = methodName.replace(".", "_");
			MethodInvocation factoryCall = null;
			if(instance.getAnonymousClassDeclaration() != null) {
				status.addError("anonymous class instance");
				return;
			}
			if(!alreadyCreated(methodName) && !exists(methodName)) {
				createInitMethodInFactory(instance, methodName, status);
				methodsCreated.add(methodName);
				factoryCall = getFactoryCall(methodName);
			} else {
				factoryCall = getFactoryCall(methodName);
			}
//			sourceRewriter.replace(instance, factoryCall, null);
		}
		for(QualifiedName instance : visitor.getSfiToReplace()) {
			String methodName = "initSFI_" + instance.getQualifier().getFullyQualifiedName() + "_" + instance.getName().toString();
			methodName = methodName.replace(".", "_");
			MethodInvocation factoryCall = null;
			if(!alreadyCreated(methodName) && !exists(methodName)) {
				createInitSFIMethodInFactory(instance, methodName, status);
				methodsCreated.add(methodName);
				factoryCall = getFactoryCall(methodName);
			} else {
				factoryCall = getFactoryCall(methodName);
			}
//			sourceRewriter.replace(instance, factoryCall, null);
		}
		for(MethodInvocation instance : visitor.getSmiToReplace()) {
			String methodName = "initSMI_" + instance.getExpression().resolveTypeBinding().getQualifiedName() + "_" + instance.getName().toString();
			methodName = methodName.replace(".", "_");
			MethodInvocation factoryCall = null;
			if(!alreadyCreated(methodName) && !exists(methodName)) {
				createInitSMIMethodInFactory(instance, methodName, status);
				methodsCreated.add(methodName);
				factoryCall = getFactoryCall(methodName);
			} else {
				factoryCall = getFactoryCall(methodName);
			}
//			sourceRewriter.replace(instance, factoryCall, null);
		}
		methodsCreated.clear();
		if(!status.hasError()){
//			writeDownCodeChanges(sourceRewriter);
		}
	}
	private boolean alreadyCreated(String methodName) {
		for(String method : methodsCreated) {
			if(method.equals(methodName)) return true;
		}
		return false;
	}

	private void createInitSMIMethodInFactory(MethodInvocation instance,
			String methodName, RefactoringStatus status) {
		createInitMethodInFactory(instance, methodName, status);
		
	}
	private void createInitSFIMethodInFactory(QualifiedName instance,
			String methodName,RefactoringStatus status) {
		createInitMethodInFactory(instance, methodName, status);
	}
	
	private void createInitMethodInFactory(ASTNode instance, String methodName, RefactoringStatus status) {
		TypeDeclaration topLevelTypeDeclaration = getFactoryTypeDeclaration();
		AST ast = topLevelTypeDeclaration.getAST();
		ASTRewrite sourceRewriter = ASTRewrite.create(ast);
		MethodDeclaration method = ast.newMethodDeclaration();
		AST methodast = method.getAST();
		sourceRewriter.set(method, MethodDeclaration.NAME_PROPERTY, methodast.newSimpleName(methodName), null);
		String factoryMethodReturnType = null;
		if(instance instanceof QualifiedName ) {
			ITypeBinding fieldType = ((QualifiedName)instance).getName().resolveTypeBinding();
			if(fieldType.isArray()) {
				status.addError("array type");
				return;
			}
			String fieldTypeName = null;
			if(fieldType.isPrimitive()) {
				fieldTypeName = getAccessedFieldTypeName(fieldType);
				if(fieldTypeName.equals("void")){
					sourceRewriter.set(method, MethodDeclaration.RETURN_TYPE2_PROPERTY, methodast.newPrimitiveType(PrimitiveType.VOID), null);
				} else {
					sourceRewriter.set(method, MethodDeclaration.RETURN_TYPE2_PROPERTY, methodast.newSimpleType(methodast.newName(fieldTypeName)), null);
				}
			} else if(fieldType.isParameterizedType()){
				fieldTypeName = fieldType.getErasure().getQualifiedName();
				sourceRewriter.set(method, MethodDeclaration.RETURN_TYPE2_PROPERTY, methodast.newSimpleType(methodast.newName(fieldTypeName)), null);
			} else {
				fieldTypeName = fieldType.getQualifiedName();
				sourceRewriter.set(method, MethodDeclaration.RETURN_TYPE2_PROPERTY, methodast.newSimpleType(methodast.newName(fieldTypeName)), null);
			}
			factoryMethodReturnType = fieldTypeName;
		} else if(instance instanceof MethodInvocation) {
			ITypeBinding smiReturnType = ((MethodInvocation)instance).getName().resolveTypeBinding();
			if(smiReturnType.isArray()) {
				status.addError("array type");
				return;
			}
			String returnTypeName = null;
			if(smiReturnType.isPrimitive()) {
				returnTypeName = getAccessedFieldTypeName(smiReturnType);
				if(returnTypeName.equals("void")){
					sourceRewriter.set(method, MethodDeclaration.RETURN_TYPE2_PROPERTY, methodast.newPrimitiveType(PrimitiveType.VOID), null);
				} else {
					sourceRewriter.set(method, MethodDeclaration.RETURN_TYPE2_PROPERTY, methodast.newSimpleType(methodast.newName(returnTypeName)), null);
				}
			} else if (smiReturnType.isParameterizedType()) {
				returnTypeName = smiReturnType.getErasure().getQualifiedName();
				sourceRewriter.set(method, MethodDeclaration.RETURN_TYPE2_PROPERTY, methodast.newSimpleType(methodast.newName(returnTypeName)), null);
			}
			else {
				returnTypeName = smiReturnType.getQualifiedName();
				if(returnTypeName.equals(targetClass.getName())) {
					if(targetSuperclassName != null) returnTypeName = targetSuperclassName;
					else status.addError("no supertype found");
					return;
				}
				assert returnTypeName!=null: "return type is: " + smiReturnType.getQualifiedName();
				sourceRewriter.set(method, MethodDeclaration.RETURN_TYPE2_PROPERTY, methodast.newSimpleType(methodast.newName(returnTypeName)), null);
			}
			factoryMethodReturnType = returnTypeName;
		} else {
			if(targetSuperclassName != null) {
				sourceRewriter.set(method, MethodDeclaration.RETURN_TYPE2_PROPERTY, methodast.newSimpleType(methodast.newName(targetSuperclassName)), null);
				factoryMethodReturnType = targetSuperclassName;
			} else {
				String superQualifiedName = null;
				try {
					if(targetClass.getSuperclass() !=null) {superQualifiedName = targetClass.getSuperclass().getClassType();}
					else if(targetClass.getInterfaceIterator().next() != null)  {superQualifiedName = targetClass.getInterfaceIterator().next().getClassType();}
					else superQualifiedName = "java.lang.Object";
					sourceRewriter.set(method, MethodDeclaration.RETURN_TYPE2_PROPERTY, methodast.newSimpleType(methodast.newName(superQualifiedName)), null);
					factoryMethodReturnType = superQualifiedName;
				} catch (Exception e){
					factoryMethodReturnType = "java.lang.Object";
				}
				
			}
		}
		ListRewrite modifierRewrite = sourceRewriter.getListRewrite(method, MethodDeclaration.MODIFIERS2_PROPERTY);
		Modifier publicModifier = methodast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD);
		Modifier staticModifier = methodast.newModifier(Modifier.ModifierKeyword.STATIC_KEYWORD);
		modifierRewrite.insertFirst(publicModifier, null);
		modifierRewrite.insertLast(staticModifier, null);
		ListRewrite exceptionsRewrite = sourceRewriter.getListRewrite(method, MethodDeclaration.THROWN_EXCEPTIONS_PROPERTY);
		ITypeBinding[] exceptions = null;
		if(instance instanceof ClassInstanceCreation) {
			exceptions = ((ClassInstanceCreation)instance).resolveConstructorBinding().getExceptionTypes();
		}
		if(instance instanceof MethodInvocation) {
			exceptions = ((MethodInvocation)instance).resolveMethodBinding().getExceptionTypes();
		}
		if(exceptions != null) {
			for(ITypeBinding exception : exceptions) {
				exceptionsRewrite.insertLast(methodast.newSimpleType(methodast.newName(exception.getQualifiedName())), null);
			}
		}
		//add method body
		Block newMethodBody = methodast.newBlock();
		ListRewrite methodBodyRewrite = sourceRewriter.getListRewrite(newMethodBody, Block.STATEMENTS_PROPERTY);
		if(!factoryMethodReturnType.equals("void")){
			ReturnStatement returnStatement = methodast.newReturnStatement();
			returnStatement.setExpression(methodast.newNullLiteral());
//			ClassInstanceCreation returnInstance = (ClassInstanceCreation) ASTNode.copySubtree(methodast, instance);
//			sourceRewriter.set(returnStatement, ReturnStatement.EXPRESSION_PROPERTY, returnInstance, null);
			methodBodyRewrite.insertLast(returnStatement, null);
		}
		method.setBody(newMethodBody);
		//add method to source type
		ListRewrite bodyRewrite = sourceRewriter.getListRewrite(topLevelTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
		bodyRewrite.insertLast(method, null);
		try {
			TextEdit sourceEdit = sourceRewriter.rewriteAST();
			multiTextEdit.addChild(sourceEdit);
			this.factoryCompilationUnitChange.addTextEditGroup(new TextEditGroup("Add Factory Refactoring", new TextEdit[] {sourceEdit}));
		}
		catch(JavaModelException javaModelException) {
			return;
		}
	}
	private String getAccessedFieldTypeName(ITypeBinding fieldType) {
		String fieldTypeName = null;
		String name = fieldType.getName();
		if(name.equals("int")) fieldTypeName = "Integer";
		else if(name.equals("boolean")) fieldTypeName = "Boolean";
		else if(name.equals("char")) fieldTypeName = "Character";
		else if(name.equals("byte")) fieldTypeName = "Byte";
		else if(name.equals("float")) fieldTypeName = "Float";
		else if(name.equals("long")) fieldTypeName = "Long";
		else if(name.equals("double")) fieldTypeName = "Double";
		else if(name.equals("short")) fieldTypeName = "Short";
		else fieldTypeName = "void";
		return fieldTypeName;
	}
	@SuppressWarnings("unchecked")
	private TypeDeclaration getFactoryTypeDeclaration() {
		TypeDeclaration topLevelTypeDeclaration = null;
		List<AbstractTypeDeclaration> topLevelTypeDeclarations = factoryCompilationUnit.types();
        for(AbstractTypeDeclaration abstractTypeDeclaration : topLevelTypeDeclarations) {
        	if(abstractTypeDeclaration instanceof TypeDeclaration) {
        		topLevelTypeDeclaration = (TypeDeclaration)abstractTypeDeclaration;
        		break; //this class has only one type declaration
        	}
        }
		return topLevelTypeDeclaration;
	}
	private boolean exists(final String methodName){
//		if(factoryCompilationUnit == null) return false;
		MethodVisitor visitor = new MethodVisitor();
		visitor.process(factoryCompilationUnit);
		for(MethodDeclaration method : visitor.getMethods()) {
			if(method.getName().toString().equals(methodName)) return true;
		}
		return false;
	}
	
	private MethodInvocation getFactoryCall(String methodName) {
		AST ast = factoryCompilationUnit.getAST();
		MethodInvocation call = ast.newMethodInvocation();
		call.setName(ast.newSimpleName(methodName));
		call.setExpression(ast.newName("registry.ServiceLocator"));
		return call;
	}
	
	private void writeDownCodeChanges(ASTRewrite sourceRewriter) {
		try {
			TextEdit sourceEdit = sourceRewriter.rewriteAST();
			try {
				sourceMultiTextEdit.addChild(sourceEdit);
			}catch(Exception e) { return; }
			compilationUnitChange.addTextEditGroup(new TextEditGroup("Add Factory Refactoring", new TextEdit[] {sourceEdit}));
		}
		catch(JavaModelException javaModelException) {
			return;
		}
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException,
			OperationCanceledException {
		try {
			pm.beginTask("Creating change...", 1);
//			final Collection<TextFileChange> changes = new ArrayList<TextFileChange>();
//			changes.add(factoryCompilationUnitChange);
//			changes.add(compilationUnitChange);
			CompositeChange changes = new CompositeChange("REplaceByFactory");
			changes.add(factoryCompilationUnitChange);
//			changes.add(compilationUnitChange);
//			CompositeChange change = new CompositeChange(getName(), changes.toArray(new Change[changes.size()])) {
//				@Override
//				public ChangeDescriptor getDescriptor() {
//					String project = ASTReader.getExaminedProject().getElementName();
//					String description = "ReplaceByFactory Refactoring";//MessageFormat.format("Extract from method ''{0}''", new Object[] { sourceTypeDeclaration.getName().getIdentifier()});
//					String comment = "";
//					return new RefactoringChangeDescriptor(new NullRefactoringDescriptor("1", project, description, comment, 0));
//				}
//			};
			return changes;
		} finally {
			pm.done();
		}
	}
}
