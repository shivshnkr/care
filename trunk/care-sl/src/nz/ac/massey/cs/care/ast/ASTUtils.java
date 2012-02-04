package nz.ac.massey.cs.care.ast;

import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.ClassObject;
import nz.ac.massey.cs.care.refactoring.views.AbstractionView;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.internal.corext.refactoring.changes.CreateCompilationUnitChange;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

public class ASTUtils {
	@SuppressWarnings({ "restriction" })
	public static void createFactoryDeclaration(IJavaProject javaProject) {
//		ClassObject factory = ASTReader.getSystemObject().getClassObject("myfactory.MyFactory");
//		if(factory != null) return;
//		IJavaProject javaProject = ASTReader.getExaminedProject();
		IFolder folder = javaProject.getProject().getFolder("src");
		IPackageFragmentRoot srcFolder = javaProject.getPackageFragmentRoot(folder);
		try {
			srcFolder.createPackageFragment("myfactory", true, null);
		} catch (JavaModelException e1) {
			e1.printStackTrace();
		}
		IFile classFile = javaProject.getProject().getFile("/src/myfactory/MyFactory.java");
		ICompilationUnit classICompilationUnit = JavaCore.createCompilationUnitFrom(classFile);
		ASTParser classParser = ASTParser.newParser(AST.JLS3);
		classParser.setKind(ASTParser.K_COMPILATION_UNIT);
		Document extractedClassDocument = new Document();
		classParser.setSource(extractedClassDocument.get().toCharArray());

		CompilationUnit classCompilationUnit = (CompilationUnit)classParser.createAST(null);
		AST classAST = classCompilationUnit.getAST();
		ASTRewrite classRewriter = ASTRewrite.create(classAST);
		
		PackageDeclaration packageDeclaration = classAST.newPackageDeclaration();
		classRewriter.set(packageDeclaration, PackageDeclaration.NAME_PROPERTY,classAST.newSimpleName("myfactory"), null);
        
        ListRewrite classTypesRewrite = classRewriter.getListRewrite(classCompilationUnit, CompilationUnit.TYPES_PROPERTY);
        classRewriter.set(classCompilationUnit, CompilationUnit.PACKAGE_PROPERTY, packageDeclaration, null);
       
        TypeDeclaration classTypeDeclaration = classAST.newTypeDeclaration();
        SimpleName className = classAST.newSimpleName("MyFactory");
        classRewriter.set(classTypeDeclaration, TypeDeclaration.NAME_PROPERTY, className, null);
        ListRewrite classModifiersRewrite = classRewriter.getListRewrite(classTypeDeclaration, TypeDeclaration.MODIFIERS2_PROPERTY);
        classModifiersRewrite.insertLast(classAST.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
        classTypesRewrite.insertLast(classTypeDeclaration, null);
        classCompilationUnit.types().add(classTypeDeclaration);
        
        try {
//        	extractedClassICompilationUnit.commitWorkingCopy(true, new NullProgressMonitor());
        	TextEdit extractedClassEdit = classRewriter.rewriteAST(extractedClassDocument, null);
        	extractedClassEdit.apply(extractedClassDocument);
        	CreateCompilationUnitChange createCompilationUnitChange =
        		new CreateCompilationUnitChange(classICompilationUnit, extractedClassDocument.get(), classFile.getCharset());

        	createCompilationUnitChange.perform(new NullProgressMonitor());
//        	AbstractionView.refreshProject();
        } catch (CoreException e) {
        	e.printStackTrace();
        } catch (MalformedTreeException e) {
        	e.printStackTrace();
        } catch (BadLocationException e) {
        	e.printStackTrace();
        }
	}
}
