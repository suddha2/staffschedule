package com.midco.rota.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A standing pin the solver did not apply when generating a rota, and why (carer has left,
 * wrong gender, 7th consecutive day, clash with another pin, ...). Saved with the solved rota
 * so planners can see and fix stale pins instead of them disappearing into the server log.
 */
@Entity
@Table(name = "rota_skipped_pin")
public class SkippedPin {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "rota_id")
	private Long rotaId;

	@Column(name = "shift_date")
	private LocalDate shiftDate;

	private String location;

	@Column(name = "shift_type")
	private String shiftType;

	@Column(name = "shift_template_id")
	private Long shiftTemplateId;

	@Column(name = "week_of_period")
	private Short weekOfPeriod;

	@Column(name = "employee_id")
	private Long employeeId;

	@Column(name = "employee_name")
	private String employeeName;

	private String reason;

	@Column(name = "created_at")
	private LocalDateTime createdAt = LocalDateTime.now();

	public SkippedPin() {
	}

	public SkippedPin(LocalDate shiftDate, String location, String shiftType, Long shiftTemplateId, Short weekOfPeriod,
			Long employeeId, String employeeName, String reason) {
		this.shiftDate = shiftDate;
		this.location = location;
		this.shiftType = shiftType;
		this.shiftTemplateId = shiftTemplateId;
		this.weekOfPeriod = weekOfPeriod;
		this.employeeId = employeeId;
		this.employeeName = employeeName;
		this.reason = reason;
	}

	public Long getId() { return id; }
	public Long getRotaId() { return rotaId; }
	public void setRotaId(Long rotaId) { this.rotaId = rotaId; }
	public LocalDate getShiftDate() { return shiftDate; }
	public String getLocation() { return location; }
	public String getShiftType() { return shiftType; }
	public Long getShiftTemplateId() { return shiftTemplateId; }
	public Short getWeekOfPeriod() { return weekOfPeriod; }
	public Long getEmployeeId() { return employeeId; }
	public String getEmployeeName() { return employeeName; }
	public String getReason() { return reason; }
	public LocalDateTime getCreatedAt() { return createdAt; }
}
