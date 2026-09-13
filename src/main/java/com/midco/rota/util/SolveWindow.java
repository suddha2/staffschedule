package com.midco.rota.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * The date range a solve actually runs over. An ad-hoc request (e.g. a
 * short-notice Wed–Fri cover range) is expanded outward to whole ISO weeks
 * (Monday–Sunday) so the weekly constraints — the alternating cap, the weekly
 * minimum, per-location weekly limits — always see complete weeks rather than a
 * partial slice that would misfire at the edges.
 *
 * <p>A range that is already week-aligned (like the Monday-anchored 28-day pay
 * periods) snaps to itself, so existing period solves are unaffected.
 */
public record SolveWindow(LocalDate start, LocalDate end) {

	/**
	 * Expand [requestedStart, requestedEnd] outward to the enclosing whole weeks:
	 * start moves back to its Monday, end moves forward to its Sunday.
	 */
	public static SolveWindow snapToWholeWeeks(LocalDate requestedStart, LocalDate requestedEnd) {
		LocalDate start = requestedStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		LocalDate end = requestedEnd.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
		return new SolveWindow(start, end);
	}

	/** True if this window already begins on a Monday and ends on a Sunday. */
	public boolean isWholeWeeks() {
		return start.getDayOfWeek() == DayOfWeek.MONDAY && end.getDayOfWeek() == DayOfWeek.SUNDAY;
	}
}
