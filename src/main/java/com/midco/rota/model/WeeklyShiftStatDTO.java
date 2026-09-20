package com.midco.rota.model;

import java.time.LocalDate;
import java.util.Map;

public class WeeklyShiftStatDTO {
	public int weekNumber;
	public LocalDate weekStart;
	public LocalDate weekEnd;
	// Keyed by shift-type CODE (data-driven), not the legacy enum, so new types flow through.
	public Map<String, ShiftSummaryDTO> shiftSummary;

	public WeeklyShiftStatDTO(int weekNumber, LocalDate weekStart, LocalDate weekEnd,
			Map<String, ShiftSummaryDTO> shiftSummary) {
		this.weekNumber = weekNumber;
		this.weekStart = weekStart;
		this.weekEnd = weekEnd;
		this.shiftSummary = shiftSummary;
	}

}
