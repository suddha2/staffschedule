package com.midco.rota.opt;

import java.util.Set;

import org.optaplanner.core.api.domain.constraintweight.ConstraintConfiguration;
import org.optaplanner.core.api.domain.constraintweight.ConstraintWeight;
import org.optaplanner.core.api.score.buildin.hardsoftlong.HardSoftLongScore;

/**
 * Database-driven weights for every constraint in {@link RotaConstraintProvider}.
 *
 * <p>Each field holds a whole {@link HardSoftLongScore}, so the <b>severity</b>
 * of a rule is data rather than code: the same field can carry
 * {@code ofHard(1)} or {@code ofSoft(100_000)}. Demoting a rule that is making a
 * period unsolvable is therefore a row update rather than a release.
 *
 * <p><b>A weight of {@link HardSoftLongScore#ZERO} switches a constraint off
 * completely, at no cost.</b> OptaPlanner compares each constraint weight
 * against zero when it builds the scoring session and skips the matches
 * entirely, so a disabled constraint never enters the node network. That is why
 * every constraint can be registered unconditionally and left for the database
 * to turn on or off, rather than commenting entries out of
 * {@code defineConstraints}.
 *
 * <p>The {@code constraintPackage} must match the package of
 * {@link RotaConstraintProvider}, because {@code asConstraint(name)} defaults
 * the package to that of the provider.
 *
 * <p>Defaults reproduce the historical hard-coded values, with two deliberate
 * exceptions flagged in their comments: the permanent weekly cap and the
 * permanent weekly minimum, which were HARD and are now SOFT. Measured against
 * 209,514 real shifts, those two rules together left only 24.9% of observed
 * employee-weeks legal, which is why solves went infeasible.
 *
 * <p>Values are loaded from the {@code constraint_setting} table by
 * {@link com.midco.rota.service.SolverConfigService}.
 */
@ConstraintConfiguration(constraintPackage = "com.midco.rota.opt")
public class RotaConstraintConfiguration {

	/**
	 * Constraints that were defined but never listed in {@code defineConstraints},
	 * so they have never actually run. They are now registered like everything
	 * else but seed into {@code constraint_setting} as {@code enabled = false},
	 * which preserves the behaviour the system has always had.
	 *
	 * <p>The weight each one carries below is what it will use once switched on,
	 * so enabling it is a single {@code UPDATE ... SET enabled = true}. If a row
	 * is missing altogether these still resolve to
	 * {@link HardSoftLongScore#ZERO} rather than their code default, so deleting
	 * a row cannot silently activate a rule that has never been in force.
	 *
	 * <p><b>Worth reviewing:</b> "Missing required skill" is in this list, which
	 * means required skills are not being enforced at all.
	 */
	public static final Set<String> INACTIVE_BY_DEFAULT = Set.of(
			"Missing required skill",
			"Week-on week-off pattern",
			"Employee schedule pattern violation",
			"Too many working days in 4-week cycle",
			"Not enough rest days in 4-week cycle",
			"Too many consecutive weeks worked",
			"Insufficient weeks off after work streak",
			"Monthly hours cap",
			"Max non-floating shifts per location per week",
			"Max locations per period",
			"Limit locations per employee per week",
			"Penalize daily location switches",
			"Reward consecutive days at same location");

	// ------------------------------------------------ hard: physical impossibilities

	@ConstraintWeight("Duplicate assignment of employee to same shift")
	private HardSoftLongScore duplicateAssignment = HardSoftLongScore.ONE_HARD;

	@ConstraintWeight("Too many employees for shift")
	private HardSoftLongScore tooManyEmployeesPerShift = HardSoftLongScore.ONE_HARD;

	@ConstraintWeight("No invalid same-day shift combinations")
	private HardSoftLongScore invalidSameDayCombination = HardSoftLongScore.ofHard(1000);

	@ConstraintWeight("No incompatible back-to-back shifts")
	private HardSoftLongScore incompatibleBackToBack = HardSoftLongScore.ofHard(1);

	/** Time-overlap of two shifts for the same carer — physically impossible, so a hard
	 *  block. Data-driven (works for any shift type), catching overlaps the enum-keyed
	 *  same-day/back-to-back rules miss (e.g. a shift-lead overlapping a day shift). */
	@ConstraintWeight("Overlapping shifts")
	private HardSoftLongScore overlappingShifts = HardSoftLongScore.ofHard(1000);

	/** Minimum rest between a carer's consecutive shifts. Gated by solver_tuning
	 *  {@code minRestHours} (0 = off), so this stays inert until a threshold is set.
	 *  Soft by default; switch to hard in constraint_setting if rest must be guaranteed. */
	@ConstraintWeight("Minimum rest between shifts")
	private HardSoftLongScore minRestBetweenShifts = HardSoftLongScore.ofSoft(100_000);

	// ------------------------------------------------ hard: eligibility

	@ConstraintWeight("Gender mismatch")
	private HardSoftLongScore genderMismatch = HardSoftLongScore.ofHard(1);

	@ConstraintWeight("Restricted day of week")
	private HardSoftLongScore restrictedDayOfWeek = HardSoftLongScore.ONE_HARD;

	@ConstraintWeight("Restricted Shift Type")
	private HardSoftLongScore restrictedShiftType = HardSoftLongScore.ONE_HARD;

	@ConstraintWeight("Restricted Service")
	private HardSoftLongScore restrictedService = HardSoftLongScore.ONE_HARD;

	/** Active by default: this is the requested "never allocate onto booked leave" rule. */
	@ConstraintWeight("Employee unavailable (leave)")
	private HardSoftLongScore employeeUnavailable = HardSoftLongScore.ONE_HARD;

	/** Inactive by default: skills are not currently enforced. */
	@ConstraintWeight("Missing required skill")
	private HardSoftLongScore missingRequiredSkill = HardSoftLongScore.ONE_HARD;

	/** Inactive by default. */
	@ConstraintWeight("Employee schedule pattern violation")
	private HardSoftLongScore schedulePatternViolation = HardSoftLongScore.ONE_HARD;

	/** Inactive by default. */
	@ConstraintWeight("Week-on week-off pattern")
	private HardSoftLongScore weekOnWeekOffPattern = HardSoftLongScore.ONE_HARD;

	// ------------------------------------------------ hard: working-time limits

	@ConstraintWeight("Max hours per shift type per day")
	private HardSoftLongScore maxHoursPerShiftTypePerDay = HardSoftLongScore.ONE_HARD;

	@ConstraintWeight("Weekly limit per shift type")
	private HardSoftLongScore weeklyLimitPerShiftType = HardSoftLongScore.ONE_HARD;

	/** Inactive by default. Threshold comes from {@code Employee.daysOn}. */
	@ConstraintWeight("Too many working days in 4-week cycle")
	private HardSoftLongScore tooManyWorkingDays = HardSoftLongScore.ONE_HARD;

	/** Inactive by default. Threshold comes from {@code Employee.restDays}. */
	@ConstraintWeight("Not enough rest days in 4-week cycle")
	private HardSoftLongScore notEnoughRestDays = HardSoftLongScore.ONE_HARD;

	/** Inactive by default. */
	@ConstraintWeight("Too many consecutive weeks worked")
	private HardSoftLongScore tooManyConsecutiveWeeks = HardSoftLongScore.ONE_HARD;

	/** Inactive by default. */
	@ConstraintWeight("Insufficient weeks off after work streak")
	private HardSoftLongScore insufficientWeeksOff = HardSoftLongScore.ONE_HARD;

	/** Inactive by default. Threshold {@code monthlyHoursCap} in solver_tuning. */
	@ConstraintWeight("Monthly hours cap")
	private HardSoftLongScore monthlyHoursCap = HardSoftLongScore.ofHard(100);

	/** Inactive by default. Threshold widened from 4 to 10 in {@link SolverTuning}. */
	@ConstraintWeight("Max non-floating shifts per location per week")
	private HardSoftLongScore maxNonFloatingShiftsPerLocationPerWeek = HardSoftLongScore.ofHard(1);

	/** Inactive by default. Threshold widened from 3 to 6 in {@link SolverTuning}. */
	@ConstraintWeight("Max locations per period")
	private HardSoftLongScore maxLocationsPerPeriod = HardSoftLongScore.ONE_HARD;

	// ------------------------------------------------ contractual shape (DEMOTED from hard to soft)

	/**
	 * Was {@code ONE_HARD}. About 38% of real employee-weeks exceed the cap of
	 * 6, so as a hard rule it forbade more than a third of observed behaviour.
	 * Soft keeps the solver aiming at the 6-5-6-5 shape without declaring the
	 * problem unsolvable when the staffing fit genuinely will not allow it.
	 */
	@ConstraintWeight("Permanent weekly shift cap")
	private HardSoftLongScore permanentWeeklyCap = HardSoftLongScore.ofSoft(100_000);

	/**
	 * Was {@code ONE_HARD}. 37.1% of real weeks fall below 5. Paired with the
	 * cap above it left only weeks of exactly 5 or 6 legal, which is 24.9% of
	 * observed reality.
	 */
	@ConstraintWeight("Permanent weekly shift minimum")
	private HardSoftLongScore permanentWeeklyMinimum = HardSoftLongScore.ofSoft(100_000);

	// ------------------------------------------------ soft: coverage and hours

	@ConstraintWeight("Unassigned shift")
	private HardSoftLongScore unassignedShift = HardSoftLongScore.ofSoft(1_000_000);

	@ConstraintWeight("Assigned shift")
	private HardSoftLongScore assignedShift = HardSoftLongScore.ONE_SOFT;

	@ConstraintWeight("Max hours per week")
	private HardSoftLongScore maxHoursPerWeek = HardSoftLongScore.ofSoft(10_000);

	@ConstraintWeight("Max weekly hours exceeded")
	private HardSoftLongScore maxWeeklyHoursExceeded = HardSoftLongScore.ofSoft(20_000);

	@ConstraintWeight("Min weekly hours not met")
	private HardSoftLongScore minWeeklyHoursNotMet = HardSoftLongScore.ofSoft(500);

	@ConstraintWeight("Penalize overloading individual employees")
	private HardSoftLongScore overloadingEmployees = HardSoftLongScore.ofSoft(100);

	@ConstraintWeight("Encourage balanced hours around midpoint")
	private HardSoftLongScore balancedHours = HardSoftLongScore.ofSoft(10);

	@ConstraintWeight("Allow zero-hours employee assignments")
	private HardSoftLongScore zeroHoursAssignments = HardSoftLongScore.ofSoft(3000);

	// ------------------------------------------------ soft: preferences

	@ConstraintWeight("Prefer working on preferred days")
	private HardSoftLongScore preferredDays = HardSoftLongScore.ONE_SOFT;

	@ConstraintWeight("Prefer working on preferred shift")
	private HardSoftLongScore preferredShiftType = HardSoftLongScore.ONE_SOFT;

	@ConstraintWeight("Prioritized allocation")
	private HardSoftLongScore prioritizedAllocation = HardSoftLongScore.ONE_SOFT;

	@ConstraintWeight("Prioritize high-priority location assignments")
	private HardSoftLongScore highPriorityLocations = HardSoftLongScore.ONE_SOFT;

	/**
	 * Affinity strength: how hard the solver pulls each carer toward their
	 * historically-worked houses. Backtesting took this from the old 10,000 (which
	 * left familiarity ~50%) to 1,000,000, which reproduced manager-level
	 * concentration (~72% familiar) with better coverage. This is the seeded
	 * default so a fresh deploy is good out of the box; still DB-tunable. Only
	 * bites once employees carry mined {@code preferred_service} weights.
	 */
	@ConstraintWeight("Location preferences (reward only)")
	private HardSoftLongScore locationPreferences = HardSoftLongScore.ofSoft(1_000_000);

	// ------------------------------------------------ soft: continuity of place

	@ConstraintWeight("Min 2 days per location per week (SOFT)")
	private HardSoftLongScore minDaysPerLocationPerWeek = HardSoftLongScore.ofSoft(200_000);

	@ConstraintWeight("Max days per location per week")
	private HardSoftLongScore maxDaysPerLocationPerWeek = HardSoftLongScore.ofSoft(10_000);

	/** Inactive by default. */
	@ConstraintWeight("Limit locations per employee per week")
	private HardSoftLongScore locationsPerWeek = HardSoftLongScore.ofSoft(1000);

	/** Inactive by default. */
	@ConstraintWeight("Penalize daily location switches")
	private HardSoftLongScore dailyLocationSwitches = HardSoftLongScore.ofSoft(1_500_000);

	/** Inactive by default. */
	@ConstraintWeight("Reward consecutive days at same location")
	private HardSoftLongScore consecutiveDaysSameLocation = HardSoftLongScore.ofSoft(500_000);

	/**
	 * Person-level continuity: keep each carer in the slot they held last period.
	 * Active by default. Weight sits above ordinary preferences but below coverage
	 * ({@code Unassigned shift} 1,000,000) so the solver keeps the prior carer
	 * unless leave/a hard limit forces a change — never at the cost of leaving a
	 * shift empty.
	 */
	@ConstraintWeight("Continuity - keep carer in seeded slot")
	private HardSoftLongScore continuityKeepSeededCarer = HardSoftLongScore.ofSoft(300_000);

	// ------------------------------------------------ accessors

	public HardSoftLongScore getDuplicateAssignment() { return duplicateAssignment; }
	public void setDuplicateAssignment(HardSoftLongScore v) { this.duplicateAssignment = v; }
	public HardSoftLongScore getTooManyEmployeesPerShift() { return tooManyEmployeesPerShift; }
	public void setTooManyEmployeesPerShift(HardSoftLongScore v) { this.tooManyEmployeesPerShift = v; }
	public HardSoftLongScore getInvalidSameDayCombination() { return invalidSameDayCombination; }
	public void setInvalidSameDayCombination(HardSoftLongScore v) { this.invalidSameDayCombination = v; }

	public HardSoftLongScore getOverlappingShifts() { return overlappingShifts; }
	public void setOverlappingShifts(HardSoftLongScore v) { this.overlappingShifts = v; }

	public HardSoftLongScore getMinRestBetweenShifts() { return minRestBetweenShifts; }
	public void setMinRestBetweenShifts(HardSoftLongScore v) { this.minRestBetweenShifts = v; }
	public HardSoftLongScore getIncompatibleBackToBack() { return incompatibleBackToBack; }
	public void setIncompatibleBackToBack(HardSoftLongScore v) { this.incompatibleBackToBack = v; }
	public HardSoftLongScore getGenderMismatch() { return genderMismatch; }
	public void setGenderMismatch(HardSoftLongScore v) { this.genderMismatch = v; }
	public HardSoftLongScore getRestrictedDayOfWeek() { return restrictedDayOfWeek; }
	public void setRestrictedDayOfWeek(HardSoftLongScore v) { this.restrictedDayOfWeek = v; }
	public HardSoftLongScore getRestrictedShiftType() { return restrictedShiftType; }
	public void setRestrictedShiftType(HardSoftLongScore v) { this.restrictedShiftType = v; }
	public HardSoftLongScore getRestrictedService() { return restrictedService; }
	public void setRestrictedService(HardSoftLongScore v) { this.restrictedService = v; }
	public HardSoftLongScore getEmployeeUnavailable() { return employeeUnavailable; }
	public void setEmployeeUnavailable(HardSoftLongScore v) { this.employeeUnavailable = v; }
	public HardSoftLongScore getMissingRequiredSkill() { return missingRequiredSkill; }
	public void setMissingRequiredSkill(HardSoftLongScore v) { this.missingRequiredSkill = v; }
	public HardSoftLongScore getSchedulePatternViolation() { return schedulePatternViolation; }
	public void setSchedulePatternViolation(HardSoftLongScore v) { this.schedulePatternViolation = v; }
	public HardSoftLongScore getWeekOnWeekOffPattern() { return weekOnWeekOffPattern; }
	public void setWeekOnWeekOffPattern(HardSoftLongScore v) { this.weekOnWeekOffPattern = v; }
	public HardSoftLongScore getMaxHoursPerShiftTypePerDay() { return maxHoursPerShiftTypePerDay; }
	public void setMaxHoursPerShiftTypePerDay(HardSoftLongScore v) { this.maxHoursPerShiftTypePerDay = v; }
	public HardSoftLongScore getWeeklyLimitPerShiftType() { return weeklyLimitPerShiftType; }
	public void setWeeklyLimitPerShiftType(HardSoftLongScore v) { this.weeklyLimitPerShiftType = v; }
	public HardSoftLongScore getTooManyWorkingDays() { return tooManyWorkingDays; }
	public void setTooManyWorkingDays(HardSoftLongScore v) { this.tooManyWorkingDays = v; }
	public HardSoftLongScore getNotEnoughRestDays() { return notEnoughRestDays; }
	public void setNotEnoughRestDays(HardSoftLongScore v) { this.notEnoughRestDays = v; }
	public HardSoftLongScore getTooManyConsecutiveWeeks() { return tooManyConsecutiveWeeks; }
	public void setTooManyConsecutiveWeeks(HardSoftLongScore v) { this.tooManyConsecutiveWeeks = v; }
	public HardSoftLongScore getInsufficientWeeksOff() { return insufficientWeeksOff; }
	public void setInsufficientWeeksOff(HardSoftLongScore v) { this.insufficientWeeksOff = v; }
	public HardSoftLongScore getMonthlyHoursCap() { return monthlyHoursCap; }
	public void setMonthlyHoursCap(HardSoftLongScore v) { this.monthlyHoursCap = v; }
	public HardSoftLongScore getMaxNonFloatingShiftsPerLocationPerWeek() { return maxNonFloatingShiftsPerLocationPerWeek; }
	public void setMaxNonFloatingShiftsPerLocationPerWeek(HardSoftLongScore v) { this.maxNonFloatingShiftsPerLocationPerWeek = v; }
	public HardSoftLongScore getMaxLocationsPerPeriod() { return maxLocationsPerPeriod; }
	public void setMaxLocationsPerPeriod(HardSoftLongScore v) { this.maxLocationsPerPeriod = v; }
	public HardSoftLongScore getPermanentWeeklyCap() { return permanentWeeklyCap; }
	public void setPermanentWeeklyCap(HardSoftLongScore v) { this.permanentWeeklyCap = v; }
	public HardSoftLongScore getPermanentWeeklyMinimum() { return permanentWeeklyMinimum; }
	public void setPermanentWeeklyMinimum(HardSoftLongScore v) { this.permanentWeeklyMinimum = v; }
	public HardSoftLongScore getUnassignedShift() { return unassignedShift; }
	public void setUnassignedShift(HardSoftLongScore v) { this.unassignedShift = v; }
	public HardSoftLongScore getAssignedShift() { return assignedShift; }
	public void setAssignedShift(HardSoftLongScore v) { this.assignedShift = v; }
	public HardSoftLongScore getMaxHoursPerWeek() { return maxHoursPerWeek; }
	public void setMaxHoursPerWeek(HardSoftLongScore v) { this.maxHoursPerWeek = v; }
	public HardSoftLongScore getMaxWeeklyHoursExceeded() { return maxWeeklyHoursExceeded; }
	public void setMaxWeeklyHoursExceeded(HardSoftLongScore v) { this.maxWeeklyHoursExceeded = v; }
	public HardSoftLongScore getMinWeeklyHoursNotMet() { return minWeeklyHoursNotMet; }
	public void setMinWeeklyHoursNotMet(HardSoftLongScore v) { this.minWeeklyHoursNotMet = v; }
	public HardSoftLongScore getOverloadingEmployees() { return overloadingEmployees; }
	public void setOverloadingEmployees(HardSoftLongScore v) { this.overloadingEmployees = v; }
	public HardSoftLongScore getBalancedHours() { return balancedHours; }
	public void setBalancedHours(HardSoftLongScore v) { this.balancedHours = v; }
	public HardSoftLongScore getZeroHoursAssignments() { return zeroHoursAssignments; }
	public void setZeroHoursAssignments(HardSoftLongScore v) { this.zeroHoursAssignments = v; }
	public HardSoftLongScore getPreferredDays() { return preferredDays; }
	public void setPreferredDays(HardSoftLongScore v) { this.preferredDays = v; }
	public HardSoftLongScore getPreferredShiftType() { return preferredShiftType; }
	public void setPreferredShiftType(HardSoftLongScore v) { this.preferredShiftType = v; }
	public HardSoftLongScore getPrioritizedAllocation() { return prioritizedAllocation; }
	public void setPrioritizedAllocation(HardSoftLongScore v) { this.prioritizedAllocation = v; }
	public HardSoftLongScore getHighPriorityLocations() { return highPriorityLocations; }
	public void setHighPriorityLocations(HardSoftLongScore v) { this.highPriorityLocations = v; }
	public HardSoftLongScore getLocationPreferences() { return locationPreferences; }
	public void setLocationPreferences(HardSoftLongScore v) { this.locationPreferences = v; }
	public HardSoftLongScore getMinDaysPerLocationPerWeek() { return minDaysPerLocationPerWeek; }
	public void setMinDaysPerLocationPerWeek(HardSoftLongScore v) { this.minDaysPerLocationPerWeek = v; }
	public HardSoftLongScore getMaxDaysPerLocationPerWeek() { return maxDaysPerLocationPerWeek; }
	public void setMaxDaysPerLocationPerWeek(HardSoftLongScore v) { this.maxDaysPerLocationPerWeek = v; }
	public HardSoftLongScore getLocationsPerWeek() { return locationsPerWeek; }
	public void setLocationsPerWeek(HardSoftLongScore v) { this.locationsPerWeek = v; }
	public HardSoftLongScore getDailyLocationSwitches() { return dailyLocationSwitches; }
	public void setDailyLocationSwitches(HardSoftLongScore v) { this.dailyLocationSwitches = v; }
	public HardSoftLongScore getConsecutiveDaysSameLocation() { return consecutiveDaysSameLocation; }
	public void setConsecutiveDaysSameLocation(HardSoftLongScore v) { this.consecutiveDaysSameLocation = v; }
	public HardSoftLongScore getContinuityKeepSeededCarer() { return continuityKeepSeededCarer; }
	public void setContinuityKeepSeededCarer(HardSoftLongScore v) { this.continuityKeepSeededCarer = v; }
}
