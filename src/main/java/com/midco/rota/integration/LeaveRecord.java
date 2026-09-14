package com.midco.rota.integration;

import java.time.LocalDate;

/**
 * A leave / unavailability record normalised across source systems (People
 * Planner, PeopleHR, ...). Each {@link LeaveSource} maps its own API response
 * into this shape; {@code LeaveSyncService} resolves the employee and upserts it
 * into {@code employee_availability} keyed by (source, externalRef).
 *
 * <p><b>Employee matching:</b> primarily by the source system's own employee id
 * ({@code sourceEmployeeId}), held on the employee record as {@code pp_employee_id}
 * / {@code peoplehr_employee_id}. This is stable across email changes and encodes
 * the domain split (zero-hours ↔ People Planner, contracted ↔ PeopleHR). Email is
 * kept as a bootstrap fallback for employees whose external id is not populated
 * yet.
 *
 * @param sourceEmployeeId the employee's id in THIS source system (primary match key)
 * @param employeeEmail    fallback match key when the external id is not yet stored
 * @param startDate        first day off (inclusive)
 * @param endDate          last day off (inclusive)
 * @param type             source's raw category (mapped to AvailabilityType by the sync)
 * @param externalRef      stable id of this leave record in the source system (upsert key)
 * @param reason           optional free text
 * @param cancelled        true if the source says this leave was withdrawn (sync deletes it)
 */
public record LeaveRecord(
		String sourceEmployeeId,
		String employeeEmail,
		LocalDate startDate,
		LocalDate endDate,
		String type,
		String externalRef,
		String reason,
		boolean cancelled) {

	/** Usable only with the fields the resolve + upsert need: some employee key, dates, and a record id. */
	public boolean isValid() {
		boolean hasEmployeeKey = (sourceEmployeeId != null && !sourceEmployeeId.isBlank())
				|| (employeeEmail != null && !employeeEmail.isBlank());
		return hasEmployeeKey && startDate != null && endDate != null
				&& externalRef != null && !externalRef.isBlank();
	}
}
