package nz.ac.massey.cs.care.io;



import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
	
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}
	
	
	
}
