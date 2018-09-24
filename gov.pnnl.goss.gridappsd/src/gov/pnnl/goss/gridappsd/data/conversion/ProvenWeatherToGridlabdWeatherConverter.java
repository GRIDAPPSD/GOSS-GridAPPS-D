package gov.pnnl.goss.gridappsd.data.conversion;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;

import gov.pnnl.goss.gridappsd.api.DataManager;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.dto.LogMessage;
import gov.pnnl.goss.gridappsd.dto.ProvenWeatherRecord;
import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;

@Component
public class ProvenWeatherToGridlabdWeatherConverter implements DataFormatConverter {
	protected static SimpleDateFormat sdfIn = new SimpleDateFormat("yyyy:MM:dd:HH:mm:ss");
	protected static SimpleDateFormat sdfOut = new SimpleDateFormat("MM:dd:HH:mm:ss");
	
	public static String INPUT_FORMAT = "PROVEN_WEATHER";
	public static String OUTPUT_FORMAT = "GRIDLAB-D_WEATHER";
	
	
	@ServiceDependency
	private volatile DataManager dataManager;
	@ServiceDependency 
	private volatile LogManager logManager;
	
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
		printGLDHeader(outputContent);
		BufferedReader reader = new BufferedReader(new StringReader(inputContent));
		String nextLine = reader.readLine();
		while(reader.ready() && nextLine!=null){
			convertLine(nextLine, outputContent);
			nextLine = reader.readLine();
		}
	}

	@Override
	public void convert(InputStream inputContent, PrintWriter outputContent)  throws Exception {
		printGLDHeader(outputContent);
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputContent));
		while(reader.ready()){
			String nextLine = reader.readLine();
			convertLine(nextLine, outputContent);
		}
	}

	protected void printGLDHeader(PrintWriter outputContent){
		//TODO this needs to come from data or tags within proven
		outputContent.println("#Columbus	OH weather file for 2009");
		outputContent.println("# data obtained from http://www.wunderground.com/history/airport/KCMH");
		outputContent.println("$state_name=Ohio");
		outputContent.println("$city_name=Columbus");
		
		outputContent.println("time,temperature,humidity,wind_speed,solar_dir,solar_diff,solar_global");
	}
	protected void convertLine(String line, PrintWriter outputContent){
		if(line==null || line.trim().length()==0)
			return;
		
		//See https://github.com/gridlab-d/gridlab-d/blob/master/climate/climate.cpp for format requirements
		ProvenWeatherRecord record = ProvenWeatherRecord.parse(line);

		//print time
		outputContent.print(sdfOut.format(new Date(record.getDateTime()))+",");
		//print temperature in Fahrenheit and convert from celcius to Fahrenheit
		double temp_cel = record.getAmbientTemperature();
		double temp_fahr = (temp_cel * 1.8) + 32;
		outputContent.print(temp_fahr+",");
		//print humidity 
		outputContent.print(record.getHumidity()+",");
		//print wind_speed and no conversion necessary
		double speed_m = record.getSpeed();
		outputContent.print(speed_m+",");
		//print solar_direct and convert from watts/m^2 to watts/f^s
		double solar_direct_m = record.getIrradanceDirectNormal();
		double solar_direct_f = solar_direct_m*(1/10.764);
		outputContent.print(solar_direct_f+",");
		//print solar_diffuse convert from watts/m^2 to watts/f^s
		double solar_diffuse_m = record.getIrradanceDiffuseHorizontal();
		double solar_diffuse_f = solar_diffuse_m*(1/10.764);
		outputContent.print(solar_diffuse_f+",");
		//print solar_global convert from watts/m^2 to watts/f^s
		double solar_global_m = record.getIrradanceGlobalHorizontal();
		double solar_global_f = solar_global_m*(1/10.764);
		outputContent.print(solar_global_f+"");
		outputContent.println();
		outputContent.flush();
	}
	
	
	public static void main(String[] args) {
//		01:01:00:01:00,  33.1,  0.31,  10.4,  0,  0,  0
			try {
			ProvenWeatherRecord record = new ProvenWeatherRecord();
			record.setDateTime(sdfIn.parse("2009:01:01:00:01:00").getTime());
			record.setAmbientTemperature(.6112);
			record.setHumidity(0.31);
			record.setSpeed(10.4);
			record.setIrradanceGlobalHorizontal(0);
			record.setIrradanceDiffuseHorizontal(0);
			record.setIrradanceDirectNormal(0);
			
			String provenInput = record.toString();
			
			new ProvenWeatherToGridlabdWeatherConverter().convert(provenInput, new PrintWriter(System.out));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
}
