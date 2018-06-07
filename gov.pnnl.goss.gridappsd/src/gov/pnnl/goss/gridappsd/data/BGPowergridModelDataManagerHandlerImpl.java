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
		} else {
			//TODO report error, request type not recognized
			System.out.println("DOESNT RECOGNIZE REQUEST TYPE "+pgDataRequest.requestType);
		}
		
		return null;
	}

	
	boolean verifyResultFormat(String resultFormat) {
		return resultFormat!=null && (resultFormat.equals(ResultFormat.JSON) || resultFormat.equals(ResultFormat.XML));
	}
}
