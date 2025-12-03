package com.midco.rota.model;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Represents a 4-week government-mandated period
 * Each period contains 4 weeks for employee scheduling
 */
@Entity
@Table(name = "pay_cycle_periods")
public class Period {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;
    
    @Column(name = "active")
    private Boolean isActive;
    
    // No-arg constructor
    public Period() {
    }
    
    /**
     * Check if a date falls within this period
     */
    public boolean contains(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }
    
    /**
     * Calculate which week (1-4) a date falls into within this period
     * Assumes period starts on a Monday
     * 
     * @param date The date to check (must be within this period)
     * @return Week number (1, 2, 3, or 4)
     */
    public int getWeekNumber(LocalDate date) {
        if (!contains(date)) {
            throw new IllegalArgumentException(
                String.format("Date %s is not in period %s (%s to %s)", 
                    date, name, startDate, endDate));
        }
        
        long daysSinceStart = java.time.temporal.ChronoUnit.DAYS.between(startDate, date);
        int weekNumber = (int) (daysSinceStart / 7) + 1;
        
        // Clamp to 1-4 in case of any edge cases
        return Math.min(4, Math.max(1, weekNumber));
    }
    
    // Getters and setters
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public LocalDate getStartDate() {
        return startDate;
    }
    
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }
    
    public LocalDate getEndDate() {
        return endDate;
    }
    
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    @Override
    public String toString() {
        return String.format("Period[id=%d, name='%s', %s to %s]", 
            id, name, startDate, endDate);
    }
}