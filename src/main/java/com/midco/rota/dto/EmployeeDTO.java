package com.midco.rota.dto;

import com.midco.rota.converter.DayOfWeekListConverter;
import com.midco.rota.converter.ShiftTypeListConverter;
import com.midco.rota.converter.StringListConverter;
import com.midco.rota.model.Employee;
import com.midco.rota.util.ContractType;
import com.midco.rota.util.Gender;
import com.midco.rota.util.RateCode;
import com.midco.rota.util.ShiftType;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.List;

import org.optaplanner.core.api.domain.lookup.PlanningId;

public class EmployeeDTO {
	private Integer id;
	private String firstName;
	private String lastName;
	private String name;
	private Gender gender;
	private ContractType contractType;
	private BigDecimal minHrs;
	private BigDecimal maxHrs;

	private RateCode rateCode;

	private Integer restDays;

	private String preferredRegion;

	private List<String> preferredService;

	private List<String> restrictedService;

	private List<DayOfWeek> preferredDays;

	private List<DayOfWeek> restrictedDays;

	private List<ShiftType> preferredShifts;

	private List<ShiftType> restrictedShifts;

	private List<String> skills;

	private Integer daysOn;

	private Integer daysOff;

	private Integer weekOn;

	private Integer weekOff;

	private Boolean invertPattern;

	// Getters and setters
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	// ... all other getters/setters

	// ✅ Converter from Entity
	public static EmployeeDTO fromEntity(Employee emp) {
		if (emp == null)
			return null;

		EmployeeDTO dto = new EmployeeDTO();
		dto.setId(emp.getId());
		dto.setFirstName(emp.getFirstName());
		dto.setLastName(emp.getLastName());
		dto.setName(emp.getName());
		dto.setGender(emp.getGender());
		dto.setContractType(emp.getContractType());
		dto.setMinHrs(emp.getMinHrs());
		dto.setMaxHrs(emp.getMaxHrs());

		return dto;
	}

	private void setMaxHrs(BigDecimal maxHrs2) {
		this.maxHrs = maxHrs2;
	}

	private void setMinHrs(BigDecimal minHrs2) {
		this.minHrs=minHrs2;

	}

	private void setContractType(ContractType contractType2) {
		this.contractType=contractType2;

	}

	private void setGender(Gender gender2) {
		this.gender=gender2;

	}

	public Gender getGender() {
		return gender;
	}

	public ContractType getContractType() {
		return contractType;
	}

	public BigDecimal getMinHrs() {
		return minHrs;
	}

	public BigDecimal getMaxHrs() {
		return maxHrs;
	}
}