/*******************************************************************************
 * Copyright (c) 2017, Battelle Memorial Institute All rights reserved.
 * Battelle Memorial Institute (hereinafter Battelle) hereby grants permission to any person or entity
 * lawfully obtaining a copy of this software and associated documentation files (hereinafter the
 * Software) to redistribute and use the Software in source and binary forms, with or without modification.
 * Such person or entity may use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and may permit others to do so, subject to the following conditions:
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the
 * following disclaimers.
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials provided with the distribution.
 * Other than as used herein, neither the name Battelle Memorial Institute or Battelle may be used in any
 * form whatsoever without the express written consent of Battelle.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * BATTELLE OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * General disclaimer for use with OSS licenses
 *
 * This material was prepared as an account of work sponsored by an agency of the United States Government.
 * Neither the United States Government nor the United States Department of Energy, nor Battelle, nor any
 * of their employees, nor any jurisdiction or organization that has cooperated in the development of these
 * materials, makes any warranty, express or implied, or assumes any legal liability or responsibility for
 * the accuracy, completeness, or usefulness or any information, apparatus, product, software, or process
 * disclosed, or represents that its use would not infringe privately owned rights.
 *
 * Reference herein to any specific commercial product, process, or service by trade name, trademark, manufacturer,
 * or otherwise does not necessarily constitute or imply its endorsement, recommendation, or favoring by the United
 * States Government or any agency thereof, or Battelle Memorial Institute. The views and opinions of authors expressed
 * herein do not necessarily state or reflect those of the United States Government or any agency thereof.
 *
 * PACIFIC NORTHWEST NATIONAL LABORATORY operated by BATTELLE for the
 * UNITED STATES DEPARTMENT OF ENERGY under Contract DE-AC05-76RL01830
 ******************************************************************************/
package gov.pnnl.goss.gridappsd.testmanager;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.jms.Destination;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.Activate;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HTTP;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import gov.pnnl.goss.gridappsd.api.DataManager;
import gov.pnnl.goss.gridappsd.api.LogDataManager;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.api.SimulationManager;
import gov.pnnl.goss.gridappsd.api.TestManager;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import gov.pnnl.goss.gridappsd.dto.RequestTestUpdate;
import gov.pnnl.goss.gridappsd.dto.RequestTestUpdate.RequestType;
import gov.pnnl.goss.gridappsd.dto.RuleSettings;
import gov.pnnl.goss.gridappsd.dto.RuntimeTypeAdapterFactory;
import gov.pnnl.goss.gridappsd.dto.SimulationContext;
import gov.pnnl.goss.gridappsd.dto.TestConfig;
import gov.pnnl.goss.gridappsd.dto.TestConfig.TestType;
import gov.pnnl.goss.gridappsd.dto.events.CommOutage;
import gov.pnnl.goss.gridappsd.dto.events.Event;
import gov.pnnl.goss.gridappsd.dto.events.Fault;
import gov.pnnl.goss.gridappsd.dto.events.ScheduledCommandEvent;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;
import pnnl.goss.core.Client;
import pnnl.goss.core.Client.PROTOCOL;
import pnnl.goss.core.ClientFactory;
import pnnl.goss.core.DataResponse;
import pnnl.goss.core.GossResponseEvent;
// TODO: Security removed in GOSS Java 21 upgrade - needs reimplementation
//import pnnl.goss.core.security.SecurityConfig;

/**
 *
 * @author jsimpson
 *
 */
@Component(service = TestManager.class)
public class TestManagerImpl implements TestManager {

    @Reference
    private volatile ClientFactory clientFactory;

    @Reference
    private volatile LogManager logManager;

    @Reference
    private volatile LogDataManager logDataManager;

    @Reference
    private volatile DataManager dataManager;

    @Reference
    private volatile SimulationManager simulationManager;

    // TODO: Security removed in GOSS Java 21 upgrade - needs reimplementation
    // @Reference
    // private volatile SecurityConfig securityConfig;

    private Hashtable<String, AtomicInteger> rulePorts = new Hashtable<String, AtomicInteger>();

    public enum SimulationStatus {
        STARTED, RUNNING, FINISHED
    }

    private Hashtable<String, SimulationStatus> sim_status = new Hashtable<String, SimulationStatus>();

    private Random randPort = new Random();

    // private enum EventStatus {
    // SCHEDULED, INITIATED, CLEARED, CANCELLED
    // }
    //
    // private Map<String,List<Event>> TestContext = new HashMap<String,
    // List<Event>>();
    // private Map<String,EventStatus> EventStatus = new HashMap<String,
    // EventStatus>();
    private HashMap<String, ProcessEvents> processEventsMap = new HashMap<String, ProcessEvents>(10);

    Client client;

    Gson gson;

    String testOutputTopic = GridAppsDConstants.topic_simulationTestOutput;
    // String testOutputTopic = "/topic/goss.gridappsd.simulation.test.output.";

    public TestManagerImpl() {
    }

    public TestManagerImpl(ClientFactory clientFactory,
            LogManager logManager,
            LogDataManager logDataManager,
            DataManager dataManager,
            SimulationManager simulationManager) {
        this.clientFactory = clientFactory;
        this.logManager = logManager;
        this.logDataManager = logDataManager;
        this.dataManager = dataManager;
        this.simulationManager = simulationManager;
    }

    // Setter methods for manual dependency injection (used by GridAppsDBoot)
    public void setClientFactory(ClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public void setLogManager(LogManager logManager) {
        this.logManager = logManager;
    }

    public void setLogDataManager(LogDataManager logDataManager) {
        this.logDataManager = logDataManager;
    }

    public void setDataManager(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    public void setSimulationManager(SimulationManager simulationManager) {
        this.simulationManager = simulationManager;
    }

    @Activate
    public void start() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        RuntimeTypeAdapterFactory<Event> commandAdapterFactory = RuntimeTypeAdapterFactory.of(Event.class, "event_type")
                .registerSubtype(CommOutage.class, "CommOutage").registerSubtype(Fault.class, "Fault")
                .registerSubtype(ScheduledCommandEvent.class, "ScheduledCommandEvent");
        gsonBuilder.registerTypeAdapterFactory(commandAdapterFactory);
        gsonBuilder.setPrettyPrinting();
        this.gson = gsonBuilder.create();

        try {

            // Log - "Starting "+this.getClass().getName());
            Credentials credentials = new UsernamePasswordCredentials(
                    "testmanager1", "testmanager");
            client = clientFactory.create(PROTOCOL.STOMP, credentials);

            client.subscribe(GridAppsDConstants.topic_simulationTestInput + ">", new GossResponseEvent() {

                @Override
                public void onMessage(Serializable message) {

                    DataResponse request;
                    if (message instanceof DataResponse) {
                        request = (DataResponse) message;
                        String requestDestination = request.getDestination();
                        String simulationId = requestDestination.substring(requestDestination.lastIndexOf(".") + 1,
                                requestDestination.length());
                        RequestTestUpdate requestTest = RequestTestUpdate.parse(request.getData().toString());
                        TestConfig testConfig = TestConfig.parse(request.getData().toString());
                        if (testConfig != null) {
                            if (testConfig.getTestType() == TestType.expected_vs_timeseries
                                    && testConfig.getCompareWithSimId() != null
                                    && testConfig.getExpectedResultObject() != null) {
                                compareTimeseriesSimulationWithExpected(testConfig, simulationId,
                                        testConfig.getCompareWithSimId(), testConfig.getExpectedResultObject(),
                                        request);
                            } else if (testConfig.getTestType() == TestType.timeseries_vs_timeseries
                                    && testConfig.getCompareWithSimId() != null
                                    && testConfig.getCompareWithSimIdTwo() != null) {
                                compareSimulations(testConfig, testConfig.getCompareWithSimId(),
                                        testConfig.getCompareWithSimIdTwo(), request);
                            } else {
                                publishResponse(request, "Missing parameter for testConfig simId");
                                logManager.error(ProcessStatus.RUNNING, null,
                                        "Missing parameter for testConfig " + testConfig.toString());
                            }
                        }
                        if (requestTest != null) {
                            RequestTestUpdate requestTestUpdate = gson.fromJson(request.getData().toString(),
                                    RequestTestUpdate.class);
                            if (requestTestUpdate.getCommand() == RequestType.new_events) {
                                sendEventsToSimulation(requestTestUpdate.getEvents(), simulationId);
                            } else if (requestTestUpdate.getCommand() == RequestType.update_events) {
                                updateEventForSimulation(requestTestUpdate.getEvents(), simulationId);
                                publishResponse(request);
                            } else if (requestTestUpdate.getCommand() == RequestType.query_events) {
                                sendEventStatus(simulationId, request.getReplyDestination());
                            }
                        }
                    }
                }

            });

        } catch (Exception e) {
            // TODO-log.error("Error in test manager", e);
            logManager.error(ProcessStatus.ERROR, null, "Error in test manager ");
        }
    }

    public void publishResponse(DataResponse request) {
        String r = "{\"data\":[],\"responseComplete\":true,\"id\":\"null\"}";
        System.out.println("TestManager topic dest" + request.getReplyDestination());
        if (request.getReplyDestination() != null) {
            client.publish(request.getReplyDestination(), r);
        }
    }

    public void publishResponse(DataResponse request, String message) {
        String r = "{\"data\":[\"" + message + "\"],\"responseComplete\":true,\"id\":\"null\"}";
        System.out.println("TestManager topic dest" + request.getReplyDestination());
        if (request.getReplyDestination() != null) {
            client.publish(request.getReplyDestination(), r);
        }
    }

    public void handleTestRequest(TestConfig testConfig, SimulationContext simulationContext) {
        String simulationId = simulationContext.getSimulationId();
        String simulationDir = simulationContext.getSimulationDir();
        sim_status.put(simulationId, SimulationStatus.STARTED);
        if (testConfig == null) {
            logManager.warn(ProcessStatus.RUNNING, simulationId, "testConfig is null");
            return;
        }

        if (testConfig.getEvents() != null && testConfig.getEvents().size() > 0) {
            sendEventsToSimulation(testConfig.getEvents(), simulationId);
        }

        if (testConfig.getTestType() == TestType.simulation_vs_expected
                && testConfig.getExpectedResultObject() != null) {
            checkForStoppedSimulation(testConfig, simulationId);
            client.publish(testOutputTopic + testConfig.getTestId(), "{\"status\":\"start\"}");
            compareRunningSimulationOutputWithExpected(testConfig, simulationId, testConfig.getExpectedResultObject(),
                    "expected");
            compareRunningSimulationInputWithExpected(testConfig, simulationId, testConfig.getExpectedResultObject(),
                    "expected");
            // client.publish(testOutputTopic+testConfig.getTestId(),"{\"status\":\"finish\"}");
        }

        if (testConfig.getTestType() == TestType.simulation_vs_timeseries && testConfig.getCompareWithSimId() != null
                && testConfig.getExpectedResultObject() == null) {
            checkForStoppedSimulation(testConfig, simulationId);
            compareRunningWithTimeseriesSimulation(testConfig, simulationId, testConfig.getCompareWithSimId());
        }

        if (testConfig.getRules() != null && testConfig.getRules().size() > 0) {
            comapareSimOutputWithAppRules(simulationId, simulationDir, testConfig.getAppId(), testConfig.getRules());
        }
    }

    @Override
    public List<Event> sendEventsToSimulation(List<Event> events, String simulationId) {
        // TODO: Add simulation and events in TestContext Map variable
        // TODO : Update events status in EventStatus Map variable
        // ProcessEvents pe = new ProcessEvents(logManager, events);
        // pe.processEvents(client, simulationId);
        // EventCommand eventCommand = EventCommand.parse(dataStr);
        ProcessEvents pe = getProcessEvents(client, simulationId);
        pe.addEvents(events);
        return pe.getEvents();

        // if(eventCommand.command.equalsIgnoreCase("CommEvent")){
        // pe.addEventCommandMessage(eventCommand);
        // }
    }

    private ProcessEvents getProcessEvents(Client client, String simulationId) {
        ProcessEvents pe;
        if (!processEventsMap.containsKey(simulationId)) {
            // TODO: Security removed in GOSS Java 21 upgrade - needs reimplementation
            // pe = processEventsMap.getOrDefault(simulationId, new
            // ProcessEvents(logManager, client, simulationId, simulationManager,
            // securityConfig.getManagerUser()));
            pe = processEventsMap.getOrDefault(simulationId,
                    new ProcessEvents(logManager, client, simulationId, simulationManager, "system"));
            processEventsMap.putIfAbsent(simulationId, pe);
        }
        pe = processEventsMap.get(simulationId);
        return pe;
    }

    @Override
    public void updateEventForSimulation(List<Event> events, String simulationId) {
        ProcessEvents pe = processEventsMap.get(simulationId);
        pe.updateEventTimes(events);
        // TODO : Check and Update events status in EventStatus Map variable
    }

    @Override
    public void sendEventStatus(String simulationId, Destination replyDestination) {
        // {
        // "data": [
        // {"faultMRID" : String,
        // "simulation_id": int,
        // "faultType": String,
        // "fault": <Fault Object>,
        // "timeInitiated":long,
        // "timeCleared":long,
        // "status": "scheduled"}, # "scheduled", "inprogress", "cleared"
        // }
        // }
        try {

            if (processEventsMap.containsKey(simulationId)) {
                JsonObject statusJson = processEventsMap.get(simulationId).getStatusJson();
                client.publish(replyDestination, statusJson.toString());
            } else {
                String r = "{\"data\":[],\"responseComplete\":true,\"id\":\"null\"}";
                System.out.println("TestManager topic dest" + replyDestination);
                client.publish(replyDestination, r);
            }
        } catch (Exception e) {
            e.printStackTrace();
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            // this.error(null,sw.toString());
            // TODO log error and send error response
        }
    }

    public void storeResults(TestConfig testConfig, String simulationIdOne, String simulationIdTwo,
            TestResultSeries testResultSeries) {
        // (String test_id, String processId, long simulation_time,
        // String mrid, String property, String expected, String actual, String
        // difference_direction, String difference_mrid)
        for (Map<String, String> simulationTime : testResultSeries.results.keySet()) {
            TestResults tr = testResultSeries.results.get(simulationTime);
            for (Entry<String, HashMap<String, TestResultDetails>> entry : tr.getObjectPropComparison().entrySet()) {
                HashMap<String, TestResultDetails> propMap = entry.getValue();
                for (Entry<String, TestResultDetails> prop : propMap.entrySet()) {
                    // System.out.println(String.format("%10s, %10s, %10s, %10s, %10s %10s %10s %10s
                    // %10s %10s ",
                    // app_id,
                    // test_id,
                    // simulationIdOne,
                    // simulationIdTwo,
                    // simulationTime,
                    // entry.getKey(),
                    // prop.getKey(),
                    // prop.getValue().getExpected(),
                    // prop.getValue().getActual(),
                    // prop.getValue().getDiff_mrid(),
                    // prop.getValue().getDiff_type()
                    // ));
                    if (!testConfig.getStoreMatches() && prop.getValue().getMatch())
                        continue;
                    logDataManager.storeExpectedResults(
                            testConfig.getAppId(),
                            testConfig.getTestId(),
                            simulationIdOne,
                            simulationIdTwo,
                            Long.parseLong(simulationTime.entrySet().iterator().next().getKey()),
                            Long.parseLong(simulationTime.entrySet().iterator().next().getValue()),
                            entry.getKey(),
                            prop.getKey(),
                            prop.getValue().getExpected(),
                            prop.getValue().getActual(),
                            prop.getValue().getDiffMrid(),
                            prop.getValue().getDiffType(),
                            prop.getValue().getMatch());
                }
            }
        }
    }

    @Override
    public void compareSimulations(TestConfig testConfig, String simulationIdOne, String simulationIdTwo,
            DataResponse request) {
        // TODO: Security removed in GOSS Java 21 upgrade - needs reimplementation
        // HistoricalComparison hc = new HistoricalComparison(dataManager,
        // securityConfig.getManagerUser(),client);
        HistoricalComparison hc = new HistoricalComparison(dataManager, "system", client);
        client.publish(testOutputTopic + testConfig.getTestId(), "{\"status\":\"start\"}");
        TestResultSeries testResultsSeries = hc.testProven(simulationIdOne, simulationIdTwo, testConfig);
        // publishTestResults(testConfig.getTestId(), testResultsSeries,
        // testConfig.getStoreMatches());
        // publishResponse(request);
        storeResults(testConfig, simulationIdOne, simulationIdTwo, testResultsSeries);
        client.publish(testOutputTopic + testConfig.getTestId(), "{\"status\":\"finish\"}");
    }

    public void publishTestResults(String id, TestResultSeries testResultsSeries, Boolean storeMatches) {
        // client.publish(testOutputTopic+id, testResultsSeries);
        // for (Map<String, String> key : testResultsSeries.results.keySet()) {

        // for (Entry<Map<String, String>, TestResults> entry :
        // testResultsSeries.results.entrySet()) {
        // client.publish(testOutputTopic+id, "Index: " + entry.getKey() + " TestManager
        // number of conflicts: " + testResultsSeries.getTotal());
        // System.out.println("Index: " + entry.getKey() + " TestManager number of
        // conflicts: " + entry.getValue().getNumberOfConflicts());
        // }
        // System.out.println(testOutputTopic+id);
        client.publish(testOutputTopic + id, testResultsSeries.toJson(storeMatches));
    }

    @Override
    public void compareRunningWithTimeseriesSimulation(TestConfig testConfig, String currentSimulationId,
            String simulationIdOne) {
        // TODO: Security removed in GOSS Java 21 upgrade - needs reimplementation
        // HistoricalComparison hc = new HistoricalComparison(dataManager,
        // securityConfig.getManagerUser(),client);
        HistoricalComparison hc = new HistoricalComparison(dataManager, "system", client);
        client.publish(testOutputTopic + testConfig.getTestId(), "{\"status\":\"start\"}");
        String response = hc.timeSeriesQuery(simulationIdOne, "1532971828475", null, null);
        JsonObject expectedObject = hc.getExpectedFrom(response);
        if (expectedObject == null) {
            logManager.error(ProcessStatus.ERROR, currentSimulationId,
                    "Response from time sereis db is empty for simulation " + simulationIdOne);
            return;
        }
        // JsonObject simOutputObject = expectedObject.get("output").getAsJsonObject();
        // JsonObject simInputObject = expectedObject.get("input").getAsJsonObject();
        //
        compareRunningSimulationOutputWithExpected(testConfig, currentSimulationId, expectedObject, simulationIdOne);
        compareRunningSimulationInputWithExpected(testConfig, currentSimulationId, expectedObject, simulationIdOne);
        // client.publish(testOutputTopic+testConfig.getTestId(),"{\"status\":\"finish\"}");
    }

    public void compareTimeseriesSimulationWithExpected(TestConfig testConfig, String currentSimulationId,
            String simulationIdOne, JsonObject expectedResultObject, DataResponse request) {
        client.publish(testOutputTopic + testConfig.getTestId(), "{\"status\":\"start\"}");
        // TODO: Security removed in GOSS Java 21 upgrade - needs reimplementation
        // HistoricalComparison hc = new HistoricalComparison(dataManager,
        // securityConfig.getManagerUser(),client);
        HistoricalComparison hc = new HistoricalComparison(dataManager, "system", client);
        TestResultSeries testResultsSeries = hc.testProven(simulationIdOne, testConfig, expectedResultObject);
        // publishTestResults(testConfig.getTestId(), testResultsSeries,
        // testConfig.getStoreMatches());

        publishResponse(request);
        storeResults(testConfig, simulationIdOne, "expectedJson", testResultsSeries);
        client.publish(testOutputTopic + testConfig.getTestId(), "{\"status\":\"finish\"}");
    }

    @Override
    public void compareRunningSimulationInputWithExpected(TestConfig testConfig, String simulationId,
            JsonObject expectedResults, String expectedOrSimulationIdTwo) {
        client.subscribe(GridAppsDConstants.topic_simulationInput + "." + simulationId,

                new GossResponseEvent() {

                    int inputCount = 0;
                    int expecetedLast = 0;

                    // int first1=0;
                    // int first2=0;
                    // Boolean firstSet = false;
                    // SortedSet<Integer> inputKeys2 = new TreeSet<Integer>();
                    // for (Entry<String, JsonElement> time_entry :
                    // expectedResults.get("input").getAsJsonObject().entrySet()) {
                    // inputKeys2.add(Integer.valueOf(time_entry.getKey()));
                    // }

                    // System.out.println(inputKeys2.toString());
                    public void onMessage(Serializable message) {
                        sim_status.put(simulationId, SimulationStatus.RUNNING);
                        TestResultSeries testResultSeries = new TestResultSeries();
                        DataResponse event = (DataResponse) message;
                        String simOutputStr = event.getData().toString();
                        System.out.println(simOutputStr);
                        // if (simOutputStr.length() >= 200)
                        // simOutputStr = simOutputStr.substring(0, 200);
                        // TODO: Log debug - "TestManager received message: " + simOutput + " on topic "
                        // + event.getDestination()
                        // {"command": "update", "input": {"simulation_id": "797789794", "message":
                        // {"timestamp": 1588968302
                        CompareResults compareResults = new CompareResults(client, testConfig);
                        JsonObject simJsonObj = CompareResults.getSimulationJson(simOutputStr);
                        simJsonObj = simJsonObj.get("input").getAsJsonObject();

                        if (!simJsonObj.has("message")) {
                            logManager.error(ProcessStatus.ERROR, simulationId,
                                    "TestManager received empty message key in simulation input");
                            return;
                        }

                        // TODO rebase
                        // if rebase then
                        // rebase versus expected

                        // JsonObject simInputObject = simJsonObj;
                        // JsonObject expected_input_series =
                        // expectedResults.get("input").getAsJsonObject();
                        // HashMap<Integer, Integer> newKeys1 = rebase_keys(simInputObject,
                        // expected_input_series);
                        SortedSet<Integer> inputKeys2 = new TreeSet<Integer>();
                        for (Entry<String, JsonElement> time_entry : expectedResults.get("input").getAsJsonObject()
                                .entrySet()) {
                            inputKeys2.add(Integer.valueOf(time_entry.getKey()));
                        }
                        System.out.println("Input Keys");
                        System.out.println(inputKeys2.toString());
                        System.out.println(inputCount);
                        if (inputKeys2.isEmpty()) {
                            System.out.println("No input for expected results");
                            return;
                        }
                        String simulationTimestamp = simJsonObj.getAsJsonObject().get("message").getAsJsonObject()
                                .get("timestamp").getAsString();
                        // if( ! firstSet){
                        // first1 = Integer.valueOf(simulationTimestamp);
                        // firstSet=true;
                        // }
                        //
                        // inputCount= getNextCount(inputKeys2, Integer.valueOf(simulationTimestamp),
                        // first1, inputCount);
                        // System.out.println("size and inputCount");
                        // System.out.println(inputKeys2.size());
                        // System.out.println(inputCount);
                        // if(inputKeys2.size() <= inputCount){
                        // return;
                        // }

                        String originalTimestamp = simJsonObj.getAsJsonObject().get("message").getAsJsonObject()
                                .get("timestamp").getAsString();
                        expecetedLast = Integer.valueOf(originalTimestamp) - expecetedLast;

                        // Is this needed ?
                        // simJsonObj.getAsJsonObject().get("message").getAsJsonObject().addProperty("timestamp",inputKeys2.toArray()[inputCount].toString());
                        // simulationTimestamp =
                        // simJsonObj.getAsJsonObject().get("message").getAsJsonObject().get("timestamp").getAsString();
                        JsonObject simJsonObjAtTime = new JsonObject();
                        simJsonObjAtTime.add(simulationTimestamp, simJsonObj);
                        // System.out.println("simulationTimestamp");
                        // System.out.println(simulationTimestamp);
                        // System.out.println("simJsonObjAtTime");
                        // System.out.println(simJsonObjAtTime);
                        // System.out.println(expectedResults);
                        TestResults testResults = compareResults.compareExpectedWithSimulationInput(simulationTimestamp,
                                simulationTimestamp,
                                // inputKeys2.toArray()[inputCount].toString(),
                                simJsonObjAtTime, expectedResults);
                        testResultSeries.add(originalTimestamp,
                                simulationTimestamp,
                                // inputKeys2.toArray()[inputCount].toString(),
                                testResults);
                        // if (testResults != null) {
                        // client.publish(testOutputTopic+testConfig.getTestId(),
                        // testResultSeries.toJson(testConfig.getStoreMatches()));
                        // }
                        storeResults(testConfig, simulationId, expectedOrSimulationIdTwo, testResultSeries);
                        inputCount++;
                    }
                });
    }

    /**
     * Determine if the input count should be incremented.
     *
     * @param inputKeys
     * @param simulationTimestamp
     * @param first1
     * @param inputCount
     * @return
     */
    public int getNextCount(SortedSet<Integer> inputKeys, int simulationTimestamp, int first1, int origInputCount) {
        int inputCount = origInputCount;
        int first2 = (int) inputKeys.toArray()[0];
        while (inputKeys.size() > inputCount) {
            int key2 = (int) inputKeys.toArray()[inputCount];
            int diff = key2 - first2;
            System.out.println(diff);
            System.out.println(simulationTimestamp);
            System.out.println(first1 + diff);

            if (Integer.valueOf(simulationTimestamp) != first1 + diff) {
                inputCount++;
            } else {
                break;
            }
        }
        return inputCount;
    }

    @Override
    public void compareRunningSimulationOutputWithExpected(TestConfig testConfig, String simulationId,
            JsonObject expectedResults, String expectedOrSimulationIdTwo) {
        client.subscribe(GridAppsDConstants.topic_simulationOutput + "." + simulationId,

                new GossResponseEvent() {
                    public void onMessage(Serializable message) {
                        sim_status.put(simulationId, SimulationStatus.RUNNING);
                        TestResultSeries testResultSeries = new TestResultSeries();
                        DataResponse event = (DataResponse) message;
                        String simOutputStr = event.getData().toString();
                        // if (simOutputStr.length() >= 200)
                        // simOutputStr = simOutputStr.substring(0, 200);
                        // TODO: Log debug - "TestManager received message: " + simOutput + " on topic "
                        // + event.getDestination()

                        CompareResults compareResults = new CompareResults(client, testConfig);
                        JsonObject simOutputJsonObj = CompareResults.getSimulationJson(simOutputStr);

                        if (!simOutputJsonObj.has("message")) {
                            logManager.error(ProcessStatus.RUNNING, simulationId,
                                    "TestManager received empty message key in simulation output");
                            return;
                        }

                        String simulationTimestamp = simOutputJsonObj.getAsJsonObject().get("message").getAsJsonObject()
                                .get("timestamp").getAsString();
                        TestResults testResults = compareResults.compareExpectedWithSimulationOutput(
                                simulationTimestamp,
                                simOutputJsonObj, expectedResults);
                        testResultSeries.add(simulationTimestamp, simulationTimestamp, testResults);
                        if (testResults != null) {
                            String resultJson = testResultSeries.toJson(testConfig.getStoreMatches());
                            // if (resultJson != null && ! resultJson.isEmpty()){
                            // client.publish(testOutputTopic+testConfig.getTestId(), resultJson);
                            // }
                            // TODO: Store results in timeseries store.
                        }
                        storeResults(testConfig, simulationId, expectedOrSimulationIdTwo, testResultSeries);
                    }

                });
    }

    public void checkForStoppedSimulation(TestConfig testConfig, String simulationId) {
        // BASE_SIMULATION_STATUS_TOPIC = "/topic/goss.gridappsd.simulation.log"
        client.subscribe(GridAppsDConstants.topic_applicationLog + "." + simulationId,

                new GossResponseEvent() {
                    public void onMessage(Serializable message) {
                        DataResponse event = (DataResponse) message;
                        String simOutputStr = event.getData().toString();
                        JsonObject simOutputJsonObj = CompareResults.getSimulationJson(simOutputStr);
                        if (simOutputJsonObj.has("processStatus") &&
                                simOutputJsonObj.get("processStatus").getAsString().equals("COMPLETE")) {
                            // if 'processStatus' in message and message['processStatus'] == "COMPLETE":
                            // print(message)
                            sim_status.put(simulationId, SimulationStatus.FINISHED);
                            // publish finish
                            System.out.println(
                                    testOutputTopic + testConfig.getTestId() + " " + "{\"status\":\"finish\"}");
                            client.publish(testOutputTopic + testConfig.getTestId(), "{\"status\":\"finish\"}");
                        }
                    }

                });
    }

    // TODO: cross check this port with port used for FNCS
    private int assignTestPort(String simulationId) throws Exception {
        if (!rulePorts.containsKey(simulationId)) {
            int tempPort = 49152 + randPort.nextInt(16384);
            AtomicInteger tempPortObj = new AtomicInteger(tempPort);
            while (rulePorts.containsValue(tempPortObj)) {
                int newTempPort = 49152 + randPort.nextInt(16384);
                tempPortObj.set(newTempPort);
            }
            rulePorts.put(simulationId, tempPortObj);
            return tempPortObj.get();
            // TODO: test host:port is available
        } else {
            throw new Exception("RulePort already assigned to the simulation id : " + simulationId);
        }
    }

    private void forwardSimInputToRuleEngine(Client client, String simulationID, int rulePort) {
        client.subscribe(GridAppsDConstants.topic_simulationInput + "." + simulationID,
                new GossResponseEvent() {
                    public void onMessage(Serializable message) {
                        JsonObject jsonObject = CompareResults.getSimulationJson(message.toString());
                        jsonObject = CompareResults.getSimulationJson(jsonObject.get("data").getAsString());
                        JsonObject forwardObject = jsonObject.get("input").getAsJsonObject();
                        forwardSimOutputToRuleEngine(forwardObject, rulePort, "output", "localhost");
                    }
                });
    }

    private void forwardSimOutputToRuleEngine(JsonObject jsonObject, int port, String topic, String host) {
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        try {

            HttpPost request = new HttpPost("http://" + host + ":" + port + "/" + topic + "/events");
            StringEntity params = new StringEntity(jsonObject.toString());
            request.addHeader(HTTP.CONTENT_TYPE, "application/json");
            request.addHeader("Accept", "application/json");
            request.setEntity(params);
            CloseableHttpResponse response = httpClient.execute(request);
            /* Checking response */
            if (response != null) {
                InputStream in = response.getEntity().getContent(); // Get the data in the entity
            }

        } catch (Exception ex) {
            // TODO: Log error - handle exception here
        } finally {
            try {
                httpClient.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    /**
     * This method compare simulation output with more complex rules using a rule
     * engine. This is still in test mode will be available in later release.
     */
    public void comapareSimOutputWithAppRules(String simulationId, String simulationDir, String appId,
            List<RuleSettings> rules) {

        Process rulesProcess = null;
        try {
            int rulePort = assignTestPort(simulationId);
            String appRuleName = rules.get(0).name;
            // TODO: Log info - "Calling python "+appDirectory+File.separator+appRuleName+"
            // "+simulationID
            ProcessBuilder ruleAppBuilder = new ProcessBuilder("python", simulationDir + File.separator + appRuleName,
                    "-t", "input", "-p", "" + rulePort, "--id", "" + simulationId);
            ruleAppBuilder.redirectErrorStream(true);
            ruleAppBuilder.redirectOutput(new File(simulationDir + File.separator + "rule_app.log"));
            rulesProcess = ruleAppBuilder.start();
            // TODO: Add this ruleProcess to simulationContext so it can be stopped when
            // simulation stops
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (!rulesProcess.isAlive()) {
                // TODO: log error - "Process " + appDirectory+File.separator+appRuleName+" "
                // +"did not start check rule script and that redis is running."
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Watch the process
        if (rulesProcess != null)
            watch(rulesProcess, "RulesApp" + simulationId);
    }

    private void watch(final Process process, String processName) {
        new Thread() {
            public void run() {
                BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line = null;
                try {
                    while ((line = input.readLine()) != null) {
                        // TODO: debug - processName+":"+line
                    }
                } catch (IOException e) {
                    // TODO:log error - "Error on process "+ processName, e
                }
            }
        }.start();
    }

}
