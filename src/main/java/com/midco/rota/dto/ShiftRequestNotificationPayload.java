package com.midco.rota.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ShiftRequestNotificationPayload {

	private Long rotaId;
	private Integer employeeId;
	private String employeeName;
	private List<Long> shiftAssignmentIds;
	private int requestCount;
	private LocalDateTime submittedAt;

	public ShiftRequestNotificationPayload() {
	}

	public ShiftRequestNotificationPayload(Long rotaId, Integer employeeId, String employeeName,
			List<Long> shiftAssignmentIds, int requestCount, LocalDateTime submittedAt) {
		this.rotaId = rotaId;
		this.employeeId = employeeId;
		this.employeeName = employeeName;
		this.shiftAssignmentIds = shiftAssignmentIds;
		this.requestCount = requestCount;
		this.submittedAt = submittedAt;
	}

	public Long getRotaId() {
		return rotaId;
	}

	public void setRotaId(Long rotaId) {
		this.rotaId = rotaId;
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

	public List<Long> getShiftAssignmentIds() {
		return shiftAssignmentIds;
	}

	public void setShiftAssignmentIds(List<Long> shiftAssignmentIds) {
		this.shiftAssignmentIds = shiftAssignmentIds;
	}

	public int getRequestCount() {
		return requestCount;
	}

	public void setRequestCount(int requestCount) {
		this.requestCount = requestCount;
	}

	public LocalDateTime getSubmittedAt() {
		return submittedAt;
	}

	public void setSubmittedAt(LocalDateTime submittedAt) {
		this.submittedAt = submittedAt;
	}
}
