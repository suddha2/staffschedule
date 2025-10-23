package com.midco.rota.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class WeekStatsDTO {
	public int weekNumber;
	public LocalDate start;
	public LocalDate end;
	public List<ShiftTypeStatsDTO> shiftStats = new ArrayList<>();

}
