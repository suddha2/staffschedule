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

	@Bean
	public static Map<ShiftType, Integer> weeklyShiftTypeLimit() {
		return Map.of(ShiftType.LONG_DAY, 7, ShiftType.DAY, 4, ShiftType.FLOATING, 4, ShiftType.WAKING_NIGHT, 6);
	}
}
