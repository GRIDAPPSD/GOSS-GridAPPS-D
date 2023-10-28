package gov.pnnl.goss.gridappsd.data;

import java.io.Serializable;

import gov.pnnl.goss.gridappsd.api.DataManagerHandler;
import gov.pnnl.goss.gridappsd.api.PowergridModelDataManager;
import gov.pnnl.goss.gridappsd.api.PowergridModelDataManager.ResultFormat;
import gov.pnnl.goss.gridappsd.dto.PowergridModelDataRequest;

public class BGPowergridModelDataManagerHandlerImpl implements DataManagerHandler {

	PowergridModelDataManager dataManager;
	
	public BGPowergridModelDataManagerHandlerImpl(PowergridModelDataManager dataManager) {
		this.dataManager = dataManager;
	}
	
	
	@Override
	public Serializable handle(Serializable requestContent, String processId, String username) throws Exception {
		PowergridModelDataRequest pgDataRequest = null;
		if(requestContent instanceof PowergridModelDataRequest){
			pgDataRequest = (PowergridModelDataRequest)requestContent;
		} else {
			pgDataRequest = PowergridModelDataRequest.parse(requestContent.toString());
		}
		
		if(PowergridModelDataRequest.RequestType.QUERY.toString().equals(pgDataRequest.requestType)){
			if (pgDataRequest.getQueryString()==null || !verifyResultFormat(pgDataRequest.getResultFormat())){
				//TODO send error
			}
			return dataManager.query(pgDataRequest.getModelId(), pgDataRequest.getQueryString(), pgDataRequest.getResultFormat(), processId, username);
		} else if(PowergridModelDataRequest.RequestType.QUERY_MODEL.toString().equals(pgDataRequest.requestType)){
			if (pgDataRequest.getModelId()==null || !verifyResultFormat(pgDataRequest.getResultFormat())){
				//TODO send error
			}
			return dataManager.queryModel(pgDataRequest.getModelId(), pgDataRequest.getObjectType(), pgDataRequest.getFilter(), pgDataRequest.getResultFormat(), processId, username);
		} else if(PowergridModelDataRequest.RequestType.QUERY_MODEL_NAMES.toString().equals(pgDataRequest.requestType)){
			if (!verifyResultFormat(pgDataRequest.getResultFormat())){
				//TODO send error
			}
			return dataManager.queryModelNames(pgDataRequest.getResultFormat(), processId, username);
		} else if(PowergridModelDataRequest.RequestType.QUERY_MODEL_INFO.toString().equals(pgDataRequest.requestType)){
			if (!verifyResultFormat(pgDataRequest.getResultFormat())){
				//TODO send error
			}
			return dataManager.queryModelNamesAndIds(pgDataRequest.getResultFormat(), processId, username);
		} else if(PowergridModelDataRequest.RequestType.QUERY_OBJECT.toString().equals(pgDataRequest.requestType)){
			if (pgDataRequest.getModelId()==null || pgDataRequest.getObjectId()==null || !verifyResultFormat(pgDataRequest.getResultFormat())){
				//TODO send error
			}
			return dataManager.queryObject(pgDataRequest.getModelId(), pgDataRequest.getObjectId(), pgDataRequest.getResultFormat(), processId, username);
		} else if(PowergridModelDataRequest.RequestType.QUERY_OBJECT_TYPES.toString().equals(pgDataRequest.requestType)){
			if (pgDataRequest.getModelId()==null || !verifyResultFormat(pgDataRequest.getResultFormat())){
				//TODO send error
			}
			return dataManager.queryObjectTypes(pgDataRequest.getModelId(), pgDataRequest.getResultFormat(), processId, username);
		} else if(PowergridModelDataRequest.RequestType.QUERY_OBJECT_IDS.toString().equals(pgDataRequest.requestType)){
			if (pgDataRequest.getModelId()==null || !verifyResultFormat(pgDataRequest.getResultFormat())){
				//TODO send error
			}
			return dataManager.queryObjectIds(pgDataRequest.getResultFormat(), pgDataRequest.getModelId(), pgDataRequest.getObjectType(), processId, username);
		} else if(PowergridModelDataRequest.RequestType.QUERY_OBJECT_DICT.toString().equals(pgDataRequest.requestType)){
			if (pgDataRequest.getModelId()==null || !verifyResultFormat(pgDataRequest.getResultFormat())){
				//TODO send error
			}
			return dataManager.queryObjectDictByType(pgDataRequest.getResultFormat(), pgDataRequest.getModelId(), pgDataRequest.getObjectType(), pgDataRequest.getObjectId(), processId, username);
		} else if(PowergridModelDataRequest.RequestType.QUERY_OBJECT_MEASUREMENTS.toString().equals(pgDataRequest.requestType)){
			if (pgDataRequest.getModelId()==null || !verifyResultFormat(pgDataRequest.getResultFormat())){
				//TODO send error
			}
			return dataManager.queryMeasurementDictByObject(pgDataRequest.getResultFormat(), pgDataRequest.getModelId(), pgDataRequest.getObjectType(), pgDataRequest.getObjectId(), processId, username);
		} else if(PowergridModelDataRequest.RequestType.INSERT_ALL_HOUSES.toString().equals(pgDataRequest.requestType)){
			
			dataManager.insertAllHouses(processId, username, pgDataRequest.getModelList());
			return true;
		} else if(PowergridModelDataRequest.RequestType.INSERT_HOUSES.toString().equals(pgDataRequest.requestType)){
			if ((pgDataRequest.getModelId()==null || pgDataRequest.getModelId().length()==0)  && (pgDataRequest.getModel().getModelId()==null || pgDataRequest.getModel().getModelId().length()==0)){
				return "{\"message\":\"No model provided\"}";
			}
			
			if((pgDataRequest.getModelId()!=null && pgDataRequest.getModelId().length()>0))
				dataManager.insertHouses(pgDataRequest.getModelId(), pgDataRequest.getModelName(), "3", 0, 1.0, processId, username);
			else{
				dataManager.insertHouses(pgDataRequest.getModel().getModelId(), pgDataRequest.getModel().getModelName(), pgDataRequest.getModel().getRegion(), pgDataRequest.getModel().getSeed(), pgDataRequest.getModel().getScale(), processId, username);
			}
			return true;
		} else if(PowergridModelDataRequest.RequestType.DROP_HOUSES.toString().equals(pgDataRequest.requestType)){
			if ((pgDataRequest.getModelId()==null || pgDataRequest.getModelId().length()==0)  && (pgDataRequest.getModel().getModelId()==null || pgDataRequest.getModel().getModelId().length()==0)){
				return "{\"message\":\"No model provided\"}";
			}
			
			if((pgDataRequest.getModelId()!=null && pgDataRequest.getModelId().length()>0))
				dataManager.dropHouses(pgDataRequest.getModelId(), pgDataRequest.getModelName(), processId, username);
			else{
				dataManager.dropHouses(pgDataRequest.getModel().getModelId(), pgDataRequest.getModel().getModelName(), processId, username);
			}
			return true;
		} else if(PowergridModelDataRequest.RequestType.DROP_ALL_HOUSES.toString().equals(pgDataRequest.requestType)){
			dataManager.dropAllHouses(processId, username, pgDataRequest.getModelList());
			return true;
		} else if(PowergridModelDataRequest.RequestType.INSERT_ALL_MEASURMENTS.toString().equals(pgDataRequest.requestType)){
			dataManager.insertAllMeasurements(processId, username, pgDataRequest.getModelList());
			return true;
		} else if(PowergridModelDataRequest.RequestType.INSERT_MEASUREMENTS.toString().equals(pgDataRequest.requestType)){
			if ((pgDataRequest.getModelId()==null || pgDataRequest.getModelId().length()==0)  && (pgDataRequest.getModel().getModelId()==null || pgDataRequest.getModel().getModelId().length()==0)){
				return "{\"message\":\"No model provided\"}";
			}
			
			if((pgDataRequest.getModelId()!=null && pgDataRequest.getModelId().length()>0))
				dataManager.insertMeasurements(pgDataRequest.getModelId(), pgDataRequest.getModelName(), processId, username);
			else{
				dataManager.insertMeasurements(pgDataRequest.getModel().getModelId(), pgDataRequest.getModel().getModelName(), processId, username);
			}
		
			return true;
		}  else if(PowergridModelDataRequest.RequestType.DROP_MEASUREMENTS.toString().equals(pgDataRequest.requestType)){
			if ((pgDataRequest.getModelId()==null || pgDataRequest.getModelId().length()==0)  && (pgDataRequest.getModel().getModelId()==null || pgDataRequest.getModel().getModelId().length()==0)){
				return "{\"message\":\"No model provided\"}";
			}
			
			if((pgDataRequest.getModelId()!=null && pgDataRequest.getModelId().length()>0))
				dataManager.dropMeasurements(pgDataRequest.getModelId(), pgDataRequest.getModelName(), processId, username);
			else{
				dataManager.dropMeasurements(pgDataRequest.getModel().getModelId(), pgDataRequest.getModel().getModelName(), processId, username);
			}
		
			return true;
		}  else if(PowergridModelDataRequest.RequestType.DROP_ALL_MEASUREMENTS.toString().equals(pgDataRequest.requestType)){
			dataManager.dropAllMeasurements(processId, username, pgDataRequest.getModelList());
		
			return true;
		}  else {
			//TODO report error, request type not recognized
			System.out.println("DOESNT RECOGNIZE REQUEST TYPE "+pgDataRequest.requestType);
		}
		
		return null;
	}

	
	boolean verifyResultFormat(String resultFormat) {
		return resultFormat!=null && (resultFormat.equals(ResultFormat.JSON) || resultFormat.equals(ResultFormat.XML));
	}
}
