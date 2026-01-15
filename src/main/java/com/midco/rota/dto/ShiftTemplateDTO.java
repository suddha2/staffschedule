package com.midco.rota.dto;

import com.midco.rota.model.ShiftTemplate;
import com.midco.rota.util.Gender;
import com.midco.rota.util.ShiftType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

public class ShiftTemplateDTO {
    private Integer id;
    private String location;
    private String region;
    private ShiftType shiftType;
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private LocalTime breakStart;
    private LocalTime breakEnd;
    private BigDecimal totalHours;
    private Gender requiredGender;
    private List<String> requiredSkills;
    private int empCount;
    private int priority;
    private boolean active;
    
    // Getters and setters (all of them)
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    
    public ShiftType getShiftType() { return shiftType; }
    public void setShiftType(ShiftType shiftType) { this.shiftType = shiftType; }
    
    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(DayOfWeek dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    
    public LocalTime getBreakStart() { return breakStart; }
    public void setBreakStart(LocalTime breakStart) { this.breakStart = breakStart; }
    
    public LocalTime getBreakEnd() { return breakEnd; }
    public void setBreakEnd(LocalTime breakEnd) { this.breakEnd = breakEnd; }
    
    public BigDecimal getTotalHours() { return totalHours; }
    public void setTotalHours(BigDecimal totalHours) { this.totalHours = totalHours; }
    
    public Gender getRequiredGender() { return requiredGender; }
    public void setRequiredGender(Gender requiredGender) { this.requiredGender = requiredGender; }
    
    public List<String> getRequiredSkills() { return requiredSkills; }
    public void setRequiredSkills(List<String> requiredSkills) { this.requiredSkills = requiredSkills; }
    
    public int getEmpCount() { return empCount; }
    public void setEmpCount(int empCount) { this.empCount = empCount; }
    
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    
    // Convenience getters for frontend compatibility
    public DayOfWeek getDay() { return dayOfWeek; }
    public Gender getGender() { return requiredGender; }
    
    // ✅ Converter from Entity
    public static ShiftTemplateDTO fromEntity(ShiftTemplate template) {
        ShiftTemplateDTO dto = new ShiftTemplateDTO();
        dto.setId(template.getId());
        dto.setLocation(template.getLocation());
        dto.setRegion(template.getRegion());
        dto.setShiftType(template.getShiftType());
        dto.setDayOfWeek(template.getDayOfWeek());
        dto.setStartTime(template.getStartTime());
        dto.setEndTime(template.getEndTime());
        dto.setBreakStart(template.getBreakStart());
        dto.setBreakEnd(template.getBreakEnd());
        dto.setTotalHours(template.getTotalHours());
        dto.setRequiredGender(template.getRequiredGender());
        dto.setRequiredSkills(template.getRequiredSkills());
        dto.setEmpCount(template.getEmpCount());
        dto.setPriority(template.getPriority());
        dto.setActive(template.isActive());
        return dto;
    }
}