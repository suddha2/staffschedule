package com.midco.rota.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

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

	@Column(name = "shift_start")
	private LocalDate shiftStart;
	@Column(name = "shift_end")
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
		this.shiftEnd = LocalDateTime.of(shiftDate, shiftTemplate.getEndTime()).toLocalDate();

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

	public BigDecimal getDurationInHours() {
		Duration duration = Duration.between(shiftStart.atTime(shiftTemplate.getStartTime()),
				shiftEnd.atTime(shiftTemplate.getEndTime()));

		Duration breakDuration = Optional.ofNullable(shiftTemplate.getBreakStart())
				.flatMap(start -> Optional.ofNullable(shiftTemplate.getBreakEnd())
						.map(end -> Duration.between(shiftStart.atTime(start), shiftStart.atTime(end))))
				.orElse(Duration.ZERO);

		// return duration.minus(breakDuration).toHoursPart();
		long minutes = duration.minus(breakDuration).toMinutes();
		BigDecimal hours = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
		return hours;
	}

	public long getDurationInMins() {
		Duration duration = Duration.between(shiftStart.atTime(shiftTemplate.getStartTime()),
				shiftEnd.atTime(shiftTemplate.getEndTime()));

		Duration breakDuration = Duration.ZERO;

		if (shiftTemplate.getBreakStart() != null && shiftTemplate.getBreakEnd() != null) {
			LocalTime breakStart = shiftTemplate.getBreakStart();
			LocalTime breakEnd = shiftTemplate.getBreakEnd();

			// Calculate break duration considering potential overnight
			long breakMins = Duration.between(breakStart, breakEnd).toMinutes();
			if (breakMins < 0) {
				breakMins += 24 * 60; // Break spans midnight
			}
			breakDuration = Duration.ofMinutes(breakMins);
		}

		long minutes = duration.minus(breakDuration).toMinutes();

		// This should rarely be needed if shiftEnd >= shiftStart
		if (minutes < 0) {
			minutes += 24 * 60;
		}

		return minutes;
	}
}
