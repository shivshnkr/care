package nz.ac.massey.cs.care.move.refactoring.movehelper;

public class ConstraintsResult {
	
	private Boolean rename = null;
	private Boolean accessability = null;
	private Boolean blacklisted = null;
	private Boolean compilation = null;
	private Boolean instanceCount = null;
	
	public Boolean arePresPassed () {
		if(rename == true && accessability == true && blacklisted == true) return true;
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
}
