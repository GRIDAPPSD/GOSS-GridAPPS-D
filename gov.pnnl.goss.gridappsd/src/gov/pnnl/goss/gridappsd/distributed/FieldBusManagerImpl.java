	package gov.pnnl.goss.gridappsd.distributed;
	
	import java.io.Serializable;
	import java.util.ArrayList;
	import java.util.Dictionary;
	import java.util.HashMap;
	import java.util.List;
	import java.util.Map;
	import java.util.stream.Collectors;
	
	import org.apache.felix.dm.annotation.api.Component;
	import org.apache.felix.dm.annotation.api.ConfigurationDependency;
	import org.apache.felix.dm.annotation.api.ServiceDependency;
	import org.apache.felix.dm.annotation.api.Start;
	import org.apache.http.auth.Credentials;
	import org.apache.http.auth.UsernamePasswordCredentials;
	import org.json.JSONArray;
	import org.json.JSONObject;
	
	import com.google.gson.Gson;
	import com.google.gson.JsonElement;
	import com.google.gson.JsonObject;
	import com.google.gson.JsonParser;
	
	import gov.pnnl.goss.gridappsd.api.DataManager;
	import gov.pnnl.goss.gridappsd.api.FieldBusManager;
	import gov.pnnl.goss.gridappsd.api.LogManager;
	import gov.pnnl.goss.gridappsd.api.PowergridModelDataManager;
	import gov.pnnl.goss.gridappsd.api.ServiceManager;
	import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
	import gov.pnnl.goss.gridappsd.dto.PowergridModelDataRequest;
	import gov.pnnl.goss.gridappsd.dto.ServiceInfo;
	import gov.pnnl.goss.gridappsd.dto.field.Feeder;
	import gov.pnnl.goss.gridappsd.dto.field.RequestFieldContext;
	import gov.pnnl.goss.gridappsd.dto.field.Root;
	import gov.pnnl.goss.gridappsd.dto.field.SecondaryArea;
	import gov.pnnl.goss.gridappsd.dto.field.SwitchArea;
	import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;
	import pnnl.goss.core.Client;
	import pnnl.goss.core.Client.PROTOCOL;
	import pnnl.goss.core.ClientFactory;
	import pnnl.goss.core.DataResponse;
	import pnnl.goss.core.GossResponseEvent;
	import pnnl.goss.core.Request.RESPONSE_FORMAT;
	import pnnl.goss.core.Response;
	import pnnl.goss.core.security.SecurityConfig;
	
	@Component
	public class FieldBusManagerImpl implements FieldBusManager {
	
		private static final String CONFIG_PID = "pnnl.goss.gridappsd";
		private Dictionary<String, ?> configurationProperties;
	
		String topology_reponse;
		String topicPrefix = "goss.gridappsd.process.request.field";
		String topicContext = topicPrefix + ".context";
	
		@ServiceDependency
		ClientFactory clientFactory;
	
		@ServiceDependency
		private volatile DataManager dataManager;
	
		@ServiceDependency
		private volatile ServiceManager serviceManager;
	
		@ServiceDependency
		private volatile LogManager logManager;
	
		private volatile TopologyRequestProcess topology;
	
		@ServiceDependency
		SecurityConfig securityConfig;
	
		@ServiceDependency
		PowergridModelDataManager powergridModelDataManager;
	
		Client client;
	
		Map<String, List<String>> messageBus_measIds_map = new HashMap<String, List<String>>();
		//Map<String, String> measId_messageBus_map = new HashMap<String, String>();
	
		String fieldModelId = null;
	
		// FileWriter writer = null;
	
		public FieldBusManagerImpl() {
			System.out.println("Starting FieldBusManager");
		}
	
		@Start
		public void start() {
	
			try {
	
				Credentials credentials = new UsernamePasswordCredentials(securityConfig.getManagerUser(),
						securityConfig.getManagerPassword());
				client = clientFactory.create(PROTOCOL.STOMP, credentials, true);
	
				ServiceInfo serviceInfo = serviceManager.getService("gridappsd-topology-daemon");
				if (serviceInfo == null) {
					logManager.warn(ProcessStatus.RUNNING, null,
							"Topology deamon service is not available. Stopping FieldBusManager.");
					return;
				}
	
				fieldModelId = getFieldModelMrid();
				if (fieldModelId == null) {
					logManager.warn(ProcessStatus.RUNNING, null,
							"Field model mrid is not available. Stopping FieldBusManager. "
									+ "Check conf/pnnl.goss.gridappsd.cfg file and add field.model.mrid key with value of deployed field model mrid. ");
					return;
				}
	
				topology = new TopologyRequestProcess(fieldModelId, client, dataManager, securityConfig);
				topology.start();
	
				this.publishDeviceOutput();
	
			} catch (Exception e) {
				e.printStackTrace();
			}
	
		}
	
		@Override
		public Serializable handleRequest(String request_queue, Serializable request) {
	
			Feeder responseFeeder = null;
	
			if (request_queue.endsWith("context")) {
	
				RequestFieldContext requestFieldContext = RequestFieldContext.parse(request.toString());
				if (requestFieldContext.areaId == null)
					return topology.root.feeders;
				else {
					for (SwitchArea switchArea : topology.root.feeders.switch_areas) {
						if (requestFieldContext.areaId.equals(switchArea.message_bus_id))
							return switchArea;
						for (SecondaryArea secondaryArea : switchArea.secondary_areas) {
							if (requestFieldContext.areaId.equals(secondaryArea.message_bus_id))
								return secondaryArea;
						}
					}
				}
	
			}
	
			return responseFeeder;
		}
	
		public void publishDeviceOutput() {
	
			client.subscribe(GridAppsDConstants.topic_simulationOutput + ".>", new GossResponseEvent() {
	
				@Override
				public void onMessage(Serializable response) {
	
					DataResponse event = (DataResponse) response;
					String simulationId = event.getDestination().substring(event.getDestination().lastIndexOf(".") + 1,
							event.getDestination().length());
					String simOutputStr = event.getData().toString();
					JsonObject simOutputJsonObj = null;
	
					JsonParser parser = new JsonParser();
					JsonElement simOutputObject = parser.parse(simOutputStr);
	
					if (simOutputObject.isJsonObject()) {
						simOutputJsonObj = simOutputObject.getAsJsonObject();
					}
	
					JsonObject tempObj = simOutputJsonObj.getAsJsonObject("message");
					Map<String, JsonElement> expectedOutputMap = tempObj.getAsJsonObject("measurements").entrySet().stream()
							.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()));
	
					try {
	
						for (String measurementMrid : expectedOutputMap.keySet()) {
							
							String messageBusId = null;
							if (topology.measId_messageBus_map.get(measurementMrid) != null) {
								messageBusId = "goss.gridappsd.field.simulation.output." + simulationId + "."
										+ topology.measId_messageBus_map.get(measurementMrid);
							} else {
								messageBusId = "goss.gridappsd.field.simulation.output." + simulationId + "."
										+ topology.root.feeders.message_bus_id;
							}
	
							JsonObject obj = new JsonObject();
							obj.add(measurementMrid, expectedOutputMap.get(measurementMrid));
	
							client.publish(messageBusId, obj.toString());
			
						}
	
					} catch (Exception e1) {
						e1.printStackTrace();
					}
					
	
				}
	
			});
	
		}
	
		@ConfigurationDependency(pid = CONFIG_PID)
		public synchronized void updated(Dictionary<String, ?> config) {
			this.configurationProperties = config;
		}
	
		public String getFieldModelMrid() {
			if (this.configurationProperties != null) {
				Object value = this.configurationProperties.get("field.model.mrid");
				if (value != null)
					return value.toString();
			}
			return null;
		}
	
	}
	
	class TopologyRequest implements Serializable {
	
		private static final long serialVersionUID = 4279262793871885409L;
	
		String requestType = "GET_SWITCH_AREAS";
		String modelID = null;
		String resultFormat = "JSON";
	
		@Override
		public String toString() {
			Gson gson = new Gson();
			return gson.toJson(this);
		}
	
	}
	
	class TopologyRequestProcess extends Thread {
	
		String fieldModelMrid;
		Client client;
		DataManager dataManager;
		SecurityConfig securityConfig;
		Root root = null;
		Map<String, List<String>> messageBus_measIds_map = new HashMap<String, List<String>>();
		Map<String, String> measId_messageBus_map = new HashMap<String, String>();
	
		public TopologyRequestProcess(String fieldModelMrid, Client client, DataManager dataManager,
				SecurityConfig securityConfig) {
			this.fieldModelMrid = fieldModelMrid;
			this.client = client;
			this.dataManager = dataManager;
			this.securityConfig = securityConfig;
		}
	
		@Override
		public void run() {
			try {
				Gson gson = new Gson();
				TopologyRequest request = new TopologyRequest();
				request.modelID = fieldModelMrid;
				Serializable topoResponse = client.getResponse(request.toString(), "goss.gridappsd.request.data.topology",
						RESPONSE_FORMAT.JSON);
				int attempt = 1;
				if (topoResponse == null && attempt < 6) {
					// May have to wait for Topology processor to initialize
					topoResponse = client.getResponse(request.toString(), "goss.gridappsd.request.data.topology",
							RESPONSE_FORMAT.JSON);
					Thread.sleep(1000);
					attempt++;
				}
				if (topoResponse != null && (topoResponse instanceof DataResponse)) {
					String str = ((DataResponse) topoResponse).getData().toString();
					root = gson.fromJson(str, Root.class);
				} else {
					root = gson.fromJson(topoResponse.toString(), Root.class);
				}
	
				/*
				 * feederList = root.feeders; if(root == null || feederList == null
				 * || feederList.size() == 0){ throw new
				 * Exception("No Feeder available to create field message bus"); }
				 */
	
				root.feeders.message_bus_id = root.feeders.feeder_id;
				int switch_area_index = 0;
				for (SwitchArea switchArea : root.feeders.switch_areas) {
					switchArea.message_bus_id = root.feeders.feeder_id + "." + switch_area_index;
					int secondary_area_index = 0;
					for (SecondaryArea secondaryArea : switchArea.secondary_areas) {
						secondaryArea.message_bus_id = root.feeders.feeder_id + "." + switch_area_index + "."
								+ secondary_area_index;
						secondary_area_index++;
					}
					switch_area_index++;
				}
	
				this.getFieldMeasurementIds(fieldModelMrid);
	
			} catch (Exception e) {
				e.printStackTrace();
			}
	
		}
	
		public void getFieldMeasurementIds(String fieldModelMrid) {
	
			PowergridModelDataRequest request = new PowergridModelDataRequest();
			request.modelId = fieldModelMrid;
			request.requestType = PowergridModelDataRequest.RequestType.QUERY_OBJECT_MEASUREMENTS.toString();
			Response response = null;
			List<String> measurementList = new ArrayList<String>();
	
			try {
	
				// Get Feeder level measurement ids
				for (String equipmentId : root.feeders.addressable_equipment) {
					request.objectId = equipmentId;
					response = dataManager.processDataRequest(request, "powergridmodel", null, null,
							securityConfig.getManagerUser());
					if (response != null && (response instanceof DataResponse)) {
						String str = ((DataResponse) response).getData().toString();
						JSONArray array = new JSONArray(str);
						measurementList.clear();
						for (int i = 0; i < array.length(); i++) {
							JSONObject object = array.getJSONObject(i);
							String measid = object.getString("measid");
							measId_messageBus_map.put(measid, root.feeders.message_bus_id);
							measurementList.add(measid);
						}
						messageBus_measIds_map.put(root.feeders.message_bus_id, measurementList);
					}
	
				}
	
				// Get switch level measurement ids
				for (SwitchArea switchArea : root.feeders.switch_areas) {
	
					for (String equipmentId : switchArea.addressable_equipment) {
						request.objectId = equipmentId;
						response = dataManager.processDataRequest(request, "powergridmodel", null, null,
								securityConfig.getManagerUser());
						if (response != null && (response instanceof DataResponse)) {
							String str = ((DataResponse) response).getData().toString();
							JSONArray array = new JSONArray(str);
							measurementList.clear();
							for (int i = 0; i < array.length(); i++) {
								JSONObject object = array.getJSONObject(i);
								String measid = object.getString("measid");
								measId_messageBus_map.put(measid, switchArea.message_bus_id);
								measurementList.add(measid);
							}
							messageBus_measIds_map.put(switchArea.message_bus_id, measurementList);
						}
					}
	
					// Get Secondary level measurement ids
					for (SecondaryArea secondaryArea : switchArea.secondary_areas) {
						for (String equipmentid : secondaryArea.addressable_equipment) {
							request.objectId = equipmentid;
							response = dataManager.processDataRequest(request, "powergridmodel", null, null,
									securityConfig.getManagerUser());
							if (response != null && (response instanceof DataResponse)) {
								String str = ((DataResponse) response).getData().toString();
								JSONArray array = new JSONArray(str);
								measurementList.clear();
								for (int i = 0; i < array.length(); i++) {
									JSONObject object = array.getJSONObject(i);
									String measid = object.getString("measid");
									measId_messageBus_map.put(measid, secondaryArea.message_bus_id);
									measurementList.add(measid);
								}
								messageBus_measIds_map.put(secondaryArea.message_bus_id, measurementList);
							}
						}
					}
	
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
	
		}
	
	}