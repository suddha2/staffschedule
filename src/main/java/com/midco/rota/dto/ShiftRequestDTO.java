package com.midco.rota.dto;

import java.time.LocalDate;
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

	// Paycycle grouping for the mobile My Requests screen — the 4-week period the
	// shift falls in, plus the week (1-4) within it.
	private Long periodId;
	private String periodName;
	private LocalDate periodStart;
	private LocalDate periodEnd;
	private Integer weekNumber;

	// True when approving this request would clash with another shift the
	// employee already has that day (see PinValidationService).
	private boolean conflict;

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

	public Long getPeriodId() {
		return periodId;
	}

	public void setPeriodId(Long periodId) {
		this.periodId = periodId;
	}

	public String getPeriodName() {
		return periodName;
	}

	public void setPeriodName(String periodName) {
		this.periodName = periodName;
	}

	public LocalDate getPeriodStart() {
		return periodStart;
	}

	public void setPeriodStart(LocalDate periodStart) {
		this.periodStart = periodStart;
	}

	public LocalDate getPeriodEnd() {
		return periodEnd;
	}

	public void setPeriodEnd(LocalDate periodEnd) {
		this.periodEnd = periodEnd;
	}

	public Integer getWeekNumber() {
		return weekNumber;
	}

	public void setWeekNumber(Integer weekNumber) {
		this.weekNumber = weekNumber;
	}

	public boolean isConflict() {
		return conflict;
	}

	public void setConflict(boolean conflict) {
		this.conflict = conflict;
	}
}
