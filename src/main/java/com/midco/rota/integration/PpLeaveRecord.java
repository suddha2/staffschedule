package com.midco.rota.integration;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One leave / unavailability record as returned by the People Planner Data
 * Engine API. Only the fields the sync needs are mapped; everything else is
 * ignored.
 *
 * <p><b>Field mapping is provisional</b> — the {@code @JsonProperty} names below
 * are placeholders. When the real API response shape is available, adjust these
 * names to match (that is the only change needed to go live). The staging
 * fields the entity needs are: employee id, start date, end date, a stable
 * record id (for idempotent upsert), and optionally a type/reason.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PpLeaveRecord {

	/** PP employee id — must match live employee id (or be bridged upstream). */
	@JsonProperty("employeeId")
	private Integer employeeId;

	@JsonProperty("startDate")
	private LocalDate startDate;

	@JsonProperty("endDate")
	private LocalDate endDate;

	/** Stable id of this unavailability in PP (e.g. EmployeeUnavailabilityID) — the upsert key. */
	@JsonProperty("unavailabilityId")
	private String externalRef;

	/** Free-text category from PP; mapped to AvailabilityType by the sync (defaults to PLANNED_LEAVE). */
	@JsonProperty("type")
	private String type;

	@JsonProperty("reason")
	private String reason;

	/** True if this record represents a cancelled/withdrawn leave — the sync deletes it locally. */
	@JsonProperty("cancelled")
	private boolean cancelled;

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

	public String getExternalRef() {
		return externalRef;
	}

	public void setExternalRef(String externalRef) {
		this.externalRef = externalRef;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public boolean isCancelled() {
		return cancelled;
	}

	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}
}
