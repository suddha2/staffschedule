package com.midco.rota.dto;

public class RollbackRequest {
    private Long targetVersionId;
    private String username;
    private String reason;
    
    public RollbackRequest() {}
    
    public RollbackRequest(Long targetVersionId, String username, String reason) {
        this.targetVersionId = targetVersionId;
        this.username = username;
        this.reason = reason;
    }
    
    public Long getTargetVersionId() { return targetVersionId; }
    public void setTargetVersionId(Long targetVersionId) { this.targetVersionId = targetVersionId; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
