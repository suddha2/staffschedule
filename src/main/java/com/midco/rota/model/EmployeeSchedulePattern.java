package com.midco.rota.model;

import com.midco.rota.util.ShiftType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "employee_schedule_patterns")
public class EmployeeSchedulePattern {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "employee_id")
    private Employee employee;
    
    @Column(name = "location")
    private String location;
    
    @Column(name = "week_number")
    private Integer weekNumber;  // 1-4 for 4-week rotation
    
    @Column(name = "day_of_week")
    private String dayOfWeek;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "shift_type")
    private ShiftType shiftType;  // NULL means OFF
    
    @Column(name = "is_available")
    private Boolean isAvailable;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Employee getEmployee() {
		return employee;
	}

	public void setEmployee(Employee employee) {
		this.employee = employee;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public Integer getWeekNumber() {
		return weekNumber;
	}

	public void setWeekNumber(Integer weekNumber) {
		this.weekNumber = weekNumber;
	}

	public String getDayOfWeek() {
		return dayOfWeek;
	}

	public void setDayOfWeek(String dayOfWeek) {
		this.dayOfWeek = dayOfWeek;
	}

	public ShiftType getShiftType() {
		return shiftType;
	}

	public void setShiftType(ShiftType shiftType) {
		this.shiftType = shiftType;
	}

	public Boolean getIsAvailable() {
		return isAvailable;
	}

	public void setIsAvailable(Boolean isAvailable) {
		this.isAvailable = isAvailable;
	}

	@Override
	public String toString() {
		return "EmployeeSchedulePattern [id=" + id + ", employee=" + employee + ", location=" + location
				+ ", weekNumber=" + weekNumber + ", dayOfWeek=" + dayOfWeek + ", shiftType=" + shiftType
				+ ", isAvailable=" + isAvailable + "]";
	}
    

}
