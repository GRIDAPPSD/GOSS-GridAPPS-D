package gov.pnnl.goss.gridappsd.dto;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class TimeSeriesEntryResult {
    ArrayList<HashMap<String, Object>> data;

    public ArrayList<HashMap<String, Object>> getData() {
        if (data == null) {
            data = new ArrayList<HashMap<String, Object>>();
        }
        return data;
    }

    public void setData(ArrayList<HashMap<String, Object>> data) {
        this.data = data;
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public static TimeSeriesEntryResult parse(String jsonString) {
        Type listType = new TypeToken<ArrayList<HashMap<String, Object>>>() {
        }.getType();
        Gson gson = new Gson();
        TimeSeriesEntryResult obj = new TimeSeriesEntryResult();
        ArrayList<HashMap<String, Object>> data = gson.fromJson(jsonString, listType);
        obj.setData(data);
        if (obj.data == null)
            throw new JsonSyntaxException("Expected attribute measurements not found");
        return obj;
    }

}
