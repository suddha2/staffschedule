package com.midco.rota.util;

public class MorphiaUtil {

	public static ShiftType toShiftType(String s) {
		String key = s.trim().toUpperCase();
		ShiftType type;
		switch (key) {
		case "DAY":
			type = ShiftType.DAY;
			break;
		case "LONG DAY":
			type = ShiftType.LONG_DAY;
			break;
		case "WAKING NIGHT":
			type = ShiftType.WAKING_NIGHT;
			break;
		case "FLOATING":
			type = ShiftType.FLOATING;
			break;
		default:
			throw new IllegalArgumentException("Unknown shift type: " + s);
		}
		return type;
	}

	public static Day toDay(String s) {
		String key = s.trim().toUpperCase();
		Day day;
		switch (key) {
		case "SUNDAY", "SUN":
			day = Day.SUN;
			break;
		case "MONDAY", "MON":
			day = Day.MON;
			break;
		case "TUESDAY", "TUE":
			day = Day.TUE;
			break;
		case "WEDNESDAY", "WED":
			day = Day.WED;
			break;
		case "THURSDAY", "THU":
			day = Day.THU;
			break;
		case "FRIDAY", "FRI":
			day = Day.FRI;
			break;
		case "SATURDAY", "SAT":
			day = Day.SAT;
			break;
		default:
			throw new IllegalArgumentException("Unknown day type: " + s);
		}
		return day;
	}
}
