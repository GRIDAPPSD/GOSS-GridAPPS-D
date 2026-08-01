package gov.pnnl.goss.gridappsd.dto;

import java.io.Serializable;
import java.util.HashMap;

public class ServiceConfig implements Serializable {

    private static final long serialVersionUID = -2413334775260242364L;

    String id;
    HashMap<String, Object> user_options;
    Object user_value;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public HashMap<String, Object> getUser_options() {
        return user_options;
    }

    public void setUser_input(HashMap<String, Object> user_options) {
        this.user_options = user_options;
    }

}
