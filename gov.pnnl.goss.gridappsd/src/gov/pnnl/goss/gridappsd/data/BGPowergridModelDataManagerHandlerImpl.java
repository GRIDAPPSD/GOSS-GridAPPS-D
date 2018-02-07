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
	public Serializable handle(Serializable requestContent) throws Exception {
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
			return dataManager.query(pgDataRequest.getModelId(), pgDataRequest.getQueryString(), pgDataRequest.getResultFormat());
		} else if(PowergridModelDataRequest.RequestType.QUERY_MODEL.toString().equals(pgDataRequest.requestType)){
			if (pgDataRequest.getModelId()==null || !verifyResultFormat(pgDataRequest.getResultFormat())){
				//TODO send error
			}
			return dataManager.queryModel(pgDataRequest.getModelId(), pgDataRequest.getObjectType(), pgDataRequest.getFilter(), pgDataRequest.getResultFormat());
		} else if(PowergridModelDataRequest.RequestType.QUERY_MODEL_NAMES.toString().equals(pgDataRequest.requestType)){
			if (!verifyResultFormat(pgDataRequest.getResultFormat())){
				//TODO send error
			}
			return dataManager.queryModelNames(pgDataRequest.getResultFormat());
		} else if(PowergridModelDataRequest.RequestType.QUERY_OBJECT.toString().equals(pgDataRequest.requestType)){
			if (pgDataRequest.getModelId()==null || pgDataRequest.getObjectID()==null || !verifyResultFormat(pgDataRequest.getResultFormat())){
				//TODO send error
			}
			return dataManager.queryObject(pgDataRequest.getModelId(), pgDataRequest.getObjectID(), pgDataRequest.getResultFormat());
		} else if(PowergridModelDataRequest.RequestType.QUERY_OBJECT_TYPES.toString().equals(pgDataRequest.requestType)){
			if (pgDataRequest.getModelId()==null || !verifyResultFormat(pgDataRequest.getResultFormat())){
				//TODO send error
			}
			return dataManager.queryObjectTypes(pgDataRequest.getModelId(), pgDataRequest.getResultFormat());
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
