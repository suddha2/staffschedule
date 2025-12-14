package com.midco.rota.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * JPA Entity for shift_assignment_version table
 * NO LOMBOK VERSION
 */
@Entity
@Table(name = "shift_assignment_version")
public class ShiftAssignmentVersion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "version_id", nullable = false)
    private Long versionId;
    
    @Column(name = "shift_id", nullable = false)
    private Long shiftId;
    
    @Column(name = "employee_id")
    private Integer employeeId;
    
    @Column(name = "rota_id", nullable = false)
    private Long rotaId;
    
    @Column(name = "assigned_at")
    private LocalDateTime assignedAt = LocalDateTime.now();
    
    @Transient
    private String changeType;
    
    // Constructors
    
    public ShiftAssignmentVersion() {
    }
    
    public ShiftAssignmentVersion(Long versionId, Long shiftId, Integer employeeId, Long rotaId) {
        this.versionId = versionId;
        this.shiftId = shiftId;
        this.employeeId = employeeId;
        this.rotaId = rotaId;
    }
    
    // Getters and Setters
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getVersionId() {
        return versionId;
    }
    
    public void setVersionId(Long versionId) {
        this.versionId = versionId;
    }
    
    public Long getShiftId() {
        return shiftId;
    }
    
    public void setShiftId(Long shiftId) {
        this.shiftId = shiftId;
    }
    
    public Integer getEmployeeId() {
        return employeeId;
    }
    
    public void setEmployeeId(Integer employeeId) {
        this.employeeId = employeeId;
    }
    
    public Long getRotaId() {
        return rotaId;
    }
    
    public void setRotaId(Long rotaId) {
        this.rotaId = rotaId;
    }
    
    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }
    
    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }
    
    public String getChangeType() {
        return changeType;
    }
    
    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }
    
    // Builder pattern
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private Long versionId;
        private Long shiftId;
        private Integer employeeId;
        private Long rotaId;
        
        public Builder versionId(Long versionId) {
            this.versionId = versionId;
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
        
        public Builder rotaId(Long rotaId) {
            this.rotaId = rotaId;
            return this;
        }
        
        public ShiftAssignmentVersion build() {
            ShiftAssignmentVersion version = new ShiftAssignmentVersion();
            version.setVersionId(this.versionId);
            version.setShiftId(this.shiftId);
            version.setEmployeeId(this.employeeId);
            version.setRotaId(this.rotaId);
            return version;
        }
    }
}