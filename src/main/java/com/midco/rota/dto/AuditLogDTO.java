package com.midco.rota.dto;

import java.time.LocalDateTime;
import java.util.List;

public class AuditLogDTO {
    private Long auditId;
    private Long versionId;
    private String action;
    private String performedBy;
    private LocalDateTime performedAt;
    private String ipAddress;
    private String userAgent;

    // Constructors
    public AuditLogDTO() {}

    public AuditLogDTO(Long auditId, Long versionId, String action, String performedBy, LocalDateTime performedAt, String ipAddress, String userAgent) {
        this.auditId = auditId;
        this.versionId = versionId;
        this.action = action;
        this.performedBy = performedBy;
        this.performedAt = performedAt;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    // Getters and Setters
    public Long getAuditId() { return auditId; }
    public void setAuditId(Long auditId) { this.auditId = auditId; }

    public Long getVersionId() { return versionId; }
    public void setVersionId(Long versionId) { this.versionId = versionId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getPerformedBy() { return performedBy; }
    public void setPerformedBy(String performedBy) { this.performedBy = performedBy; }

    public LocalDateTime getPerformedAt() { return performedAt; }
    public void setPerformedAt(LocalDateTime performedAt) { this.performedAt = performedAt; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

}
