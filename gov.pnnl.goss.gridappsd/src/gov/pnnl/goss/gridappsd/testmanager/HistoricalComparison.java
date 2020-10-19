package gov.pnnl.goss.gridappsd.testmanager;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gov.pnnl.goss.gridappsd.api.DataManager;
import gov.pnnl.goss.gridappsd.dto.RequestTimeseriesData;
import gov.pnnl.goss.gridappsd.dto.TestConfig;
import pnnl.goss.core.Client;

public class HistoricalComparison {
	
	DataManager dataManager;
	String username;
	Client client;
	private Logger log = LoggerFactory.getLogger(getClass());
	
	public HistoricalComparison(DataManager dataManager, String username, Client client) {
		this.dataManager = dataManager;
		this.username = username;
		this.client = client;
	}

	public TestResultSeries testProven(String simulationId, TestConfig testConfig, JsonObject expected_output_series){

		String responseStr = timeSeriesQuery(simulationId, "1532971828475", null, null);
		if(! responseStr.contains("data")){
			System.out.println("No data for " + simulationId);
			log.warn("No timeseries data for simulation " + simulationId);
			return new TestResultSeries();
		}
		TestResultSeries testResultSeries = processWithAllTimes(expected_output_series, testConfig, responseStr);
		return testResultSeries;
	}
	
	public TestResultSeries testProven(String simulationIdOne, String simulationIdTwo, TestConfig testConfig){

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
		TestResultSeries testResultSeries = processWithAllTimes("123", testConfig, responseStrOne, responseStrTwo);
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
					simulationId, null,
					username);
		} catch (Exception e) {
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

	public TestResultSeries processWithAllTimes(String simulationId,TestConfig testConfig, String responseOne, String responseTwo) {
		TestResultSeries testResultSeries = new TestResultSeries();
		CompareResults compareResults = new CompareResults(client, testConfig);

		JsonObject expectedObjectOne = getExpectedFrom(responseOne);
		JsonObject simOutputObjectOne = expectedObjectOne.get("output").getAsJsonObject();
		
		JsonObject expectedObjectTwo = getExpectedFrom(responseTwo);
		JsonObject simOutputObjectTwo = expectedObjectTwo.get("output").getAsJsonObject();

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
		
		
		JsonObject simInputObject = expectedObjectOne.get("input").getAsJsonObject();
		JsonObject expected_input_series = expectedObjectTwo.get("input").getAsJsonObject();
		if(simInputObject.entrySet().isEmpty() || expected_input_series.entrySet().isEmpty()){
			System.out.println("Empty inputs");
			return testResultSeries;
		}else{
			for (Entry<String, JsonElement> time_entry : simInputObject.entrySet()) {
				System.out.println(time_entry);
				TestResults tr = compareResults.compareExpectedWithSimulationInput(time_entry.getKey().toString(), time_entry.getKey().toString(), simInputObject, expected_input_series);
				if (tr != null) {
					testResultSeries.add(time_entry.getKey(), time_entry.getKey(), tr);
				}
			}
		}
		
		//rebaseAndCompare(testResultSeries, compareResults, simInputObject, expected_input_series);
		return testResultSeries;
	}

	public void rebaseAndCompare(TestResultSeries testResultSeries, CompareResults compareResults,
			JsonObject simInputObject, JsonObject expected_input_series) {
//		int index = 0;
		HashMap<Integer, Integer> newKeys1 = rebaseKeys(simInputObject, expected_input_series);
//		System.out.println(newKeys1);
		// Rebase or set to match output ...
		for (Entry<Integer,Integer> time_entry : newKeys1.entrySet()) {
			System.out.println(time_entry);
			String timeOne = time_entry.getKey().toString();
			String timeTwo = time_entry.getValue().toString();
			if(simInputObject.has(timeOne)){
				TestResults tr = compareResults.compareExpectedWithSimulationInput(timeOne, timeTwo, simInputObject, expected_input_series);
				if (tr != null) {
					testResultSeries.add(timeOne, timeTwo, tr);
				}
			}
//			index++;
		}
//		System.out.println("Index: " + index + " TestManager number of conflicts: "+ " total " + testResultSeries.getTotal());
	}

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
	
	public TestResultSeries processWithAllTimes(JsonObject expected_series, TestConfig testConfig, String response) {
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

			for (Entry<String, JsonElement> time_entry : simInputObject.entrySet()) {
				System.out.println(time_entry);
				TestResults tr = compareResults.compareExpectedWithSimulationInput(time_entry.getKey().toString(), time_entry.getKey().toString(), simInputObject, expected_input_series);
				if (tr != null) {
					testResultSeries.add(time_entry.getKey(), time_entry.getKey(), tr);
				}
			}
			
//			rebaseAndCompare(testResultSeries, compareResults, simInputObject, expected_input_series);
		}
		return testResultSeries;
	}

	public HashMap<Integer, Integer> rebaseKeys(JsonObject simInputObject, JsonObject expected_input_series) {
		SortedSet<Integer> inputKeys1 = new TreeSet<>();
		SortedSet<Integer> inputKeys2 = new TreeSet<>();

//		keys.addAll(simInputObject.keySet()));

		for (Entry<String, JsonElement> time_entry : simInputObject.entrySet()) {
			inputKeys1.add(Integer.valueOf(time_entry.getKey()));
		}

		for (Entry<String, JsonElement> time_entry : expected_input_series.entrySet()) {
			inputKeys2.add(Integer.valueOf(time_entry.getKey()));
		}
//		inputKeys1.add(1590612488); inputKeys1.add(1590612503); inputKeys1.add(1590612518);
//		inputKeys2.add(1590616253); inputKeys2.add(1590616268); inputKeys2.add(1590616283);
		
//		System.out.println("input keys");
//		System.out.println(inputKeys1.toString());
//		System.out.println(inputKeys2.toString());
		HashMap<Integer, Integer> x = getTimeMap(inputKeys1, inputKeys2);
		System.out.println(x);
		return x;
	}
	
	/**
	 * Create a map to match the match the indexes of the input keys.
	 * The mapping is created by using the differences between the values of the set of inputKeys2 and the 
	 * first value of the inputKeys1 to keys a map that is based on the difference between the times instead of using exact matches. 
	 * @param inputKeys1
	 * @param inputKeys2
	 * @return
	 */
	public HashMap<Integer, Integer> getTimeMap(SortedSet<Integer> inputKeys1, SortedSet<Integer> inputKeys2) {
		HashMap<Integer,Integer> newKeys1 = new HashMap<Integer,Integer>();
		Integer first1 = inputKeys1.first();
		Integer first2 = inputKeys2.first();
		
		int diff = 0;

		Iterator<Integer> it2 = inputKeys2.iterator();

		while (it2.hasNext()) {
			Integer key2 = it2.next();
			diff = key2-first2;
			newKeys1.put(first1+ diff, key2);
		}

		return newKeys1;
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
	
//	public String getListOfTime(String  simulationId, JsonObject expected_output_series){
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
	
//	public Set<String> getTimesEpoch(String responses) {
////		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
////		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
//
//		Set<String> times = new HashSet<String>();
//		JsonObject jsonObject = CompareResults.getSimulationJson(responses);
//		JsonArray ma = jsonObject.get("measurements").getAsJsonArray();
//		for (JsonElement meas : ma) {
//			JsonArray points_array = meas.getAsJsonObject().get("points").getAsJsonArray();
//			for (JsonElement point : points_array) {
//				JsonArray entry_array = point.getAsJsonObject().get("row").getAsJsonObject().get("entry")
//						.getAsJsonArray();
//				for (JsonElement entry : entry_array) {
//					String key = entry.getAsJsonObject().get("key").getAsString();
//					if (key.equals("time")) {
//						Long value = entry.getAsJsonObject().get("value").getAsLong();
////						Date startTime = null;
////						try {
////							startTime = sdf.parse(value);
////						} catch (ParseException e) {
////							e.printStackTrace();
////						}
//						long startTime2 = value;
//						// System.out.println(startTime2);
//						times.add(startTime2 + "");
//					}
//				}
//			}
//		}
//		return times;
//	}
	
//	public Set<String> getTimes(String responses) {
//		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
//		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
//
//		Set<String> times = new HashSet<String>();
//		JsonObject jsonObject = CompareResults.getSimulationJson(responses);
//		JsonArray ma = jsonObject.get("measurements").getAsJsonArray();
//		for (JsonElement meas : ma) {
//			JsonArray points_array = meas.getAsJsonObject().get("points").getAsJsonArray();
//			for (JsonElement point : points_array) {
//				JsonArray entry_array = point.getAsJsonObject().get("row").getAsJsonObject().get("entry")
//						.getAsJsonArray();
//				for (JsonElement entry : entry_array) {
//					String key = entry.getAsJsonObject().get("key").getAsString();
//					if (key.equals("time")) {
//						String value = entry.getAsJsonObject().get("value").getAsString();
//						Date startTime = null;
//						try {
//							startTime = sdf.parse(value);
//						} catch (ParseException e) {
//							e.printStackTrace();
//						}
//						long startTime2 = startTime.getTime();
//						// System.out.println(startTime2);
//						times.add(startTime2 + "");
//					}
//				}
//			}
//		}
//		return times;
//	}
	
}
