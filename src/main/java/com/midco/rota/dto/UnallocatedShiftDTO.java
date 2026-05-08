package com.midco.rota.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.math.BigDecimal;

import com.midco.rota.model.ShiftAssignment;
import com.midco.rota.util.ShiftType;

public class UnallocatedShiftDTO {

	private Long shiftAssignmentId;
	private Long shiftId;
	private LocalDate shiftStart;
	private LocalDate shiftEnd;
	private LocalTime startTime;
	private LocalTime endTime;
	private String location;
	private String region;
	private ShiftType shiftType;
	private BigDecimal durationInHours;

	public UnallocatedShiftDTO() {
	}

	public static UnallocatedShiftDTO fromEntity(ShiftAssignment sa) {
		UnallocatedShiftDTO dto = new UnallocatedShiftDTO();
		dto.shiftAssignmentId = sa.getId();
		if (sa.getShift() != null) {
			dto.shiftId = sa.getShift().getId();
			dto.shiftStart = sa.getShift().getShiftStart();
			dto.shiftEnd = sa.getShift().getShiftEnd();
			dto.durationInHours = sa.getShift().getDurationInHours();
			if (sa.getShift().getShiftTemplate() != null) {
				dto.startTime = sa.getShift().getShiftTemplate().getStartTime();
				dto.endTime = sa.getShift().getShiftTemplate().getEndTime();
				dto.location = sa.getShift().getShiftTemplate().getLocation();
				dto.region = sa.getShift().getShiftTemplate().getRegion();
				dto.shiftType = sa.getShift().getShiftTemplate().getShiftType();
			}
		}
		return dto;
	}

	public Long getShiftAssignmentId() {
		return shiftAssignmentId;
	}

	public void setShiftAssignmentId(Long shiftAssignmentId) {
		this.shiftAssignmentId = shiftAssignmentId;
	}

	public Long getShiftId() {
		return shiftId;
	}

	public void setShiftId(Long shiftId) {
		this.shiftId = shiftId;
	}

	public LocalDate getShiftStart() {
		return shiftStart;
	}

	public void setShiftStart(LocalDate shiftStart) {
		this.shiftStart = shiftStart;
	}

	public LocalDate getShiftEnd() {
		return shiftEnd;
	}

	public void setShiftEnd(LocalDate shiftEnd) {
		this.shiftEnd = shiftEnd;
	}

	public LocalTime getStartTime() {
		return startTime;
	}

	public void setStartTime(LocalTime startTime) {
		this.startTime = startTime;
	}

	public LocalTime getEndTime() {
		return endTime;
	}

	public void setEndTime(LocalTime endTime) {
		this.endTime = endTime;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public ShiftType getShiftType() {
		return shiftType;
	}

	public void setShiftType(ShiftType shiftType) {
		this.shiftType = shiftType;
	}

	public BigDecimal getDurationInHours() {
		return durationInHours;
	}

	public void setDurationInHours(BigDecimal durationInHours) {
		this.durationInHours = durationInHours;
	}
}
