package nz.ac.massey.cs.care.metrics;

import java.util.Map;

public class MetricsResult {
	private double modularity = 0.0;
	private double distance = -1;
	private long time = 0;
	private SCCMetrics sccMetrics;
	private Map<String, PackageMetrics.Metrics> packageMetrics;
	
	public double getModularity() {
		return modularity;
	}
	public void setModularity(double modularity) {
		this.modularity = modularity;
	}
	public double getDistance() {
		return distance;
	}
	public void setDistance(double distance) {
		this.distance = distance;
	}
	public long getTime() {
		return time;
	}
	public void setTime(long time) {
		this.time = time;
	}
	public SCCMetrics getSccMetrics() {
		return sccMetrics;
	}
	public void setSccMetrics(SCCMetrics sccMetrics) {
		this.sccMetrics = sccMetrics;
	}
	public Map<String, PackageMetrics.Metrics> getPackageMetrics() {
		return packageMetrics;
	}
	public void setPackageMetrics(Map<String, PackageMetrics.Metrics> packageMetrics) {
		this.packageMetrics = packageMetrics;
	}
	

}
