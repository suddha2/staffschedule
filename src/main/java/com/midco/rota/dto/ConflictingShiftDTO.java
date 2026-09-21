package com.midco.rota.dto;

import java.time.LocalTime;

public class ConflictingShiftDTO {
    private String location;
    /** Data-driven shift-type CODE (e.g. LONG_DAY, SHIFT_LEAD). Was the ShiftType enum,
     *  which serialised null for new types that have no enum constant. */
    private String shiftType;
    private LocalTime startTime;
    private LocalTime endTime;

    public ConflictingShiftDTO() {
    }

    public ConflictingShiftDTO(String location, String shiftType, LocalTime startTime, LocalTime endTime) {
        this.location = location;
        this.shiftType = shiftType;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    // Getters and Setters
    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getShiftType() {
        return shiftType;
    }

    public void setShiftType(String shiftType) {
        this.shiftType = shiftType;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String location;
        private String shiftType;
        private LocalTime startTime;
        private LocalTime endTime;

        public Builder location(String location) {
            this.location = location;
            return this;
        }

        public Builder shiftType(String shiftType) {
            this.shiftType = shiftType;
            return this;
        }

        public Builder startTime(LocalTime localTime) {
            this.startTime = localTime;
            return this;
        }

        public Builder endTime(LocalTime endTime) {
            this.endTime = endTime;
            return this;
        }

        public ConflictingShiftDTO build() {
            return new ConflictingShiftDTO(location, shiftType, startTime, endTime);
        }
    }
}
