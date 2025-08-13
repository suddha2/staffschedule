package com.midco.rota.model;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import com.midco.rota.converter.StringListConverter;
import com.midco.rota.util.Gender;
import com.midco.rota.util.ShiftType;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

@Entity
@Table(name = "shifts")
@NamedQuery(name="Shift.findAllRegion", query = "select distinct s.region from Shift s order by 1")

public class Shift {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(length = 50)
	private String location;

	@Column(length = 50)
	private String region;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "shift_type", length = 20)
	private ShiftType shiftType;
	@Enumerated(EnumType.STRING)
	@Column(name = "day_of_week", length = 15)
	private DayOfWeek dayOfWeek;

	@Column(name = "start_time")
	private LocalTime startTime;

	@Column(name = "end_time")
	private LocalTime endTime;

	@Column(name = "break_start")
	private LocalTime breakStart;

	@Column(name = "break_end")
	private LocalTime breakEnd;

	@Column(name = "total_hours", precision = 4, scale = 2)
	private BigDecimal totalHours;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "required_gender", length = 10)
	private Gender requiredGender;

	@Convert(converter = StringListConverter.class)
	@Column(name = "required_skills", columnDefinition = "TEXT")
	private List<String> requiredSkills;

	public Shift() {
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public void setShiftType(ShiftType shiftType) {
		this.shiftType = shiftType;
	}

	public void setDay(DayOfWeek day) {
		this.dayOfWeek = day;
	}

	public void setStartTime(LocalTime startTime) {
		this.startTime = startTime;
	}

	public void setEndTime(LocalTime endTime) {
		this.endTime = endTime;
	}

	public void setTotalHours(BigDecimal totalHours) {
		this.totalHours = totalHours;
	}

	public void setGender(Gender gender) {
		this.requiredGender = gender;
	}

	public String getLocation() {
		return location;
	}

	public ShiftType getShiftType() {
		return shiftType;
	}

	public DayOfWeek getDay() {
		return dayOfWeek;
	}

	public LocalTime getStartTime() {
		return startTime;
	}

	public LocalTime getEndTime() {
		return endTime;
	}

	public BigDecimal getTotalHours() {
		return totalHours;
	}

	public Gender getGender() {
		return requiredGender;
	}

	@Override
	public String toString() {
		return "Shift [id=" + id + ", region="+region+", location=" + location + ", shiftType=" + shiftType + ", day=" + dayOfWeek
				+ ", startTime=" + startTime + ", endTime=" + endTime + ", totalHours=" + totalHours + ", gender="
				+ requiredGender + "skills = " + requiredSkills + "]";
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

}
