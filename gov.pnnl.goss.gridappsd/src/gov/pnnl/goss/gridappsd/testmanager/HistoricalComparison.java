package gov.pnnl.goss.gridappsd.testmanager;

import gov.pnnl.goss.gridappsd.api.DataManager;
import gov.pnnl.goss.gridappsd.dto.RequestTimeseriesData;
import gov.pnnl.goss.gridappsd.dto.RequestTimeseriesData.RequestType;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class HistoricalComparison {
	
	DataManager dataManager;
	
	public HistoricalComparison(DataManager dataManager) {
		this.dataManager = dataManager;
	}

	public TestResultSeries test_proven(String simulationId, JsonObject expected_output_series){

		String responseStr = null;
		responseStr = timeSeriesQuery(simulationId, "1532971828475", null, null);
		TestResultSeries testResultSeries = processWithAllTimes(expected_output_series, simulationId, responseStr);
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

		HashMap<String, String> queryFilter = new HashMap<String, String>();
		queryFilter.put("hasSimulationId", simulationId);
		queryFilter.put("startTime", startTime);
		queryFilter.put("endTime", endTime);

		RequestTimeseriesData request = new RequestTimeseriesData();
		request.setQueryMeasurement(RequestType.PROVEN_MEASUREMENT);
		request.setQueryFilter(queryFilter);

		Serializable response = null;

		try {
			response = dataManager.processDataRequest(request, "timeseries",
					Integer.parseInt(simulationId), null,
					GridAppsDConstants.username);
		} catch (Exception e) {
			// TODO: Log error - excpetion
		}
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
		simOutputObject.addProperty("timestame", time);
		simOutputObject.add("measurements", measurements);
		JsonObject msgObject = new JsonObject();
		msgObject.addProperty("simulation_id", simulationId);
		msgObject.add("message", simOutputObject);

		JsonObject outputObject = new JsonObject();
		outputObject.add("output", msgObject);
		return outputObject;
	}

	public TestResultSeries processWithAllTimes(JsonObject expected_output_series, String simulationId, String response) {
		TestResultSeries testResultSeries = new TestResultSeries();
		CompareResults compareResults = new CompareResults();
		JsonObject jsonObject = CompareResults.getSimulationJson(response);
		JsonObject simOutputObject = new JsonObject();
		JsonArray meas_array = new JsonArray();
		JsonObject temp_times = new JsonObject();

		
		JsonArray ma = jsonObject.get("measurements").getAsJsonArray();
		for (JsonElement meas : ma) {
			JsonArray points_array = meas.getAsJsonObject().get("points").getAsJsonArray();
			for (JsonElement point : points_array) {
//				System.out.println(point.toString());
				JsonArray entry_array = point.getAsJsonObject().get("row").getAsJsonObject().get("entry").getAsJsonArray();
				System.out.println(" ");
				JsonObject innerObject = new JsonObject();
				meas_array.add(innerObject);
				for (JsonElement entry : entry_array) {
					JsonObject kv_pair = entry.getAsJsonObject();
					System.out.println(kv_pair.toString());

					buildMeasurementObject(innerObject, entry);
				}
				String time = innerObject.get("timestamp").getAsString();
				innerObject.remove("timestamp");
				if(! temp_times.has(time) ){
					temp_times.add(time,new JsonArray());
				}
				temp_times.get(time).getAsJsonArray().add(innerObject);
			}
		}
		
		int index = 0;
		for (Entry<String, JsonElement> time_entry : temp_times.entrySet()) {
			JsonObject outputObject = buildOutputObject(simulationId, simOutputObject, time_entry.getKey(), time_entry.getValue().getAsJsonArray());
//			System.out.println(simOutputObject.toString());
			TestResults tr = compareResults.compareExpectedWithSimulationOutput(index+"", outputObject, expected_output_series);
			if (tr != null) {
				testResultSeries.add(index+"", tr);
			}
			index++;
		}
		
		System.out.println("Index: " + index + " TestManager number of conflicts: "+ " total " + testResultSeries.getTotal());
		return testResultSeries;
	}
	
	public String getListOfTime(String  simulationId, JsonObject expected_output_series){
		CompareResults compareResults = new CompareResults();
		Map<String, JsonElement> expectedOutputMap = compareResults.getExpectedOutputMap("0", expected_output_series);
		Set<String> keySet = expectedOutputMap.keySet();
//		return timeSeriesQuery(simulationId,"_e10b535c-79f3-498b-a38f-11d1cc50f3a0", null,null);
		for (String mrid : keySet) {
//			String response = query(simulationId, mrid, null,null,null).result.toString();
			String response = timeSeriesQuery(simulationId, mrid, null,null);
			if (response.contains("PROVEN_MEASUREMENT"))
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
//			if (response.contains("PROVEN_MEASUREMENT"))
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
