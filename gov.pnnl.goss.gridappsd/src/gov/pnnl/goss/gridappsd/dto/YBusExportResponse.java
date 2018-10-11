package gov.pnnl.goss.gridappsd.dto;

import java.io.Serializable;
import java.util.List;

import com.google.gson.Gson;

public class YBusExportResponse implements Serializable {

	private static final long serialVersionUID = 1L;

	List<String> yParseFilePath;
	List<String> nodeListFilePath;
	List<String> summaryFilePath;
	
	public List<String> getyParseFilePath() {
		return yParseFilePath;
	}

	public void setyParseFilePath(List<String> yParseFilePath) {
		this.yParseFilePath = yParseFilePath;
	}

	public List<String> getNodeListFilePath() {
		return nodeListFilePath;
	}

	public void setNodeListFilePath(List<String> nodeListFilePath) {
		this.nodeListFilePath = nodeListFilePath;
	}

	public List<String> getSummaryFilePath() {
		return summaryFilePath;
	}

	public void setSummaryFilePath(List<String> summaryFilePath) {
		this.summaryFilePath = summaryFilePath;
	}

	@Override
	public String toString() {
		Gson gson = new Gson();
		return gson.toJson(this);
	}

}
