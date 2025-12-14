package com.midco.rota.dto;

import java.time.LocalDateTime;

public class AssignmentDiffDTO {
    private Long shiftId;
    private String location;
    private String shiftType;
    private LocalDateTime shiftStart;
    private String changeType;
    private Integer employeeAId;
    private String employeeAName;
    private Integer employeeBId;
    private String employeeBName;

    // Constructors
    public AssignmentDiffDTO() {}

    public AssignmentDiffDTO(Long shiftId, String location, String shiftType, LocalDateTime shiftStart, String changeType, Integer employeeAId, String employeeAName, Integer employeeBId, String employeeBName) {
        this.shiftId = shiftId;
        this.location = location;
        this.shiftType = shiftType;
        this.shiftStart = shiftStart;
        this.changeType = changeType;
        this.employeeAId = employeeAId;
        this.employeeAName = employeeAName;
        this.employeeBId = employeeBId;
        this.employeeBName = employeeBName;
    }

    // Getters and Setters
    public Long getShiftId() { return shiftId; }
    public void setShiftId(Long shiftId) { this.shiftId = shiftId; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getShiftType() { return shiftType; }
    public void setShiftType(String shiftType) { this.shiftType = shiftType; }

    public LocalDateTime getShiftStart() { return shiftStart; }
    public void setShiftStart(LocalDateTime shiftStart) { this.shiftStart = shiftStart; }

    public String getChangeType() { return changeType; }
    public void setChangeType(String changeType) { this.changeType = changeType; }

    public Integer getEmployeeAId() { return employeeAId; }
    public void setEmployeeAId(Integer employeeAId) { this.employeeAId = employeeAId; }

    public String getEmployeeAName() { return employeeAName; }
    public void setEmployeeAName(String employeeAName) { this.employeeAName = employeeAName; }

    public Integer getEmployeeBId() { return employeeBId; }
    public void setEmployeeBId(Integer employeeBId) { this.employeeBId = employeeBId; }

    public String getEmployeeBName() { return employeeBName; }
    public void setEmployeeBName(String employeeBName) { this.employeeBName = employeeBName; }

    
    // Builder pattern
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private Long shiftId;
        private String location;
        private String shiftType;
        private LocalDateTime shiftStart;
        private String changeType;
        private Integer employeeAId;
        private String employeeAName;
        private Integer employeeBId;
        private String employeeBName;
        
        public Builder shiftId(Long shiftId) {
            this.shiftId = shiftId;
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
        
        public Builder shiftStart(LocalDateTime shiftStart) {
            this.shiftStart = shiftStart;
            return this;
        }
        
        public Builder changeType(String changeType) {
            this.changeType = changeType;
            return this;
        }
        
        public Builder employeeAId(Integer employeeAId) {
            this.employeeAId = employeeAId;
            return this;
        }
        
        public Builder employeeAName(String employeeAName) {
            this.employeeAName = employeeAName;
            return this;
        }
        
        public Builder employeeBId(Integer employeeBId) {
            this.employeeBId = employeeBId;
            return this;
        }
        
        public Builder employeeBName(String employeeBName) {
            this.employeeBName = employeeBName;
            return this;
        }
        
        public AssignmentDiffDTO build() {
            AssignmentDiffDTO obj = new AssignmentDiffDTO();
            obj.setShiftId(this.shiftId);
            obj.setLocation(this.location);
            obj.setShiftType(this.shiftType);
            obj.setShiftStart(this.shiftStart);
            obj.setChangeType(this.changeType);
            obj.setEmployeeAId(this.employeeAId);
            obj.setEmployeeAName(this.employeeAName);
            obj.setEmployeeBId(this.employeeBId);
            obj.setEmployeeBName(this.employeeBName);
            return obj;
        }
    }
}