package pnnl.goss.gridappsd.api;

/**
 * Interface for reporting status to a message bus.
 * 
 * @author D3M614
 *
 */
public interface StatusReporter {
	
	/**
	 * Allows a reporting of status with context embedded in the parameter.
	 * 
	 * @param status
	 */
	void reportStatus(String status);
	
	/**
	 * Report the status on a specific topic (message bus).
	 * 
	 * @param topioc
	 * @param status
	 */
	void reportStatus(String topic, String status);
	
}
