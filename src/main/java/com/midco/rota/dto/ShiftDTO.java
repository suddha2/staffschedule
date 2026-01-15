package com.midco.rota.dto;

import com.midco.rota.model.Shift;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ShiftDTO {
    private Long id;
    private LocalDate shiftStart;
    private LocalDate shiftEnd;
    private ShiftTemplateDTO shiftTemplate;  // ✅ Always included
    private String pairId;
    private Integer absoluteWeek;
    private BigDecimal durationInHours;
    private Long durationInMins;
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public LocalDate getShiftStart() { return shiftStart; }
    public void setShiftStart(LocalDate shiftStart) { this.shiftStart = shiftStart; }
    
    public LocalDate getShiftEnd() { return shiftEnd; }
    public void setShiftEnd(LocalDate shiftEnd) { this.shiftEnd = shiftEnd; }
    
    public ShiftTemplateDTO getShiftTemplate() { return shiftTemplate; }
    public void setShiftTemplate(ShiftTemplateDTO template) { this.shiftTemplate = template; }
    
    public String getPairId() { return pairId; }
    public void setPairId(String pairId) { this.pairId = pairId; }
    
    public Integer getAbsoluteWeek() { return absoluteWeek; }
    public void setAbsoluteWeek(Integer absoluteWeek) { this.absoluteWeek = absoluteWeek; }
    
    public BigDecimal getDurationInHours() { return durationInHours; }
    public void setDurationInHours(BigDecimal hours) { this.durationInHours = hours; }
    
    public Long getDurationInMins() { return durationInMins; }
    public void setDurationInMins(Long mins) { this.durationInMins = mins; }
    
    // ✅ Converter from Entity
    public static ShiftDTO fromEntity(Shift shift) {
        ShiftDTO dto = new ShiftDTO();
        dto.setId(shift.getId());
        dto.setShiftStart(shift.getShiftStart());
        dto.setShiftEnd(shift.getShiftEnd());
        
        // ✅ CRITICAL: Always include template
        if (shift.getShiftTemplate() != null) {
            dto.setShiftTemplate(ShiftTemplateDTO.fromEntity(shift.getShiftTemplate()));
        }
        
        dto.setPairId(shift.getPairId());
        dto.setAbsoluteWeek(shift.getAbsoluteWeek());
        dto.setDurationInHours(shift.getDurationInHours());
        dto.setDurationInMins(shift.getDurationInMins());
        
        return dto;
    }
}