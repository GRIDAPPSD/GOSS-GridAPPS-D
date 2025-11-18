package gov.pnnl.goss.gridappsd.dto;

import java.io.Serializable;
import java.util.List;

import com.google.gson.Gson;

public class YBusExportResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    List<String> yParse;
    List<String> nodeList;
    List<String> summary;
    List<String> vnom;

    public List<String> getyParse() {
        return yParse;
    }

    public void setyParse(List<String> yParse) {
        this.yParse = yParse;
    }

    public List<String> getNodeList() {
        return nodeList;
    }

    public void setNodeList(List<String> nodeList) {
        this.nodeList = nodeList;
    }

    public List<String> getSummary() {
        return summary;
    }

    public void setSummary(List<String> summary) {
        this.summary = summary;
    }

    public List<String> getVnom() {
        return vnom;
    }

    public void setVnom(List<String> vnom) {
        this.vnom = vnom;
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

}
