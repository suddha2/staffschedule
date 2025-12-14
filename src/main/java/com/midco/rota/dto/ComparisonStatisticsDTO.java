package com.midco.rota.dto;

public class ComparisonStatisticsDTO {
    private Integer totalShifts;
    private Integer unchangedShifts;
    private Integer changedShifts;
    private Integer shiftsAdded;
    private Integer shiftsRemoved;
    private Integer shiftsReassigned;
    private Double changePercentage;

    // Constructors
    public ComparisonStatisticsDTO() {}

    public ComparisonStatisticsDTO(Integer totalShifts, Integer unchangedShifts, Integer changedShifts, Integer shiftsAdded, Integer shiftsRemoved, Integer shiftsReassigned, Double changePercentage) {
        this.totalShifts = totalShifts;
        this.unchangedShifts = unchangedShifts;
        this.changedShifts = changedShifts;
        this.shiftsAdded = shiftsAdded;
        this.shiftsRemoved = shiftsRemoved;
        this.shiftsReassigned = shiftsReassigned;
        this.changePercentage = changePercentage;
    }

    // Getters and Setters
    public Integer getTotalShifts() { return totalShifts; }
    public void setTotalShifts(Integer totalShifts) { this.totalShifts = totalShifts; }

    public Integer getUnchangedShifts() { return unchangedShifts; }
    public void setUnchangedShifts(Integer unchangedShifts) { this.unchangedShifts = unchangedShifts; }

    public Integer getChangedShifts() { return changedShifts; }
    public void setChangedShifts(Integer changedShifts) { this.changedShifts = changedShifts; }

    public Integer getShiftsAdded() { return shiftsAdded; }
    public void setShiftsAdded(Integer shiftsAdded) { this.shiftsAdded = shiftsAdded; }

    public Integer getShiftsRemoved() { return shiftsRemoved; }
    public void setShiftsRemoved(Integer shiftsRemoved) { this.shiftsRemoved = shiftsRemoved; }

    public Integer getShiftsReassigned() { return shiftsReassigned; }
    public void setShiftsReassigned(Integer shiftsReassigned) { this.shiftsReassigned = shiftsReassigned; }

    public Double getChangePercentage() { return changePercentage; }
    public void setChangePercentage(Double changePercentage) { this.changePercentage = changePercentage; }

    
    // Builder pattern
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private Integer totalShifts;
        private Integer unchangedShifts;
        private Integer changedShifts;
        private Integer shiftsAdded;
        private Integer shiftsRemoved;
        private Integer shiftsReassigned;
        private Double changePercentage;
        
        public Builder totalShifts(Integer totalShifts) {
            this.totalShifts = totalShifts;
            return this;
        }
        
        public Builder unchangedShifts(Integer unchangedShifts) {
            this.unchangedShifts = unchangedShifts;
            return this;
        }
        
        public Builder changedShifts(Integer changedShifts) {
            this.changedShifts = changedShifts;
            return this;
        }
        
        public Builder shiftsAdded(Integer shiftsAdded) {
            this.shiftsAdded = shiftsAdded;
            return this;
        }
        
        public Builder shiftsRemoved(Integer shiftsRemoved) {
            this.shiftsRemoved = shiftsRemoved;
            return this;
        }
        
        public Builder shiftsReassigned(Integer shiftsReassigned) {
            this.shiftsReassigned = shiftsReassigned;
            return this;
        }
        
        public Builder changePercentage(Double changePercentage) {
            this.changePercentage = changePercentage;
            return this;
        }
        
        public ComparisonStatisticsDTO build() {
            ComparisonStatisticsDTO obj = new ComparisonStatisticsDTO();
            obj.setTotalShifts(this.totalShifts);
            obj.setUnchangedShifts(this.unchangedShifts);
            obj.setChangedShifts(this.changedShifts);
            obj.setShiftsAdded(this.shiftsAdded);
            obj.setShiftsRemoved(this.shiftsRemoved);
            obj.setShiftsReassigned(this.shiftsReassigned);
            obj.setChangePercentage(this.changePercentage);
            return obj;
        }
    }
}