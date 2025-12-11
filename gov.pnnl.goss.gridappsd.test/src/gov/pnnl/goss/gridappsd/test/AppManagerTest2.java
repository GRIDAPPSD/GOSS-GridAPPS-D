package gov.pnnl.goss.gridappsd.test;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.fusesource.stomp.jms.StompJmsConnectionFactory;
import org.fusesource.stomp.jms.StompJmsDestination;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.gson.Gson;
import com.northconcepts.exception.SystemException;

import gov.pnnl.goss.gridappsd.dto.AppInfo;
import gov.pnnl.goss.gridappsd.dto.AppInfo.AppType;
import gov.pnnl.goss.gridappsd.dto.RequestAppRegister;
import gov.pnnl.goss.gridappsd.dto.RequestAppStart;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;
import pnnl.goss.core.server.ServerControl;
import pnnl.goss.core.Client;
import pnnl.goss.core.Client.PROTOCOL;

@RunWith(MockitoJUnitRunner.class)
public class AppManagerTest2 {

    // ClientFactory clientFactory;
    Client client;

    public static final String APPLICATION_OBJECT_CONFIG = "{\\\"static_inputs\\\": {\\\"ieee8500\\\": {\\\"control_method\\\": \\\"ACTIVE\\\",\\\"capacitor_delay\\\": 60,\\\"regulator_delay\\\": 60,\\\"desired_pf\\\": 0.99,\\\"d_max\\\": 0.9,\\\"d_min\\\": 0.1,\\\"substation_link\\\": \\\"xf_hvmv_sub\\\",\\\"regulator_list\\\": [\\\"reg_FEEDER_REG\\\",\\\"reg_VREG2\\\",\\\"reg_VREG3\\\",\\\"reg_VREG4\\\"],\\\"regulator_configuration_list\\\": [\\\"rcon_FEEDER_REG\\\",\\\"rcon_VREG2\\\",\\\"rcon_VREG3\\\",\\\"rcon_VREG4\\\"],\\\"capacitor_list\\\": [\\\"cap_capbank0a\\\",\\\"cap_capbank0b\\\",\\\"cap_capbank0c\\\",\\\"cap_capbank1a\\\",\\\"cap_capbank1b\\\",\\\"cap_capbank1c\\\",\\\"cap_capbank2a\\\",\\\"cap_capbank2b\\\",\\\"cap_capbank2c\\\",\\\"cap_capbank3\\\"],\\\"voltage_measurements\\\": [\\\"l2955047,1\\\",\\\"l3160107,1\\\",\\\"l2673313,2\\\",\\\"l2876814,2\\\",\\\"m1047574,3\\\",\\\"l3254238,4\\\"],\\\"maximum_voltages\\\": 7500,\\\"minimum_voltages\\\": 6500,\\\"max_vdrop\\\": 5200,\\\"high_load_deadband\\\": 100,\\\"desired_voltages\\\": 7000,\\\"low_load_deadband\\\": 100,\\\"pf_phase\\\": \\\"ABC\\\"}}}";

    public static void main(String[] args) {
        // TODO Auto-generated method stub
        new AppManagerTest2().test();
        System.exit(0);
    }

    public void test() {

        try {
            // Dictionary properties = new Hashtable();
            // properties.put("goss.system.manager", "system");
            // properties.put("goss.system.manager.password", "manager");
            //
            // // The following are used for the core-client connection.
            // properties.put("goss.openwire.uri", "tcp://0.0.0.0:61616");
            // properties.put("goss.stomp.uri", "stomp://0.0.0.0:61613");
            // properties.put("goss.ws.uri", "ws://0.0.0.0:61614");
            // properties.put("goss.ssl.uri", "ssl://0.0.0.0:61443");
            // testConfig = configure(this)
            // .add(CoreGossConfig.configureServerAndClientPropertiesConfig())
            // .add(createServiceDependency().setService(ClientFactory.class));
            // testConfig.apply();
            // ClientServiceFactory clientFactory = new ClientServiceFactory();
            // clientFactory.updated(properties);

            // Step1: Create GOSS Client
            // Credentials credentials = new UsernamePasswordCredentials(
            // GridAppsDConstants.username, GridAppsDConstants.password);
            // client = clientFactory.create(PROTOCOL.OPENWIRE, credentials);

            // Create Request Simulation object
            // PowerSystemConfig powerSystemConfig = new PowerSystemConfig();
            // powerSystemConfig.GeographicalRegion_name = "ieee8500_Region";
            // powerSystemConfig.SubGeographicalRegion_name = "ieee8500_SubRegion";
            // powerSystemConfig.Line_name = "ieee8500";

            // Gson gson = new Gson();
            // String request = gson.toJson(powerSystemConfig);
            // DataRequest request = new DataRequest();
            // request.setRequestContent(powerSystemConfig);
            // System.out.println(client);

            registerApp();

            // AppInfo
            // String response = client.getResponse("",GridAppsDConstants.topic_requestData,
            // RESPONSE_FORMAT.JSON).toString();
            //
            // //TODO subscribe to response
            // client.subscribe(GridAppsDConstants.topic_simulationOutput+response, new
            // GossResponseEvent() {
            //
            // @Override
            // public void onMessage(Serializable response) {
            // // TODO Auto-generated method stub
            // System.out.println("RESPNOSE "+response);
            // }
            // });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void registerApp() throws IOException, SystemException, JMSException {

        AppInfo appInfo = new AppInfo();
        appInfo.setId("vvo");
        appInfo.setCreator("pnnl");
        appInfo.setDescription("VVO app");
        appInfo.setExecution_path("app/vvoapp.py");

        List<String> inputs = new ArrayList<String>();
        inputs.add(GridAppsDConstants.topic_COSIM_input);
        appInfo.setInputs(inputs);

        List<String> outputs = new ArrayList<String>();
        outputs.add(GridAppsDConstants.topic_COSIM_input);
        appInfo.setOutputs(outputs);

        appInfo.setLaunch_on_startup(false);
        appInfo.setMultiple_instances(true);
        List<String> options = new ArrayList<String>();
        options.add("SIMULATION_ID");
        appInfo.setOptions(options);
        List<String> prereqs = new ArrayList<String>();
        prereqs.add("fncsgossbridge");
        appInfo.setPrereqs(prereqs);
        appInfo.setType(AppType.PYTHON);

        System.out.println(appInfo);

        File parentDir = new File(".");
        File f = new File(parentDir.getAbsolutePath() + File.separator + "resources" + File.separator + "vvo.zip");
        System.out.println(f.getAbsolutePath());
        byte[] fileData = Files.readAllBytes(f.toPath());

        RequestAppRegister appRegister = new RequestAppRegister(appInfo, fileData);
        System.out.println("REGISTER" + appRegister);

        // DataRequest request = new DataRequest();
        // request.setRequestContent(appRegister);
        // client.publish(GridAppsDConstants.topic_requestSimulation, appRegister);
        sendMessage(GridAppsDConstants.topic_app_register, appRegister);
        // String response =
        // client.getResponse(request,GridAppsDConstants.topic_app_register,
        // RESPONSE_FORMAT.JSON).toString();
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        String runtimeOptions = "-c \"" + APPLICATION_OBJECT_CONFIG + "\"";

        String simulationId = "12345";
        RequestAppStart appStart = new RequestAppStart(appInfo.getId(), runtimeOptions, simulationId);
        sendMessage(GridAppsDConstants.topic_app_start, appStart);
        System.out.println(appStart);

        try {
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // sendMessage(GridAppsDConstants.topic_app_deregister, appInfo.getId());
        // System.out.println("RESPONSE "+response);

    }

    private void sendMessage(String destination, Serializable message) throws JMSException {
        Gson gson = new Gson();
        StompJmsConnectionFactory connectionFactory = new StompJmsConnectionFactory();
        connectionFactory.setBrokerURI("tcp://localhost:61613");
        connectionFactory.setUsername("system");
        connectionFactory.setPassword("manager");
        Connection connection = connectionFactory.createConnection();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        MessageProducer producer = session.createProducer(new StompJmsDestination(destination));
        TextMessage textMessage = null;
        if (message instanceof String) {
            textMessage = session.createTextMessage(message.toString());
        } else {
            textMessage = session.createTextMessage(gson.toJson(message));

        }
        producer.send(textMessage);
    }

}
