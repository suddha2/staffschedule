package com.midco.rota.model;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.lookup.PlanningId;
import org.optaplanner.core.api.domain.variable.PlanningVariable;

import com.midco.rota.util.Gender;
import com.midco.rota.util.ShiftType;

@PlanningEntity
public class ShiftAssignment {
	private LocalDate date;
	@PlanningId
	private Long id;

	private String location;

	private ShiftType shiftType;

	private DayOfWeek day;

	private LocalTime startTime;

	private LocalTime endTime;

	private BigDecimal totalHours;

	private Gender gender;
	@PlanningVariable(valueRangeProviderRefs = "employeeRange", nullable = true)
	private Employee employee; // planning variable

	public ShiftAssignment(LocalDate date, Long id, String location, ShiftType shiftType, DayOfWeek day,
			LocalTime startTime, LocalTime endTime, BigDecimal totalHours, Gender gender, Employee employee) {
		super();
		this.date = date;
		this.id = id;
		this.location = location;
		this.shiftType = shiftType;
		this.day = day;
		this.startTime = startTime;
		this.endTime = endTime;
		this.totalHours = totalHours;
		this.gender = gender;
		this.employee = employee;
	}

	public ShiftAssignment() {
		super();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public ShiftType getShiftType() {
		return shiftType;
	}

	public void setShiftType(ShiftType shiftType) {
		this.shiftType = shiftType;
	}

	public DayOfWeek getDay() {
		return day;
	}

	public void setDay(DayOfWeek day) {
		this.day = day;
	}

	public LocalTime getStartTime() {
		return startTime;
	}

	public void setStartTime(LocalTime startTime) {
		this.startTime = startTime;
	}

	public LocalTime getEndTime() {
		return endTime;
	}

	public void setEndTime(LocalTime endTime) {
		this.endTime = endTime;
	}

	public BigDecimal getTotalHours() {
		return totalHours;
	}

	public void setTotalHours(BigDecimal totalHours) {
		this.totalHours = totalHours;
	}

	public Gender getGender() {
		return gender;
	}

	public void setGender(Gender gender) {
		this.gender = gender;
	}

	public void setDate(LocalDate date) {
		this.date = date;
	}

	public LocalDate getDate() {
		return date;
	}

	
	public Employee getEmployee() {
		return employee;
	}

	public void setEmployee(Employee employee) {
		this.employee = employee;
	}

	@Override
	public String toString() {
		return " ShiftAssignment [date=" + date + ", id=" + id + ", location=" + location + ", shiftType=" + shiftType
				+ ", day=" + day + ", startTime=" + startTime + ", endTime=" + endTime + ", totalHours=" + totalHours
				+ ", gender=" + gender + ", employee="
				+ (employee == null ? "UNASSIGNED" : employee.getFirstName() + " " + employee.getLastName()) + "]";
	}

}
