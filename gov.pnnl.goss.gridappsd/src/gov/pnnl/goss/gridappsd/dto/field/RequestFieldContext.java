package gov.pnnl.goss.gridappsd.dto.field;

import java.io.Serializable;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class RequestFieldContext implements Serializable {

    private static final long serialVersionUID = -8105887994597368008L;

    public String modelId;
    public String areaId = null;

    public static RequestFieldContext parse(String jsonString) throws JsonSyntaxException {
        Gson gson = new Gson();
        RequestFieldContext obj = gson.fromJson(jsonString, RequestFieldContext.class);
        return obj;
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

}
