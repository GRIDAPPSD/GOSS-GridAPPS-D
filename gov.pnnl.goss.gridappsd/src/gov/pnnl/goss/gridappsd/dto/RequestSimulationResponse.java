package gov.pnnl.goss.gridappsd.dto;

import gov.pnnl.goss.gridappsd.dto.events.Event;

import java.io.Serializable;
import java.util.List;

public class RequestSimulationResponse implements Serializable {

	private static final long serialVersionUID = 6306119544266941758L;
	
	String simulationId;
	List<Event> events;
	public String getSimulationId() {
		return simulationId;
	}
	public void setSimulationId(String simulationId) {
		this.simulationId = simulationId;
	}
	public List<Event> getEvents() {
		return events;
	}
	public void setEvents(List<Event> events) {
		this.events = events;
	}
	
	

}
