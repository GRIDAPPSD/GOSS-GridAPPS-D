package gov.pnnl.goss.gridappsd.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

public class DifferenceMessage implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = -1387553095619324553L;

    public long timestamp;

    public String difference_mrid;

    public List<Object> forward_differences = new ArrayList<Object>();

    public List<Object> reverse_differences = new ArrayList<Object>();

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public JsonElement toJsonElement() {
        Gson gson = new Gson();
        return gson.toJsonTree(this);
    }

    public static DifferenceMessage parse(String jsonString) {
        Gson gson = new Gson();
        DifferenceMessage obj = gson.fromJson(jsonString, DifferenceMessage.class);
        if (obj.difference_mrid == null)
            throw new RuntimeException("Expected attribute difference_mrid not found");
        return obj;
    }
}
