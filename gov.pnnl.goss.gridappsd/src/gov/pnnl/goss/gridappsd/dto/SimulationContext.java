package gov.pnnl.goss.gridappsd.dto;

import java.io.Serializable;
import java.util.List;


public class SimulationContext implements Serializable {

	private static final long serialVersionUID = 1L;

	public String simulationId;
	public String simulationHost = "127.0.0.1";
	public int simulationPort;
	public String simulationDir;
	public String startupFile;
	public RequestSimulation request;
	public String simulatorPath;
	public List<String> appInstanceIds;
	public List<String> serviceInstanceIds;
	public String simulationUser;
	public int numFederates = 2;

	public String getSimulationId() {
		return simulationId;
	}

	public void setSimulationId(String simulationId) {
		this.simulationId = simulationId;
	}

	public String getHost() {
		return simulationHost;
	}

	public void setHost(String host) {
		this.simulationHost = host;
	}

	public int getPort() {
		return simulationPort;
	}

	public void setPort(int port) {
		this.simulationPort = port;
	}

	public String getSimulationDir() {
		return simulationDir;
	}

	public void setSimulationDir(String simulationDir) {
		this.simulationDir = simulationDir;
	}

	public String getStartupFile() {
		return startupFile;
	}

	public void setStartupFile(String startupFile) {
		this.startupFile = startupFile;
	}

	public RequestSimulation getRequest() {
		return request;
	}

	public void setRequest(RequestSimulation request) {
		this.request = request;
	}

	public String getSimulatorPath() {
		return simulatorPath;
	}

	public void setSimulatorPath(String simulatorPath) {
		this.simulatorPath = simulatorPath;
	}

	public List<String> getAppInstanceIds() {
		return appInstanceIds;
	}

	public void setAppInstanceIds(List<String> appInstanceIds) {
		this.appInstanceIds = appInstanceIds;
	}

	public List<String> getServiceInstanceIds() {
		return serviceInstanceIds;
	}

	public void setServiceInstanceIds(List<String> serviceInstanceIds) {
		this.serviceInstanceIds = serviceInstanceIds;
	}
	
	public void addServiceInstanceIds(String serviceInstanceId) {
		this.serviceInstanceIds.add(serviceInstanceId);
	}

	public String getSimulationUser() {
		return simulationUser;
	}

	public void setSimulationUser(String simulationUser) {
		this.simulationUser = simulationUser;
	}

	public int getNumFederates() {
		return numFederates;
	}

	public void setNumFederates(int numFederates) {
		this.numFederates = numFederates;
	}

	

}
