package gov.pnnl.goss.gridappsd.dto;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class RequestTimeseriesDataBasic extends RequestTimeseriesData {

    private static final long serialVersionUID = -820277813503252513L;

    Map<String, Object> queryFilter;

    public Map<String, Object> getQueryFilter() {
        return queryFilter;
    }

    public void setQueryFilter(Map<String, Object> queryFilter) {
        this.queryFilter = queryFilter;
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public static RequestTimeseriesDataBasic parse(String jsonString) {
        ObjectMapper objectMapper = new ObjectMapper();
        RequestTimeseriesDataBasic obj = null;
        String error = "";
        try {
            obj = objectMapper.readValue(jsonString, RequestTimeseriesDataBasic.class);
        } catch (JsonParseException e) {
            error = ExceptionUtils.getStackTrace(e);
        } catch (JsonMappingException e) {
            error = ExceptionUtils.getStackTrace(e);
        } catch (IOException e) {
            error = ExceptionUtils.getStackTrace(e);
        }
        if (obj == null) {
            throw new JsonSyntaxException("Request time series data request could not be parsed: " + error);
        }
        // if(obj!=null && obj.queryMeasurement.equals("simulation"))
        // if(obj.queryFilter==null || !obj.queryFilter.containsKey("simulation_id"))
        // throw new JsonSyntaxException("Expected filter simulation_id not found.");
        return obj;
    }

}
