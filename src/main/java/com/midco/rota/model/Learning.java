package com.midco.rota.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Stores learned patterns extracted from manual corrections
 * These learnings can be applied to improve future OptaPlanner allocations
 */
@Entity
@Table(name = "learning")
public class Learning {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private LearningType type;
    
    // For EMPLOYEE_PREFERENCE, DAY_PREFERENCE, SHIFT_TYPE_PREFERENCE
    @Column(name = "employee_id")
    private Integer employeeId;
    
    // Location name, day name, shift type, constraint name, etc.
    private String parameter;
    
    // Previous value (e.g., "GOLDERS RISE:25")
    @Column(name = "old_value")
    private String oldValue;
    
    // Suggested new value (e.g., "GOLDERS RISE:90")
    @Column(name = "new_value")
    private String newValue;
    
    // 0.0 to 1.0 (e.g., 0.85 = 85% confidence)
    private Double confidence;
    
    // Number of corrections supporting this learning
    @Column(name = "supporting_evidence")
    private Integer supportingEvidence;
    
    // Human-readable explanation
    @Column(columnDefinition = "TEXT")
    private String description;
    
    // Has this learning been applied to the database?
    private Boolean applied;
    
    @Column(name = "discovered_date")
    private LocalDateTime discoveredDate;
    
    @Column(name = "applied_date")
    private LocalDateTime appliedDate;
    
    public enum LearningType {
        EMPLOYEE_PREFERENCE,        // Update employee preferredService (location weightage)
        DAY_PREFERENCE,             // Update employee preferredDays
        SHIFT_TYPE_PREFERENCE,      // Update employee preferredShifts
        CONSTRAINT_WEIGHT,          // Adjust constraint penalty/reward in code
        NEW_PATTERN                 // New pattern discovered (needs manual review)
    }
    
    // Constructors
    public Learning() {
        this.discoveredDate = LocalDateTime.now();
        this.applied = false;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public LearningType getType() {
        return type;
    }
    
    public void setType(LearningType type) {
        this.type = type;
    }
    
    public Integer getEmployeeId() {
        return employeeId;
    }
    
    public void setEmployeeId(Integer employeeId) {
        this.employeeId = employeeId;
    }
    
    public String getParameter() {
        return parameter;
    }
    
    public void setParameter(String parameter) {
        this.parameter = parameter;
    }
    
    public String getOldValue() {
        return oldValue;
    }
    
    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }
    
    public String getNewValue() {
        return newValue;
    }
    
    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }
    
    public Double getConfidence() {
        return confidence;
    }
    
    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }
    
    public Integer getSupportingEvidence() {
        return supportingEvidence;
    }
    
    public void setSupportingEvidence(Integer supportingEvidence) {
        this.supportingEvidence = supportingEvidence;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Boolean getApplied() {
        return applied;
    }
    
    public void setApplied(Boolean applied) {
        this.applied = applied;
    }
    
    public LocalDateTime getDiscoveredDate() {
        return discoveredDate;
    }
    
    public void setDiscoveredDate(LocalDateTime discoveredDate) {
        this.discoveredDate = discoveredDate;
    }
    
    public LocalDateTime getAppliedDate() {
        return appliedDate;
    }
    
    public void setAppliedDate(LocalDateTime appliedDate) {
        this.appliedDate = appliedDate;
    }
    
    @Override
    public String toString() {
        return "Learning{" +
                "id=" + id +
                ", type=" + type +
                ", employeeId=" + employeeId +
                ", parameter='" + parameter + '\'' +
                ", oldValue='" + oldValue + '\'' +
                ", newValue='" + newValue + '\'' +
                ", confidence=" + String.format("%.0f%%", confidence * 100) +
                ", supportingEvidence=" + supportingEvidence +
                ", applied=" + applied +
                ", discoveredDate=" + discoveredDate +
                '}';
    }
}