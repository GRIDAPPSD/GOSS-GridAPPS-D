package gov.pnnl.goss.gridappsd.data.conversion;

import java.io.InputStream;
import java.io.PrintWriter;

public interface DataFormatConverter {

	public void convert(String inputContent, PrintWriter outputContent) throws Exception;
	public void convert(InputStream inputContent, PrintWriter outputContent) throws Exception;
}
