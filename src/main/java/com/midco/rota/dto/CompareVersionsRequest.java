package com.midco.rota.dto;

public class CompareVersionsRequest {
    private Long versionAId;
    private Long versionBId;
    
    public CompareVersionsRequest() {}
    
    public CompareVersionsRequest(Long versionAId, Long versionBId) {
        this.versionAId = versionAId;
        this.versionBId = versionBId;
    }
    
    public Long getVersionAId() { return versionAId; }
    public void setVersionAId(Long versionAId) { this.versionAId = versionAId; }
    
    public Long getVersionBId() { return versionBId; }
    public void setVersionBId(Long versionBId) { this.versionBId = versionBId; }
}
