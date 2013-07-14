package nz.ac.massey.cs.care.io;

import static nz.ac.massey.cs.care.io.LoggerUtils.getSimpleName;
import static nz.ac.massey.cs.care.io.LoggerUtils.log;
import gr.uom.java.ast.ASTReader;
import gr.uom.java.ast.ClassObject;

import java.io.IOException;

import nz.ac.massey.cs.care.Edge;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class LoggerUtils {
	private static Logger LOGGER = Logger.getLogger("batch-script");
	
	public static void addRefacCodeAppender(Edge winner, String path) throws IOException {
		String filename = getSimpleName(winner.getStart().getFullname())+".txt";
		String fullpath = path + "/" + filename;
		//configure the appender
		FileAppender appender = new FileAppender(new PatternLayout(),fullpath);
		appender.setName(filename);
		if(LOGGER.getAppender(filename) == null) {
			LOGGER.addAppender(appender);
		}
		//add new code
		String classname = winner.getStart().getFullname();
		classname = classname.replace("$", ".");
		ClassObject object = ASTReader.getSystemObject().getClassObject(classname);
		if(object == null) return;
		if(object.getTypeDeclaration()==null) return;
		CompilationUnit cu = (CompilationUnit) object.getCompilationUnit();
		ICompilationUnit icu =  (ICompilationUnit) cu.getJavaElement();
		try {
			final String newSource = icu.getSource();
			log(newSource);
			log("//*********************REFACTORED CODE ****************");
			LOGGER.removeAllAppenders();
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
	}
	public static void addOldCodeAppender(Edge winner, String path) throws IOException {
		String filename = getSimpleName(winner.getStart().getFullname())+"_old.txt";
		String fullpath = path  + "/" + filename;
		//configure the appender
		FileAppender appender = new FileAppender(new PatternLayout(),fullpath);
		appender.setName(filename);
		if(LOGGER.getAppender(filename) == null) LOGGER.addAppender(appender);
		//add old source code
		String classname = winner.getStart().getFullname();
		classname = classname.replace("$", ".");
		ClassObject object = ASTReader.getSystemObject().getClassObject(classname);
		if(object == null) return;
//		if(dependency.getSourceClassObject().getTypeDeclaration() == null) return;
		CompilationUnit cu = (CompilationUnit) object.getCompilationUnit();//getTypeDeclaration().getRoot();
		ICompilationUnit icu =  (ICompilationUnit) cu.getJavaElement();
		try {
			final String oldSource = icu.getSource();
			log(oldSource);
			LOGGER.removeAllAppenders();
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		
	}
	public static void addErrorAppender(String errors, String path) throws IOException {
		String filename = "error.log";
		String fullpath = path + "/" + filename;
		//configure the appender
		FileAppender appender = new FileAppender(new PatternLayout(),fullpath);
		appender.setName(filename);
		if(LOGGER.getAppender(filename) == null) LOGGER.addAppender(appender);
		log(errors);
		LOGGER.removeAllAppenders();
	}
	
	public static String getSimpleName(String fullname){
		String simpleName = fullname.substring(fullname.lastIndexOf(".")+1);
		return simpleName;
	}
	
	public static void log(Object... s) {
		StringBuffer b = new StringBuffer();
		for (Object t:s) {
			b.append(t);
		}
		LOGGER.warn(b.toString());
//		System.out.println(b.toString());
	}

}
