package nz.ac.massey.cs.care.refactoring.movehelper;

import java.util.ArrayList;
import java.util.List;

import nz.ac.massey.cs.care.dependency.Dependency;
import nz.ac.massey.cs.care.refactoring.executers.CareRefactoring;

public class RefactoringResult {
	
	private Boolean hasError = false;
	private CareRefactoring firstAttempted = null;
	private CareRefactoring secondAttepted = null;
	private CareRefactoring finalRefactoringApplied = null;
	private Dependency dependencyType = null;
	private List<String> failedPreconditions = new ArrayList<String>();
	private List<String> failedPostconditions =  new ArrayList<String>();

	public void setError(boolean b) {
		this.hasError = b;
		
	}
	
	public Boolean hasError() {
		return hasError;
	}

	public void setFailedPreconditions(List<String> failedPreconditions) {
		this.failedPreconditions = failedPreconditions;
	}
	
	public List<String> getFailedPreconditions() {
		return failedPreconditions;
	}

	public void setFailedPostconditions(List<String> failedPostconditions) {
		this.failedPostconditions  = failedPostconditions;
	}
	
	public List<String> getFailedPostconditions() {
		return failedPostconditions;
	}

	public void clear() {
		this.failedPostconditions.clear();
		this.failedPreconditions.clear();
		this.hasError = false;
	}

	public CareRefactoring getFirstAttempted() {
		return firstAttempted;
	}

	public void setFirstAttempted(CareRefactoring firstAttempted) {
		this.firstAttempted = firstAttempted;
	}

	public CareRefactoring getSecondAttepted() {
		return secondAttepted;
	}

	public void setSecondAttepted(CareRefactoring secondAttepted) {
		this.secondAttepted = secondAttepted;
	}

	public CareRefactoring getFinalRefactoringApplied() {
		return finalRefactoringApplied;
	}

	public void setFinalRefactoringApplied(CareRefactoring finalRefactoringApplied) {
		this.finalRefactoringApplied = finalRefactoringApplied;
	}

	public Dependency getDependencyType() {
		return dependencyType;
	}

	public void setDependencyType(Dependency dependencyType) {
		this.dependencyType = dependencyType;
	}
}
