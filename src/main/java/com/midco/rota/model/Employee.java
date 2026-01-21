package com.midco.rota.model;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.optaplanner.core.api.domain.lookup.PlanningId;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.midco.rota.converter.DayOfWeekListConverter;
import com.midco.rota.converter.ShiftTypeListConverter;
import com.midco.rota.converter.StringListConverter;
import com.midco.rota.service.PeriodService;
import com.midco.rota.util.ContractType;
import com.midco.rota.util.Gender;
import com.midco.rota.util.RateCode;
import com.midco.rota.util.ShiftType;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "employee")
//@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")

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

	@Column(name = "invert_pattern")
	
	private Boolean invertPattern;
	
	@Column(name="active")
	private boolean active;

	@OneToMany(mappedBy = "employee", fetch = FetchType.EAGER)
	@JsonIgnore
	private List<EmployeeSchedulePattern> schedulePatterns;

	@Transient
	@JsonIgnore
	private static PeriodService periodService;

	@Transient
	@JsonIgnore
	private Map<String, Map<Integer, Map<String, EmployeeSchedulePattern>>> patternCache;

	@Transient
	@JsonIgnore
	private Map<String, Integer> preferredServiceWeightsMap;

	// ============================================================================
	// CONSTRUCTORS
	// ============================================================================

	public Employee() {
	}

	// ============================================================================
	// LOCATION PREFERENCE HELPERS (NEW)
	// ============================================================================

	/**
	 * Parse preferred service list into location -> weightage map
	 * Format: "LOCATION:60" or "LOCATION" (defaults to 100)
	 */
	private void parsePreferredServiceWeights() {
		preferredServiceWeightsMap = new HashMap<>();

		if (preferredService == null || preferredService.isEmpty()) {
			return;
		}

		for (String entry : preferredService) {
			if (entry == null || entry.trim().isEmpty()) {
				continue;
			}

			try {
				// Check if entry has weight (contains colon)
				if (!entry.contains(":")) {
					// Old format without weight - treat as 100% preference
					preferredServiceWeightsMap.put(entry.trim(), 100);
					continue;
				}

				// Parse "Location:60"
				String[] parts = entry.split(":", 2); // Limit to 2 parts in case location name has ":"
				String location = parts[0].trim();
				int weight = Integer.parseInt(parts[1].trim());

				// Validate weight range
				if (weight >= 0 && weight <= 100) {
					preferredServiceWeightsMap.put(location, weight);
				}

			} catch (NumberFormatException e) {
				// Invalid weight format - skip this entry
				continue;
			} catch (Exception e) {
				// Any other parsing error - skip
				continue;
			}
		}
	}

	/**
	 * Get the weightage map (lazy initialization)
	 */
	public Map<String, Integer> getPreferredServiceWeightsMap() {
		if (preferredServiceWeightsMap == null) {
			parsePreferredServiceWeights();
		}
		return preferredServiceWeightsMap;
	}

	/**
	 * Get weightage for a specific location (0-100)
	 */
	public int getServiceWeightage(String serviceLocation) {
		return getPreferredServiceWeightsMap().getOrDefault(serviceLocation, 0);
	}

	/**
	 * Check if employee has any service preferences defined
	 */
	public boolean hasServicePreferences() {
		return preferredService != null && !preferredService.isEmpty() 
				&& !getPreferredServiceWeightsMap().isEmpty();
	}

	/**
	 * Get list of all preferred locations (without weights)
	 */
//	public List<String> getPreferredLocations() {
//		if (preferredService == null) {
//			return List.of();
//		}
//
//		return preferredService.stream().map(entry -> {
//			// Extract location part (before colon)
//			if (entry.contains(":")) {
//				return entry.split(":")[0].trim();
//			}
//			return entry.trim();
//		}).filter(loc -> !loc.isEmpty()).collect(Collectors.toList());
//	}

	/**
	 * Check if employee prefers working at a specific location (any weight > 0)
	 * Replaces: preferredService.contains(location) for backward compatibility
	 */
	public boolean prefersLocation(String location) {
		return getServiceWeightage(location) > 0;
	}

	/**
	 * ✅ NEW: Check if employee is dedicated to exactly ONE location with 100% preference
	 * Used for: Removing max hours/shifts constraints for dedicated employees
	 */
	public boolean isDedicatedToSingleLocation() {
		if (!hasServicePreferences()) {
			return false;
		}

		// Find all locations with 100% weightage
		long hundredPercentCount = getPreferredServiceWeightsMap().values().stream()
				.filter(weight -> weight == 100)
				.count();

		// Dedicated only if exactly ONE location has 100%
		return hundredPercentCount == 1;
	}

	/**
	 * ✅ NEW: Get the single dedicated location (100% preference)
	 * Returns null if not dedicated to a single location
	 */
	public String getDedicatedLocation() {
		if (!isDedicatedToSingleLocation()) {
			return null;
		}

		return getPreferredServiceWeightsMap().entrySet().stream()
				.filter(e -> e.getValue() == 100)
				.map(Map.Entry::getKey)
				.findFirst()
				.orElse(null);
	}

	/**
	 * ✅ NEW: Check if employee is primary at a location (>= 50% preference)
	 * Used for: Tiered constraint limits (primary locations get higher limits)
	 */
	public boolean isPrimaryAtLocation(String location) {
		if (!hasServicePreferences()) {
			return false;
		}

		Integer weightage = getPreferredServiceWeightsMap().get(location);
		return weightage != null && weightage >= 50;
	}

	/**
	 * ✅ NEW: Get all primary locations (>= 50% preference)
	 * Used for: Identifying employees with split responsibilities
	 */
	public List<String> getPrimaryLocations() {
		if (!hasServicePreferences()) {
			return List.of();
		}

		return getPreferredServiceWeightsMap().entrySet().stream()
				.filter(e -> e.getValue() >= 50)
				.map(Map.Entry::getKey)
				.collect(Collectors.toList());
	}

	/**
	 * ✅ NEW: Check if employee has split preferences (multiple locations with significant weightage)
	 * Used for: Determining if floating constraints should apply
	 */
	public boolean hasMultipleSignificantLocations() {
		if (!hasServicePreferences()) {
			return false;
		}

		// Count locations with >= 30% weightage
		long significantLocations = getPreferredServiceWeightsMap().values().stream()
				.filter(weight -> weight >= 30)
				.count();

		return significantLocations >= 2;
	}

	// ============================================================================
	// SCHEDULE PATTERN HELPERS
	// ============================================================================

	/**
	 * Check if employee can work specific shift based on schedule patterns
	 */
	public boolean canWorkShift(String location, LocalDate date, ShiftType shiftType) {
		if (schedulePatterns == null || schedulePatterns.isEmpty()) {
			return true; // No pattern = unrestricted
		}

		// Build cache if not exists
		if (patternCache == null) {
			buildPatternCache();
		}

		// Calculate week number using PeriodService
		int weekNumber = 0;
		if (periodService != null) {
			weekNumber = periodService.calculateWeekNumber(date);
			if (weekNumber == 0) {
				// Date not in any period - allow by default (or could deny)
				return true;
			}
		} else {
			new Exception("PeriodService not initialized");
		}

		String dayOfWeek = date.getDayOfWeek().name();

		// Check pattern
		EmployeeSchedulePattern pattern = patternCache.getOrDefault(location, Map.of())
				.getOrDefault(weekNumber, Map.of()).get(dayOfWeek);

		if (pattern == null) {
			return true; // No specific pattern = allowed
		}

		if (!pattern.getIsAvailable()) {
			return false; // Explicitly marked as OFF
		}

		if (pattern.getShiftType() != null && pattern.getShiftType() != shiftType) {
			return false; // Wrong shift type for this day
		}

		return true;
	}

	/**
	 * Build cache for faster pattern lookups
	 * Structure: location -> weekNumber -> dayOfWeek -> pattern
	 */
	private void buildPatternCache() {
		patternCache = new HashMap<>();

		for (EmployeeSchedulePattern pattern : schedulePatterns) {
			patternCache.computeIfAbsent(pattern.getLocation(), k -> new HashMap<>())
					.computeIfAbsent(pattern.getWeekNumber(), k -> new HashMap<>())
					.put(pattern.getDayOfWeek(), pattern);
		}
	}

	public Map<String, Map<Integer, Map<String, EmployeeSchedulePattern>>> getPatternCache() {
		if (patternCache == null) {
			buildPatternCache();
		}
		return patternCache;
	}

	/**
	 * Check if employee should be working in a given absolute week number
	 * Based on week-on/week-off pattern
	 */
	public boolean shouldBeWorkingInAbsoluteWeek(int absoluteWeekNumber) {
		if (weekOn == null || weekOff == null) {
			return true;
		}

		int cycleLength = weekOn + weekOff;
		int positionInCycle = (absoluteWeekNumber - 1) % cycleLength;

		// ✅ Invert if flag is true
		if (Boolean.TRUE.equals(invertPattern)) {
			positionInCycle = (positionInCycle + weekOn) % cycleLength;
		}

		return positionInCycle < weekOn;
	}

	// ============================================================================
	// UTILITY METHODS
	// ============================================================================

	public String getName() {
		return this.firstName + " " + this.lastName;
	}

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

	// ============================================================================
	// GETTERS AND SETTERS
	// ============================================================================

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
		this.preferredServiceWeightsMap = null; // Invalidate cache when preferences change
	}

//	public List<ShiftType> getPreferredShift() {
//		return preferredShifts;
//	}

//	public void setPreferredShift(List<ShiftType> preferredShifts) {
//		this.preferredShifts = preferredShifts;
//	}

//	public List<DayOfWeek> getPreferredDay() {
//		return preferredDays;
//	}

//	public void setPreferredDay(List<DayOfWeek> preferredDay) {
//		this.preferredDays = preferredDay;
//	}

//	public List<DayOfWeek> getRestrictedDay() {
//		return restrictedDays;
//	}

//	public void setRestrictedDay(List<DayOfWeek> restrictedDay) {
//		this.restrictedDays = restrictedDay;
//	}

//	public List<ShiftType> getRestrictedShift() {
//		return restrictedShifts;
//	}

//	public void setRestrictedShift(List<ShiftType> restrictedShift) {
//		this.restrictedShifts = restrictedShift;
//	}

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
	
	public List<ShiftType> getRestrictedShifts() {  // Added 's'
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

	public Boolean getInvertPattern() {
		return invertPattern;
	}

	public void setInvertPattern(Boolean invertPattern) {
		this.invertPattern = invertPattern;
	}

	public List<EmployeeSchedulePattern> getSchedulePatterns() {
		return schedulePatterns;
	}

	public void setSchedulePatterns(List<EmployeeSchedulePattern> schedulePatterns) {
		this.schedulePatterns = schedulePatterns;
		this.patternCache = null; // Invalidate cache when patterns change
	}

	public static void setPeriodService(PeriodService service) {
		periodService = service;
	}

	public static PeriodService getPeriodService() {
		return periodService;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
}