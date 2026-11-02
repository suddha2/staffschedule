package com.midco.rota.dto;

/** One standing pin, readable: this carer on this template in this week of the period. */
public record StandingPin(long id, long employeeId, String carer, boolean carerActive, String location, String shiftType,
		String dayOfWeek, int week) {
}
