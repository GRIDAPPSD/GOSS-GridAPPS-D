package gov.pnnl.goss.gridappsd.data.conversion;

import java.io.InputStream;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;

import gov.pnnl.goss.gridappsd.api.DataManager;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.dto.LogMessage;
import gov.pnnl.goss.gridappsd.dto.TimeSeriesResult;
import gov.pnnl.goss.gridappsd.dto.TimeSeriesResult.MeasurementResult;
import gov.pnnl.goss.gridappsd.dto.TimeSeriesResult.RowResult;
import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;

@Component
public class ProvenWeatherToGridlabdWeatherConverter implements DataFormatConverter {
	protected static SimpleDateFormat sdfIn = new SimpleDateFormat("MM/dd/yyyy HH:mm");
	protected static SimpleDateFormat sdfOut = new SimpleDateFormat("MM:dd:HH:mm:ss");
	
	public static String INPUT_FORMAT = "PROVEN_WEATHER";
	public static String OUTPUT_FORMAT = "GRIDLABD_WEATHER";
	
	
	public static String SOLAR_DIFFUSE = "Diffuse";
	public static String AVG_WIND_SPEED = "AvgWindSpeed";
	public static String AVG_WIND_DIRECTION = "AvgWindDirection";
	public static String HUMIDITY = "TowerRH";
	public static String LONGITUDE = "long";
	public static String LATITUDE = "lat";
	public static String MST = "MST";
	public static String TEMPERATURE = "TowerDryBulbTemp";
	public static String DATE = "DATE";
	public static String TIME = "time";
	public static String SOLAR_DIRECT = "DirectCH1";
	public static String SOLAR_GLOBAL = "GlobalCM22";
	public static String PLACE = "place";
	
	@ServiceDependency
	private volatile DataManager dataManager;
	@ServiceDependency 
	private volatile LogManager logManager;
	
	static{
	    sdfIn.setTimeZone(TimeZone.getTimeZone("MST"));
		sdfOut.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	
	public ProvenWeatherToGridlabdWeatherConverter(){}
	public ProvenWeatherToGridlabdWeatherConverter(LogManager logManager, DataManager dataManager) {
		this.logManager = logManager;
		this.dataManager = dataManager;
	}
	
	@Start
	public void start(){
		if(dataManager!=null) {
			dataManager.registerConverter(INPUT_FORMAT, OUTPUT_FORMAT, this);
		}
		else { 
			//TODO send log message and exception
			if(logManager!=null){
				//log.warn("No Data manager available for "+getClass());
				logManager.log(
						new LogMessage(this.getClass().getName(), new Integer(
								0).toString(), new Date().getTime(),
								"No Data manager available for "+getClass(), LogLevel.WARN,
								ProcessStatus.RUNNING, false), "system",
						GridAppsDConstants.topic_platformLog);
			}
		}
	}
	
	
	@Override
	public void convert(String inputContent, PrintWriter outputContent) throws Exception {
		boolean headerPrinted = false;
		
		TimeSeriesResult resultObj = TimeSeriesResult.parse(inputContent);
		for(MeasurementResult record: resultObj.getMeasurements()){
			if(!headerPrinted){
				printGLDHeader(record, outputContent);
				headerPrinted = true;
			}
			convertRecord(record, outputContent);
		}
	}

	@Override
	public void convert(InputStream inputContent, PrintWriter outputContent)  throws Exception {
		boolean headerPrinted = false;
		
		String strContent = IOUtils.toString(inputContent);
		TimeSeriesResult resultObj = TimeSeriesResult.parse(strContent);
		for(MeasurementResult record: resultObj.getMeasurements()){
			if(!headerPrinted){
				printGLDHeader(record, outputContent);
				headerPrinted = true;
			}
			convertRecord(record, outputContent);
		}

	}

	protected void printGLDHeader(MeasurementResult record, PrintWriter outputContent){
		//TODO this needs to come from data or tags within proven
		String placeStr="", yearStr="", latlong = "";
		
		try{
		HashMap<String, String> map = record.getPoints().get(0).getRow().getEntryMap();
		placeStr = map.get(PLACE);
		placeStr = placeStr.replaceAll("\"", "");
		latlong = map.get(LATITUDE)+","+map.get(LONGITUDE);
		
		String dateStr = map.get(DATE);
		String[] dateArr = StringUtils.split(dateStr,"/");
		yearStr = dateArr[2];
		}catch (Exception e) {
			//TODO log warning
		}
		
		outputContent.println("#"+placeStr+" ("+latlong+") file for "+yearStr);
		outputContent.println("# data obtained from ...");
		outputContent.println("$state_name=N/A");
		outputContent.println("$city_name=N/A");
		
		outputContent.println("time,temperature,humidity,wind_speed,solar_dir,solar_diff,solar_global");
	}
	
	protected void convertRecord(MeasurementResult record, PrintWriter outputContent){
		//See https://github.com/gridlab-d/gridlab-d/blob/master/climate/climate.cpp for gridlabd format requirements
		for(RowResult result: record.getPoints()){
			Map<String, String> map = result.getRow().getEntryMap();
			
			String dateStr = map.get(DATE);
			String timeStr = map.get(MST);
			try {
				Date datetime = sdfIn.parse(dateStr+" "+timeStr);
				outputContent.print(sdfOut.format(datetime)+",");
			} catch (ParseException e) {
				e.printStackTrace();
				//todo throw exception
			}
			
			//print temperature in Fahrenheit and convert from celcius to Fahrenheit
			double temp_cel = readDouble(map, TEMPERATURE, -100000000);
			double temp_fahr = (temp_cel * 1.8) + 32;
			outputContent.print(temp_fahr+",");
			//print humidity 
			outputContent.print(map.get(HUMIDITY)+",");
			//print wind_speed and no conversion necessary
			double speed_m = readDouble(map, AVG_WIND_SPEED, 0);
			outputContent.print(speed_m+",");
			//print solar_direct and convert from watts/m^2 to watts/f^s
			double solar_direct_m = readDouble(map,SOLAR_DIRECT, 0);
			double solar_direct_f = solar_direct_m*(1/10.764);
			outputContent.print(solar_direct_f+",");
			//print solar_diffuse convert from watts/m^2 to watts/f^s
//			double solar_diffuse_m = record.getIrradanceDiffuseHorizontal();
			double solar_diffuse_m = readDouble(map, SOLAR_DIFFUSE, 0);
			double solar_diffuse_f = solar_diffuse_m*(1/10.764);
			outputContent.print(solar_diffuse_f+",");
			//print solar_global convert from watts/m^2 to watts/f^s
//			double solar_global_m = record.getIrradanceGlobalHorizontal();
			double solar_global_m = readDouble(map, SOLAR_GLOBAL, 0);
			double solar_global_f = solar_global_m*(1/10.764);
			outputContent.print(solar_global_f+"");
			outputContent.println();
			outputContent.flush();
		}
	}
	
	protected double readDouble(Map<String, String> map, String key, double minimumValue){
		double result = 0;
		if(map.containsKey(key)){
			String strVal = map.get(key);
			try {
				double res = new Double(strVal);
				if(res<minimumValue){
					return minimumValue;
				}
				else {
					return res;
				}
			} catch (Exception e) {
				System.out.println("Could not convert "+key+": "+strVal);
			}
		}
		
		return result;
	}
	
	public static void main(String[] args) {
//		01:01:00:01:00,  33.1,  0.31,  10.4,  0,  0,  0
			try {
//			ProvenWeatherRecord record = new ProvenWeatherRecord();
//			record.setDateTime(sdfIn.parse("2009:01:01:00:01:00").getTime());
//			record.setAmbientTemperature(.6112);
//			record.setHumidity(0.31);
//			record.setSpeed(10.4);
//			record.setIrradanceGlobalHorizontal(0);
//			record.setIrradanceDiffuseHorizontal(0);
//			record.setIrradanceDirectNormal(0);
//			
//			String provenInput = record.toString();
				
			String provenInput = "{\"measurements\":[{\"name\":\"weather\",\"points\":[{\"row\":{\"entry\":[{\"key\":"
					+ "\"Diffuse\",\"value\":\"-0.006386875\"},{\"key\":\"AvgWindSpeed\",\"value\":\"0.0\"},{\"key\":"
					+ "\"TowerRH\",\"value\":\"86.8\"},{\"key\":\"long\",\"value\":\"\\\"105.18 W\\\"\"},{\"key\":"
					+ "\"MST\",\"value\":\"00:00\"},{\"key\":\"TowerDryBulbTemp\",\"value\":\"13.316\"},{\"key\":"
					+ "\"DATE\",\"value\":\"1/1/2013\"},{\"key\":\"DirectCH1\",\"value\":\"-0.0402521765\"},{\"key\":"
					+ "\"GlobalCM22\",\"value\":\"-0.037676152399999996\"},{\"key\":\"AvgWindDirection\",\"value\":"
					+ "\"0.0\"},{\"key\":\"time\",\"value\":\"1970-01-16T16:57:28.8Z\"},{\"key\":\"place\",\"value\":"
					+ "\"\\\"Solar Radiation Research Laboratory\\\"\"},{\"key\":\"lat\",\"value\":\"\\\"39.74 N\\\"\""
					+ "}]}},{\"row\":{\"entry\":[{\"key\":\"Diffuse\",\"value\":\"-0.005538233499999999\"},{\"key\":"
					+ "\"AvgWindSpeed\",\"value\":\"0.0\"},{\"key\":\"TowerRH\",\"value\":\"86.9\"},{\"key\":\"long\","
					+ "\"value\":\"\\\"105.18 W\\\"\"},{\"key\":\"MST\",\"value\":\"00:01\"},{\"key\":\"TowerDryBulbTemp\","
					+ "\"value\":\"13.406\"},{\"key\":\"DATE\",\"value\":\"1/1/2013\"},{\"key\":\"DirectCH1\",\"value\":"
					+ "\"-0.0395396335\"},{\"key\":\"GlobalCM22\",\"value\":\"-0.0369521827\"},{\"key\":\"AvgWindDirection\","
					+ "\"value\":\"0.0\"},{\"key\":\"time\",\"value\":\"1970-01-16T16:57:28.86Z\"},{\"key\":\"place\","
					+ "\"value\":\"\\\"Solar Radiation Research Laboratory\\\"\"},{\"key\":\"lat\",\"value\":\"\\\"39.74 N\\\"\"}]}}]}]}";
			
			new ProvenWeatherToGridlabdWeatherConverter().convert(provenInput, new PrintWriter(System.out));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
}
