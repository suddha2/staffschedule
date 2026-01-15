package com.midco.rota.dto;

import com.midco.rota.model.ShiftAssignment;
import java.util.List;

public class ShiftAssignmentDTO {
    private Long id;
    private String planningId;
    private ShiftDTO shift;  // ✅ Always full object, never just ID
    private EmployeeDTO employee;
    private List<String> diagnosticReasons;
    private List<String> unassignmentReasons;
    private boolean pinned;
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getPlanningId() { return planningId; }
    public void setPlanningId(String planningId) { this.planningId = planningId; }
    
    public ShiftDTO getShift() { return shift; }
    public void setShift(ShiftDTO shift) { this.shift = shift; }
    
    public EmployeeDTO getEmployee() { return employee; }
    public void setEmployee(EmployeeDTO employee) { this.employee = employee; }
    
    public List<String> getDiagnosticReasons() { return diagnosticReasons; }
    public void setDiagnosticReasons(List<String> reasons) { this.diagnosticReasons = reasons; }
    
    public List<String> getUnassignmentReasons() { return unassignmentReasons; }
    public void setUnassignmentReasons(List<String> reasons) { this.unassignmentReasons = reasons; }
    
    public boolean isPinned() { return pinned; }
    public void setPinned(boolean pinned) { this.pinned = pinned; }
    
    // ✅ Converter from Entity
    public static ShiftAssignmentDTO fromEntity(ShiftAssignment sa) {
        ShiftAssignmentDTO dto = new ShiftAssignmentDTO();
        dto.setId(sa.getId());
        dto.setPlanningId(sa.getPlanningId());
        
        // ✅ CRITICAL: Always convert Shift to DTO (never just ID)
        if (sa.getShift() != null) {
            dto.setShift(ShiftDTO.fromEntity(sa.getShift()));
        }
        
        // Convert employee
        if (sa.getEmployee() != null) {
            dto.setEmployee(EmployeeDTO.fromEntity(sa.getEmployee()));
        }
        
        dto.setDiagnosticReasons(sa.getDiagnosticReasons());
        dto.setUnassignmentReasons(sa.getUnassignmentReasons());
        dto.setPinned(sa.isPinned());
        
        return dto;
    }
}