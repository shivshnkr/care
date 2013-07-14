package nz.ac.massey.cs.care.scripts.stat;

public class PackageStats {
	private String programName = null;
	private double mergedPackages = 0;
	private double totalPackages = 0;
	private double ratio;
	public String getProgramName() {
		return programName;
	}
	public void setProgramName(String programName) {
		this.programName = programName;
	}
	public double getMergedPackages() {
		return mergedPackages;
	}
	public void setMergedPackages(double merged) {
		this.mergedPackages = merged;
	}
	public double getTotalPackages() {
		return totalPackages;
	}
	public void setTotalPackages(double total) {
		this.totalPackages = total;
	}
	public void setRatio(double ratioThisProgram) {
		this.ratio = ratioThisProgram;
	}
	public Double getRatio() {
		return ratio;
	}
	
}