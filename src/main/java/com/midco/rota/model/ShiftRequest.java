package com.midco.rota.model;

import java.time.LocalDateTime;

import com.midco.rota.util.ShiftRequestStatus;

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

@Entity
@Table(name = "shift_request")
public class ShiftRequest {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "shift_assignment_id", nullable = false)
	private ShiftAssignment shiftAssignment;

	@ManyToOne
	@JoinColumn(name = "employee_id", nullable = false)
	private Employee employee;

	@Column(name = "rota_id", nullable = false)
	private Long rotaId;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", length = 20, nullable = false)
	private ShiftRequestStatus status = ShiftRequestStatus.PENDING;

	@Column(name = "requested_at", nullable = false)
	private LocalDateTime requestedAt = LocalDateTime.now();

	@Column(name = "resolved_at")
	private LocalDateTime resolvedAt;

	@Column(name = "resolved_by", length = 100)
	private String resolvedBy;

	public ShiftRequest() {
	}

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

	public Employee getEmployee() {
		return employee;
	}

	public void setEmployee(Employee employee) {
		this.employee = employee;
	}

	public Long getRotaId() {
		return rotaId;
	}

	public void setRotaId(Long rotaId) {
		this.rotaId = rotaId;
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
}
