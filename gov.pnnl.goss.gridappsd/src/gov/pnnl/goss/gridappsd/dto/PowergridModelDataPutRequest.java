package gov.pnnl.goss.gridappsd.dto;

import java.io.Serializable;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class PowergridModelDataPutRequest implements Serializable {
    public enum RequestType {
        PUT_MODEL
    }

    public enum ResultFormat {
        JSON, XML
    }

    public PowergridModelDataPutRequest() {
    }

    // Expected to match RequestType enum
    public String requestType;
    // For all except query model names
    public String modelId;
    // Expected to match ResultFormat enum
    public String inputFormat;

    // For query
    public String modelContent;

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public String getInputFormat() {
        return inputFormat;
    }

    public void setInputFormat(String inputFormat) {
        this.inputFormat = inputFormat;
    }

    public String getModelContent() {
        return modelContent;
    }

    public void setModelContent(String modelContent) {
        this.modelContent = modelContent;
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public static PowergridModelDataPutRequest parse(String jsonString) {
        Gson gson = new Gson();
        PowergridModelDataPutRequest obj = gson.fromJson(jsonString, PowergridModelDataPutRequest.class);
        if (obj.requestType == null)
            throw new JsonSyntaxException("Expected attribute requestType not found: " + jsonString);
        return obj;
    }
}
