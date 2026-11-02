package com.midco.rota.dto;

import java.time.LocalDate;
import java.util.List;

/** What the home screen shows: where the rota stands now, region by region, and what needs doing. */
public record DashboardSummary(LocalDate today, Period currentPeriod, Period nextPeriod, List<RegionRow> regions,
		int pendingRequests, int namesToFix, int solvesRunning) {

	public record Period(String name, LocalDate startDate, LocalDate endDate) {
	}

	/** One period's rota for a region. {@code status} is NOT_STARTED, SOLVING or READY. */
	public record RotaState(String status, Long rotaId, int shifts, int filled, int open) {
	}

	public record RegionRow(String region, int activeCarers, RotaState current, RotaState next, int belowContract,
			int pinsNotApplied) {
	}
}
