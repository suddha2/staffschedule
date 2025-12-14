package com.midco.rota.dto;

import java.time.LocalDateTime;

public class ChangeDTO {
    private Long changeId;
    private Long shiftId;
    private String changeType;
    private Integer oldEmployeeId;
    private String oldEmployeeFirstName;
    private String oldEmployeeLastName;
    private Integer newEmployeeId;
    private String newEmployeeFirstName;
    private String newEmployeeLastName;
    private String changeReason;
    private LocalDateTime changedAt;
    private String changedBy;
    private String location;
    private String shiftType;
    private LocalDateTime shiftStart;

    // Constructors
    public ChangeDTO() {}

    public ChangeDTO(Long changeId, Long shiftId, String changeType, Integer oldEmployeeId, String oldEmployeeFirstName, String oldEmployeeLastName, Integer newEmployeeId, String newEmployeeFirstName, String newEmployeeLastName, String changeReason, LocalDateTime changedAt, String changedBy, String location, String shiftType, LocalDateTime shiftStart) {
        this.changeId = changeId;
        this.shiftId = shiftId;
        this.changeType = changeType;
        this.oldEmployeeId = oldEmployeeId;
        this.oldEmployeeFirstName = oldEmployeeFirstName;
        this.oldEmployeeLastName = oldEmployeeLastName;
        this.newEmployeeId = newEmployeeId;
        this.newEmployeeFirstName = newEmployeeFirstName;
        this.newEmployeeLastName = newEmployeeLastName;
        this.changeReason = changeReason;
        this.changedAt = changedAt;
        this.changedBy = changedBy;
        this.location = location;
        this.shiftType = shiftType;
        this.shiftStart = shiftStart;
    }

    // Getters and Setters
    public Long getChangeId() { return changeId; }
    public void setChangeId(Long changeId) { this.changeId = changeId; }

    public Long getShiftId() { return shiftId; }
    public void setShiftId(Long shiftId) { this.shiftId = shiftId; }

    public String getChangeType() { return changeType; }
    public void setChangeType(String changeType) { this.changeType = changeType; }

    public Integer getOldEmployeeId() { return oldEmployeeId; }
    public void setOldEmployeeId(Integer oldEmployeeId) { this.oldEmployeeId = oldEmployeeId; }

    public String getOldEmployeeFirstName() { return oldEmployeeFirstName; }
    public void setOldEmployeeFirstName(String oldEmployeeFirstName) { this.oldEmployeeFirstName = oldEmployeeFirstName; }

    public String getOldEmployeeLastName() { return oldEmployeeLastName; }
    public void setOldEmployeeLastName(String oldEmployeeLastName) { this.oldEmployeeLastName = oldEmployeeLastName; }

    public Integer getNewEmployeeId() { return newEmployeeId; }
    public void setNewEmployeeId(Integer newEmployeeId) { this.newEmployeeId = newEmployeeId; }

    public String getNewEmployeeFirstName() { return newEmployeeFirstName; }
    public void setNewEmployeeFirstName(String newEmployeeFirstName) { this.newEmployeeFirstName = newEmployeeFirstName; }

    public String getNewEmployeeLastName() { return newEmployeeLastName; }
    public void setNewEmployeeLastName(String newEmployeeLastName) { this.newEmployeeLastName = newEmployeeLastName; }

    public String getChangeReason() { return changeReason; }
    public void setChangeReason(String changeReason) { this.changeReason = changeReason; }

    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }

    public String getChangedBy() { return changedBy; }
    public void setChangedBy(String changedBy) { this.changedBy = changedBy; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getShiftType() { return shiftType; }
    public void setShiftType(String shiftType) { this.shiftType = shiftType; }

    public LocalDateTime getShiftStart() { return shiftStart; }
    public void setShiftStart(LocalDateTime shiftStart) { this.shiftStart = shiftStart; }

    
    // Builder pattern
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private Long changeId;
        private Long shiftId;
        private String changeType;
        private Integer oldEmployeeId;
        private String oldEmployeeFirstName;
        private String oldEmployeeLastName;
        private Integer newEmployeeId;
        private String newEmployeeFirstName;
        private String newEmployeeLastName;
        private String changeReason;
        private LocalDateTime changedAt;
        private String changedBy;
        private String location;
        private String shiftType;
        private LocalDateTime shiftStart;
        
        public Builder changeId(Long changeId) {
            this.changeId = changeId;
            return this;
        }
        
        public Builder shiftId(Long shiftId) {
            this.shiftId = shiftId;
            return this;
        }
        
        public Builder changeType(String changeType) {
            this.changeType = changeType;
            return this;
        }
        
        public Builder oldEmployeeId(Integer oldEmployeeId) {
            this.oldEmployeeId = oldEmployeeId;
            return this;
        }
        
        public Builder oldEmployeeFirstName(String oldEmployeeFirstName) {
            this.oldEmployeeFirstName = oldEmployeeFirstName;
            return this;
        }
        
        public Builder oldEmployeeLastName(String oldEmployeeLastName) {
            this.oldEmployeeLastName = oldEmployeeLastName;
            return this;
        }
        
        public Builder newEmployeeId(Integer newEmployeeId) {
            this.newEmployeeId = newEmployeeId;
            return this;
        }
        
        public Builder newEmployeeFirstName(String newEmployeeFirstName) {
            this.newEmployeeFirstName = newEmployeeFirstName;
            return this;
        }
        
        public Builder newEmployeeLastName(String newEmployeeLastName) {
            this.newEmployeeLastName = newEmployeeLastName;
            return this;
        }
        
        public Builder changeReason(String changeReason) {
            this.changeReason = changeReason;
            return this;
        }
        
        public Builder changedAt(LocalDateTime changedAt) {
            this.changedAt = changedAt;
            return this;
        }
        
        public Builder changedBy(String changedBy) {
            this.changedBy = changedBy;
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
        
        public ChangeDTO build() {
            ChangeDTO obj = new ChangeDTO();
            obj.setChangeId(this.changeId);
            obj.setShiftId(this.shiftId);
            obj.setChangeType(this.changeType);
            obj.setOldEmployeeId(this.oldEmployeeId);
            obj.setOldEmployeeFirstName(this.oldEmployeeFirstName);
            obj.setOldEmployeeLastName(this.oldEmployeeLastName);
            obj.setNewEmployeeId(this.newEmployeeId);
            obj.setNewEmployeeFirstName(this.newEmployeeFirstName);
            obj.setNewEmployeeLastName(this.newEmployeeLastName);
            obj.setChangeReason(this.changeReason);
            obj.setChangedAt(this.changedAt);
            obj.setChangedBy(this.changedBy);
            obj.setLocation(this.location);
            obj.setShiftType(this.shiftType);
            obj.setShiftStart(this.shiftStart);
            return obj;
        }
    }
}