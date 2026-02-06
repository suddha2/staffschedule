package com.midco.rota.dto;

import java.time.LocalTime;

import com.midco.rota.util.ShiftType;

public class ConflictingShiftDTO {
    private String location;
    private ShiftType shiftType;
    private LocalTime startTime;
    private LocalTime endTime;

    public ConflictingShiftDTO() {
    }

    public ConflictingShiftDTO(String location, ShiftType shiftType, LocalTime startTime, LocalTime endTime) {
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

    public ShiftType getShiftType() {
        return shiftType;
    }

    public void setShiftType(ShiftType shiftType) {
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
        private ShiftType shiftType;
        private LocalTime startTime;
        private LocalTime endTime;

        public Builder location(String location) {
            this.location = location;
            return this;
        }

        public Builder shiftType(ShiftType shiftType) {
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