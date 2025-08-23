package com.midco.rota.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Shift {
	private long id;
	private LocalDate  shiftStart;
	private LocalDate shiftEnd;
	private ShiftTemplate shiftTemplate;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public LocalDate getShiftStart() {
		return shiftStart;
	}

	public void setShiftStart(LocalDate shiftStart) {
		this.shiftStart = shiftStart;
	}

	public ShiftTemplate getShiftTemplate() {
		return shiftTemplate;
	}

	public void setShiftTemplate(ShiftTemplate shiftTemplate) {
		this.shiftTemplate = shiftTemplate;
	}

	public Shift(long id, LocalDate shiftDate, ShiftTemplate shiftTemplate) {
		super();
		this.id = id;
		this.shiftStart =shiftDate;// LocalDateTime.of(shiftDate, shiftTemplate.getStartTime());
		this.shiftTemplate = shiftTemplate;
		
		// adjust date to cover overnight shifts
		if(LocalDateTime.of(shiftDate, shiftTemplate.getEndTime()).isBefore(LocalDateTime.of(shiftDate, shiftTemplate.getStartTime()))) {
			this.shiftEnd=this.shiftStart.plusDays(1);
			
		}
	}

	public LocalDate  getShiftEnd() {
		return shiftEnd;
	}

	public void setShiftEnd(LocalDate shiftEnd) {
		this.shiftEnd = shiftEnd;
	}

}
