package com.midco.rota.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;


@Entity
@Table(name = "schedule_version")
public class ScheduleVersion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "rota_id", nullable = false)
    private Long rotaId;
    
    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;
    
    @Column(name = "version_label", length = 100)
    private String versionLabel;
    
    @Column(name = "is_current")
    private Boolean isCurrent = false;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;
    
    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;
    
    @Column(name = "total_assignments")
    private Integer totalAssignments = 0;
    
    @Column(name = "changes_from_previous")
    private Integer changesFromPrevious = 0;
    
    // Constructors
    
    public ScheduleVersion() {
    }
    
    public ScheduleVersion(Long rotaId, Integer versionNumber, String versionLabel, 
                          Boolean isCurrent, String createdBy, String comment) {
        this.rotaId = rotaId;
        this.versionNumber = versionNumber;
        this.versionLabel = versionLabel;
        this.isCurrent = isCurrent;
        this.createdBy = createdBy;
        this.comment = comment;
    }
    
    // Getters and Setters
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getRotaId() {
        return rotaId;
    }
    
    public void setRotaId(Long rotaId) {
        this.rotaId = rotaId;
    }
    
    public Integer getVersionNumber() {
        return versionNumber;
    }
    
    public void setVersionNumber(Integer versionNumber) {
        this.versionNumber = versionNumber;
    }
    
    public String getVersionLabel() {
        return versionLabel;
    }
    
    public void setVersionLabel(String versionLabel) {
        this.versionLabel = versionLabel;
    }
    
    public Boolean getIsCurrent() {
        return isCurrent;
    }
    
    public void setIsCurrent(Boolean isCurrent) {
        this.isCurrent = isCurrent;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    public String getComment() {
        return comment;
    }
    
    public void setComment(String comment) {
        this.comment = comment;
    }
    
    public Integer getTotalAssignments() {
        return totalAssignments;
    }
    
    public void setTotalAssignments(Integer totalAssignments) {
        this.totalAssignments = totalAssignments;
    }
    
    public Integer getChangesFromPrevious() {
        return changesFromPrevious;
    }
    
    public void setChangesFromPrevious(Integer changesFromPrevious) {
        this.changesFromPrevious = changesFromPrevious;
    }
    
    // Builder pattern (replaces @Builder from Lombok)
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private Long rotaId;
        private Integer versionNumber;
        private String versionLabel;
        private Boolean isCurrent;
        private String createdBy;
        private String comment;
        private Integer totalAssignments;
        private Integer changesFromPrevious;
        
        public Builder rotaId(Long rotaId) {
            this.rotaId = rotaId;
            return this;
        }
        
        public Builder versionNumber(Integer versionNumber) {
            this.versionNumber = versionNumber;
            return this;
        }
        
        public Builder versionLabel(String versionLabel) {
            this.versionLabel = versionLabel;
            return this;
        }
        
        public Builder isCurrent(Boolean isCurrent) {
            this.isCurrent = isCurrent;
            return this;
        }
        
        public Builder createdBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }
        
        public Builder comment(String comment) {
            this.comment = comment;
            return this;
        }
        
        public Builder totalAssignments(Integer totalAssignments) {
            this.totalAssignments = totalAssignments;
            return this;
        }
        
        public Builder changesFromPrevious(Integer changesFromPrevious) {
            this.changesFromPrevious = changesFromPrevious;
            return this;
        }
        
        public ScheduleVersion build() {
            ScheduleVersion version = new ScheduleVersion();
            version.setRotaId(this.rotaId);
            version.setVersionNumber(this.versionNumber);
            version.setVersionLabel(this.versionLabel);
            version.setIsCurrent(this.isCurrent);
            version.setCreatedBy(this.createdBy);
            version.setComment(this.comment);
            version.setTotalAssignments(this.totalAssignments);
            version.setChangesFromPrevious(this.changesFromPrevious);
            return version;
        }
    }
}