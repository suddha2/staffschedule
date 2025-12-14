package com.midco.rota.dto;

import java.util.List;

public class VersionHistoryDTO {
    private Long rotaId;
    private List<VersionSummaryDTO> versions;
    private VersionSummaryDTO currentVersion;
    private Integer totalVersions;

    // Constructors
    public VersionHistoryDTO() {}

    public VersionHistoryDTO(Long rotaId, List<VersionSummaryDTO> versions, VersionSummaryDTO currentVersion, Integer totalVersions) {
        this.rotaId = rotaId;
        this.versions = versions;
        this.currentVersion = currentVersion;
        this.totalVersions = totalVersions;
    }

    // Getters and Setters
    public Long getRotaId() { return rotaId; }
    public void setRotaId(Long rotaId) { this.rotaId = rotaId; }

    public List<VersionSummaryDTO> getVersions() { return versions; }
    public void setVersions(List<VersionSummaryDTO> versions) { this.versions = versions; }

    public VersionSummaryDTO getCurrentVersion() { return currentVersion; }
    public void setCurrentVersion(VersionSummaryDTO currentVersion) { this.currentVersion = currentVersion; }

    public Integer getTotalVersions() { return totalVersions; }
    public void setTotalVersions(Integer totalVersions) { this.totalVersions = totalVersions; }

    
    // Builder pattern
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private Long rotaId;
        private List<VersionSummaryDTO> versions;
        private VersionSummaryDTO currentVersion;
        private Integer totalVersions;
        
        public Builder rotaId(Long rotaId) {
            this.rotaId = rotaId;
            return this;
        }
        
        public Builder versions(List<VersionSummaryDTO> versions) {
            this.versions = versions;
            return this;
        }
        
        public Builder currentVersion(VersionSummaryDTO currentVersion) {
            this.currentVersion = currentVersion;
            return this;
        }
        
        public Builder totalVersions(Integer totalVersions) {
            this.totalVersions = totalVersions;
            return this;
        }
        
        public VersionHistoryDTO build() {
            VersionHistoryDTO obj = new VersionHistoryDTO();
            obj.setRotaId(this.rotaId);
            obj.setVersions(this.versions);
            obj.setCurrentVersion(this.currentVersion);
            obj.setTotalVersions(this.totalVersions);
            return obj;
        }
    }
}