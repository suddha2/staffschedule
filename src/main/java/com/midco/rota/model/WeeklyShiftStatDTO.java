package com.midco.rota.model;

import java.time.LocalDate;
import java.util.Map;

import com.midco.rota.util.ShiftType;

public class WeeklyShiftStatDTO {
	public int weekNumber;
	public LocalDate weekStart;
	public LocalDate weekEnd;
	public Map<ShiftType, ShiftSummaryDTO> shiftSummary;

	public WeeklyShiftStatDTO(int weekNumber,LocalDate weekStart, LocalDate weekEnd, Map<ShiftType, ShiftSummaryDTO> shiftSummary) {
		this.weekNumber=weekNumber;
		this.weekStart = weekStart;
		this.weekEnd = weekEnd;
		this.shiftSummary = shiftSummary;
	}

}
