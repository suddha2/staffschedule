package com.midco.rota.dto;

import java.util.List;

public class VersionComparisonDTO {
    private VersionSummaryDTO versionA;
    private VersionSummaryDTO versionB;
    private List<AssignmentDiffDTO> differences;
    private ComparisonStatisticsDTO statistics;

    // Constructors
    public VersionComparisonDTO() {}

    public VersionComparisonDTO(VersionSummaryDTO versionA, VersionSummaryDTO versionB, List<AssignmentDiffDTO> differences, ComparisonStatisticsDTO statistics) {
        this.versionA = versionA;
        this.versionB = versionB;
        this.differences = differences;
        this.statistics = statistics;
    }

    // Getters and Setters
    public VersionSummaryDTO getVersionA() { return versionA; }
    public void setVersionA(VersionSummaryDTO versionA) { this.versionA = versionA; }

    public VersionSummaryDTO getVersionB() { return versionB; }
    public void setVersionB(VersionSummaryDTO versionB) { this.versionB = versionB; }

    public List<AssignmentDiffDTO> getDifferences() { return differences; }
    public void setDifferences(List<AssignmentDiffDTO> differences) { this.differences = differences; }

    public ComparisonStatisticsDTO getStatistics() { return statistics; }
    public void setStatistics(ComparisonStatisticsDTO statistics) { this.statistics = statistics; }

    
    // Builder pattern
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private VersionSummaryDTO versionA;
        private VersionSummaryDTO versionB;
        private List<AssignmentDiffDTO> differences;
        private ComparisonStatisticsDTO statistics;
        
        public Builder versionA(VersionSummaryDTO versionA) {
            this.versionA = versionA;
            return this;
        }
        
        public Builder versionB(VersionSummaryDTO versionB) {
            this.versionB = versionB;
            return this;
        }
        
        public Builder differences(List<AssignmentDiffDTO> differences) {
            this.differences = differences;
            return this;
        }
        
        public Builder statistics(ComparisonStatisticsDTO statistics) {
            this.statistics = statistics;
            return this;
        }
        
        public VersionComparisonDTO build() {
            VersionComparisonDTO obj = new VersionComparisonDTO();
            obj.setVersionA(this.versionA);
            obj.setVersionB(this.versionB);
            obj.setDifferences(this.differences);
            obj.setStatistics(this.statistics);
            return obj;
        }
    }
}