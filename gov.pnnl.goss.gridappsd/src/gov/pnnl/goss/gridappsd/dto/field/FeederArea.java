package gov.pnnl.goss.gridappsd.dto.field;

import java.io.Serializable;
import java.util.ArrayList;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

public class FeederArea implements Serializable {

    private static final long serialVersionUID = 1L;

    @SerializedName("@id")
    public String id;

    @SerializedName("@type")
    public String type;

    public ArrayList<FieldObject> BoundaryTerminals;
    public ArrayList<FieldObject> AddressableEquipment;
    public ArrayList<FieldObject> UnaddressableEquipment;
    public ArrayList<FieldObject> Measurements;
    public ArrayList<SwitchArea> SwitchAreas;

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

}
