package gov.pnnl.goss.gridappsd.dto.events;

import java.io.Serializable;

public class ObjectMridAttributeMap implements Serializable {

    private static final long serialVersionUID = 1L;

    String objectMRID;
    String attribute;

    public ObjectMridAttributeMap() {
        super();
    }

    public ObjectMridAttributeMap(String objectMRID, String attribute) {
        super();
        this.objectMRID = objectMRID;
        this.attribute = attribute;
    }

    public String getObjectMRID() {
        return objectMRID;
    }

    public void setObjectMRID(String objectMRID) {
        this.objectMRID = objectMRID;
    }

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

}
