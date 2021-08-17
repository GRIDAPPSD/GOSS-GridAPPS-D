package gov.pnnl.goss.gridappsd.testmanager;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gov.pnnl.goss.gridappsd.api.DataManager;
import gov.pnnl.goss.gridappsd.dto.RequestTimeseriesData;
import gov.pnnl.goss.gridappsd.dto.TestConfig;
import pnnl.goss.core.Client;

public class HistoricalComparison {
	
	private DataManager dataManager;
	private String username;
	private Client client;
	private Logger log = LoggerFactory.getLogger(getClass());
	
	public HistoricalComparison(DataManager dataManager, String username, Client client) {
		this.dataManager = dataManager;
		this.username = username;
		this.client = client;
	}
	
	public <T> T deepCopy(T object, Class<T> type) {
	    try {
	        Gson gson = new Gson();
	        return gson.fromJson(gson.toJson(object, type), type);
	    } catch (Exception e) {
	        e.printStackTrace();
	        return null;
	    }
	}
	
	/**
	 * Get expected data between start and end time
	 * @param expected_series
	 * @param start_time
	 * @param end_time
	 * @return
	 */
	public JsonObject getExpectedBetweenStartAndEnd(JsonObject expected_series, long start_time, long end_time){
		if(start_time == end_time) end_time++;
		JsonObject expected_output_series_window = deepCopy(expected_series, JsonObject.class);
		
		if (expected_series.has("output")){
			expected_output_series_window.remove("output");
			JsonObject outputs = new JsonObject();
			expected_output_series_window.add("output",outputs);
			JsonObject expected_input_series = expected_series.get("output").getAsJsonObject();
			for (Entry<String, JsonElement> time_entry : expected_input_series.entrySet()) {
				long t_time = Long.parseLong(time_entry.getKey() );
				System.out.println("getExpectedBetweenStartAndEnd " + start_time +  " " + end_time);
				System.out.println(t_time);
				System.out.println(t_time >= start_time && t_time < end_time);
				if(t_time >= start_time && t_time < end_time){
					outputs.add(time_entry.getKey(), time_entry.getValue());	
				}
			}
		}
		if (expected_series.has("input")){
			expected_output_series_window.remove("input");
			JsonObject inputs = new JsonObject();
			expected_output_series_window.add("input",inputs);
			JsonObject expected_input_series = expected_series.get("input").getAsJsonObject();
			for (Entry<String, JsonElement> time_entry : expected_input_series.entrySet()) {
				long t_time = Long.parseLong(time_entry.getKey() );
				System.out.println("getExpectedBetweenStartAndEnd " + start_time +  " " + end_time);
				System.out.println(t_time);
				System.out.println(t_time >= start_time && t_time < end_time);
				if(t_time >= start_time && t_time < end_time){
					inputs.add(time_entry.getKey(), time_entry.getValue());	
				}
			}
		}
		return expected_output_series_window;
	}

	/**
	 * Get proven timeseries data and compare with expected results
	 * @param simulationId
	 * @param testConfig
	 * @param expected_output_series
	 * @return
	 */
	public TestResultSeries compareSimulationWithExpected(String simulationId, TestConfig testConfig, JsonObject expected_output_series){
		TestResultSeries testResultSeries = new TestResultSeries();
		long start_time = testConfig.getStart_time();
		int duration = testConfig.getDuration();
		int interval = testConfig.getInterval();
		for (long window_Start_time = start_time; window_Start_time < start_time+duration; window_Start_time += interval) {
			long window_end_time= start_time+duration;
			if (window_Start_time+interval< window_end_time){window_end_time = window_Start_time + interval;}
			System.out.println("start time " + window_Start_time + "end time " + window_end_time);
		
			String responseStr = timeSeriesQuery(simulationId, "1532971828475", ""+window_Start_time, ""+window_end_time);
			if(! responseStr.contains("data")){
				System.out.println("No data for " + simulationId);
				log.warn("No timeseries data for simulation " + simulationId);
				return new TestResultSeries();
			}
			
			JsonObject expected_series_window = getExpectedBetweenStartAndEnd(expected_output_series, window_Start_time, window_end_time);
			
			TestResultSeries testResultSeries_temp = processExpectedAndTimeseries(expected_series_window, testConfig, responseStr, window_Start_time, window_end_time);
			for (Entry<Map<String,String>, TestResults> iterable_element : testResultSeries_temp.results.entrySet()) {
				Map<String,String> tm = iterable_element.getKey(); 
//				tm.forEach((key, value) -> testResultSeries.add(key, value, iterable_element.getValue() );
////				testResultSeries.add(key, value, iterable_element.getValue());
				for (Entry<String, String> pair : tm.entrySet()) {
				    System.out.println(String.format("Key (name) is: %s, Value (age) is : %s", pair.getKey(), pair.getValue()));
				    testResultSeries.add(pair.getKey(), pair.getValue(), iterable_element.getValue());
				}
			}
		}
		return testResultSeries;
	}
	
	/**
	 * Get timeseries data for two simulations and compare
	 * @param simulationIdOne
	 * @param simulationIdTwo
	 * @param testConfig
	 * @return
	 */
	public TestResultSeries compareTimeseriesSimulationWithTimeseriesSimulation(String simulationIdOne, String simulationIdTwo, TestConfig testConfig){
		TestResultSeries testResultSeries = new TestResultSeries();
		long start_time = testConfig.getStart_time();
		int duration = testConfig.getDuration();
		int interval = testConfig.getInterval();
		for (long window_Start_time = start_time; window_Start_time < start_time+duration; window_Start_time += interval) {
			long window_end_time= start_time+duration;
			if (window_Start_time+interval< window_end_time){window_end_time = window_Start_time + interval;}
			System.out.println("start time " + window_Start_time + "end time " + window_end_time);
			String responseStrOne = timeSeriesQuery(simulationIdOne, "1532971828475", ""+window_Start_time, ""+window_end_time);
			String responseStrTwo = timeSeriesQuery(simulationIdTwo, "1532971828475", ""+window_Start_time, ""+window_end_time);
			if(! responseStrOne.contains("data")){
				System.out.println("No data for " + simulationIdOne);
				return new TestResultSeries();
			}
			if(! responseStrTwo.contains("data")){
				System.out.println("No data for " + responseStrTwo);
				return new TestResultSeries();
			}
			
			TestResultSeries testResultSeries_temp = processTimeseriesAndTimeseries("123", testConfig, responseStrOne, responseStrTwo);
			for (Entry<Map<String,String>, TestResults> iterable_element : testResultSeries_temp.results.entrySet()) {
				Map<String,String> tm = iterable_element.getKey(); 
				for (Entry<String, String> pair : tm.entrySet()) {
				    System.out.println(String.format("Key (name) is: %s, Value (age) is : %s", pair.getKey(), pair.getValue()));
				    testResultSeries.add(pair.getKey(), pair.getValue(), iterable_element.getValue());
				}
			}
		}
		return testResultSeries;
	}
	
//	public TestResultSeries test_proven_one_at_a_time(String simulationId, JsonObject expected_output_series){
//		String responses = getListOfTime(simulationId, expected_output_series);
////		Set<String> times = getTimes(responses);
//		Set<String> times = getTimesEpoch(responses);
//		TestResultSeries testResultSeries = new TestResultSeries();
//		int index = 0;
//		for (String time : times) {
//			String responseStr = timeSeriesQuery(simulationId, null, time, time);	
////			responseStr = query(simulationId, null, time, time,  keywords).result.toString();
//			processATime(expected_output_series, simulationId, responseStr, testResultSeries, index+"");
//			index++;
//		}
//		
//		System.out.println("TestManager number of conflicts: "+ " total " + testResultSeries.getTotal());
//		return testResultSeries;
//	}

	/**
	 * Query timeseries db for simulation ID
	 * @param simulationId
	 * @param hasMrid
	 * @param startTime
	 * @param endTime
	 * @return
	 */
	public String timeSeriesQuery(String simulationId, String hasMrid,
			String startTime, String endTime) {

		// {"queryMeasurement":"simulation","queryFilter":{"simulation_id":"145774843"},"responseFormat":"JSON","queryType":"time-series","simulationYear":0}
		HashMap<String, Object> queryFilter = new HashMap<String, Object>();
		queryFilter.put("simulation_id", simulationId);
		queryFilter.put("startTime", startTime);
		queryFilter.put("endTime", endTime);
		System.out.println(queryFilter);
					
		RequestTimeseriesData request = new RequestTimeseriesData();
		request.setSimulationYear(0);
		request.setQueryMeasurement("simulation");
		request.setQueryFilter(queryFilter);

		Serializable response = null;

		try {
			response = dataManager.processDataRequest(request, "timeseries",
					simulationId, null,
					username);
		} catch (Exception e) {
			e.printStackTrace();
			// TODO: Log error - excpetion
		}
//		System.out.println(response.toString());
		return response.toString();
	}
	
	/**
	 * Query timeseries db for simulation ID
	 * @param simulationId
	 * @param hasMrid
	 * @param startTime
	 * @param endTime
	 * @return
	 */
	public String timeSeriesQueryInput(String simulationId, String hasMrid,
			String startTime, String endTime) {

		// {"queryMeasurement":"simulation","queryFilter":{"simulation_id":"145774843"},"responseFormat":"JSON","queryType":"time-series","simulationYear":0}
		HashMap<String, Object> queryFilter = new HashMap<String, Object>();
		queryFilter.put("simulation_id", simulationId);
		queryFilter.put("startTime", startTime);
		queryFilter.put("endTime", endTime);
		queryFilter.put("hasSimulationMessageType", "INPUT");
		// request	" {"queryMeasurement": "simulation",\n"queryFilter": {"startTime":1626293345, \n"endTime":1626293345,\n"simulation_id":"6220760",\n"hasSimulationMessageType": "INPUT"},\n"responseFormat": "JSON"}" (id=8778)	

		//"hasSimulationMessageType": "INPUT"
//		System.out.println(queryFilter);
					
		RequestTimeseriesData request = new RequestTimeseriesData();
		request.setSimulationYear(0);
		request.setQueryMeasurement("simulation");
		request.setQueryFilter(queryFilter);
		System.out.println(request.toString());

		Serializable response = null;

		try {
			response = dataManager.processDataRequest(request, "timeseries",
					simulationId, null,
					username);
		} catch (Exception e) {
			e.printStackTrace();
			// TODO: Log error - excpetion
		}
//		System.out.println(response.toString());
		return response.toString();
	}
	
//	public void buildMeasurementObject(JsonObject innerObject, JsonElement entry) {
//		String key = entry.getAsJsonObject().get("key").getAsString();
//		if(key.equals("hasValue")){
//			float value = entry.getAsJsonObject().get("value").getAsFloat();
//			innerObject.addProperty("value", value);
//		}
//		if(key.equals("hasAngle")){
//			float value = entry.getAsJsonObject().get("value").getAsFloat();
//			innerObject.addProperty("angle", value);
//		}
//		if(key.equals("hasMagnitude")){
//			float value = entry.getAsJsonObject().get("value").getAsFloat();
//			innerObject.addProperty("magnitude", value);
//		}
//		if(key.equals("hasMrid")){
//			String value = entry.getAsJsonObject().get("value").getAsString();
//			innerObject.addProperty("measurement_mrid", value);
//		}
//		if(key.equals("time")){
//			String value = entry.getAsJsonObject().get("value").getAsString();
//			innerObject.addProperty("timestamp", value);
//		}
//	}
	
//	public void processATime(JsonObject expected_output_series, String simulationId, String responseStr, TestResultSeries testResultSeries, String index) {
//		CompareResults compareResults = new CompareResults();
//		JsonObject jsonObject = CompareResults.getSimulationJson(responseStr);
//		JsonObject simOutputObject = new JsonObject();
//		JsonArray meas_array = new JsonArray();
//		String time = null;
//		JsonArray measurements = new JsonArray();
//		
//		JsonArray ma = jsonObject.get("measurements").getAsJsonArray();
//		for (JsonElement meas : ma) {
//			JsonArray points_array = meas.getAsJsonObject().get("points").getAsJsonArray();
//			for (JsonElement point : points_array) {
//				JsonArray entry_array = point.getAsJsonObject().get("row").getAsJsonObject().get("entry").getAsJsonArray();
////				System.out.println(" ");
//				JsonObject innerObject = new JsonObject();
//				meas_array.add(innerObject);
//				for (JsonElement entry : entry_array) {
//					buildMeasurementObject(innerObject, entry);
//				}
//				time = innerObject.get("timestamp").getAsString();
//				innerObject.remove("timestamp");
//				measurements.add(innerObject);
//			}
//		}
//
//		JsonObject outputObject = buildOutputObject(simulationId, simOutputObject, time, measurements);
//		System.out.println(simOutputObject.toString());
//		TestResults tr = compareResults.compareExpectedWithSimulationOutput(index+"", outputObject, expected_output_series);
//		if (tr != null) {
//			testResultSeries.add(index+"", tr);
//		}
//	}

	public JsonObject buildOutputObject(String simulationId, JsonObject simOutputObject, String time,
			JsonArray measurements) {
		simOutputObject.addProperty("timestamp", time);
		simOutputObject.add("measurements", measurements);
		JsonObject msgObject = new JsonObject();
		msgObject.addProperty("simulation_id", simulationId);
		msgObject.add("message", simOutputObject);

		JsonObject outputObject = new JsonObject();
		outputObject.add("output", msgObject);
		return outputObject;
	}

	/**
	 * 
	 * @param simulationId
	 * @param testConfig
	 * @param responseOne
	 * @param responseTwo
	 * @return
	 */
	public TestResultSeries processTimeseriesAndTimeseries(String simulationId,TestConfig testConfig, String responseOne, String responseTwo) {
		TestResultSeries testResultSeries = new TestResultSeries();
		CompareResults compareResults = new CompareResults(client, testConfig);

		JsonObject expectedObjectOne = getExpectedFrom(responseOne);
		JsonObject simOutputObjectOne = expectedObjectOne.get("output").getAsJsonObject();
		
		JsonObject expectedObjectTwo = getExpectedFrom(responseTwo);
		JsonObject simOutputObjectTwo = expectedObjectTwo.get("output").getAsJsonObject();
		if(testConfig.getTestOutput()){	
	//		int index = 0;	
			for (Entry<String, JsonElement> time_entry : simOutputObjectOne.entrySet()) {
	//			System.out.println(time_entry);
				TestResults tr = compareResults.compareExpectedWithSimulationOutput(time_entry.getKey(), time_entry.getValue().getAsJsonObject(), simOutputObjectTwo);
	//			tr.pprint();
				if (tr != null) {
					testResultSeries.add(time_entry.getKey(), time_entry.getKey(), tr);
				}
	//			index++;
			}
	//		System.out.println("Index: " + index + " TestManager number of conflicts: "+ " total " + testResultSeries.getTotal());
		}
		
		JsonObject simInputObject = expectedObjectOne.get("input").getAsJsonObject();
		JsonObject expected_input_series = expectedObjectTwo.get("input").getAsJsonObject();
		// TODO process missing inputs
		if(simInputObject.entrySet().isEmpty() && expected_input_series.entrySet().isEmpty()){
			System.out.println("Empty inputs");
			return testResultSeries;
		}else{
			HashSet <String> combinedTimes = new HashSet<String>();
			for (Entry<String, JsonElement> time_entry : simInputObject.entrySet()) {
				combinedTimes.add(time_entry.getKey());
				System.out.println("processWithAllTimes two sim"+time_entry.getKey());
			}
			for (Entry<String, JsonElement> time_entry : expected_input_series.entrySet()) {
				combinedTimes.add(time_entry.getKey());
				System.out.println("processWithAllTimes two sim"+time_entry.getKey());
			}
//			for (Entry<String, JsonElement> time_entry : simInputObject.entrySet()) {
			for (String time_entry : combinedTimes) {
				System.out.println(time_entry);
				TestResults tr = compareResults.compareExpectedWithSimulationInput(time_entry,
						time_entry, 
						simInputObject, 
						expected_input_series, 
						testConfig.getStart_time(), 
						testConfig.getStart_time() + testConfig.getDuration());
//				TestResults tr2 = compareResults.compareExpectedWithSimulationInput(time_entry, time_entry, simInputObject, expected_input_series, window_start_time, window_end_time);
				
				if (tr != null) {
					testResultSeries.add(time_entry, time_entry, tr);
				}
			}
		}
		
		//rebaseAndCompare(testResultSeries, compareResults, simInputObject, expected_input_series);
		return testResultSeries;
	}


	/**
	 * Get the expected data from response.
	 * Get the measurement array from the "data" key entry then build the expected results.
	 * @param responseOne
	 * @return
	 */
	public JsonObject getExpectedFrom(String responseOne) {
		JsonObject jsonObject = CompareResults.getSimulationJson(responseOne);
		if (! jsonObject.has("data")){
			return null;
		}
		String data = jsonObject.get("data").getAsString();
		System.out.println(data.substring(0, 100));
		JsonParser parser = new JsonParser();
		JsonArray measurements = (JsonArray) parser.parse(data);
		
		JsonObject expectedObject = buildExpectedFromTimeseries(measurements);
		return expectedObject;
	}
	
	/**
	 * Process expected results series with response string from timeseries query. 
	 * @param expected_series
	 * @param testConfig
	 * @param response
	 * @param window_start_time 
	 * @param window_end_time
	 * @return
	 */
	public TestResultSeries processExpectedAndTimeseries(JsonObject expected_series, TestConfig testConfig, String response, long window_start_time, long window_end_time) {
		TestResultSeries testResultSeries = new TestResultSeries();
		CompareResults compareResults = new CompareResults(client, testConfig);
//		System.out.println(response);

		JsonObject expectedObject = getExpectedFrom(response);
		if (expectedObject.has("output") && expected_series.has("output") ){
			JsonObject simOutputObject = expectedObject.get("output").getAsJsonObject();
			JsonObject expected_output_series = expected_series.get("output").getAsJsonObject();
			
	//		int index = 0;
			for (Entry<String, JsonElement> time_entry : simOutputObject.entrySet()) {
	//			System.out.println(time_entry);
				TestResults tr = compareResults.compareExpectedWithSimulationOutput(time_entry.getKey(), time_entry.getValue().getAsJsonObject(), expected_output_series);
				if (tr != null) {
					testResultSeries.add(time_entry.getKey(), time_entry.getKey(), tr);
				}
	//			index++;
			}
	//		System.out.println("Index: " + index + " TestManager number of conflicts: "+ " total " + testResultSeries.getTotal());
		} 
		if (expectedObject.has("input") && expected_series.has("input") ){
			JsonObject simInputObject = expectedObject.get("input").getAsJsonObject();
			JsonObject expected_input_series = expected_series.get("input").getAsJsonObject();
			HashSet <String> combinedTimes = new HashSet<String>();
			for (Entry<String, JsonElement> time_entry : simInputObject.entrySet()) {
				combinedTimes.add(time_entry.getKey());
			}
			for (Entry<String, JsonElement> time_entry : expected_input_series.entrySet()) {
				combinedTimes.add(time_entry.getKey());
			}
			
//			for (Entry<String, JsonElement> time_entry : simInputObject.entrySet()) {
			for (String time_entry : combinedTimes) {
				System.out.println(time_entry);
				TestResults tr = compareResults.compareExpectedWithSimulationInput(time_entry, time_entry, simInputObject, expected_input_series, window_start_time, window_end_time);
				if (tr != null) {
					testResultSeries.add(time_entry, time_entry, tr);
				}
			}
			
//			rebaseAndCompare(testResultSeries, compareResults, simInputObject, expected_input_series);
		}
		return testResultSeries;
	}
	
/**
 * Build a structure that resembles the expect results from the simulation measurements
 * Example:
 * {"output":{"1248156002":{"message":{"measurements":[{"measurement_mrid":"_0055de94-7d7e-4931-a884-cab596cc191b","angle":-2.066423674487563,"magnitude":2361.0733024639117},{"measurement_mrid":"_fff9a11e-d5d1-4824-a457-13d944ffcfdf","angle":-122.80107769837849,"magnitude":2520.2169329056983},{"measurement_mrid":"_0058123f-da11-4f7c-a429-e47e5949465f","angle":-122.70461031091335,"magnitude":2522.818525429715}]}}},"input":{"1587670650":{"message":{"measurements":[{"hasMeasurementDifference":"FORWARD","difference_mrid":"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4","attribute":"ShuntCompensator.sections","value":0.0,"object":"_307E4291-5FEA-4388-B2E0-2B3D22FE8183"},{"hasMeasurementDifference":"REVERSE","difference_mrid":"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4","attribute":"ShuntCompensator.sections","value":1.0,"object":"_307E4291-5FEA-4388-B2E0-2B3D22FE8183"}]}},"1587670665":{"message":{"measurements":[{"hasMeasurementDifference":"FORWARD","difference_mrid":"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4","attribute":"ShuntCompensator.sections","value":1.0,"object":"_307E4291-5FEA-4388-B2E0-2B3D22FE8183"},{"hasMeasurementDifference":"REVERSE","difference_mrid":"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4","attribute":"ShuntCompensator.sections","value":0.0,"object":"_307E4291-5FEA-4388-B2E0-2B3D22FE8183"}]}}}}
 * @param measurements
 * @return
 */
	public JsonObject buildExpectedFromTimeseries(JsonArray measurements) {
//		JsonArray meas_array = new JsonArray();
		JsonObject simExpected = new JsonObject();
		JsonObject simOutputObject = new JsonObject();
		JsonObject simInputObject = new JsonObject();

		for (JsonElement measurement : measurements) {
			String time = measurement.getAsJsonObject().get("time").getAsString();
			
			if (measurement.getAsJsonObject().get("hasSimulationMessageType").getAsString().equals("OUTPUT") ){
				if (! simOutputObject.has(time)){
					JsonObject measurementsObject = new JsonObject();
					JsonObject messageObject = new JsonObject();
					measurementsObject.add("measurements", new JsonArray());
					messageObject.add("message", measurementsObject);
					simOutputObject.add(time, messageObject);
				} 
				measurement.getAsJsonObject().remove("hasSimulationMessageType");
				measurement.getAsJsonObject().remove("simulation_id");
				measurement.getAsJsonObject().remove("time");
				simOutputObject.get(time).getAsJsonObject().get("message").getAsJsonObject().get("measurements").getAsJsonArray().add(measurement);
			} else { // INPUT
//				System.out.println("input measurement");
//				System.out.println(measurement.toString());
				if (! simInputObject.has(time)){
					JsonObject measurementsObject = new JsonObject();
					JsonObject messageObject = new JsonObject();
					measurementsObject.add("measurements", new JsonArray());
					messageObject.add("message", measurementsObject);
					simInputObject.add(time, messageObject);
				} 
				measurement.getAsJsonObject().remove("hasSimulationMessageType");
				measurement.getAsJsonObject().remove("simulation_id");
				measurement.getAsJsonObject().remove("time");
			    simInputObject.get(time).getAsJsonObject().get("message").getAsJsonObject().get("measurements").getAsJsonArray().add(measurement);
			}
			
//			System.out.println(measurement.getAsJsonObject().get("time"));
			// Remove unneeded proven metadata
//			measurement.getAsJsonObject().remove("hasSimulationMessageType");
//			measurement.getAsJsonObject().remove("simulation_id");
//			measurement.getAsJsonObject().remove("time");
//			meas_array.add(measurement);
		}
		simExpected.add("output", simOutputObject);
		simExpected.add("input", simInputObject);
		return simExpected;
	}
	
}
