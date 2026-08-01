package gov.pnnl.goss.gridappsd.dto.field;

import java.io.Serializable;

import com.google.gson.Gson;

public class Root implements Serializable {

    private static final long serialVersionUID = 1L;

    public DistributionArea DistributionArea;

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

}
