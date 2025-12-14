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


@Entity
@Table(name = "schedule_version_audit")
public class ScheduleVersionAudit {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "version_id", nullable = false)
    private Long versionId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 50)
    private AuditAction action;
    
    @Column(name = "performed_by", nullable = false, length = 100)
    private String performedBy;
    
    @Column(name = "performed_at")
    private LocalDateTime performedAt = LocalDateTime.now();
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @Column(name = "user_agent", length = 255)
    private String userAgent;
    
    @Column(name = "additional_info", columnDefinition = "TEXT")
    private String additionalInfo;
    
    // Enum
    
    public enum AuditAction {
        CREATED,
        PUBLISHED,
        ROLLED_BACK,
        COMPARED,
        VIEWED,
        EXPORTED,
        CLONED,
        DELETED,
        COMMENTED,
        LABELED
    }
    
    // Constructors
    
    public ScheduleVersionAudit() {
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
    
    public AuditAction getAction() {
        return action;
    }
    
    public void setAction(AuditAction action) {
        this.action = action;
    }
    
    public String getPerformedBy() {
        return performedBy;
    }
    
    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }
    
    public LocalDateTime getPerformedAt() {
        return performedAt;
    }
    
    public void setPerformedAt(LocalDateTime performedAt) {
        this.performedAt = performedAt;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    
    public String getAdditionalInfo() {
        return additionalInfo;
    }
    
    public void setAdditionalInfo(String additionalInfo) {
        this.additionalInfo = additionalInfo;
    }
    
    // Builder pattern
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private Long versionId;
        private AuditAction action;
        private String performedBy;
        private String ipAddress;
        private String userAgent;
        private String additionalInfo;
        
        public Builder versionId(Long versionId) {
            this.versionId = versionId;
            return this;
        }
        
        public Builder action(AuditAction action) {
            this.action = action;
            return this;
        }
        
        public Builder performedBy(String performedBy) {
            this.performedBy = performedBy;
            return this;
        }
        
        public Builder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }
        
        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }
        
        public Builder additionalInfo(String additionalInfo) {
            this.additionalInfo = additionalInfo;
            return this;
        }
        
        public ScheduleVersionAudit build() {
            ScheduleVersionAudit audit = new ScheduleVersionAudit();
            audit.setVersionId(this.versionId);
            audit.setAction(this.action);
            audit.setPerformedBy(this.performedBy);
            audit.setIpAddress(this.ipAddress);
            audit.setUserAgent(this.userAgent);
            audit.setAdditionalInfo(this.additionalInfo);
            return audit;
        }
    }
}