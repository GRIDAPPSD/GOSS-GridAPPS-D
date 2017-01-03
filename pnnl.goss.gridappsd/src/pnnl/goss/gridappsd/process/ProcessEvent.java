package pnnl.goss.gridappsd.process;

import java.io.Serializable;

import pnnl.goss.core.GossResponseEvent;

public class ProcessEvent implements GossResponseEvent {
	
	@Override
	public void onMessage(Serializable message) {
		
		System.out.println(message);
		
		
	}
	
}
