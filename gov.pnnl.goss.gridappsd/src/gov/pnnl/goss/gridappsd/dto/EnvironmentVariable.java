package gov.pnnl.goss.gridappsd.dto;

import java.io.Serializable;

public class EnvironmentVariable implements Serializable {

    String envName;
    String envValue;

    public String getEnvName() {
        return envName;
    }

    public void setEnvName(String envName) {
        this.envName = envName;
    }

    public String getEnvValue() {
        return envValue;
    }

    public void setEnvValue(String envValue) {
        this.envValue = envValue;
    }

}
