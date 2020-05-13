package gov.pnnl.goss.gridappsd.testmanager;

import gov.pnnl.goss.gridappsd.api.DataManager;
import gov.pnnl.goss.gridappsd.dto.RequestTimeseriesData;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.TimeZone;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class HistoricalComparison {
	
	DataManager dataManager;
	String username;
	private Logger log = LoggerFactory.getLogger(getClass());
	
	public HistoricalComparison(DataManager dataManager, String username) {
		this.dataManager = dataManager;
		this.username = username;
	}

	public TestResultSeries test_proven(String simulationId, JsonObject expected_output_series){

		String responseStr = timeSeriesQuery(simulationId, "1532971828475", null, null);
		if(! responseStr.contains("data")){
			System.out.println("No data for " + simulationId);
			log.warn("No timeseries data for simulation " + simulationId);
			return new TestResultSeries();
		}
		TestResultSeries testResultSeries = processWithAllTimes(expected_output_series, simulationId, responseStr);
		return testResultSeries;
	}
	
	public TestResultSeries test_proven(String simulationIdOne, String simulationIdTwo){

		String responseStrOne = timeSeriesQuery(simulationIdOne, "1532971828475", null, null);
		String responseStrTwo = timeSeriesQuery(simulationIdTwo, "1532971828475", null, null);
		if(! responseStrOne.contains("data")){
			System.out.println("No data for " + simulationIdOne);
			return new TestResultSeries();
		}
		if(! responseStrTwo.contains("data")){
			System.out.println("No data for " + responseStrTwo);
			return new TestResultSeries();
		}
		TestResultSeries testResultSeries = processWithAllTimes("123", responseStrOne, responseStrTwo);
		return testResultSeries;
	}
	
	public TestResultSeries test_proven_one_at_a_time(String simulationId, JsonObject expected_output_series){
		String responses = getListOfTime(simulationId, expected_output_series);
//		Set<String> times = getTimes(responses);
		Set<String> times = getTimesEpoch(responses);
		TestResultSeries testResultSeries = new TestResultSeries();
		int index = 0;
		for (String time : times) {
			String responseStr = timeSeriesQuery(simulationId, null, time, time);	
//			responseStr = query(simulationId, null, time, time,  keywords).result.toString();
			processATime(expected_output_series, simulationId, responseStr, testResultSeries, index+"");
			index++;
		}
		
		System.out.println("TestManager number of conflicts: "+ " total " + testResultSeries.getTotal());
		return testResultSeries;
	}

	
	public String timeSeriesQuery(String simulationId, String hasMrid,
			String startTime, String endTime) {

		// {"queryMeasurement":"simulation","queryFilter":{"simulation_id":"145774843"},"responseFormat":"JSON","queryType":"time-series","simulationYear":0}
		HashMap<String, Object> queryFilter = new HashMap<String, Object>();
		queryFilter.put("hasSimulationId", simulationId);
		queryFilter.put("startTime", startTime);
		queryFilter.put("endTime", endTime);
		
		 queryFilter = new HashMap<String, Object>();
		 queryFilter.put("simulation_id", simulationId);
			
		RequestTimeseriesData request = new RequestTimeseriesData();
		request.setSimulationYear(0);
		request.setQueryMeasurement("simulation");
		request.setQueryFilter(queryFilter);

		Serializable response = null;

		try {
			response = dataManager.processDataRequest(request, "timeseries",
					Integer.parseInt(simulationId), null,
					username);
		} catch (Exception e) {
			// TODO: Log error - excpetion
		}
//		System.out.println(response.toString());
		return response.toString();
	}
	
	public void buildMeasurementObject(JsonObject innerObject, JsonElement entry) {
		String key = entry.getAsJsonObject().get("key").getAsString();
		if(key.equals("hasValue")){
			float value = entry.getAsJsonObject().get("value").getAsFloat();
			innerObject.addProperty("value", value);
		}
		if(key.equals("hasAngle")){
			float value = entry.getAsJsonObject().get("value").getAsFloat();
			innerObject.addProperty("angle", value);
		}
		if(key.equals("hasMagnitude")){
			float value = entry.getAsJsonObject().get("value").getAsFloat();
			innerObject.addProperty("magnitude", value);
		}
		if(key.equals("hasMrid")){
			String value = entry.getAsJsonObject().get("value").getAsString();
			innerObject.addProperty("measurement_mrid", value);
		}
		if(key.equals("time")){
			String value = entry.getAsJsonObject().get("value").getAsString();
			innerObject.addProperty("timestamp", value);
		}
	}
	
	public void processATime(JsonObject expected_output_series, String simulationId, String responseStr, TestResultSeries testResultSeries, String index) {
		CompareResults compareResults = new CompareResults();
		JsonObject jsonObject = CompareResults.getSimulationJson(responseStr);
		JsonObject simOutputObject = new JsonObject();
		JsonArray meas_array = new JsonArray();
		String time = null;
		JsonArray measurements = new JsonArray();
		
		JsonArray ma = jsonObject.get("measurements").getAsJsonArray();
		for (JsonElement meas : ma) {
			JsonArray points_array = meas.getAsJsonObject().get("points").getAsJsonArray();
			for (JsonElement point : points_array) {
				JsonArray entry_array = point.getAsJsonObject().get("row").getAsJsonObject().get("entry").getAsJsonArray();
//				System.out.println(" ");
				JsonObject innerObject = new JsonObject();
				meas_array.add(innerObject);
				for (JsonElement entry : entry_array) {
					buildMeasurementObject(innerObject, entry);
				}
				time = innerObject.get("timestamp").getAsString();
				innerObject.remove("timestamp");
				measurements.add(innerObject);
			}
		}

		JsonObject outputObject = buildOutputObject(simulationId, simOutputObject, time, measurements);
		System.out.println(simOutputObject.toString());
		TestResults tr = compareResults.compareExpectedWithSimulationOutput(index+"", outputObject, expected_output_series);
		if (tr != null) {
			testResultSeries.add(index+"", tr);
		}
	}



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

	public TestResultSeries processWithAllTimes(String simulationId, String responseOne, String responseTwo) {
		TestResultSeries testResultSeries = new TestResultSeries();
		CompareResults compareResults = new CompareResults();

		JsonObject expectedObjectOne = getExpectedFrom(responseOne);
		JsonObject simOutputObjectOne = expectedObjectOne.get("output").getAsJsonObject();
		
		JsonObject expectedObjectTwo = getExpectedFrom(responseTwo);
		JsonObject simOutputObjectTwo = expectedObjectTwo.get("output").getAsJsonObject();

		int index = 0;
		for (Entry<String, JsonElement> time_entry : simOutputObjectOne.entrySet()) {
//			System.out.println(time_entry);
			TestResults tr = compareResults.compareExpectedWithSimulationOutput(time_entry.getKey(), time_entry.getValue().getAsJsonObject(), simOutputObjectTwo);
			tr.pprint();
			if (tr != null) {
				testResultSeries.add(time_entry.getKey(), tr);
			}
			index++;
		}
		System.out.println("Index: " + index + " TestManager number of conflicts: "+ " total " + testResultSeries.getTotal());
		
		
		JsonObject simInputObjectOne = expectedObjectOne.get("input").getAsJsonObject();
		JsonObject simInputObjectTwo = expectedObjectTwo.get("input").getAsJsonObject();
		index = 0;
		for (Entry<String, JsonElement> time_entry : simInputObjectOne.entrySet()) {
			System.out.println(time_entry);
			TestResults tr = compareResults.compareExpectedWithSimulationInput(time_entry.getKey(), time_entry.getValue().getAsJsonObject(), simInputObjectTwo);
			if (tr != null) {
				testResultSeries.add(time_entry.getKey(), tr);
			}
			index++;
		}
		System.out.println("Index: " + index + " TestManager number of conflicts: "+ " total " + testResultSeries.getTotal());
		return testResultSeries;
	}

	public JsonObject getExpectedFrom(String responseOne) {
		JsonObject jsonObject = CompareResults.getSimulationJson(responseOne);
		String data = jsonObject.get("data").getAsString();
		System.out.println(data.substring(0, 100));
		JsonParser parser = new JsonParser();
		JsonArray measurements = (JsonArray) parser.parse(data);
		
		JsonObject expectedObject = buildExpectedFromTimeseries(measurements);
		return expectedObject;
	}
	
	public TestResultSeries processWithAllTimes(JsonObject expected_series, String simulationId, String response) {
		TestResultSeries testResultSeries = new TestResultSeries();
		CompareResults compareResults = new CompareResults();
//		System.out.println("processWithAllTimes");
//		System.out.println(expected_output_series.toString().replace("\"", "\\\""));

		JsonObject expectedObject = getExpectedFrom(response);
		JsonObject simOutputObject = expectedObject.get("output").getAsJsonObject();
		JsonObject expected_output_series = expected_series.get("output").getAsJsonObject();
		
		int index = 0;
		for (Entry<String, JsonElement> time_entry : simOutputObject.entrySet()) {
//			System.out.println(time_entry);
			TestResults tr = compareResults.compareExpectedWithSimulationOutput(time_entry.getKey(), time_entry.getValue().getAsJsonObject(), expected_output_series);
			if (tr != null) {
				testResultSeries.add(time_entry.getKey(), tr);
			}
			index++;
		}
		System.out.println("Index: " + index + " TestManager number of conflicts: "+ " total " + testResultSeries.getTotal());
		
		
		JsonObject simInputObject = expectedObject.get("input").getAsJsonObject();
		JsonObject expected_input_series = expected_series.get("input").getAsJsonObject();
		index = 0;
		for (Entry<String, JsonElement> time_entry : simInputObject.entrySet()) {
			System.out.println(time_entry);
			TestResults tr = compareResults.compareExpectedWithSimulationInput(time_entry.getKey(), time_entry.getValue().getAsJsonObject(), expected_input_series);
			if (tr != null) {
				testResultSeries.add(time_entry.getKey(), tr);
			}
			index++;
		}
		System.out.println("Index: " + index + " TestManager number of conflicts: "+ " total " + testResultSeries.getTotal());
		return testResultSeries;
	}

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
	
	public String getListOfTime(String  simulationId, JsonObject expected_output_series){
		CompareResults compareResults = new CompareResults();
		Map<String, JsonElement> expectedOutputMap = compareResults.getExpectedOutputMap("0", expected_output_series);
		Set<String> keySet = expectedOutputMap.keySet();
//		return timeSeriesQuery(simulationId,"_e10b535c-79f3-498b-a38f-11d1cc50f3a0", null,null);
		for (String mrid : keySet) {
//			String response = query(simulationId, mrid, null,null,null).result.toString();
			String response = timeSeriesQuery(simulationId, mrid, null,null);
			if (response.contains("simulation"))
				return response;
		}
		return null;
	}
	
//	public String getListOfTime(String  simulationId, String expected_output_series){
//		CompareResults compareResults = new CompareResults();
//		Map<String, JsonElement> expectedOutputMap = compareResults.getExpectedOutputMap("0", expected_output_series);
//		Set<String> keySet = expectedOutputMap.keySet();
////		return timeSeriesQuery(simulationId,"_e10b535c-79f3-498b-a38f-11d1cc50f3a0", null,null);
//		for (String mrid : keySet) {
////			String response = query(simulationId, mrid, null,null,null).result.toString();
//			String response = timeSeriesQuery(simulationId, mrid, null,null);
//			if (response.contains("simulation"))
//				return response;
//		}
//		return null;
//	}
	
	public Set<String> getTimesEpoch(String responses) {
//		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
//		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

		Set<String> times = new HashSet<String>();
		JsonObject jsonObject = CompareResults.getSimulationJson(responses);
		JsonArray ma = jsonObject.get("measurements").getAsJsonArray();
		for (JsonElement meas : ma) {
			JsonArray points_array = meas.getAsJsonObject().get("points").getAsJsonArray();
			for (JsonElement point : points_array) {
				JsonArray entry_array = point.getAsJsonObject().get("row").getAsJsonObject().get("entry")
						.getAsJsonArray();
				for (JsonElement entry : entry_array) {
					String key = entry.getAsJsonObject().get("key").getAsString();
					if (key.equals("time")) {
						Long value = entry.getAsJsonObject().get("value").getAsLong();
//						Date startTime = null;
//						try {
//							startTime = sdf.parse(value);
//						} catch (ParseException e) {
//							e.printStackTrace();
//						}
						long startTime2 = value;
						// System.out.println(startTime2);
						times.add(startTime2 + "");
					}
				}
			}
		}
		return times;
	}
	
	public Set<String> getTimes(String responses) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

		Set<String> times = new HashSet<String>();
		JsonObject jsonObject = CompareResults.getSimulationJson(responses);
		JsonArray ma = jsonObject.get("measurements").getAsJsonArray();
		for (JsonElement meas : ma) {
			JsonArray points_array = meas.getAsJsonObject().get("points").getAsJsonArray();
			for (JsonElement point : points_array) {
				JsonArray entry_array = point.getAsJsonObject().get("row").getAsJsonObject().get("entry")
						.getAsJsonArray();
				for (JsonElement entry : entry_array) {
					String key = entry.getAsJsonObject().get("key").getAsString();
					if (key.equals("time")) {
						String value = entry.getAsJsonObject().get("value").getAsString();
						Date startTime = null;
						try {
							startTime = sdf.parse(value);
						} catch (ParseException e) {
							e.printStackTrace();
						}
						long startTime2 = startTime.getTime();
						// System.out.println(startTime2);
						times.add(startTime2 + "");
					}
				}
			}
		}
		return times;
	}
	
}
