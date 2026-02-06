package gov.pnnl.goss.gridappsd.api;

import java.io.Serializable;

import gov.pnnl.goss.gridappsd.dto.RequestTimeseriesData;
import gov.pnnl.goss.gridappsd.dto.SimulationContext;

public interface TimeseriesDataManager {
    // public enum ResultFormat {
    // JSON, XML, CSV
    // }

    Serializable query(RequestTimeseriesData requestTimeseriesData) throws Exception;

    void storeSimulationOutput(String simulationId) throws Exception;

    void storeSimulationInput(String simulationId) throws Exception;

    void storeServiceOutput(String simulationId, String serviceId,
            String instanceId) throws Exception;

    void storeServiceInput(String simulationId, String serviceId,
            String instanceId) throws Exception;

    void storeAppOutput(String simulationId, String appId, String instanceId)
            throws Exception;

    void storeAppInput(String simulationId, String appId, String instanceId)
            throws Exception;

    void storeAllData(SimulationContext simulationContext) throws Exception;

}
