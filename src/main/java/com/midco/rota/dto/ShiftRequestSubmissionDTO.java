package com.midco.rota.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ShiftRequestSubmissionDTO {

	@NotNull
	private Long rotaId;

	@NotEmpty
	@Size(max = 50, message = "cannot request more than 50 shifts at once")
	private List<Long> shiftAssignmentIds;

	public ShiftRequestSubmissionDTO() {
	}

	public Long getRotaId() {
		return rotaId;
	}

	public void setRotaId(Long rotaId) {
		this.rotaId = rotaId;
	}

	public List<Long> getShiftAssignmentIds() {
		return shiftAssignmentIds;
	}

	public void setShiftAssignmentIds(List<Long> shiftAssignmentIds) {
		this.shiftAssignmentIds = shiftAssignmentIds;
	}
}
