package gov.pnnl.goss.gridappsd.api;

import java.io.Serializable;
import java.util.List;

import gov.pnnl.goss.gridappsd.dto.field.AgentDetails;

public interface FieldBusManager {

    public Serializable handleRequest(String requestQueue, Serializable request);

    public String getFieldModelMrid();

    public List<AgentDetails> getFieldAgents();

}
