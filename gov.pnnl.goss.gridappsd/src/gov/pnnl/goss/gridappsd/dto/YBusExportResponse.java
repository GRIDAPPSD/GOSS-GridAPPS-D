package gov.pnnl.goss.gridappsd.dto;

import java.io.Serializable;

import com.google.gson.Gson;

public class YBusExportResponse implements Serializable {

	private static final long serialVersionUID = 1L;

	String yParseFilePath;
	String nodeListFilePath;
	String summaryFilePath;

	public String getyParseFilePath() {
		return yParseFilePath;
	}

	public void setyParseFilePath(String yParseFilePath) {
		this.yParseFilePath = yParseFilePath;
	}

	public String getNodeListFilePath() {
		return nodeListFilePath;
	}

	public void setNodeListFilePath(String nodeListFilePath) {
		this.nodeListFilePath = nodeListFilePath;
	}

	public String getSummaryFilePath() {
		return summaryFilePath;
	}

	public void setSummaryFilePath(String summaryFilePath) {
		this.summaryFilePath = summaryFilePath;
	}

	@Override
	public String toString() {
		Gson gson = new Gson();
		return gson.toJson(this);
	}

}
