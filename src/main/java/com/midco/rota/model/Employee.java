package com.midco.rota.model;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.List;

import org.optaplanner.core.api.domain.lookup.PlanningId;

import com.midco.rota.converter.DayOfWeekListConverter;
import com.midco.rota.converter.ShiftTypeListConverter;
import com.midco.rota.converter.StringListConverter;
import com.midco.rota.util.ContractType;
import com.midco.rota.util.Gender;
import com.midco.rota.util.RateCode;
import com.midco.rota.util.ShiftType;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "employee")
public class Employee {
	@Id
	@PlanningId
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(name = "first_name", length = 50)
	private String firstName;

	@Column(name = "last_name", length = 50)
	private String lastName;

	@Enumerated(EnumType.STRING)
	@Column(length = 10)
	private Gender gender;

	@Enumerated(EnumType.STRING)
	@Column(name = "contract_type", length = 20)
	private ContractType contractType;

	@Column(name = "min_hours", precision = 5, scale = 2)
	private BigDecimal minHrs;

	@Column(name = "max_hours", precision = 5, scale = 2)
	private BigDecimal maxHrs;

	@Enumerated(EnumType.STRING)
	@Column(name = "rate_code", length = 10)
	private RateCode rateCode;

	@Column(name = "rest_days_per_cycle")
	private Integer restDays;

//	@Convert(converter = StringListConverter.class)
//	@Column(name = "preferred_region", length = 50)
	private String preferredRegion;

	@Convert(converter = StringListConverter.class)
	@Column(name = "preferred_service", columnDefinition = "TEXT")
	private List<String> preferredService;

	@Convert(converter = StringListConverter.class)
	@Column(name = "restricted_service", columnDefinition = "TEXT")
	private List<String> restrictedService;

	@Convert(converter = DayOfWeekListConverter.class)
	@Column(name = "preferred_days", columnDefinition = "TEXT")
	private List<DayOfWeek> preferredDays;

	@Convert(converter = DayOfWeekListConverter.class)
	@Column(name = "restricted_days", columnDefinition = "TEXT")
	private List<DayOfWeek> restrictedDays;

	@Convert(converter = ShiftTypeListConverter.class)
	@Column(name = "preferred_shifts", columnDefinition = "TEXT")
	private List<ShiftType> preferredShifts;

	@Convert(converter = ShiftTypeListConverter.class)
	@Column(name = "restricted_shifts", columnDefinition = "TEXT")
	private List<ShiftType> restrictedShifts;

	@Convert(converter = StringListConverter.class)
	@Column(name = "skills", columnDefinition = "TEXT")
	private List<String> skills;

	@Column(name = "days_on")
	private Integer daysOn;

	@Column(name = "days_off")
	private Integer daysOff;

	@Column(name = "week_on")
	private Integer weekOn;

	@Column(name = "week_off")
	private Integer weekOff;

	@Override
	public String toString() {
		return "Employee [id=" + id + ", firstName=" + firstName + ", lastName=" + lastName + ", gender=" + gender
				+ ", contractType=" + contractType + ", minHrs=" + minHrs + ", maxHrs=" + maxHrs + ", rateCode="
				+ rateCode + ", restDays=" + restDays + ", preferredRegion=" + preferredRegion + ", preferredService="
				+ preferredService + ", preferredShift=" + preferredShifts + ", preferredDay=" + preferredDays
				+ ", restrictedDay=" + restrictedDays + ", restrictedShift=" + restrictedShifts + ", restrictedService="
				+ restrictedService + ", contDaysOn=" + daysOn + ", contDaysOff=" + daysOff + ", contWeekOn=" + weekOn
				+ ", contWeekOff=" + weekOff + " skills " + skills + "]";
	}

	// No-arg constructor
	public Employee() {
	}

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

	public Gender getGender() {
		return gender;
	}

	public void setGender(Gender gender) {
		this.gender = gender;
	}

	public ContractType getContractType() {
		return contractType;
	}

	public void setContractType(ContractType contractType) {
		this.contractType = contractType;
	}

	public BigDecimal getMinHrs() {
		return minHrs;
	}

	public void setMinHrs(BigDecimal minHrs) {
		this.minHrs = minHrs;
	}

	public BigDecimal getMaxHrs() {
		return maxHrs;
	}

	public void setMaxHrs(BigDecimal maxHrs) {
		this.maxHrs = maxHrs;
	}

	public RateCode getRateCode() {
		return rateCode;
	}

	public void setRateCode(RateCode rateCode) {
		this.rateCode = rateCode;
	}

	public Integer getRestDays() {
		return restDays;
	}

	public void setRestDays(Integer restDays) {
		this.restDays = restDays;
	}

	public String getPreferredRegion() {
		return preferredRegion;
	}

	public void setPreferredRegion(String preferredRegion) {
		this.preferredRegion = preferredRegion;
	}

	public List<String> getPreferredService() {
		return preferredService;
	}

	public void setPreferredService(List<String> preferredService) {
		this.preferredService = preferredService;
	}

	public List<ShiftType> getPreferredShift() {
		return preferredShifts;
	}

	public void setPreferredShift(List<ShiftType> preferredShifts) {
		this.preferredShifts = preferredShifts;
	}

	public List<DayOfWeek> getPreferredDay() {
		return preferredDays;
	}

	public void setPreferredDay(List<DayOfWeek> preferredDay) {
		this.preferredDays = preferredDay;
	}

	public List<DayOfWeek> getRestrictedDay() {
		return restrictedDays;
	}

	public void setRestrictedDay(List<DayOfWeek> restrictedDay) {
		this.restrictedDays = restrictedDay;
	}

	public List<ShiftType> getRestrictedShift() {
		return restrictedShifts;
	}

	public void setRestrictedShift(List<ShiftType> restrictedShift) {
		this.restrictedShifts = restrictedShift;
	}

	public List<String> getRestrictedService() {
		return restrictedService;
	}

	public void setRestrictedService(List<String> restrictedService) {
		this.restrictedService = restrictedService;
	}

	public Integer getContDaysOn() {
		return daysOn;
	}

	public void setContDaysOn(Integer contDaysOn) {
		this.daysOn = contDaysOn;
	}

	public Integer getContDaysOff() {
		return daysOff;
	}

	public void setContDaysOff(Integer contDaysOff) {
		this.daysOff = contDaysOff;
	}

	public Integer getContWeekOn() {
		return weekOn;
	}

	public void setContWeekOn(Integer contWeekOn) {
		this.weekOn = contWeekOn;
	}

	public Integer getContWeekOff() {
		return weekOff;
	}

	public void setContWeekOff(Integer contWeekOff) {
		this.weekOff = contWeekOff;
	}

	public String getName() {
		return this.firstName + " " + this.lastName;
	}

	public List<DayOfWeek> getPreferredDays() {
		return preferredDays;
	}

	public void setPreferredDays(List<DayOfWeek> preferredDays) {
		this.preferredDays = preferredDays;
	}

	public List<DayOfWeek> getRestrictedDays() {
		return restrictedDays;
	}

	public void setRestrictedDays(List<DayOfWeek> restrictedDays) {
		this.restrictedDays = restrictedDays;
	}

	public List<ShiftType> getPreferredShifts() {
		return preferredShifts;
	}

	public void setPreferredShifts(List<ShiftType> preferredShifts) {
		this.preferredShifts = preferredShifts;
	}

	public List<ShiftType> getRestrictedShifts() {
		return restrictedShifts;
	}

	public void setRestrictedShifts(List<ShiftType> restrictedShifts) {
		this.restrictedShifts = restrictedShifts;
	}

	public List<String> getSkills() {
		return skills;
	}

	public void setSkills(List<String> skills) {
		this.skills = skills;
	}

	public Integer getDaysOn() {
		return daysOn;
	}

	public void setDaysOn(Integer daysOn) {
		this.daysOn = daysOn;
	}

	public Integer getDaysOff() {
		return daysOff;
	}

	public void setDaysOff(Integer daysOff) {
		this.daysOff = daysOff;
	}

	public Integer getWeekOn() {
		return weekOn;
	}

	public void setWeekOn(Integer weekOn) {
		this.weekOn = weekOn;
	}

	public Integer getWeekOff() {
		return weekOff;
	}

	public void setWeekOff(Integer weekOff) {
		this.weekOff = weekOff;
	}

}
