package pnnl.goss.gridappsd.dto;

public class ModelCreationConfig {

	public double load_scaling_factor = .2;  ///TODO when fixed default to 1   maps to -l
	public char triplex = 'y'; //allowed values y|n to include secondary     maps to -t
	public char encoding = 'u';  //allowed values u|i for UTF-8 or ISO-8859-1    maps to -e
	public int system_frequency = 60;   // maps to -f
	public double voltage_multiplier = 1;  //multiplier that converts voltage to V for GridLAB-D  maps to -v
	public double power_unit_conversion = 1;  //allowed values {1000|1|0.001}, multiplier that converts p,q,s to VA for GridLAB-D  maps to -s
	public char unique_names = 'y';  //allowed values y|n   are unique names used?  maps to -q
	public String schedule_name = ""; // root filename for scheduled ZIP loads (defaults to none)    maps to -n
	public double z_fraction = 0;  // allowed values {0....1}  constant Z portion (defaults to 0 for CIM-defined,  maps to -z
	public double i_fraction = 0;  // allowed values {0....1}  constant I portion (defaults to 0 for CIM-defined,  maps to -i
	public double p_fraction = 0;  // allowed values {0....1}  constant P portion (defaults to 0 for CIM-defined,  maps to -p
	
	
	
	
	
	public double getLoadScalingFactor() {
		return load_scaling_factor;
	}
	public void setLoadScalingFactor(double loadScalingFactor) {
		this.load_scaling_factor = loadScalingFactor;
	}
	public char getTriplex() {
		return triplex;
	}
	public void setTriplex(char triplex) {
		this.triplex = triplex;
	}
	public char getEncoding() {
		return encoding;
	}
	public void setEncoding(char encoding) {
		this.encoding = encoding;
	}
	public int getSystemFrequency() {
		return system_frequency;
	}
	public void setSystemFrequency(int systemFrequency) {
		this.system_frequency = systemFrequency;
	}
	public double getVoltageMultiplier() {
		return voltage_multiplier;
	}
	public void setVoltageMultiplier(double voltageMultiplier) {
		this.voltage_multiplier = voltageMultiplier;
	}
	public double getPowerUnitConversion() {
		return power_unit_conversion;
	}
	public void setPowerUnitConversion(double powerUnitConversion) {
		this.power_unit_conversion = powerUnitConversion;
	}
	public char getUniqueNames() {
		return unique_names;
	}
	public void setUniqueNames(char uniqueNames) {
		this.unique_names = uniqueNames;
	}
	public String getScheduleName() {
		return schedule_name;
	}
	public void setScheduleName(String scheduleName) {
		this.schedule_name = scheduleName;
	}
	public double getzFraction() {
		return z_fraction;
	}
	public void setzFraction(double zFraction) {
		this.z_fraction = zFraction;
	}
	public double getiFraction() {
		return i_fraction;
	}
	public void setiFraction(double iFraction) {
		this.i_fraction = iFraction;
	}
	public double getpFraction() {
		return p_fraction;
	}
	public void setpFraction(double pFraction) {
		this.p_fraction = pFraction;
	}

	
	
	
	
	
	
}
