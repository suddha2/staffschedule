package com.midco.rota.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.midco.rota.util.ShiftType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Tracks manual corrections made to OptaPlanner-generated rotas
 * Extracted from rota_feeder table by comparing Auto vs Manual entries
 */
@Entity
@Table(name = "rota_correction")
public class RotaCorrection {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Reference to the shift that was corrected (can be null if extracted from rota_feeder)
    @ManyToOne
    @JoinColumn(name = "shift_assignment_id")
    private ShiftAssignment shiftAssignment;
    
    // Who OptaPlanner originally assigned (null if shift was unassigned)
    @ManyToOne
    @JoinColumn(name = "original_employee_id")
    private Employee originalEmployee;
    
    // Who the user manually assigned
    @ManyToOne
    @JoinColumn(name = "corrected_employee_id")
    private Employee correctedEmployee;
    
    // Why was this correction made? (optional, for future use)
    @Column(columnDefinition = "TEXT")
    private String correctionReason;
    
    @Column(name = "correction_date")
    private LocalDateTime correctionDate;
    
    // Denormalized fields for faster pattern analysis (avoid joins)
    private String location;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "shift_type")
    private ShiftType shiftType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week")
    private DayOfWeek dayOfWeek;
    
    @Column(name = "shift_date")
    private LocalDate shiftDate;
    
    // For tracking source of correction
    private String source; // "rota_feeder", "real_time", etc.
    
    @Column(name = "week")
    private String week; // Week identifier (e.g., "2024-W44")
    
    // Constructors
    public RotaCorrection() {
        this.correctionDate = LocalDateTime.now();
        this.source = "rota_feeder";
    }
    
    public RotaCorrection(Employee originalEmployee, Employee correctedEmployee, 
                         String location, ShiftType shiftType, DayOfWeek dayOfWeek, 
                         LocalDate shiftDate) {
        this();
        this.originalEmployee = originalEmployee;
        this.correctedEmployee = correctedEmployee;
        this.location = location;
        this.shiftType = shiftType;
        this.dayOfWeek = dayOfWeek;
        this.shiftDate = shiftDate;
    }
    
    // Helper methods
    public boolean wasUnassigned() {
        return originalEmployee == null;
    }
    
    public boolean wasReassigned() {
        return originalEmployee != null && correctedEmployee != null 
               && !originalEmployee.getId().equals(correctedEmployee.getId());
    }
    
    public String getCorrectionType() {
        if (wasUnassigned()) {
            return "FILL_UNASSIGNED";
        } else if (wasReassigned()) {
            return "REASSIGNMENT";
        } else {
            return "UNKNOWN";
        }
    }
    
    public String getOriginalEmployeeName() {
        return originalEmployee != null ? originalEmployee.getName() : "UNASSIGNED";
    }
    
    public String getCorrectedEmployeeName() {
        return correctedEmployee != null ? correctedEmployee.getName() : "UNASSIGNED";
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public ShiftAssignment getShiftAssignment() {
        return shiftAssignment;
    }
    
    public void setShiftAssignment(ShiftAssignment shiftAssignment) {
        this.shiftAssignment = shiftAssignment;
    }
    
    public Employee getOriginalEmployee() {
        return originalEmployee;
    }
    
    public void setOriginalEmployee(Employee originalEmployee) {
        this.originalEmployee = originalEmployee;
    }
    
    public Employee getCorrectedEmployee() {
        return correctedEmployee;
    }
    
    public void setCorrectedEmployee(Employee correctedEmployee) {
        this.correctedEmployee = correctedEmployee;
    }
    
    public String getCorrectionReason() {
        return correctionReason;
    }
    
    public void setCorrectionReason(String correctionReason) {
        this.correctionReason = correctionReason;
    }
    
    public LocalDateTime getCorrectionDate() {
        return correctionDate;
    }
    
    public void setCorrectionDate(LocalDateTime correctionDate) {
        this.correctionDate = correctionDate;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public ShiftType getShiftType() {
        return shiftType;
    }
    
    public void setShiftType(ShiftType shiftType) {
        this.shiftType = shiftType;
    }
    
    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }
    
    public void setDayOfWeek(DayOfWeek dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }
    
    public LocalDate getShiftDate() {
        return shiftDate;
    }
    
    public void setShiftDate(LocalDate shiftDate) {
        this.shiftDate = shiftDate;
    }
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    public String getWeek() {
        return week;
    }
    
    public void setWeek(String week) {
        this.week = week;
    }
    
    @Override
    public String toString() {
        return "RotaCorrection{" +
                "id=" + id +
                ", " + getOriginalEmployeeName() + " → " + getCorrectedEmployeeName() +
                ", location='" + location + '\'' +
                ", shiftType=" + shiftType +
                ", day=" + dayOfWeek +
                ", date=" + shiftDate +
                ", type=" + getCorrectionType() +
                '}';
    }
}