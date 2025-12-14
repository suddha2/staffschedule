package com.midco.rota.dto;

import java.time.LocalDateTime;

public class VersionSummaryDTO {
    private Long versionId;
    private Long rotaId;
    private Integer versionNumber;
    private String versionLabel;
    private Boolean isCurrent;
    private LocalDateTime createdAt;
    private String createdBy;
    private String comment;
    private Integer totalAssignments;
    private Integer changesFromPrevious;

    // Constructors
    public VersionSummaryDTO() {}

    public VersionSummaryDTO(Long versionId, Long rotaId, Integer versionNumber, String versionLabel, Boolean isCurrent, LocalDateTime createdAt, String createdBy, String comment, Integer totalAssignments, Integer changesFromPrevious) {
        this.versionId = versionId;
        this.rotaId = rotaId;
        this.versionNumber = versionNumber;
        this.versionLabel = versionLabel;
        this.isCurrent = isCurrent;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.comment = comment;
        this.totalAssignments = totalAssignments;
        this.changesFromPrevious = changesFromPrevious;
    }

    // Getters and Setters
    public Long getVersionId() { return versionId; }
    public void setVersionId(Long versionId) { this.versionId = versionId; }

    public Long getRotaId() { return rotaId; }
    public void setRotaId(Long rotaId) { this.rotaId = rotaId; }

    public Integer getVersionNumber() { return versionNumber; }
    public void setVersionNumber(Integer versionNumber) { this.versionNumber = versionNumber; }

    public String getVersionLabel() { return versionLabel; }
    public void setVersionLabel(String versionLabel) { this.versionLabel = versionLabel; }

    public Boolean getIsCurrent() { return isCurrent; }
    public void setIsCurrent(Boolean isCurrent) { this.isCurrent = isCurrent; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public Integer getTotalAssignments() { return totalAssignments; }
    public void setTotalAssignments(Integer totalAssignments) { this.totalAssignments = totalAssignments; }

    public Integer getChangesFromPrevious() { return changesFromPrevious; }
    public void setChangesFromPrevious(Integer changesFromPrevious) { this.changesFromPrevious = changesFromPrevious; }

    
    // Builder pattern
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private Long versionId;
        private Long rotaId;
        private Integer versionNumber;
        private String versionLabel;
        private Boolean isCurrent;
        private LocalDateTime createdAt;
        private String createdBy;
        private String comment;
        private Integer totalAssignments;
        private Integer changesFromPrevious;
        
        public Builder versionId(Long versionId) {
            this.versionId = versionId;
            return this;
        }
        
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
        
        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
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
        
        public VersionSummaryDTO build() {
            VersionSummaryDTO obj = new VersionSummaryDTO();
            obj.setVersionId(this.versionId);
            obj.setRotaId(this.rotaId);
            obj.setVersionNumber(this.versionNumber);
            obj.setVersionLabel(this.versionLabel);
            obj.setIsCurrent(this.isCurrent);
            obj.setCreatedAt(this.createdAt);
            obj.setCreatedBy(this.createdBy);
            obj.setComment(this.comment);
            obj.setTotalAssignments(this.totalAssignments);
            obj.setChangesFromPrevious(this.changesFromPrevious);
            return obj;
        }
    }
}