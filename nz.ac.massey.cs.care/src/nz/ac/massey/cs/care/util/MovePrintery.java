package nz.ac.massey.cs.care.util;

import static nz.ac.massey.cs.care.util.MoveUtils.round;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import nz.ac.massey.cs.care.Edge;
import nz.ac.massey.cs.care.metrics.PackageMetrics.Metrics;
import nz.ac.massey.cs.care.metrics.SCCMetrics;
import nz.ac.massey.cs.care.refactoring.movehelper.Candidate;
import nz.ac.massey.cs.care.refactoring.movehelper.ConstraintsResult;
import nz.ac.massey.cs.care.refactoring.movehelper.RefactoringResult;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;

import au.com.bytecode.opencsv.CSVReader;

/**
 * This class has utility methods for printing data to files
 * @author Ali
 *
 */
public class MovePrintery {
	private static final String SEP = ",";
	private static final String NL = System.getProperty("line.separator");

	public static void printRefactoredEdgeDetails(String filename, int i, Edge winner, int scdRemoved, int stkRemoved, int awdRemoved, int deginhRemoved, int edgeCounter, int tg, int sl, int smi, int move, long time)
			throws IOException {
		FileWriter out = new FileWriter(filename, true);

		StringBuffer b = new StringBuffer().append(i).append(SEP)
				.append(winner.getStart().getFullname()).append(SEP).append(winner.getType())
				.append(SEP).append(winner.getEnd().getFullname()).append(SEP)
				.append(scdRemoved)
				.append(SEP)
				.append(stkRemoved)
				.append(SEP)
				.append(awdRemoved)
				.append(SEP)
				.append(deginhRemoved)
				.append(SEP)
				.append(edgeCounter)
				.append(SEP)
				.append(tg)
				.append(SEP)
				.append(sl)
				.append(SEP)
				.append(smi)
				.append(SEP)
				.append(move)
				.append(SEP)
				.append(time)
				.append(NL);
		out.write(b.toString());
		out.close();
	}
	
	public static void printAppliedRefactoringType(String filename, int i, String refactoringTypeApplied) throws IOException {
		FileWriter out = new FileWriter(filename, true);
		StringBuffer b = new StringBuffer().append(i).append(SEP)
				.append(refactoringTypeApplied)
				.append(NL);
		out.write(b.toString());
		out.close();
		// log("result summary added to ",filename);
	}
	
	public static boolean hasHeader(String filename) {
		try {
			File f = new File(filename);
			CSVReader reader = new CSVReader(new FileReader(f));
			String[] nextLine= reader.readNext();
			reader.close();
			if(nextLine[0].equals("iteration")) return true; 
			else return false;
		} catch (FileNotFoundException e) {
			return false;
		} catch (IOException e) {
			return false;
		}
	}
	public static void printConstraintsInfo(IProject p, String filename,
			Candidate c) {
		try {
			IFile f = p.getFile(filename);
			InputStream st = f.getContents();
			String content = convertStreamToString(st);
			
//			CSVReader reader = new CSVReader(new FileReader(new File(filename)));
			String class2Move = c.getClassToMove();
			String toPackage = c.getTargetPackage();
			boolean exit = false;
			StringTokenizer tokenizer = new StringTokenizer(content,"\n");
			while(tokenizer.hasMoreTokens()) {
				String line = tokenizer.nextToken();
				String[] s = convertToCSV(line);
				if(s[0].equals(class2Move) && s[1].equals(toPackage)) exit = true;
			}
//			for(String[] s : reader.readAll()) {
//				if(s[0].equals(class2Move) && s[1].equals(toPackage)) exit = true; //already there.
//			}
//			reader.close();
			if(exit) return;
//			FileWriter out = new FileWriter(filename, true);
			ConstraintsResult r = c.getConstraintsResult();
			StringBuffer b = new StringBuffer().append(c.getClassToMove()).append(SEP)
					.append(c.getTargetPackage());
			Boolean result = r.isRename();
			addToAppender(result, b);
			result = r.isBlacklisted();
			addToAppender(result, b);
			result = r.isAccessability();
			addToAppender(result, b);
			result = r.isInstanceCount();
			addToAppender(result, b);
			result = r.isCompilation();
			addToAppender(result, b);
			result = r.areAllMoveConstraintsPassed();
			addToAppender(result, b);
//			b.append(NL);
//			out.write(b.toString());
//			out.close();
			println(p,filename,b.toString());
		} catch (IOException e) {
			e.printStackTrace();
			return;
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
	public static String convertStreamToString(java.io.InputStream is) {
	    java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
	    return s.hasNext() ? s.next() : "";
	}
	private static String[] convertToCSV(String line) {
		StringTokenizer t = new StringTokenizer(line,",");
		String[] result = new String[t.countTokens()];
		int count = 0;
		while(t.hasMoreTokens()) {
			result[count++] = t.nextToken();
		}
		return result;
	}
	private static void addToAppender(Boolean result, StringBuffer b) {
		b.append(SEP);
		if(result == null) b.append("n/a"); 
		else if(result == false) b.append("failed");
		else b.append("passed");	
	}

	public static void printSkippedEdges(IProject p,String filename, int i,Edge winner) throws IOException {
//		FileWriter out = new FileWriter(filename, true);
		StringBuffer b = new StringBuffer().append(i).append(SEP)
				.append(winner.getStart().getFullname())
				.append(SEP).append(winner.getType()).append(SEP)
				.append(winner.getEnd().getFullname());
//				.append(NL);
//		out.write(b.toString());
//		out.close();
		println(p,filename,b.toString());
		// log("result summary added to ",filename);
	}
//	public static void println(String filename, String text)
//			throws IOException {
//		FileWriter out = new FileWriter(filename, true);
//		out.write(text);
//		out.write(NL);
//		out.close();
//	}
	public static void println(IProject p, String filename, String text)
			throws IOException {
		if(p == null) return;
		IFile f = p.getFile(filename);
		if(f== null) return;
		try {
			if(!f.exists()) {
				f.create(new ByteArrayInputStream(text.getBytes()), IResource.NONE, null);
			} else {
				f.appendContents(new ByteArrayInputStream(text.getBytes()), IResource.NONE, null);
			}
			f.appendContents(new ByteArrayInputStream("\n".getBytes()), IResource.NONE, null);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
	
	public static void printMetrics(IProject p, String metricsFile, SCCMetrics sccBefore,
			SCCMetrics sccAfter, double modularityBefore,
			double modularityAfter, double distanceBefore,
			double distanceAfter, long totalTime) throws IOException {
		StringBuffer b = new StringBuffer()
		.append(round(sccBefore.getCompressionRatio())).append(SEP)
		.append(round(sccAfter.getCompressionRatio()))
		.append(SEP).append(sccBefore.getMaxSccSize()).append(SEP)
		.append(sccAfter.getMaxSccSize())
		.append(SEP).append(round(sccBefore.getDensity()))
		.append(SEP).append(round(sccAfter.getDensity()))
		.append(SEP).append(round(sccBefore.getTangledness()))
		.append(SEP).append(round(sccAfter.getTangledness()))
		.append(SEP).append(sccBefore.getCount())
		.append(SEP).append(sccAfter.getCount())
		.append(SEP).append(round(modularityBefore))
		.append(SEP).append(round(modularityAfter))
		.append(SEP).append(round(distanceBefore))
		.append(SEP).append(round(distanceAfter))
		.append(SEP).append(totalTime);
		String text = b.toString();
		println(p,metricsFile, text);
	}
	public static void printPackageMetrics(IProject p, String metricsFile,
			Map<String, Metrics> pmBefore, Map<String, Metrics> pmAfter) throws IOException {
		StringBuffer b = new StringBuffer();
		for(String pckg : pmBefore.keySet()){
			Metrics before = pmBefore.get(pckg);
			Metrics after = pmAfter.get(pckg);
			if(after == null) {
				int ce = -1;
				int ca = -1;
				double a = -1.0;
				double i = -1.0;
				double d = -1.0;
				b.append(pckg).append(SEP)
				.append(before.getCE()).append(SEP)
				.append(ce).append(SEP)
				.append(before.getCA()).append(SEP)
				.append(ca).append(SEP)
				.append(round(before.getA())).append(SEP)
				.append(round(a)).append(SEP)
				.append(round(before.getI())).append(SEP)
				.append(round(i)).append(SEP)
				.append(round(before.getD())).append(SEP)
				.append(round(d)).append(NL);
			} else {
				b.append(pckg).append(SEP)
				.append(before.getCE()).append(SEP)
				.append(after.getCE()).append(SEP)
				.append(before.getCA()).append(SEP)
				.append(after.getCA()).append(SEP)
				.append(round(before.getA())).append(SEP)
				.append(round(after.getA())).append(SEP)
				.append(round(before.getI())).append(SEP)
				.append(round(after.getI())).append(SEP)
				.append(round(before.getD())).append(SEP)
				.append(round(after.getD())).append(NL);
			}
			
		}
		String text = b.toString();
		println(p, metricsFile, text);
	}
//	public static void printRemovalStepStats(String filename, int i,
//			int instances, double instPercent) throws IOException {
//		FileWriter out = new FileWriter(filename, true);
//		StringBuffer b = new StringBuffer().append(i).append(SEP)
//				.append(instances).append(SEP).append(instPercent)
//				.append(NL);
//		out.write(b.toString());
//		out.close();
//		// log("result summary added to ",filename);
//	}
	
	public static void printG2CSuccess(IProject p, String g2cSuccessFile,
			Candidate candidate, boolean passed) {
		try {
//			FileWriter out = new FileWriter(new File(g2cSuccessFile), true);
			StringBuffer b = new StringBuffer()
				.append(candidate.getClassToMove())
				.append(SEP)
				.append(candidate.getTargetPackage())
				.append(SEP);
			if(passed) b.append("passed");
			else b.append("failed");
//				b.append(NL);
//			out.write(b.toString());
//			out.close();
			println(p,g2cSuccessFile,b.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	public static void printRemovalStepStats(IProject p, String filename, int i,
			int instances, double instPercent) throws IOException {
//		FileWriter out = new FileWriter(filename, true);
		StringBuffer b = new StringBuffer().append(i).append(SEP)
				.append(instances).append(SEP).append(instPercent);
//				.append(NL);
		println(p,filename,b.toString());
//		out.close();
		// log("result summary added to ",filename);
	}
	public static void printRefactoredEdgeDetails(IProject p, String filename, int i,
			Edge winner, String source, String target
			, int cd, int awd, int stk, int deginh, 
			double srcDistanceBefore, double srcDistanceAfter, double tarDistanceBefore,
			double tarDistanceAfter, int edgeCounter, long time)
			throws IOException {
//		FileWriter out = new FileWriter(filename, true);

		StringBuffer b = new StringBuffer().append(i).append(SEP)
				.append(source).append(SEP).append(winner.getType())
				.append(SEP).append(target).append(SEP)
				.append(cd).append(SEP)
				.append(awd).append(SEP).append(stk).append(SEP)
				.append(deginh)
				.append(SEP).append(srcDistanceBefore).append(SEP)
				.append(srcDistanceAfter).append(SEP).append(tarDistanceBefore)
				.append(SEP).append(tarDistanceAfter).append(SEP)
				.append(edgeCounter)
				.append(SEP)
				.append(time);
//				.append(NL);
		println(p, filename, b.toString());
//		out.close();
	}
	public static void printRefactoringApplied(IProject p, String filename, RefactoringResult result,
			Candidate candidate, String dependency) {
		
		try {
//			FileWriter out = new FileWriter(filename, true);
			StringBuffer b = new StringBuffer();
			b.append(candidate.getSourceClass())
			.append(SEP)
			.append(candidate.getTargetClass())
			.append(SEP)
			.append(dependency)
			.append(SEP)
			.append(result.getFirstAttempted().getName())
			.append(SEP);
			if(result.getSecondAttepted() == null) b.append("none");
			else b.append(result.getSecondAttepted().getName());
			b.append(SEP);
			if(result.getFinalRefactoringApplied() == null) b.append("none");
			else b.append(result.getFinalRefactoringApplied().getName());
			b.append(SEP);
			if(result.hasError()) b.append("failed");
			else b.append("passed");
			
//			b.append(NL);
			String line = b.toString();
			println(p, filename, line);
//			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public static void printRemovalStepStats(IProject p, String filename, int i,
			int instances, double instPercent, int weakerInstances,
			Double weakerPecent) throws IOException {
//		FileWriter out = new FileWriter(filename, true);
		StringBuffer b = new StringBuffer().append(i).append(SEP)
				.append(instances).append(SEP).append(instPercent).append(SEP)
				.append(weakerInstances).append(SEP).append(weakerPecent);
//				.append(NL);
//		out.write(b.toString());
//		out.close();
		println(p,filename,b.toString());
		// log("result summary added to ",filename);
	}
	public static void printMovedEdgeDetails(IProject p, String filename, int i,
			Edge winner, String source, String target, String classToMove,
			String targetPackage, int cd,
			double srcDistanceBefore, double srcDistanceAfter, double tarDistanceBefore,
			double tarDistanceAfter, int edgeCounter, long time)
			throws IOException {
//		FileWriter out = new FileWriter(filename, true);

		StringBuffer b = new StringBuffer().append(i).append(SEP)
				.append(source).append(SEP).append(winner.getType())
				.append(SEP).append(target).append(SEP).append(classToMove)
				.append(SEP).append(targetPackage).append(SEP).append(cd)
				.append(SEP).append(srcDistanceBefore).append(SEP)
				.append(srcDistanceAfter).append(SEP).append(tarDistanceBefore)
				.append(SEP).append(tarDistanceAfter).append(SEP)
				.append(edgeCounter)
				.append(SEP)
				.append(time);
//				.append(NL);
//		out.write(b.toString());
//		out.close();
		println(p,filename,b.toString());
	}
	public static void printFailedEdge(IProject p, Edge winner, Candidate candidate, String filename, String message) {
		try {
//			FileWriter out = new FileWriter(new File(filename), true);
			StringBuffer b = new StringBuffer()
				.append(winner.getStart().getFullname())
				.append(SEP)
				.append(winner.getType())
				.append(SEP)
				.append(winner.getEnd().getFullname())
				.append(SEP)
				.append(candidate.getClassToMove())
				.append(SEP)
				.append(candidate.getTargetPackage())
				.append(SEP)
				.append(message);
//				.append(NL);
//			out.write(b.toString());
//			out.close();
			println(p,filename,b.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public static void printDistanceInfoHeader(String filename) throws IOException {
		FileWriter out = new FileWriter(filename, true);
		StringBuffer b = new StringBuffer()
				.append("D before").append(SEP)
				.append("D after")
				.append(NL);
		out.write(b.toString());
		out.close();
	}
	
	public static void printSCCInfo(String filename, SCCMetrics before, SCCMetrics after) throws IOException {
		FileWriter out = new FileWriter(filename, true);
		StringBuffer b = new StringBuffer()
				.append(before.getCompressionRatio()).append(SEP)
				.append(after.getCompressionRatio())
				.append(SEP).append(before.getMaxSccSize()).append(SEP)
				.append(after.getMaxSccSize())
				.append(SEP).append(before.getDensity())
				.append(SEP).append(after.getDensity())
				.append(SEP).append(before.getTangledness())
				.append(SEP).append(after.getTangledness())
				.append(SEP).append(before.getCount())
				.append(SEP).append(after.getCount())
				.append(NL);
		out.write(b.toString());
		out.close();
		// log("result summary added to ",filename);
	}
	public void printModularityInfoHeader(String filename) throws IOException {
		FileWriter out = new FileWriter(filename, true);
		StringBuffer b = new StringBuffer()
				.append("modularity before").append(SEP)
				.append("modularity after").append(SEP)
				.append("types generalised").append(SEP)
				.append("DI used")
				.append(NL);
		out.write(b.toString());
		out.close();
		
	}
	public void printDistanceInfo(String filename, double before,
			double after) throws IOException {
		FileWriter out = new FileWriter(filename, true);
		StringBuffer b = new StringBuffer()
				.append(before).append(SEP)
				.append(after)
				.append(NL);
		out.write(b.toString());
		out.close();
	}
	
	

	public static void printSCCInfoHeader(String filename) throws IOException {
		FileWriter out = new FileWriter(filename, true);
		StringBuffer b = new StringBuffer()
				.append("compression before").append(SEP)
				.append("compression after")
				.append(SEP).append("Max SCC size before").append(SEP)
				.append("Max SCC size after")
				.append(SEP).append("density before")
				.append(SEP).append("density after")
				.append(SEP).append("spl before")
				.append(SEP).append("spl after")
				.append(SEP).append("count before")
				.append(SEP).append("count after")
				.append(NL);
		out.write(b.toString());
		out.close();
		// log("result summary added to ",filename);
	}
	
	public static void printOverview(String overviewFilename2, int attempted, int successful,
			int totalInstancesBefore2, int totalInstancesAfter2, long l) {
		try {
			FileWriter out = new FileWriter(overviewFilename2, true);
			Double percentRemoved = 100.0 - (double) ((totalInstancesAfter2 * 100) / totalInstancesBefore2);
			StringBuffer b = new StringBuffer()
					.append(attempted).append(SEP)
					.append(successful).append(SEP)
					.append(totalInstancesBefore2).append(SEP)
					.append(totalInstancesAfter2).append(SEP)
					.append(percentRemoved).append(SEP)
					.append(l).append(NL);
			out.write(b.toString());
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void printGraphProperties(String filename, String name, int vertexCount, int edgeCount) {
		try{
			FileWriter out = new FileWriter(filename, true);
			StringBuffer b = new StringBuffer()
					.append(name).append(SEP)
					.append(vertexCount).append(SEP)
					.append(edgeCount)
					.append(NL);
			out.write(b.toString());
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	public static void writeResult(String dependencyOutfile, RefactoringStatus status,
			boolean isCompilationFailed, Edge winner) {
		
		String message = null;
		RefactoringStatusEntry[] entries = status.getEntries();
		for(RefactoringStatusEntry entry : entries) {
			message = entry.getMessage();
			if(message == null) message = "";
			break;
		}
		if(message == null) message = "";
		FileWriter out;
		try {
			out = new FileWriter(dependencyOutfile,true);
			StringBuffer b = new StringBuffer();
			b.append(winner.getStart().getFullname())
			.append(SEP)
			.append(winner.getType())
			.append(SEP)
			.append(winner.getEnd().getFullname())
			.append(SEP);
			if(message.equals("isInstantiationFailed")) b.append("1");
			else b.append("0");
			b.append(SEP);
			if(status.hasError()) b.append("1");
			else b.append("0");
			b.append(SEP);
			if(isCompilationFailed) b.append("1");
			else b.append("0");
			b.append(SEP);
			if(!status.hasError() && !isCompilationFailed) b.append("Pass");
			else b.append("Fail");
			b.append(NL);
			
			out.write(b.toString());
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	public static void printHeader(String dependencyOutfile) {
		FileWriter out;
		try {
			out = new FileWriter(dependencyOutfile,true);
			StringBuffer b = new StringBuffer();
			b.append("source")
			.append(SEP)
			.append("type")
			.append(SEP)
			.append("target")
			.append(SEP)
			.append("isInstiationFailed")
			.append(SEP)
			.append("preconditionsFailed")
			.append(SEP)
			.append("postconditionsFailed")
			.append(SEP)
			.append("result")
			.append(NL);
			
			out.write(b.toString());
			out.close();
		} catch (IOException e) {
			//do nothing
		}
	}

	public static void printRanks(String string, List<Integer> ranks) {
		FileWriter out = null;
		try{
			out = new FileWriter(new File(string), true);
			StringBuffer b = new StringBuffer();
			for(Integer i : ranks) {
				b.append(i).append(SEP);
			}
			b.append(NL);
			out.write(b.toString());
			out.close();
		} catch (IOException e) {
		}
		
	}

	public static void printConstraints(IProject iProject, String filename,
			String refacName, String dependencyName, Candidate c) {
		ConstraintsResult r = c.getConstraintsResult();
		StringBuffer b = new StringBuffer()
			.append(c.getEdge().getStart().getFullname())
			.append(SEP)
			.append(c.getEdge().getEnd().getFullname())
			.append(SEP)
			.append(dependencyName)
			.append(SEP)
			.append(refacName);
		Boolean result = r.isBlacklisted();
		addToAppender(result, b);
		result = r.isAccessability();
		addToAppender(result, b);
		result = r.isRename();
		addToAppender(result, b);
		result = r.isSamePackage();
		addToAppender(result, b);
		result = r.isNoValidSupertype();
		addToAppender(result, b);
		result = r.isClassBoundryViolation();
		addToAppender(result, b);
		result = r.isSelfInstanceCreation();
		addToAppender(result, b);
		result = r.isCompilation();
		addToAppender(result, b);
		result = r.isInstanceCount();
		addToAppender(result, b);
//		result = r.areAllConstraintsPassed();
//		addToAppender(result, b);	
		try {
			println(iProject,filename,b.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
			
	}
}
