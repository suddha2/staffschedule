package com.midco.rota;

import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.midco.rota.util.ShiftType;

@Configuration
public class ShiftTypeLimitConfig {

	@Bean
	public static  Map<ShiftType, Integer> maxHoursPerShiftType() {
		return Map.of(ShiftType.LONG_DAY, 15, ShiftType.DAY, 13, ShiftType.FLOATING, 6, ShiftType.WAKING_NIGHT, 12);
	}

	// Per-shift-type weekly caps. LONG_DAY is capped at 7/week here; FLOATING keeps
	// its own cap. DAY / WAKING_NIGHT are intentionally NOT listed — they share one
	// combined weekly budget of 6 enforced by
	// RotaConstraintProvider.limitWeeklyNonLongDayShifts (which also holds each of
	// them to 6 individually). SLEEP_IN is uncapped (it pairs 1:1 with LONG_DAY).
	@Bean
	public static Map<ShiftType, Integer> weeklyShiftTypeLimit() {
		return Map.of(ShiftType.LONG_DAY, 7, ShiftType.FLOATING, 4);
	}
}
