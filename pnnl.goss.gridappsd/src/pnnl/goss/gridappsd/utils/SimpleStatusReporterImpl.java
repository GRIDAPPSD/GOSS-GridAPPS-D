package pnnl.goss.gridappsd.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pnnl.goss.gridappsd.api.StatusReporter;

public class SimpleStatusReporterImpl implements StatusReporter {
	private static Logger log = LoggerFactory.getLogger(StatusReporterImpl.class);

	
	
	@Override
	public void reportStatus(String status) {
		log.info(status);

	}

	@Override
	public void reportStatus(String topic, String status) throws Exception {
		log.info(topic+":"+status);

	}

}
