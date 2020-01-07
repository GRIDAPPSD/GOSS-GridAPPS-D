package gov.pnnl.goss.gridappsd.data.conversion;

import gov.pnnl.goss.gridappsd.dto.RequestTimeseriesData;

import java.io.InputStream;
import java.io.PrintWriter;

public interface DataFormatConverter {

	public void convert(String inputContent, PrintWriter outputContent, RequestTimeseriesData request) throws Exception;
	public void convert(InputStream inputContent, PrintWriter outputContent, RequestTimeseriesData request) throws Exception;
}
