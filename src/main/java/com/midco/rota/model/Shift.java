package com.midco.rota.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

@Entity
public class Shift {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	
	@Column(name="shift_start")
	private LocalDate shiftStart;
	@Column(name="shift_end")
	private LocalDate shiftEnd;

	@OneToOne
	@JoinColumn(name = "shift_template_id")
	private ShiftTemplate shiftTemplate;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
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
	
	public Shift() {
		
	}

	public Shift(LocalDate shiftDate, ShiftTemplate shiftTemplate) {
		
		this.shiftStart = shiftDate;// LocalDateTime.of(shiftDate, shiftTemplate.getStartTime());
		this.shiftTemplate = shiftTemplate;
		this.shiftEnd=LocalDateTime.of(shiftDate, shiftTemplate.getEndTime()).toLocalDate();

		// adjust date to cover overnight shifts
		if (LocalDateTime.of(shiftDate, shiftTemplate.getEndTime())
				.isBefore(LocalDateTime.of(shiftDate, shiftTemplate.getStartTime()))) {
			this.shiftEnd = this.shiftStart.plusDays(1);
		}
	}

	public LocalDate getShiftEnd() {
		return shiftEnd;
	}

	public void setShiftEnd(LocalDate shiftEnd) {
		this.shiftEnd = shiftEnd;
	}

}
