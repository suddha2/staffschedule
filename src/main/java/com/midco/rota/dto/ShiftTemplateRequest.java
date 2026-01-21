package com.midco.rota.dto;

import java.time.DayOfWeek;
import java.util.List;

import com.midco.rota.model.ShiftTemplate;

public class ShiftTemplateRequest extends ShiftTemplate {

	private List<DayOfWeek> daysOfWeek;

	public List<DayOfWeek> getDaysOfWeek() {
		return daysOfWeek;
	}

	public void setDaysOfWeek(List<DayOfWeek> daysOfWeek) {
		this.daysOfWeek = daysOfWeek;
	}

}
