package com.midco.rota.dto;

import java.time.LocalDateTime;

public class AssignmentVersionDTO {
    private Long assignmentId;
    private Long shiftId;
    private Integer employeeId;
    private String employeeFirstName;
    private String employeeLastName;
    private Long rotaId;
    private LocalDateTime assignedAt;
    private String location;
    private String shiftType;
    private String dayOfWeek;
    private String startTime;
    private String endTime;
    private LocalDateTime shiftStart;
    private LocalDateTime shiftEnd;
    private String changeType;

    // Constructors
    public AssignmentVersionDTO() {}

    public AssignmentVersionDTO(Long assignmentId, Long shiftId, Integer employeeId, String employeeFirstName, String employeeLastName, Long rotaId, LocalDateTime assignedAt, String location, String shiftType, String dayOfWeek, String startTime, String endTime, LocalDateTime shiftStart, LocalDateTime shiftEnd, String changeType) {
        this.assignmentId = assignmentId;
        this.shiftId = shiftId;
        this.employeeId = employeeId;
        this.employeeFirstName = employeeFirstName;
        this.employeeLastName = employeeLastName;
        this.rotaId = rotaId;
        this.assignedAt = assignedAt;
        this.location = location;
        this.shiftType = shiftType;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.shiftStart = shiftStart;
        this.shiftEnd = shiftEnd;
        this.changeType = changeType;
    }

    // Getters and Setters
    public Long getAssignmentId() { return assignmentId; }
    public void setAssignmentId(Long assignmentId) { this.assignmentId = assignmentId; }

    public Long getShiftId() { return shiftId; }
    public void setShiftId(Long shiftId) { this.shiftId = shiftId; }

    public Integer getEmployeeId() { return employeeId; }
    public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }

    public String getEmployeeFirstName() { return employeeFirstName; }
    public void setEmployeeFirstName(String employeeFirstName) { this.employeeFirstName = employeeFirstName; }

    public String getEmployeeLastName() { return employeeLastName; }
    public void setEmployeeLastName(String employeeLastName) { this.employeeLastName = employeeLastName; }

    public Long getRotaId() { return rotaId; }
    public void setRotaId(Long rotaId) { this.rotaId = rotaId; }

    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getShiftType() { return shiftType; }
    public void setShiftType(String shiftType) { this.shiftType = shiftType; }

    public String getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public LocalDateTime getShiftStart() { return shiftStart; }
    public void setShiftStart(LocalDateTime shiftStart) { this.shiftStart = shiftStart; }

    public LocalDateTime getShiftEnd() { return shiftEnd; }
    public void setShiftEnd(LocalDateTime shiftEnd) { this.shiftEnd = shiftEnd; }

    public String getChangeType() { return changeType; }
    public void setChangeType(String changeType) { this.changeType = changeType; }

    
    // Builder pattern
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private Long assignmentId;
        private Long shiftId;
        private Integer employeeId;
        private String employeeFirstName;
        private String employeeLastName;
        private Long rotaId;
        private LocalDateTime assignedAt;
        private String location;
        private String shiftType;
        private String dayOfWeek;
        private String startTime;
        private String endTime;
        private LocalDateTime shiftStart;
        private LocalDateTime shiftEnd;
        private String changeType;
        
        public Builder assignmentId(Long assignmentId) {
            this.assignmentId = assignmentId;
            return this;
        }
        
        public Builder shiftId(Long shiftId) {
            this.shiftId = shiftId;
            return this;
        }
        
        public Builder employeeId(Integer employeeId) {
            this.employeeId = employeeId;
            return this;
        }
        
        public Builder employeeFirstName(String employeeFirstName) {
            this.employeeFirstName = employeeFirstName;
            return this;
        }
        
        public Builder employeeLastName(String employeeLastName) {
            this.employeeLastName = employeeLastName;
            return this;
        }
        
        public Builder rotaId(Long rotaId) {
            this.rotaId = rotaId;
            return this;
        }
        
        public Builder assignedAt(LocalDateTime assignedAt) {
            this.assignedAt = assignedAt;
            return this;
        }
        
        public Builder location(String location) {
            this.location = location;
            return this;
        }
        
        public Builder shiftType(String shiftType) {
            this.shiftType = shiftType;
            return this;
        }
        
        public Builder dayOfWeek(String dayOfWeek) {
            this.dayOfWeek = dayOfWeek;
            return this;
        }
        
        public Builder startTime(String startTime) {
            this.startTime = startTime;
            return this;
        }
        
        public Builder endTime(String endTime) {
            this.endTime = endTime;
            return this;
        }
        
        public Builder shiftStart(LocalDateTime shiftStart) {
            this.shiftStart = shiftStart;
            return this;
        }
        
        public Builder shiftEnd(LocalDateTime shiftEnd) {
            this.shiftEnd = shiftEnd;
            return this;
        }
        
        public Builder changeType(String changeType) {
            this.changeType = changeType;
            return this;
        }
        
        public AssignmentVersionDTO build() {
            AssignmentVersionDTO obj = new AssignmentVersionDTO();
            obj.setAssignmentId(this.assignmentId);
            obj.setShiftId(this.shiftId);
            obj.setEmployeeId(this.employeeId);
            obj.setEmployeeFirstName(this.employeeFirstName);
            obj.setEmployeeLastName(this.employeeLastName);
            obj.setRotaId(this.rotaId);
            obj.setAssignedAt(this.assignedAt);
            obj.setLocation(this.location);
            obj.setShiftType(this.shiftType);
            obj.setDayOfWeek(this.dayOfWeek);
            obj.setStartTime(this.startTime);
            obj.setEndTime(this.endTime);
            obj.setShiftStart(this.shiftStart);
            obj.setShiftEnd(this.shiftEnd);
            obj.setChangeType(this.changeType);
            return obj;
        }
    }
}