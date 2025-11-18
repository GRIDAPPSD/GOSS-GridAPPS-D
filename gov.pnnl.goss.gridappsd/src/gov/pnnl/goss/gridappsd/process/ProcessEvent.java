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
package gov.pnnl.goss.gridappsd.process;

import gov.pnnl.goss.gridappsd.api.AppManager;
import gov.pnnl.goss.gridappsd.api.ConfigurationManager;
import gov.pnnl.goss.gridappsd.api.DataManager;
import gov.pnnl.goss.gridappsd.api.FieldBusManager;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.api.ServiceManager;
import gov.pnnl.goss.gridappsd.api.SimulationManager;
import gov.pnnl.goss.gridappsd.api.TestManager;
import gov.pnnl.goss.gridappsd.dto.ConfigurationRequest;
import gov.pnnl.goss.gridappsd.dto.LogMessage;
import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import gov.pnnl.goss.gridappsd.dto.events.CommOutage;
import gov.pnnl.goss.gridappsd.dto.events.Event;
import gov.pnnl.goss.gridappsd.dto.events.Fault;
import gov.pnnl.goss.gridappsd.dto.events.ScheduledCommandEvent;
import gov.pnnl.goss.gridappsd.dto.RuntimeTypeAdapterFactory;
import gov.pnnl.goss.gridappsd.dto.UserToken;
import gov.pnnl.goss.gridappsd.dto.ServiceInfo;
import gov.pnnl.goss.gridappsd.dto.PlatformStatus;
import gov.pnnl.goss.gridappsd.dto.RequestPlatformStatus;
import gov.pnnl.goss.gridappsd.dto.RequestSimulation;
import gov.pnnl.goss.gridappsd.dto.RequestSimulation.SimulationRequestType;
import gov.pnnl.goss.gridappsd.dto.RequestSimulationResponse;
import gov.pnnl.goss.gridappsd.dto.RoleList;
import gov.pnnl.goss.gridappsd.dto.YBusExportResponse;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.jms.Destination;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import pnnl.goss.core.Client;
import pnnl.goss.core.DataError;
import pnnl.goss.core.DataResponse;
import pnnl.goss.core.GossResponseEvent;
import pnnl.goss.core.Response;
import pnnl.goss.core.Request.RESPONSE_FORMAT;
// TODO: Security removed in GOSS Java 21 upgrade - needs reimplementation
//import pnnl.goss.core.security.JWTAuthenticationToken;
//import pnnl.goss.core.security.SecurityConfig;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.nimbusds.jose.Payload;
import com.nimbusds.jwt.SignedJWT;

/**
 * ProcessEvent class processes requests received by the Process Manager. These
 * requests include:
 * <p>
 * 1. Start a simulation <br>
 * 2. Query data <br>
 * 3. Logg messages <br>
 * 4. Query platform status
 *
 * @author Poorva Sharma
 * @author Tara D Gibson
 *
 */
public class ProcessEvent implements GossResponseEvent {

    Client client;
    ProcessNewSimulationRequest newSimulationProcess;
    ProcessManagerImpl processManger;
    ConfigurationManager configurationManager;
    SimulationManager simulationManager;
    AppManager appManager;
    LogManager logManager;
    ServiceManager serviceManager;
    DataManager dataManager;
    TestManager testManager;
    // TODO: Security removed in GOSS Java 21 upgrade - needs reimplementation
    // SecurityConfig securityConfig;
    FieldBusManager fieldBusManager;

    // TODO: Security removed in GOSS Java 21 upgrade - needs reimplementation
    // public ProcessEvent(ProcessManagerImpl processManager,
    // Client client, ProcessNewSimulationRequest newSimulationProcess,
    // ConfigurationManager configurationManager, SimulationManager
    // simulationManager,
    // AppManager appManager, LogManager logManager, ServiceManager serviceManager,
    // DataManager dataManager, TestManager testManager, SecurityConfig
    // securityConfig,
    // FieldBusManager fieldBusManager){
    public ProcessEvent(ProcessManagerImpl processManager,
            Client client, ProcessNewSimulationRequest newSimulationProcess,
            ConfigurationManager configurationManager, SimulationManager simulationManager,
            AppManager appManager, LogManager logManager, ServiceManager serviceManager,
            DataManager dataManager, TestManager testManager,
            FieldBusManager fieldBusManager) {
        this.client = client;
        this.processManger = processManager;
        this.newSimulationProcess = newSimulationProcess;
        this.configurationManager = configurationManager;
        this.simulationManager = simulationManager;
        this.appManager = appManager;
        this.logManager = logManager;
        this.serviceManager = serviceManager;
        this.dataManager = dataManager;
        this.testManager = testManager;
        // TODO: Security removed in GOSS Java 21 upgrade - needs reimplementation
        // this.securityConfig = securityConfig;
        this.fieldBusManager = fieldBusManager;
    }

    @Override
    public void onMessage(Serializable message) {

        DataResponse event = (DataResponse) message;
        String token = event.getUsername();

        String processId = ProcessManagerImpl.generateProcessId();

        String username = token;

        // TODO: Security removed in GOSS Java 21 upgrade - needs reimplementation
        //// If it looks like a token
        // if(token!=null && token.length()>250){
        //
        // //Verify Token, throw exception if it cannot be verified
        // boolean valid = securityConfig.validateToken(token);
        // if(!valid){
        // logManager.error(ProcessStatus.ERROR, processId,"Failure to validate
        // authentication token:"+token);
        // //TODO, USERNAME WOULD STILL BE THE FULL TOKEN HERE, HOW DO WE WANT TO
        // ADDRESS THAT?
        // sendError(client, event.getReplyDestination(), "Failure to validate
        // authentication token", processId, username);
        // return;
        // }
        // //Get username from token
        // JWTAuthenticationToken tokenObj = securityConfig.parseToken(token);
        // username = tokenObj.getSub();
        //
        //
        // }

        logManager.debug(ProcessStatus.RUNNING, processId, "Received message: " + event.getData() + " on topic "
                + event.getDestination() + " from user " + username);
        logManager.setProcessType(processId, event.getDestination());

        try {
            GsonBuilder gsonBuilder = new GsonBuilder();
            RuntimeTypeAdapterFactory<Event> commandAdapterFactory = RuntimeTypeAdapterFactory
                    .of(Event.class, "event_type")
                    .registerSubtype(CommOutage.class, "CommOutage").registerSubtype(Fault.class, "Fault")
                    .registerSubtype(ScheduledCommandEvent.class, "ScheduledCommandEvent");
            gsonBuilder.registerTypeAdapterFactory(commandAdapterFactory);
            gsonBuilder.setPrettyPrinting();
            Gson gsonSpecial = gsonBuilder.create();
            // simRequest = gson.fromJson(request.toString(), RequestSimulation.class);

            if (event.getDestination().contains(GridAppsDConstants.topic_requestSimulation)) {
                // Parse simluation request
                Serializable request;
                if (message instanceof DataResponse) {
                    request = ((DataResponse) message).getData();
                } else {
                    request = message;
                }

                RequestSimulation simRequest = null;
                if (request instanceof ConfigurationRequest) {
                    simRequest = ((RequestSimulation) request);
                } else {
                    if (request != null) {
                        // make sure it doesn't fail if request is null, although it should never be
                        // null
                        try {
                            // simRequest = RequestSimulation.parse(request.toString());
                            simRequest = gsonSpecial.fromJson(request.toString(), RequestSimulation.class);
                        } catch (JsonSyntaxException e) {
                            e.printStackTrace();
                            // TODO log error
                            sendError(client, event.getReplyDestination(), e.getMessage(), processId, username);
                        }
                    } else {
                        sendError(client, event.getReplyDestination(), "Simulation request is null", processId,
                                username);
                    }
                }
                if (simRequest != null) {
                    // if new simulation
                    if (simRequest.simulation_request_type == null
                            || simRequest.simulation_request_type.equals(SimulationRequestType.NEW)) {
                        RequestSimulationResponse response = new RequestSimulationResponse();
                        response.setSimulationId(processId);
                        // RequestSimulation config = RequestSimulation.parse(message.toString());
                        if (simRequest.getTest_config() != null)
                            response.setEvents(testManager
                                    .sendEventsToSimulation(simRequest.getTest_config().getEvents(), processId));
                        client.publish(event.getReplyDestination(), response);
                        // TODO also verify that we have the correct sub-configurations as part of the
                        // request
                        // newSimulationProcess.process(configurationManager, simulationManager,
                        // processId, event, event.getData(), appManager, serviceManager);
                        newSimulationProcess.process(configurationManager, simulationManager, processId, simRequest,
                                appManager, serviceManager, testManager, dataManager, username);
                    } else if (simRequest.simulation_request_type.equals(SimulationRequestType.PAUSE)) { // if pause
                        simulationManager.pauseSimulation(simRequest.getSimulation_id());
                    } else if (simRequest.simulation_request_type.equals(SimulationRequestType.RESUME)) { // if play
                        simulationManager.resumeSimulation(simRequest.getSimulation_id());
                    } else if (simRequest.simulation_request_type.equals(SimulationRequestType.STOP)) { // if stop
                        simulationManager.endSimulation(simRequest.getSimulation_id());
                    } else {
                        sendError(client, event.getReplyDestination(),
                                "Simulation request type not recognized: " + simRequest.simulation_request_type,
                                processId, username);
                    }
                } else {
                    sendError(client, event.getReplyDestination(), "Simulation request could not be parsed", processId,
                            username);
                }
            } else if (event.getDestination().contains(GridAppsDConstants.topic_requestApp)) {
                appManager.process(processId, event, message);

            } else if (event.getDestination().contains(GridAppsDConstants.topic_requestData)) {

                String requestTopicExtension = event.getDestination()
                        .substring(event.getDestination().indexOf(GridAppsDConstants.topic_requestData)
                                + GridAppsDConstants.topic_requestData.length());
                if (requestTopicExtension.length() > 0) {
                    requestTopicExtension = requestTopicExtension.substring(1);
                }

                if (requestTopicExtension.indexOf(".") > 0) {
                    requestTopicExtension = requestTopicExtension.substring(0, requestTopicExtension.indexOf("."));
                }
                String type = requestTopicExtension;

                logManager.debug(ProcessStatus.RUNNING, processId, "Received data request of type: " + type);

                Serializable request;
                if (message instanceof DataResponse) {
                    request = ((DataResponse) message).getData();
                } else {
                    request = message;
                }

                Response r = dataManager.processDataRequest(request, type, processId,
                        configurationManager.getConfigurationProperty(GridAppsDConstants.GRIDAPPSD_TEMP_PATH),
                        username);
                // client.publish(event.getReplyDestination(), r);
                String responseFormat = null;
                JsonObject jsonObject = new JsonParser().parse(request.toString()).getAsJsonObject();
                if (jsonObject.has("resultFormat"))
                    responseFormat = jsonObject.get("resultFormat").getAsString();
                if (jsonObject.has("responseFormat"))
                    responseFormat = jsonObject.get("responseFormat").getAsString();

                sendData(client, event.getReplyDestination(), ((DataResponse) r).getData(), processId, username,
                        responseFormat);

            } else if (event.getDestination().contains(GridAppsDConstants.topic_requestConfig)) {

                Serializable request;
                if (message instanceof DataResponse) {
                    request = ((DataResponse) message).getData();
                } else {
                    request = message;
                }

                ConfigurationRequest configRequest = null;
                if (request instanceof ConfigurationRequest) {
                    configRequest = ((ConfigurationRequest) request);
                } else {
                    try {
                        configRequest = ConfigurationRequest.parse(request.toString());
                    } catch (JsonSyntaxException e) {
                        // TODO log error
                        sendError(client, event.getReplyDestination(), e.getMessage(), processId, username);
                    }
                }
                if (configRequest != null) {
                    StringWriter sw = new StringWriter();
                    PrintWriter out = new PrintWriter(sw);

                    ServiceInfo gldService = serviceManager.getService("GridLAB-D");
                    if (gldService != null) {
                        List<String> deps = gldService.getService_dependencies();
                        String gldInterface = GridAppsDConstants.getGLDInterface(deps);
                        configRequest.getParameters().put(GridAppsDConstants.GRIDLABD_INTERFACE, gldInterface);
                    }

                    try {
                        configurationManager.generateConfiguration(configRequest.getConfigurationType(),
                                configRequest.getParameters(), out, Integer.toString(Integer.parseInt(processId)),
                                username);
                    } catch (Exception e) {
                        StringWriter sww = new StringWriter();
                        PrintWriter pw = new PrintWriter(sww);
                        e.printStackTrace(pw);
                        logManager.error(ProcessStatus.ERROR, processId, sww.toString());
                        sendError(client, event.getReplyDestination(), e.getMessage(), processId, username);
                    }
                    String result = sw.toString();

                    if (configRequest.getConfigurationType().equals("YBus Export") ||
                            configRequest.getConfigurationType().equals("Vnom Export")) {
                        Gson gson = new Gson();
                        YBusExportResponse response = gson.fromJson(result, YBusExportResponse.class);
                        sendData(client, event.getReplyDestination(), response, processId, username, null);
                    } else
                        sendData(client, event.getReplyDestination(), result, processId, username, null);

                } else {
                    logManager.error(ProcessStatus.ERROR, processId,
                            "No valid configuration request received, request: " + request);
                    sendError(client, event.getReplyDestination(),
                            "No valid configuration request received, request: " + request, processId, username);
                }

            }

            else if (event.getDestination().contains(GridAppsDConstants.topic_requestPlatformStatus)) {

                RequestPlatformStatus request = RequestPlatformStatus.parse(event.getData().toString());
                PlatformStatus platformStatus = new PlatformStatus();
                if (request.isApplications())
                    platformStatus.setApplications(appManager.listApps());
                if (request.isServices())
                    platformStatus.setServices(serviceManager.listServices());
                if (request.isAppInstances())
                    platformStatus.setAppInstances(appManager.listRunningApps());
                if (request.isServiceInstances())
                    platformStatus.setServiceInstances(serviceManager.listRunningServices());
                if (request.isField())
                    platformStatus.setField(fieldBusManager.getFieldModelMrid());
                client.publish(event.getReplyDestination(), platformStatus);

            } else if (event.getDestination().contains(GridAppsDConstants.topic_requestMyRoles)) {
                List<String> roles = new ArrayList<String>();// .getRoles(username);
                // TODO get from user token
                RoleList roleListResult = new RoleList();
                roleListResult.setRoles(roles);
                sendData(client, event.getReplyDestination(), roleListResult.toString(), processId, username, null);
            }

            else if (event.getDestination().contains(GridAppsDConstants.topic_requestField)) {

                String data = fieldBusManager.handleRequest(event.getDestination(), event.getData()).toString();
                sendData(client, event.getReplyDestination(), data, processId, username,
                        RESPONSE_FORMAT.JSON.toString());

            }
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            logManager.error(ProcessStatus.ERROR, processId, sw.toString());
            sendError(client, event.getReplyDestination(), sw.toString(), processId, username);
        }
    }

    private void sendData(Client client, Destination replyDestination, Serializable data, String processId,
            String username, String responseFormat) {
        try {
            // Make sure it is sending back something in the data field for valid json (or
            // if it is null maybe it should send error response instead???)
            if (data == null || data.toString().length() == 0) {
                data = "{}";
            }

            if (responseFormat == null || responseFormat.equals("JSON")) {
                try {
                    new JSONObject(data.toString());
                } catch (JSONException e) {
                    data = data.toString().replace("\"", "\\\"");
                    data = "\"" + data + "\"";
                }
                String r = "{\"data\":" + data + ",\"responseComplete\":true,\"id\":\"" + processId + "\"}";
                client.publish(replyDestination, r);
            } else {
                String r = data.toString();
                client.publish(replyDestination, r);
            }
        } catch (Exception e) {
            e.printStackTrace();
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            logManager.error(ProcessStatus.ERROR, processId, sw.toString());
            // TODO log error and send error response
        }
    }

    private void sendError(Client client, Destination replyDestination, String error, String processId,
            String username) {
        try {
            DataResponse r = new DataResponse();
            r.setError(new DataError(error));
            r.setResponseComplete(true);
            client.publish(replyDestination, r);
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            logManager.error(ProcessStatus.ERROR, processId, sw.toString());
        }
    }

}
