package com.midco.rota.util;

/**
 * Where an availability record came from. Kept source-agnostic so more than one
 * feed can coexist: the daily People Planner sync is the primary feed, with
 * manual entry as the always-available fallback.
 */
public enum AvailabilitySource {

	/** Synced from the People Planner Data Engine API by the daily job. */
	PP_API,

	/** Synced from an HR system. */
	HR_API,

	/** Entered by hand in the scheduler admin UI. */
	MANUAL
}
