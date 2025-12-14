package com.midco.rota.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ShiftAssignmentChangeDTO {
    private Long shiftId;
    private Integer oldEmployeeId;
    private Integer newEmployeeId;
    private String changeReason;

    // Constructors
    public ShiftAssignmentChangeDTO() {}

    public ShiftAssignmentChangeDTO(Long shiftId, Integer oldEmployeeId, Integer newEmployeeId, String changeReason) {
        this.shiftId = shiftId;
        this.oldEmployeeId = oldEmployeeId;
        this.newEmployeeId = newEmployeeId;
        this.changeReason = changeReason;
    }

    // Getters and Setters
    public Long getShiftId() { return shiftId; }
    public void setShiftId(Long shiftId) { this.shiftId = shiftId; }

    public Integer getOldEmployeeId() { return oldEmployeeId; }
    public void setOldEmployeeId(Integer oldEmployeeId) { this.oldEmployeeId = oldEmployeeId; }

    public Integer getNewEmployeeId() { return newEmployeeId; }
    public void setNewEmployeeId(Integer newEmployeeId) { this.newEmployeeId = newEmployeeId; }

    public String getChangeReason() { return changeReason; }
    public void setChangeReason(String changeReason) { this.changeReason = changeReason; }

}
