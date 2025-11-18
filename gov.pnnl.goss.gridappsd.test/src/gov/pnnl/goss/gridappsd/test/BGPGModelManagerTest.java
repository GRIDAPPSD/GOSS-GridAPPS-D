package gov.pnnl.goss.gridappsd.test;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Properties;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.ConfigurationException;

import org.amdatu.testing.configurator.TestConfiguration;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.fusesource.stomp.jms.StompJmsConnectionFactory;
import org.fusesource.stomp.jms.StompJmsDestination;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.northconcepts.exception.SystemException;

import gov.pnnl.goss.gridappsd.dto.AppInfo;
import gov.pnnl.goss.gridappsd.dto.PowergridModelDataRequest;
import gov.pnnl.goss.gridappsd.dto.AppInfo.AppType;
import gov.pnnl.goss.gridappsd.dto.RequestAppRegister;
import gov.pnnl.goss.gridappsd.dto.RequestAppStart;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;
import pnnl.goss.core.server.DataSourceType;
import pnnl.goss.core.server.ServerControl;
import pnnl.goss.core.Client;
import pnnl.goss.core.GossResponseEvent;
import pnnl.goss.core.Request.RESPONSE_FORMAT;
import pnnl.goss.core.client.ClientServiceFactory;
import pnnl.goss.core.client.GossClient;
import pnnl.goss.core.Client.PROTOCOL;
import pnnl.goss.core.DataResponse;

@RunWith(MockitoJUnitRunner.class)
public class BGPGModelManagerTest {

    // ClientFactory clientFactory;
    Client client;
    private TestConfiguration testConfig;

    public static void main(String[] args) {

        BGPGModelManagerTest tester = new BGPGModelManagerTest();

        // tester.testQueryModelNames();
        tester.testQueryModelInfo();
        // tester.testQuery();
        // tester.testQueryObjectTypes();
        tester.testQueryObject();
        // tester.testQueryModel();

        System.exit(0);
    }

    public void testQuery() {

        try {

            PowergridModelDataRequest pgDataRequest = new PowergridModelDataRequest();
            // String queryString = "SELECT ?feeder ?fid WHERE {"
            // + "?s r:type c:Feeder."
            // + "?s c:IdentifiedObject.name ?feeder."
            // + "?s c:IdentifiedObject.mRID ?fid."
            // + "?s c:Feeder.NormalEnergizingSubstation ?sub."
            // + "?sub c:IdentifiedObject.name ?station."
            // + "?sub c:IdentifiedObject.mRID ?sid."
            // + "?sub c:Substation.Region ?sgr."
            // + "?sgr c:IdentifiedObject.name ?subregion."
            // + "?sgr c:IdentifiedObject.mRID ?sgrid."
            // + "?sgr c:SubGeographicalRegion.Region ?rgn."
            // + "?rgn c:IdentifiedObject.name ?region."
            // + "?rgn c:IdentifiedObject.mRID ?rgnid."
            // + "} ORDER by ?station ?feeder";
            String queryString = "SELECT ?name ?mRID ?substationName ?substationID ?subregionName ?subregionID ?regionName ?regionID WHERE {"
                    + "?s r:type c:Feeder."
                    + "?s c:IdentifiedObject.name ?name."
                    + "?s c:IdentifiedObject.mRID ?mRID."
                    + "?s c:Feeder.NormalEnergizingSubstation ?subStation."
                    + "?subStation c:IdentifiedObject.name ?substationName."
                    + "?subStation c:IdentifiedObject.mRID ?substationID."
                    + "?subStation c:Substation.Region ?subRegion."
                    + "?subRegion c:IdentifiedObject.name ?subregionName."
                    + "?subRegion c:IdentifiedObject.mRID ?subregionID."
                    + "?subRegion c:SubGeographicalRegion.Region ?region."
                    + "?region c:IdentifiedObject.name ?regionName."
                    + "?region c:IdentifiedObject.mRID ?regionID."
                    + "}  ORDER by ?name ";
            pgDataRequest.setRequestType(PowergridModelDataRequest.RequestType.QUERY.toString());
            pgDataRequest.setQueryString(queryString);
            pgDataRequest.setResultFormat(PowergridModelDataRequest.ResultFormat.JSON.toString());
            pgDataRequest.setModelId(null);

            System.out.println("QUERY REQUEST: ");
            System.out.println(pgDataRequest);
            System.out.println();
            System.out.println();
            Client client = getClient();

            Serializable response = client.getResponse(pgDataRequest.toString(),
                    GridAppsDConstants.topic_requestData + ".powergridmodel", RESPONSE_FORMAT.JSON);

            if (response instanceof String) {
                String responseStr = response.toString();
                DataResponse dataResponse = DataResponse.parse(responseStr);
                System.out.println(dataResponse.getData());
            } else {
                System.out.println(response);
                System.out.println(response.getClass());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void testQueryModelNames() {

        try {

            PowergridModelDataRequest pgDataRequest = new PowergridModelDataRequest();
            pgDataRequest.setRequestType(PowergridModelDataRequest.RequestType.QUERY_MODEL_NAMES.toString());
            pgDataRequest.setResultFormat(PowergridModelDataRequest.ResultFormat.JSON.toString());

            System.out.println("MODEL NAMES REQUEST: " + GridAppsDConstants.topic_requestData + ".powergridmodel");
            System.out.println(pgDataRequest);
            System.out.println();
            System.out.println();

            Client client = getClient();

            Serializable response = client.getResponse(pgDataRequest.toString(),
                    GridAppsDConstants.topic_requestData + ".powergridmodel", RESPONSE_FORMAT.JSON);

            if (response instanceof String) {
                String responseStr = response.toString();
                DataResponse dataResponse = DataResponse.parse(responseStr);
                System.out.println(dataResponse.getData());
            } else {
                System.out.println(response);
                System.out.println(response.getClass());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void testQueryModelInfo() {

        try {

            PowergridModelDataRequest pgDataRequest = new PowergridModelDataRequest();
            pgDataRequest.setRequestType(PowergridModelDataRequest.RequestType.QUERY_MODEL_INFO.toString());
            pgDataRequest.setResultFormat(PowergridModelDataRequest.ResultFormat.JSON.toString());

            System.out.println("MODEL INFO REQUEST: " + GridAppsDConstants.topic_requestData + ".powergridmodel");
            System.out.println(pgDataRequest);
            System.out.println();
            System.out.println();

            Client client = getClient();

            Serializable response = client.getResponse(pgDataRequest.toString(),
                    GridAppsDConstants.topic_requestData + ".powergridmodel", RESPONSE_FORMAT.JSON);

            if (response instanceof String) {
                String responseStr = response.toString();
                DataResponse dataResponse = DataResponse.parse(responseStr);
                System.out.println(dataResponse.getData());
            } else {
                System.out.println(response);
                System.out.println(response.getClass());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void testQueryObjectTypes() {
        try {

            String modelId = "_4F76A5F9-271D-9EB8-5E31-AA362D86F2C3";

            PowergridModelDataRequest pgDataRequest = new PowergridModelDataRequest();
            pgDataRequest.setRequestType(PowergridModelDataRequest.RequestType.QUERY_OBJECT_TYPES.toString());
            pgDataRequest.setResultFormat(PowergridModelDataRequest.ResultFormat.JSON.toString());
            pgDataRequest.setModelId(modelId);

            System.out.println("OBJECT TYPES REQUEST: " + GridAppsDConstants.topic_requestData + ".powergridmodel");
            System.out.println(pgDataRequest);
            System.out.println();
            System.out.println();
            Client client = getClient();

            Serializable response = client.getResponse(pgDataRequest.toString(),
                    GridAppsDConstants.topic_requestData + ".powergridmodel", RESPONSE_FORMAT.JSON);

            if (response instanceof String) {
                String responseStr = response.toString();

                DataResponse dataResponse = DataResponse.parse(responseStr);
                System.out.println(dataResponse.getData());
            } else {
                System.out.println(response);
                System.out.println(response.getClass());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void testQueryObject() {
        try {
            String objectMrid = "_4F76A5F9-271D-9EB8-5E31-AA362D86F2C3";
            PowergridModelDataRequest pgDataRequest = new PowergridModelDataRequest();
            pgDataRequest.setRequestType(PowergridModelDataRequest.RequestType.QUERY_OBJECT.toString());
            pgDataRequest.setResultFormat(PowergridModelDataRequest.ResultFormat.JSON.toString());
            pgDataRequest.setObjectId(objectMrid);
            pgDataRequest.setModelId(null);

            System.out.println("OBJECT REQUEST: " + GridAppsDConstants.topic_requestData + ".powergridmodel");
            System.out.println(pgDataRequest);
            System.out.println();
            System.out.println();
            Client client = getClient();

            Serializable response = client.getResponse(pgDataRequest.toString(),
                    GridAppsDConstants.topic_requestData + ".powergridmodel", RESPONSE_FORMAT.JSON);

            if (response instanceof String) {
                String responseStr = response.toString();

                DataResponse dataResponse = DataResponse.parse(responseStr);
                System.out.println(dataResponse.getData());
            } else {
                System.out.println(response);
                System.out.println(response.getClass());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void testQueryModel() {
        try {
            String modelId = "_4F76A5F9-271D-9EB8-5E31-AA362D86F2C3";
            PowergridModelDataRequest pgDataRequest = new PowergridModelDataRequest();
            pgDataRequest.setRequestType(PowergridModelDataRequest.RequestType.QUERY_MODEL.toString());
            pgDataRequest.setResultFormat(PowergridModelDataRequest.ResultFormat.JSON.toString());
            pgDataRequest.setModelId(modelId);
            pgDataRequest.setObjectType("http://iec.ch/TC57/CIM100#ConnectivityNode");
            pgDataRequest.setFilter("?s cim:IdentifiedObject.name 'q14733'");

            System.out.println("QUERY MODEL REQUEST: " + GridAppsDConstants.topic_requestData + ".powergridmodel");
            System.out.println(pgDataRequest);
            System.out.println();
            System.out.println();
            Client client = getClient();

            Serializable response = client.getResponse(pgDataRequest.toString(),
                    GridAppsDConstants.topic_requestData + ".powergridmodel", RESPONSE_FORMAT.JSON);

            if (response instanceof String) {
                String responseStr = response.toString();

                DataResponse dataResponse = DataResponse.parse(responseStr);
                System.out.println(dataResponse.getData());
            } else {
                System.out.println(response);
                System.out.println(response.getClass());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    Client getClient() throws Exception {
        if (client == null) {
            Dictionary properties = new Properties();
            properties.put("goss.system.manager", "system");
            properties.put("goss.system.manager.password", "manager");

            // The following are used for the core-client connection.
            properties.put("goss.openwire.uri", "tcp://0.0.0.0:61616");
            properties.put("goss.stomp.uri", "stomp://0.0.0.0:61613");
            properties.put("goss.ws.uri", "ws://0.0.0.0:61614");
            properties.put("goss.ssl.uri", "ssl://0.0.0.0:61443");
            ClientServiceFactory clientFactory = new ClientServiceFactory();
            clientFactory.updated(properties);

            // Step1: Create GOSS Client
            Credentials credentials = new UsernamePasswordCredentials(
                    TestConstants.username, TestConstants.password);
            // client = clientFactory.create(PROTOCOL.OPENWIRE, credentials);
            client = clientFactory.create(PROTOCOL.STOMP, credentials);
        }
        return client;
    }

    @Override
    protected void finalize() throws Throwable {
        // TODO Auto-generated method stub
        super.finalize();
        if (client != null) {
            client.close();
        }
    }

}
