package com.midco.rota.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/**
 * Desired full state of a service's shift templates, as edited in the grid builder
 * (rows = shift types, columns = days). The bulk endpoint reconciles the service's
 * templates to match this: create newly-ticked (type, day) cells, reactivate/update
 * existing ones, deactivate cells that were unticked.
 */
public class ShiftTemplateBulkRequest {

    private String location;
    private String region;
    private List<Row> rows;

    public static class Row {
        private String shiftType;          // shift-type CODE
        private List<DayOfWeek> days;      // ticked days
        private LocalTime startTime;
        private LocalTime endTime;
        private Integer empCount;

        public String getShiftType() { return shiftType; }
        public void setShiftType(String shiftType) { this.shiftType = shiftType; }
        public List<DayOfWeek> getDays() { return days; }
        public void setDays(List<DayOfWeek> days) { this.days = days; }
        public LocalTime getStartTime() { return startTime; }
        public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
        public LocalTime getEndTime() { return endTime; }
        public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
        public Integer getEmpCount() { return empCount; }
        public void setEmpCount(Integer empCount) { this.empCount = empCount; }
    }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public List<Row> getRows() { return rows; }
    public void setRows(List<Row> rows) { this.rows = rows; }
}
