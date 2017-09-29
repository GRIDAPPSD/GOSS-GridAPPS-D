package gov.pnnl.goss.gridappsd.data;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Start;

import gov.pnnl.goss.gridappsd.api.LogDataManager;
import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;


@Component
public class LogDataManagerImpl implements LogDataManager {
	
	@Start
	public void start(){
	}

	@Override
	public void store(String process_id, String username, long timestamp, String log_message, LogLevel log_level,
			ProcessStatus process_status) {
		// TODO Auto-generated method stub

	}

	@Override
	public void query(String process_id, long timestamp, LogLevel log_level, ProcessStatus process_status, String username) {
		// TODO Auto-generated method stub

	}

}
