package gov.pnnl.goss.gridappsd.dto;

import java.io.Serializable;
import java.util.Properties;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class ConfigurationRequest implements Serializable {

    private static final long serialVersionUID = -3277794171736103832L;

    public ConfigurationRequest() {
    }

    // Expected to match RequestType enum
    public String configurationType;
    Properties parameters;

    public String getConfigurationType() {
        return configurationType;
    }

    public void setConfigurationType(String configurationType) {
        this.configurationType = configurationType;
    }

    public Properties getParameters() {
        return parameters;
    }

    public void setParameters(Properties parameters) {
        this.parameters = parameters;
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public static ConfigurationRequest parse(String jsonString) {
        Gson gson = new Gson();
        ConfigurationRequest obj = gson.fromJson(jsonString, ConfigurationRequest.class);
        if (obj.configurationType == null)
            throw new JsonSyntaxException("Expected attribute configurationType not found: " + jsonString);
        return obj;
    }
}
