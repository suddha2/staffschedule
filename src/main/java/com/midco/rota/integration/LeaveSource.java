package com.midco.rota.integration;

import java.time.LocalDate;
import java.util.List;

import com.midco.rota.util.AvailabilitySource;

/**
 * A feed of leave / unavailability records from one external system. Each
 * implementation is a Spring bean; {@code LeaveSyncService} injects all of them
 * and syncs each enabled one into {@code employee_availability}, tagged with its
 * {@link #source()}.
 *
 * <p>Adding a new HR/rostering system is just another bean implementing this
 * interface — no change to the sync loop.
 */
public interface LeaveSource {

	/** Which {@code source} column value rows from this feed carry. */
	AvailabilitySource source();

	/** Whether this feed is switched on (usually driven by its own properties). */
	boolean isEnabled();

	/**
	 * Fetch records overlapping [from, to]. Must return an empty list (never
	 * throw) when disabled, unconfigured, or the call fails, so a transient
	 * outage never wipes good local data.
	 */
	List<LeaveRecord> fetch(LocalDate from, LocalDate to);
}
