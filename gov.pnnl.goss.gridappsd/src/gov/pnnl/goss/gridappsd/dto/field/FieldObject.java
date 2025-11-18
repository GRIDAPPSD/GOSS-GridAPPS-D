package gov.pnnl.goss.gridappsd.dto.field;

import java.io.Serializable;

import com.google.gson.annotations.SerializedName;

public class FieldObject implements Serializable {

    private static final long serialVersionUID = 1L;

    @SerializedName("@id")
    public String id;

    @SerializedName("@type")
    public String type;

}
