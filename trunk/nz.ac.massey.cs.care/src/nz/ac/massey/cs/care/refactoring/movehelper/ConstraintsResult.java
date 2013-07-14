package nz.ac.massey.cs.care.refactoring.movehelper;

public class ConstraintsResult {
	
	private Boolean rename = null;
	private Boolean accessability = null;
	private Boolean blacklisted = null;
	private Boolean compilation = null;
	private Boolean instanceCount = null;
	private Boolean edgeType = null;
	private Boolean sourceType = null;
	private Boolean innerClass = null;
	private Boolean samePackage = null;
	private Boolean noValidSupertype = null;
	private Boolean selfInstanceCreation = null;
	private Boolean classBoundryViolation = null;
	
	/**
	 * For move refactoring preconditions
	 * @return
	 */
	public Boolean areMovePresPassed () {
		if(rename == true && accessability == true && blacklisted == true) return true;
		else return false;
	}
	
	public Boolean arePresPassed () {
		if(edgeType == true && sourceType == true && innerClass == true) return true;
		else return false;
	}
	/**
	 * For move refactoring postconditions
	 * @return
	 */
	public boolean arePostsPassed() {
		if(instanceCount == true && compilation == true) return true;
		else return false;
	}
	public Boolean isRename() {
		return rename;
	}
	public void setRename(Boolean rename) {
		this.rename = rename;
	}
	public Boolean isAccessability() {
		return accessability;
	}
	public void setAccessability(Boolean accessability) {
		this.accessability = accessability;
	}
	public Boolean isBlacklisted() {
		return blacklisted;
	}
	public void setBlacklisted(Boolean blacklisted) {
		this.blacklisted = blacklisted;
	}
	public Boolean isCompilation() {
		return compilation;
	}
	public void setCompilation(Boolean compilation) {
		this.compilation = compilation;
	}
	public Boolean isInstanceCount() {
		return instanceCount;
	}
	public void setInstanceCount(Boolean instanceCount) {
		this.instanceCount = instanceCount;
	}
	public Boolean areGraphConstraintsPassed() {
		Boolean r = null; 
		try {
			if(rename == true && blacklisted == true && accessability == true &&
					instanceCount == true ) {
				r = true;
			} else if (rename == false || blacklisted == false || accessability == false ||
					instanceCount == false ) {
				r = false;
			} else {
				//Do nothing
			}
		} catch(NullPointerException e) {
			return r;
		}
		
		return r;
	}
	public Boolean areAllConstraintsPassed() {
		Boolean r = null; 
		try {
			if(rename == true && blacklisted == true && accessability == true &&
					instanceCount == true && compilation == true ) {
				r = true;
			} else if (rename == false || blacklisted == false || accessability == false ||
					instanceCount == false || compilation == false || samePackage == false ||
					noValidSupertype == false || classBoundryViolation == false || selfInstanceCreation == false) {
				r = false;
			} else {
				r = true;
			}
		} catch(NullPointerException e) {
			return r;
		}
		
		return r;
	}
	public Boolean areAllMoveConstraintsPassed() {
		Boolean r = null; 
		try {
			if(rename == true && blacklisted == true && accessability == true &&
					instanceCount == true && compilation == true ) {
				r = true;
			} else if (rename == false || blacklisted == false || accessability == false ||
					instanceCount == false || compilation == false ) {
				r = false;
			} else {
				//Do nothing
			}
		} catch(NullPointerException e) {
			return r;
		}
		
		return r;
	}
	public Boolean isEdgeType() {
		return edgeType;
	}
	public void setEdgeType(Boolean edgeType) {
		this.edgeType = edgeType;
	}
	public Boolean isSourceType() {
		return sourceType;
	}
	public void setSourceType(Boolean sourceType) {
		this.sourceType = sourceType;
	}
	public Boolean isInnerClass() {
		return innerClass;
	}
	public void setInnerClass(Boolean innerClass) {
		this.innerClass = innerClass;
	}

	public Boolean isSamePackage() {
		return samePackage;
	}

	public void setSamePackage(Boolean samePackage) {
		this.samePackage = samePackage;
	}

	public Boolean isNoValidSupertype() {
		return noValidSupertype;
	}

	public void setNoValidSupertype(Boolean noValidSupertype) {
		this.noValidSupertype = noValidSupertype;
	}

	public Boolean isSelfInstanceCreation() {
		return selfInstanceCreation;
	}

	public void setSelfInstanceCreation(Boolean selfInstanceCreation) {
		this.selfInstanceCreation = selfInstanceCreation;
	}

	public void setClassBoundryViolation(boolean b) {
		this.classBoundryViolation = b;
		
	}
	
	public Boolean isClassBoundryViolation() {
		return classBoundryViolation;
	}
	
}
