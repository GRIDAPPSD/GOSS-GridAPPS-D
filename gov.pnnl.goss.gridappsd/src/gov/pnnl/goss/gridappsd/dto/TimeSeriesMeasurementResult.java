package gov.pnnl.goss.gridappsd.dto;

import java.util.List;

public class TimeSeriesMeasurementResult {
    String name;
    List<TimeSeriesRowResult> points;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<TimeSeriesRowResult> getPoints() {
        return points;
    }

    public void setPoints(List<TimeSeriesRowResult> points) {
        this.points = points;
    }
}
