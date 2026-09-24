package gov.pnnl.goss.gridappsd.dto.field;

import java.io.Serializable;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class AgentDetails implements Serializable {

    private static final long serialVersionUID = 1L;

    public String agent_id;
    public String app_id;
    public String description;
    public String upstream_message_bus_id;
    public String downstream_message_bus_id;

    public static AgentDetails parse(String jsonString) throws JsonSyntaxException {
        Gson gson = new Gson();
        AgentDetails obj = gson.fromJson(jsonString, AgentDetails.class);
        return obj;
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

}
