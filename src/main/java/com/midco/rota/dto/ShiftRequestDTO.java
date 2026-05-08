package com.midco.rota.dto;

import java.time.LocalDateTime;

import com.midco.rota.model.ShiftRequest;
import com.midco.rota.util.ShiftRequestStatus;

public class ShiftRequestDTO {

	private Long id;
	private Long rotaId;
	private Long shiftAssignmentId;
	private Integer employeeId;
	private String employeeName;
	private ShiftRequestStatus status;
	private LocalDateTime requestedAt;
	private LocalDateTime resolvedAt;
	private String resolvedBy;
	private UnallocatedShiftDTO shift;
	private FitDTO fit;

	public ShiftRequestDTO() {
	}

	public static ShiftRequestDTO fromEntity(ShiftRequest sr) {
		ShiftRequestDTO dto = new ShiftRequestDTO();
		dto.id = sr.getId();
		dto.rotaId = sr.getRotaId();
		dto.status = sr.getStatus();
		dto.requestedAt = sr.getRequestedAt();
		dto.resolvedAt = sr.getResolvedAt();
		dto.resolvedBy = sr.getResolvedBy();
		if (sr.getShiftAssignment() != null) {
			dto.shiftAssignmentId = sr.getShiftAssignment().getId();
			dto.shift = UnallocatedShiftDTO.fromEntity(sr.getShiftAssignment());
		}
		if (sr.getEmployee() != null) {
			dto.employeeId = sr.getEmployee().getId();
			dto.employeeName = (sr.getEmployee().getFirstName() == null ? "" : sr.getEmployee().getFirstName()) + " "
					+ (sr.getEmployee().getLastName() == null ? "" : sr.getEmployee().getLastName());
		}
		return dto;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getRotaId() {
		return rotaId;
	}

	public void setRotaId(Long rotaId) {
		this.rotaId = rotaId;
	}

	public Long getShiftAssignmentId() {
		return shiftAssignmentId;
	}

	public void setShiftAssignmentId(Long shiftAssignmentId) {
		this.shiftAssignmentId = shiftAssignmentId;
	}

	public Integer getEmployeeId() {
		return employeeId;
	}

	public void setEmployeeId(Integer employeeId) {
		this.employeeId = employeeId;
	}

	public String getEmployeeName() {
		return employeeName;
	}

	public void setEmployeeName(String employeeName) {
		this.employeeName = employeeName;
	}

	public ShiftRequestStatus getStatus() {
		return status;
	}

	public void setStatus(ShiftRequestStatus status) {
		this.status = status;
	}

	public LocalDateTime getRequestedAt() {
		return requestedAt;
	}

	public void setRequestedAt(LocalDateTime requestedAt) {
		this.requestedAt = requestedAt;
	}

	public LocalDateTime getResolvedAt() {
		return resolvedAt;
	}

	public void setResolvedAt(LocalDateTime resolvedAt) {
		this.resolvedAt = resolvedAt;
	}

	public String getResolvedBy() {
		return resolvedBy;
	}

	public void setResolvedBy(String resolvedBy) {
		this.resolvedBy = resolvedBy;
	}

	public UnallocatedShiftDTO getShift() {
		return shift;
	}

	public void setShift(UnallocatedShiftDTO shift) {
		this.shift = shift;
	}

	public FitDTO getFit() {
		return fit;
	}

	public void setFit(FitDTO fit) {
		this.fit = fit;
	}
}
