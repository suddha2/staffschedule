package com.midco.rota.integration;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One holiday / absence record from the PeopleHR API. Only the fields the sync
 * needs are mapped.
 *
 * <p><b>Field mapping is provisional</b> — the {@code @JsonProperty} names are
 * placeholders. Adjust them to PeopleHR's real response when available (the only
 * change needed to go live). PeopleHR's employee identifier must resolve to the
 * live employee id — if it uses its own code, add a bridge upstream.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PeopleHrLeaveRecord {

	/** PeopleHR employee id — the primary match key (stored on employee.peoplehr_employee_id). */
	@JsonProperty("employeeId")
	private String sourceEmployeeId;

	/** Employee email — fallback match key when the external id isn't stored yet. */
	@JsonProperty("employeeEmail")
	private String employeeEmail;

	@JsonProperty("startDate")
	private LocalDate startDate;

	@JsonProperty("endDate")
	private LocalDate endDate;

	/** Stable id of this holiday/absence in PeopleHR — the upsert key. */
	@JsonProperty("holidayId")
	private String externalRef;

	/** PeopleHR leave category (Holiday / Sickness / ...); mapped by the sync. */
	@JsonProperty("leaveType")
	private String type;

	@JsonProperty("reason")
	private String reason;

	/** True if the holiday was cancelled/declined in PeopleHR. */
	@JsonProperty("cancelled")
	private boolean cancelled;

	public String getSourceEmployeeId() {
		return sourceEmployeeId;
	}

	public void setSourceEmployeeId(String sourceEmployeeId) {
		this.sourceEmployeeId = sourceEmployeeId;
	}

	public String getEmployeeEmail() {
		return employeeEmail;
	}

	public void setEmployeeEmail(String employeeEmail) {
		this.employeeEmail = employeeEmail;
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
