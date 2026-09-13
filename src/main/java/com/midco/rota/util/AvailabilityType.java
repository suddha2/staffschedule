package com.midco.rota.util;

/**
 * Why an employee is unavailable for a shift. All types exclude allocation the
 * same way today; the distinction is kept for reporting and for future rules
 * (e.g. treating requested-but-unapproved leave as soft).
 */
public enum AvailabilityType {

	/** Booked, approved annual leave. */
	PLANNED_LEAVE,

	/** Sickness absence. */
	SICK,

	/** Any other recorded unavailability (appointments, working-pattern gaps). */
	UNAVAILABLE,

	/** Training/shadowing that blocks normal allocation. */
	TRAINING
}
