package com.midco.rota.dto;

import java.time.LocalDate;
import java.util.List;

public class ConflictError {
    private Long employeeId;
    private String employeeName;
    private LocalDate date;
    private List<ConflictingShiftDTO> conflictingShifts;
    /** Why this is a conflict, shown to the planner (e.g. "Wrong gender for this shift (needs MALE)"). */
    private String reason;
    /** A shift to highlight on the grid (the first conflicting shift). */
    private Long shiftId;

    public ConflictError() {
    }

    public ConflictError(Long employeeId, String employeeName, LocalDate date, List<ConflictingShiftDTO> conflictingShifts) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.date = date;
        this.conflictingShifts = conflictingShifts;
    }

    // Getters and Setters
    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public List<ConflictingShiftDTO> getConflictingShifts() {
        return conflictingShifts;
    }

    public void setConflictingShifts(List<ConflictingShiftDTO> conflictingShifts) {
        this.conflictingShifts = conflictingShifts;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Long getShiftId() {
        return shiftId;
    }

    public void setShiftId(Long shiftId) {
        this.shiftId = shiftId;
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long employeeId;
        private String employeeName;
        private LocalDate date;
        private List<ConflictingShiftDTO> conflictingShifts;
        private String reason;
        private Long shiftId;

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder shiftId(Long shiftId) {
            this.shiftId = shiftId;
            return this;
        }

        public Builder employeeId(Long employeeId) {
            this.employeeId = employeeId;
            return this;
        }

        public Builder employeeName(String employeeName) {
            this.employeeName = employeeName;
            return this;
        }

        public Builder date(LocalDate date) {
            this.date = date;
            return this;
        }

        public Builder conflictingShifts(List<ConflictingShiftDTO> conflictingShifts) {
            this.conflictingShifts = conflictingShifts;
            return this;
        }

        public ConflictError build() {
            ConflictError e = new ConflictError(employeeId, employeeName, date, conflictingShifts);
            e.setReason(reason);
            e.setShiftId(shiftId);
            return e;
        }
    }
}