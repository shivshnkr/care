package nz.ac.massey.cs.care.io;



import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import nz.ac.massey.cs.care.refactoring.manipulators.AbstractionCandidateRefactoring;
import nz.ac.massey.cs.gql4jung.Edge;
import au.com.bytecode.opencsv.CSVReader;

/**
 * Reads the dependency file in csv format
 * @author Ali
 *
 */
public class DependReader {

	private String sourceClass = null;
	private String targetClass = null;
	private String relationship = null;
	private String path = null;
	
	public DependReader(String path){
		this.path = path; 
	}
	
	public DependReader(Edge winner) {
		this.sourceClass = winner.getStart().getFullname();
		this.targetClass = winner.getEnd().getFullname();
		this.relationship = winner.getType();
	}
	
	public AbstractionCandidateRefactoring getCandidates(){
		AbstractionCandidateRefactoring candidate = new AbstractionCandidateRefactoring();
		candidate.setSourceClass(sourceClass);
		candidate.setTargetClass(targetClass);
		candidate.setRelationship(relationship);
		return candidate;
	}
	
	public List<AbstractionCandidateRefactoring> getAbstractionDependencies() {
		List<AbstractionCandidateRefactoring> candidates = new ArrayList<AbstractionCandidateRefactoring>();
		try {
			File csvFile = new File(getPath());
			CSVReader reader;
			reader = new CSVReader(new FileReader(csvFile));
			String[] nextLine;
			while ((nextLine = reader.readNext()) != null
					&& !nextLine[0].equals("99")) {
				// nextLine[] is an array of values from the lines of CSV file.
				if(nextLine[0].equals("iteration")) continue;
				String sourceClass = nextLine[1];
				String uses = nextLine[2];
				String targetClass = nextLine[3];
				if(!uses.equals("uses")) continue;
				AbstractionCandidateRefactoring candidate = new AbstractionCandidateRefactoring();
				candidate.setSourceClass(sourceClass);
				candidate.setRelationship(uses);
				candidate.setTargetClass(targetClass);
				candidates.add(candidate);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return candidates;
	}
	
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}
	
	
	
}
