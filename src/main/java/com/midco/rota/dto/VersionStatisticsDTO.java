package com.midco.rota.dto;

public class VersionStatisticsDTO {
    private Integer totalShifts;
    private Integer assignedShifts;
    private Integer unassignedShifts;
    private Integer uniqueEmployees;
    private Double allocationRate;

    // Constructors
    public VersionStatisticsDTO() {}

    public VersionStatisticsDTO(Integer totalShifts, Integer assignedShifts, Integer unassignedShifts, Integer uniqueEmployees, Double allocationRate) {
        this.totalShifts = totalShifts;
        this.assignedShifts = assignedShifts;
        this.unassignedShifts = unassignedShifts;
        this.uniqueEmployees = uniqueEmployees;
        this.allocationRate = allocationRate;
    }

    // Getters and Setters
    public Integer getTotalShifts() { return totalShifts; }
    public void setTotalShifts(Integer totalShifts) { this.totalShifts = totalShifts; }

    public Integer getAssignedShifts() { return assignedShifts; }
    public void setAssignedShifts(Integer assignedShifts) { this.assignedShifts = assignedShifts; }

    public Integer getUnassignedShifts() { return unassignedShifts; }
    public void setUnassignedShifts(Integer unassignedShifts) { this.unassignedShifts = unassignedShifts; }

    public Integer getUniqueEmployees() { return uniqueEmployees; }
    public void setUniqueEmployees(Integer uniqueEmployees) { this.uniqueEmployees = uniqueEmployees; }

    public Double getAllocationRate() { return allocationRate; }
    public void setAllocationRate(Double allocationRate) { this.allocationRate = allocationRate; }

    
    // Builder pattern
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private Integer totalShifts;
        private Integer assignedShifts;
        private Integer unassignedShifts;
        private Integer uniqueEmployees;
        private Double allocationRate;
        
        public Builder totalShifts(Integer totalShifts) {
            this.totalShifts = totalShifts;
            return this;
        }
        
        public Builder assignedShifts(Integer assignedShifts) {
            this.assignedShifts = assignedShifts;
            return this;
        }
        
        public Builder unassignedShifts(Integer unassignedShifts) {
            this.unassignedShifts = unassignedShifts;
            return this;
        }
        
        public Builder uniqueEmployees(Integer uniqueEmployees) {
            this.uniqueEmployees = uniqueEmployees;
            return this;
        }
        
        public Builder allocationRate(Double allocationRate) {
            this.allocationRate = allocationRate;
            return this;
        }
        
        public VersionStatisticsDTO build() {
            VersionStatisticsDTO obj = new VersionStatisticsDTO();
            obj.setTotalShifts(this.totalShifts);
            obj.setAssignedShifts(this.assignedShifts);
            obj.setUnassignedShifts(this.unassignedShifts);
            obj.setUniqueEmployees(this.uniqueEmployees);
            obj.setAllocationRate(this.allocationRate);
            return obj;
        }
    }
}