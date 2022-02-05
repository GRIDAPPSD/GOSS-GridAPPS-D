package gov.pnnl.goss.gridappsd.distributed;

import java.io.FileReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.felix.dm.annotation.api.Component;
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
import gov.pnnl.goss.gridappsd.api.PowergridModelDataManager;
import gov.pnnl.goss.gridappsd.dto.PowergridModelDataRequest;
import gov.pnnl.goss.gridappsd.dto.SimulationOutput;
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
import pnnl.goss.core.Response;
import pnnl.goss.core.security.SecurityConfig;



@Component
public class FieldBusManagerImpl implements FieldBusManager{
	
	String topology_reponse;
	String topicPrefix = "goss.gridappsd.process.request.field";
	String topicContext = topicPrefix+".context";
	
	@ServiceDependency
	ClientFactory clientFactory;
	
	@ServiceDependency 
	private volatile DataManager dataManager;
	
	@ServiceDependency
	SecurityConfig securityConfig;
	
	@ServiceDependency
	PowergridModelDataManager powergridModelDataManager;
	
	Client client;
	
	Root root = null;
	List<Feeder> feederList = null;
	Map<String,List<String>> messageBus_measIds_map = new HashMap<String, List<String>>();
	Map<String,String> measId_messageBus_map = new HashMap<String, String>();
	
	
	public FieldBusManagerImpl(){
		System.out.println("Creating FieldBusManager");
	}
	

	@Start
	public void start(){
		
		//TODO: Send request to TopologyProcessor to get feeder, switch areas and secondary devices
		//Throw error if no response or incorrect response
		Gson gson = new Gson();
		try {
			root = gson.fromJson(new FileReader("output_message.json"), Root.class);
			
			/*feederList = root.feeders;
			if(root == null || feederList == null || feederList.size() == 0){
				throw new Exception("No Feeder available to create field message bus");
			}*/	
			
			root.feeders.message_bus_id = root.feeders.feeder_id;
			int switch_area_index = 0;
			for(SwitchArea switchArea : root.feeders.switch_areas){
				switchArea.message_bus_id = root.feeders.feeder_id+"."+switch_area_index;
				switch_area_index++;
				int secondary_area_index = 0;
				for(SecondaryArea secondaryArea : switchArea.secondary_areas){
					secondaryArea.message_bus_id = root.feeders.feeder_id+"."+switch_area_index+"."+secondary_area_index;
					secondary_area_index++;
				}
			}
			
			this.getFieldMeasurementIds();
			
			Credentials credentials = new UsernamePasswordCredentials(
					securityConfig.getManagerUser(), securityConfig.getManagerPassword());
			client = clientFactory.create(PROTOCOL.STOMP, credentials, true);
			
			this.publishDeviceOutput();
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		  
	}
	
	@Override
	public Serializable handleRequest(String request_queue, Serializable request){
		
		
		Feeder responseFeeder = null;
		
		if(request_queue.endsWith("context")){
			
			RequestFieldContext requestFieldContext = RequestFieldContext.parse(request.toString());
			/*System.out.println(request.toString());
			for(Feeder feeder : feederList){
				if(feeder.feeder_id.equals(requestFieldContext.modelId))
					responseFeeder =  feeder;
			}*/
			
			if(requestFieldContext.areaId == null)
				return root.feeders;
			else{
				for(SwitchArea switchArea : root.feeders.switch_areas){
					if(requestFieldContext.areaId.equals(switchArea.message_bus_id))
						return switchArea;
					for(SecondaryArea secondaryArea : switchArea.secondary_areas){
						if(requestFieldContext.areaId.equals(secondaryArea.message_bus_id))
							return secondaryArea;
					}
				}
			}		
			
		}
		
		return responseFeeder;
	}

	public List<Feeder> processFieldRequest(String feederMRID){
		//TODO: Use volttron python api to create buses dynamically
		//Maybe Redis bus or volttron with openfmb messages  (protoBuf instead of JSON )
		return feederList;
	}
	
	public void getFieldMeasurementIds(){
		
		PowergridModelDataRequest request = new PowergridModelDataRequest();
		request.modelId = "_49AD8E07-3BF9-A4E2-CB8F-C3722F837B62";
		request.requestType = PowergridModelDataRequest.RequestType.QUERY_OBJECT_MEASUREMENTS.toString();
		Response response = null;
		List<String> measurementList = new ArrayList<String>();
		
		
		
		try{
			
			// Get Feeder level measurement ids
			for(String equipmentId : root.feeders.addressable_equipment){
				request.objectId = equipmentId;
				response = dataManager.processDataRequest(request, "powergridmodel", null, null, securityConfig.getManagerUser());				
				if(response!=null && (response instanceof DataResponse)){
					String str = ((DataResponse)response).getData().toString();
					JSONArray array = new JSONArray(str);
					measurementList.clear();
					for(int i=0; i < array.length(); i++)   
					{  
						JSONObject object = array.getJSONObject(i);  
						String measid = object.getString("measid"); 
						//System.out.println("Creaint Map +++++++++++++++++++++++++++++++"+measid+"::::::"+root.feeders.message_bus_id);
						measId_messageBus_map.put(measid, root.feeders.message_bus_id);
						measurementList.add(measid);
					}  
					messageBus_measIds_map.put(root.feeders.message_bus_id, measurementList);
				}
				
			}
			
			
			//Get switch level measurement ids
			for(SwitchArea switchArea : root.feeders.switch_areas){
				for(String equipmentId : switchArea.addressable_equipment){
					request.objectId = equipmentId;
					response = dataManager.processDataRequest(request, "powergridmodel", null, null, securityConfig.getManagerUser());				
					if(response!=null && (response instanceof DataResponse)){
						String str = ((DataResponse)response).getData().toString();
						JSONArray array = new JSONArray(str);  
						measurementList.clear();
						for(int i=0; i < array.length(); i++)   
						{  
							JSONObject object = array.getJSONObject(i);  
							String measid = object.getString("measid"); 
							measId_messageBus_map.put(measid, switchArea.message_bus_id);
							//System.out.println("Creaint Map +++++++++++++++++++++++++++++++"+measid+"::::::"+switchArea.message_bus_id);
							
							measurementList.add(measid);
						}  
						messageBus_measIds_map.put(switchArea.message_bus_id, measurementList);
					}
					
					//Get Secondary level measurement ids
					for(SecondaryArea secondaryArea : switchArea.secondary_areas){
						for(String equipmentid : secondaryArea.addressable_equipment){
							request.objectId = equipmentid;
							response = dataManager.processDataRequest(request, "powergridmodel", null, null, securityConfig.getManagerUser());				
							if(response!=null && (response instanceof DataResponse)){
								String str = ((DataResponse)response).getData().toString();
								JSONArray array = new JSONArray(str);  
								measurementList.clear();
								for(int i=0; i < array.length(); i++)   
								{  
									JSONObject object = array.getJSONObject(i);  
									String measid = object.getString("measid");  
									//System.out.println("Creaint Map +++++++++++++++++++++++++++++++"+measid+"::::::"+secondaryArea.message_bus_id);
									measId_messageBus_map.put(measid, secondaryArea.message_bus_id);
									measurementList.add(measid);
								}  
								messageBus_measIds_map.put(secondaryArea.message_bus_id, measurementList);
							}
						}
					}
				}
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
		
	}
	
	public void publishDeviceOutput(){
		
		
		client.subscribe(GridAppsDConstants.topic_simulationOutput+".>", new GossResponseEvent() {
			
			@Override
			public void onMessage(Serializable response) {
				
				DataResponse event = (DataResponse) response;
				String simulationId = event.getDestination().substring(event.getDestination().lastIndexOf(".")+1,event.getDestination().length());
				String simOutputStr = event.getData().toString();
				
				JsonObject simOutputJsonObj = null;
				
				JsonParser parser = new JsonParser();
				JsonElement simOutputObject = parser.parse(simOutputStr);
				
		
				if (simOutputObject.isJsonObject()) {
					simOutputJsonObj = simOutputObject.getAsJsonObject();
				}
		
				//String simulationTimestamp = simOutputJsonObj.getAsJsonObject().get("message").getAsJsonObject().get("timestamp").getAsString();
				
				JsonObject tempObj = simOutputJsonObj.getAsJsonObject("message");
				Map<String, JsonElement> expectedOutputMap = tempObj.getAsJsonObject("measurements").entrySet().stream()
							.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()));
				
				for(String measurementMrid : expectedOutputMap.keySet()){
					
					String messageBusId = null;
					if(measId_messageBus_map.get(measurementMrid) != null)
						messageBusId = "goss.gridappsd.simulation.output."+simulationId+"."+measId_messageBus_map.get(measurementMrid);
					else 
						messageBusId = "goss.gridappsd.simulation.output."+simulationId+"."+root.feeders.message_bus_id;
					
					JsonObject obj = new JsonObject();
					obj.add(measurementMrid, expectedOutputMap.get(measurementMrid));
					
					//System.out.println("Sending measurements to "+messageBusId);
					//System.out.println(obj.toString().substring(0, 100));
					client.publish(messageBusId, obj.toString());
				}

				
			}
			
		});
		
		
		
		
	}
	
	
	/*public static void main(String[] args){
		new FieldBusManagerImpl().start();

	}*/

}
