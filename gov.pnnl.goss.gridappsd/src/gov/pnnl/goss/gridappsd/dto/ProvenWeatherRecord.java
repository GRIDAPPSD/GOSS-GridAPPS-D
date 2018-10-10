package gov.pnnl.goss.gridappsd.dto;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class ProvenWeatherRecord {

	Long dateTime;   //epochtime
	double ambientTemperature;   //(in C)
	double bearing;   //wind direction, degrees from N
	double humidity;   //(tower RH is humidity in jeff's)
	double speed;  //wind speed (in m/s)
	double irradanceDirectNormal;  //(in watts/m^2)
	double irradanceDiffuseHorizontal;  //(in watts/m^2)
	double irradanceGlobalHorizontal;  //(in watts/m^2)
	
	
	//May not be aprt of this record
	String location;   ///weather station id
	String sourceDescription; 
	String state;
	String city;
	boolean isForecast;  //, (T/F)
	
	public Long getDateTime() {
		return dateTime;
	}
	public void setDateTime(Long dateTime) {
		this.dateTime = dateTime;
	}
	
	public double getAmbientTemperature() {
		return ambientTemperature;
	}
	public void setAmbientTemperature(double ambientTemperature) {
		this.ambientTemperature = ambientTemperature;
	}
	public double getBearing() {
		return bearing;
	}
	public void setBearing(double bearing) {
		this.bearing = bearing;
	}
	public double getHumidity() {
		return humidity;
	}
	public void setHumidity(double humidity) {
		this.humidity = humidity;
	}
	public double getSpeed() {
		return speed;
	}
	public void setSpeed(double speed) {
		this.speed = speed;
	}
	public double getIrradanceDirectNormal() {
		return irradanceDirectNormal;
	}
	public void setIrradanceDirectNormal(double irradanceDirectNormal) {
		this.irradanceDirectNormal = irradanceDirectNormal;
	}
	public double getIrradanceDiffuseHorizontal() {
		return irradanceDiffuseHorizontal;
	}
	public void setIrradanceDiffuseHorizontal(double irradanceDiffuseHorizontal) {
		this.irradanceDiffuseHorizontal = irradanceDiffuseHorizontal;
	}
	public double getIrradanceGlobalHorizontal() {
		return irradanceGlobalHorizontal;
	}
	public void setIrradanceGlobalHorizontal(double irradanceGlobalHorizontal) {
		this.irradanceGlobalHorizontal = irradanceGlobalHorizontal;
	}
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}
	public String getSourceDescription() {
		return sourceDescription;
	}
	public void setSourceDescription(String sourceDescription) {
		this.sourceDescription = sourceDescription;
	}
	public String getState() {
		return state;
	}
	public void setState(String state) {
		this.state = state;
	}
	public String getCity() {
		return city;
	}
	public void setCity(String city) {
		this.city = city;
	}
	public boolean isForecast() {
		return isForecast;
	}
	public void setForecast(boolean isForecast) {
		this.isForecast = isForecast;
	}
	
	
	@Override
	public String toString() {
		Gson  gson = new Gson();
		return gson.toJson(this);
	}
	
	public static ProvenWeatherRecord parse(String jsonString){
		Gson  gson = new Gson();
		ProvenWeatherRecord obj = gson.fromJson(jsonString, ProvenWeatherRecord.class);
		if(obj.dateTime==null)
			throw new JsonSyntaxException("Expected attribute time not found");
		return obj;
	}
	
	
}
