package gov.pnnl.goss.gridappsd.dto.events;

import java.io.Serializable;

import com.google.gson.Gson;

public abstract class BaseEventCommand implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = -1685736716041726260L;
    public String command;
    public Integer simulation_id;

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public static BaseEventCommand parse(String jsonString) {
        Gson gson = new Gson();
        BaseEventCommand obj = gson.fromJson(jsonString, BaseEventCommand.class);
        if (obj.command == null)
            throw new RuntimeException("Expected attribute object not found");
        return obj;
    }

}
