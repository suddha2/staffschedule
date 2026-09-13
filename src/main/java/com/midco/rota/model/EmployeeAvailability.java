package com.midco.rota.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.midco.rota.util.AvailabilitySource;
import com.midco.rota.util.AvailabilityType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * A span during which an employee cannot be allocated — booked leave, sickness,
 * or other unavailability. Read by the solver as a problem fact; the
 * "Employee unavailable (leave)" hard constraint stops any assignment landing on
 * a date this record covers.
 *
 * <p>Designed to be filled by a <b>daily job from the People Planner Data Engine
 * API</b>, and to coexist with manual entry. The daily job upserts on
 * ({@code source}, {@code externalRef}) so re-running it is idempotent and a
 * cancelled leave in People Planner removes the row rather than duplicating it.
 *
 * <p>{@code employeeId} is stored as a plain value (not a JPA association) so the
 * solver sees a flat fact with no lazy proxy; it matches {@link Employee#getId()}.
 * Dates are whole-day and inclusive: a shift starting on any date in
 * [{@code startDate}, {@code endDate}] is blocked.
 */
@Entity
@Table(name = "employee_availability",
		indexes = {
				@Index(name = "ix_avail_emp", columnList = "employee_id"),
				@Index(name = "ix_avail_dates", columnList = "start_date,end_date") })
// Idempotent-sync uniqueness is a PARTIAL unique index on (source, external_ref)
// where external_ref is not null (so manual rows with no ref don't collide) --
// defined in V010; not expressible as a JPA @UniqueConstraint, and ddl-auto=none
// means annotations here are informational only.
public class EmployeeAvailability {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "employee_id", nullable = false)
	private Integer employeeId;

	@Column(name = "start_date", nullable = false)
	private LocalDate startDate;

	@Column(name = "end_date", nullable = false)
	private LocalDate endDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", length = 20, nullable = false)
	private AvailabilityType type = AvailabilityType.PLANNED_LEAVE;

	@Enumerated(EnumType.STRING)
	@Column(name = "source", length = 10, nullable = false)
	private AvailabilitySource source = AvailabilitySource.MANUAL;

	/** The originating record's id in the source system (e.g. PP EmployeeUnavailabilityID). Null for manual. */
	@Column(name = "external_ref", length = 100)
	private String externalRef;

	@Column(name = "reason", length = 255)
	private String reason;

	@Column(name = "synced_at")
	private LocalDateTime syncedAt;

	public EmployeeAvailability() {
	}

	public EmployeeAvailability(Integer employeeId, LocalDate startDate, LocalDate endDate,
			AvailabilityType type, AvailabilitySource source, String externalRef) {
		this.employeeId = employeeId;
		this.startDate = startDate;
		this.endDate = endDate;
		this.type = type;
		this.source = source;
		this.externalRef = externalRef;
	}

	/** True if the given (whole-day) date falls within this unavailability span, inclusive. */
	public boolean coversDate(LocalDate date) {
		return date != null && startDate != null && endDate != null
				&& !date.isBefore(startDate) && !date.isAfter(endDate);
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Integer getEmployeeId() {
		return employeeId;
	}

	public void setEmployeeId(Integer employeeId) {
		this.employeeId = employeeId;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public void setStartDate(LocalDate startDate) {
		this.startDate = startDate;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public void setEndDate(LocalDate endDate) {
		this.endDate = endDate;
	}

	public AvailabilityType getType() {
		return type;
	}

	public void setType(AvailabilityType type) {
		this.type = type;
	}

	public AvailabilitySource getSource() {
		return source;
	}

	public void setSource(AvailabilitySource source) {
		this.source = source;
	}

	public String getExternalRef() {
		return externalRef;
	}

	public void setExternalRef(String externalRef) {
		this.externalRef = externalRef;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public LocalDateTime getSyncedAt() {
		return syncedAt;
	}

	public void setSyncedAt(LocalDateTime syncedAt) {
		this.syncedAt = syncedAt;
	}
}
