package com.midco.rota.dto;

import java.util.List;

public class VersionDetailDTO {
    private VersionSummaryDTO version;
    private List<AssignmentVersionDTO> assignments;
    private List<ChangeDTO> changes;
    private VersionStatisticsDTO statistics;

    // Constructors
    public VersionDetailDTO() {}

    public VersionDetailDTO(VersionSummaryDTO version, List<AssignmentVersionDTO> assignments, List<ChangeDTO> changes, VersionStatisticsDTO statistics) {
        this.version = version;
        this.assignments = assignments;
        this.changes = changes;
        this.statistics = statistics;
    }

    // Getters and Setters
    public VersionSummaryDTO getVersion() { return version; }
    public void setVersion(VersionSummaryDTO version) { this.version = version; }

    public List<AssignmentVersionDTO> getAssignments() { return assignments; }
    public void setAssignments(List<AssignmentVersionDTO> assignments) { this.assignments = assignments; }

    public List<ChangeDTO> getChanges() { return changes; }
    public void setChanges(List<ChangeDTO> changes) { this.changes = changes; }

    public VersionStatisticsDTO getStatistics() { return statistics; }
    public void setStatistics(VersionStatisticsDTO statistics) { this.statistics = statistics; }

    
    // Builder pattern
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private VersionSummaryDTO version;
        private List<AssignmentVersionDTO> assignments;
        private List<ChangeDTO> changes;
        private VersionStatisticsDTO statistics;
        
        public Builder version(VersionSummaryDTO version) {
            this.version = version;
            return this;
        }
        
        public Builder assignments(List<AssignmentVersionDTO> assignments) {
            this.assignments = assignments;
            return this;
        }
        
        public Builder changes(List<ChangeDTO> changes) {
            this.changes = changes;
            return this;
        }
        
        public Builder statistics(VersionStatisticsDTO statistics) {
            this.statistics = statistics;
            return this;
        }
        
        public VersionDetailDTO build() {
            VersionDetailDTO obj = new VersionDetailDTO();
            obj.setVersion(this.version);
            obj.setAssignments(this.assignments);
            obj.setChanges(this.changes);
            obj.setStatistics(this.statistics);
            return obj;
        }
    }
}