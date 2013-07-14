package nz.ac.massey.cs.care.ast;


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

@SuppressWarnings("restriction")
public class ASTUtils {
	@SuppressWarnings({ "unchecked", "deprecation" })
	public static void createFactoryDeclaration(IJavaProject javaProject) {
		IFolder folder = javaProject.getProject().getFolder("src");
		IPackageFragmentRoot srcFolder = javaProject.getPackageFragmentRoot(folder);
		try {
			srcFolder.createPackageFragment("registry", true, null);
		} catch (JavaModelException e1) {
			e1.printStackTrace();
		}
		IFile classFile = javaProject.getProject().getFile("/src/registry/ServiceLocator.java");
		ICompilationUnit classICompilationUnit = JavaCore.createCompilationUnitFrom(classFile);
		ASTParser classParser = ASTParser.newParser(AST.JLS3);
		classParser.setKind(ASTParser.K_COMPILATION_UNIT);
		Document extractedClassDocument = new Document();
		classParser.setSource(extractedClassDocument.get().toCharArray());

		CompilationUnit classCompilationUnit = (CompilationUnit)classParser.createAST(null);
		AST classAST = classCompilationUnit.getAST();
		ASTRewrite classRewriter = ASTRewrite.create(classAST);
		
		PackageDeclaration packageDeclaration = classAST.newPackageDeclaration();
		classRewriter.set(packageDeclaration, PackageDeclaration.NAME_PROPERTY,classAST.newSimpleName("registry"), null);
        
        ListRewrite classTypesRewrite = classRewriter.getListRewrite(classCompilationUnit, CompilationUnit.TYPES_PROPERTY);
        classRewriter.set(classCompilationUnit, CompilationUnit.PACKAGE_PROPERTY, packageDeclaration, null);
       
        TypeDeclaration classTypeDeclaration = classAST.newTypeDeclaration();
        SimpleName className = classAST.newSimpleName("ServiceLocator");
        classRewriter.set(classTypeDeclaration, TypeDeclaration.NAME_PROPERTY, className, null);
        ListRewrite classModifiersRewrite = classRewriter.getListRewrite(classTypeDeclaration, TypeDeclaration.MODIFIERS2_PROPERTY);
        classModifiersRewrite.insertLast(classAST.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD), null);
        classTypesRewrite.insertLast(classTypeDeclaration, null);
        classCompilationUnit.types().add(classTypeDeclaration);
        
        try {
        	TextEdit extractedClassEdit = classRewriter.rewriteAST(extractedClassDocument, null);
        	extractedClassEdit.apply(extractedClassDocument);
        	CreateCompilationUnitChange createCompilationUnitChange =
        		new CreateCompilationUnitChange(classICompilationUnit, extractedClassDocument.get(), classFile.getCharset());

        	createCompilationUnitChange.perform(new NullProgressMonitor());
        } catch (CoreException e) {
        	e.printStackTrace();
        } catch (MalformedTreeException e) {
        	e.printStackTrace();
        } catch (BadLocationException e) {
        	e.printStackTrace();
        }
	}
}
