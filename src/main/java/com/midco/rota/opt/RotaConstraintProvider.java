package com.midco.rota.opt;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintCollectors;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;
import org.optaplanner.core.api.score.stream.Joiners;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.midco.rota.ShiftTypeLimitConfig;
import com.midco.rota.model.Employee;
import com.midco.rota.model.ShiftAssignment;
import com.midco.rota.service.PeriodService;
import com.midco.rota.util.ContractType;
import com.midco.rota.util.Gender;
import com.midco.rota.util.ShiftType;

public class RotaConstraintProvider implements ConstraintProvider {

	@Autowired
	private PeriodService periodService;

	private static final Logger logger = LoggerFactory.getLogger(RotaConstraintProvider.class);

	@Override
	public Constraint[] defineConstraints(ConstraintFactory factory) {
		return new Constraint[] {
				// HARD constraints - Must be satisfied
				unassignedShiftConstraint(factory), 
				preventDuplicateAssignments(factory), 
				genderConstraint(factory),
				restrictedDayOfWeekConstraint(factory), 
				restrictedShiftTypeConstraint(factory),
				restrictedServiceConstraint(factory), 
				maxWeeklyHoursConstraint(factory),
				tooManyEmployeesPerShift(factory), 
				maxHoursPerShiftTypePerDay(factory),
				limitWeeklyShiftTypeCounts(factory), 
				noBackToBack(factory),
				//maxShiftsPerLocationPerWeek(factory), 
				//maxDaysOnIn4Weeks(factory), 
				//minDaysOffIn4Weeks(factory), 
				//maxConsecutiveWeeksOn(factory),
				//minWeeksOffAfterStreak(factory), 
				//maxMonthlyHoursWithExclusions(factory),
				//employeeSchedulePatternConstraint(factory), 
				//weekOnWeekOffPattern(factory),
				employeeMaxHours(factory),
				minDaysPerLocationPerWeek(factory),
				//maxLocationsPerEmployeePerPeriod(factory), // ✅ Keep this in HARD
				// ❌ REMOVE preferFewerLocationsPerPeriod from here

				// SOFT constraints - Optimization goals
				rewardAssignedShift(factory), minWeeklyHoursConstraint(factory), preferedWorkingDaysConstraint(factory),
				preferedShiftTypeConstraint(factory), prioritizedAllocation(factory),
				prioritizeHighPriorityLocations(factory), rewardZeroHoursAssignments(factory),
				encourageBalancedHours(factory), penalizeOverloading(factory), 
				maxDaysPerLocationPerWeek(factory),
				//penalizeDailyLocationSwitches(factory), 
				//rewardConsecutiveDaysAtLocation(factory),
				//limitLocationChangesPerWeek(factory),
				locationPreferences(factory),

		};
	}

	private Constraint penalizeOverloading(ConstraintFactory factory) {
		return factory.forEach(ShiftAssignment.class).filter(sa -> sa.getEmployee() != null)
				.filter(sa -> sa.getEmployee().getMinHrs() != null)
				// ✅ Exclude SLEEP_IN if you determined you need this
				.filter(sa -> sa.getShift().getShiftTemplate().getShiftType() != ShiftType.SLEEP_IN)
				.groupBy(ShiftAssignment::getEmployee,
						ConstraintCollectors.sumLong(sa -> sa.getShift().getDurationInMins()))
				.filter((emp, totalMins) -> {
					long minMins = emp.getMinHrs().longValue() * 60;
					// Only penalize if 30% or more above min hours
					return totalMins > (minMins * 1.3);
				}).penalize(HardSoftScore.ofSoft(100), (emp, totalMins) -> {
					long minMins = emp.getMinHrs().longValue() * 60;
					long threshold = (long) (minMins * 1.3);
					long excess = totalMins - threshold;

					int excessHours = (int) (excess / 60);
					// Progressive penalty: quadratic growth
					// 1 hour over: 1 point
					// 2 hours over: 4 points
					// 3 hours over: 9 points
					// 5 hours over: 25 points
					return excessHours * excessHours;
				}).asConstraint("Penalize overloading individual employees");
	}

	private Constraint encourageBalancedHours(ConstraintFactory factory) {
		return factory.forEach(ShiftAssignment.class).filter(sa -> sa.getEmployee() != null)
				.groupBy(ShiftAssignment::getEmployee,
						ConstraintCollectors.sumLong(sa -> sa.getShift().getDurationInMins()))
				.reward(HardSoftScore.ofSoft(10), (emp, totalMins) -> {
					double hours = totalMins / 60.0;

					// Get employee's min and max
					double min = emp.getMinHrs() != null ? emp.getMinHrs().doubleValue() : 0;
					double max = emp.getMaxHrs() != null ? emp.getMaxHrs().doubleValue() : 999;

					// Calculate midpoint (target hours)
					double target = (min + max) / 2.0;

					// Reward based on how close to target
					double distanceFromTarget = Math.abs(hours - target);

					if (distanceFromTarget <= 5) {
						return 100; // Very close to target
					} else if (distanceFromTarget <= 10) {
						return 50; // Reasonably close
					} else if (distanceFromTarget <= 20) {
						return 20; // Somewhat close
					} else {
						return 5; // Far from target
					}
				}).asConstraint("Encourage balanced hours around midpoint");
	}
	// ========== HARD CONSTRAINTS ==========

	private Constraint employeeMaxHours(ConstraintFactory factory) {
		return factory.forEach(ShiftAssignment.class).filter(sa -> sa.getEmployee() != null)
				.filter(sa -> sa.getEmployee().getMaxHrs() != null)
				.groupBy(ShiftAssignment::getEmployee, sa -> YearWeek.from(sa.getShift().getShiftStart()),
						ConstraintCollectors.sumLong(sa -> sa.getShift().getDurationInMins()))
				.filter((emp, week, totalMins) -> {
					long maxMins = emp.getMaxHrs().longValue() * 60;
					return totalMins > maxMins;
				}).penalize(HardSoftScore.ofSoft(10000), (emp, week, totalMins) -> {
					long maxMins = emp.getMaxHrs().longValue() * 60;
					long excessMins = totalMins - maxMins;
					return (int) (excessMins / 60);
				}).asConstraint("Max hours per week (SOFT 10K)");
	}

	private Constraint unassignedShiftConstraint(ConstraintFactory factory) {
		return factory.forEachIncludingNullVars(ShiftAssignment.class).filter(sa -> sa.getEmployee() == null)
				.penalize(HardSoftScore.ofSoft(1000000)).asConstraint("Unassigned shift");
	}

	private Constraint preventDuplicateAssignments(ConstraintFactory factory) {
		return factory.forEachIncludingNullVars(ShiftAssignment.class)
				.filter(assignment -> assignment.getEmployee() != null)
				.groupBy(assignment -> assignment.getShift(), assignment -> assignment.getEmployee(),
						ConstraintCollectors.count())
				.filter((shift, employee, count) -> count > 1).penalize(HardSoftScore.ONE_HARD)
				.asConstraint("Duplicate assignment of employee to same shift");
	}

	private Constraint genderConstraint(ConstraintFactory factory) {
		return factory.forEachIncludingNullVars(ShiftAssignment.class).filter(sa -> {
			Employee employee = sa.getEmployee();
			Gender required = sa.getShift().getShiftTemplate().getGender();
			if (required == Gender.ANY || employee == null) {
				return false;
			}
			return employee.getGender() != required;
		}).penalize(HardSoftScore.ofHard(1)).asConstraint("Gender mismatch");
	}

	private Constraint restrictedDayOfWeekConstraint(ConstraintFactory factory) {
		return factory.forEachIncludingNullVars(ShiftAssignment.class).filter(sa -> {
			Employee emp = sa.getEmployee();
			return emp != null && emp.getRestrictedDay() != null
					&& emp.getRestrictedDay().contains(sa.getShift().getShiftTemplate().getDay());
		}).penalize(HardSoftScore.ONE_HARD).asConstraint("Restricted day of week");
	}

	private Constraint restrictedShiftTypeConstraint(ConstraintFactory factory) {
		return factory.forEachIncludingNullVars(ShiftAssignment.class).filter(sa -> {
			Employee emp = sa.getEmployee();
			return emp != null && emp.getRestrictedShift() != null
					&& emp.getRestrictedShift().contains(sa.getShift().getShiftTemplate().getShiftType());
		}).penalize(HardSoftScore.ONE_HARD).asConstraint("Restricted Shift Type");
	}

	private Constraint restrictedServiceConstraint(ConstraintFactory factory) {
		return factory.forEachIncludingNullVars(ShiftAssignment.class).filter(sa -> {
			Employee emp = sa.getEmployee();
			return emp != null && emp.getRestrictedService() != null
					&& emp.getRestrictedService().contains(sa.getShift().getShiftTemplate().getLocation());
		}).penalize(HardSoftScore.ONE_HARD).asConstraint("Restricted Service");
	}

	private Constraint maxWeeklyHoursConstraint(ConstraintFactory factory) {
		return factory.forEachIncludingNullVars(ShiftAssignment.class).filter(sa -> sa.getEmployee() != null)
				.groupBy(ShiftAssignment::getEmployee, sa -> YearWeek.from(sa.getShift().getShiftStart()),
						ConstraintCollectors.sumLong(sa -> sa.getShift().getDurationInMins()))
				.filter((employee, week, totalMinutes) -> totalMinutes > (employee.getMaxHrs().longValue() * 60))
				.penalize(HardSoftScore.ofSoft(20000),
						(employee, week,
								totalMinutes) -> (int) (totalMinutes - (employee.getMaxHrs().longValue() * 60)))
				.asConstraint("Max weekly hours exceeded");
	}

	private Constraint tooManyEmployeesPerShift(ConstraintFactory factory) {
		return factory.forEachIncludingNullVars(ShiftAssignment.class)
				.groupBy(ShiftAssignment::getShift, ConstraintCollectors.count())
				.filter((shift, count) -> count > shift.getShiftTemplate().getEmpCount())
				.penalize(HardSoftScore.ONE_HARD).asConstraint("Too many employees for shift");
	}

	private Constraint maxHoursPerShiftTypePerDay(ConstraintFactory factory) {
		Map<ShiftType, Integer> maxHoursPerShiftType = ShiftTypeLimitConfig.maxHoursPerShiftType();

		return factory.forEachIncludingNullVars(ShiftAssignment.class).filter(sa -> sa.getEmployee() != null)
				.groupBy(sa -> sa.getEmployee(), sa -> sa.getShift().getShiftStart(),
						sa -> sa.getShift().getShiftTemplate().getShiftType(),
						ConstraintCollectors.sumLong(sa -> sa.getShift().getDurationInMins()))
				.filter((employee, date, shiftType, totalMinutes) -> {
					long maxAllowedMinutes = maxHoursPerShiftType.getOrDefault(shiftType, Integer.MAX_VALUE) * 60L;
					return totalMinutes > maxAllowedMinutes;
				}).penalize(HardSoftScore.ONE_HARD, (employee, date, shiftType, totalMinutes) -> {
					long maxAllowedMinutes = maxHoursPerShiftType.getOrDefault(shiftType, 0) * 60L;
					return (int) (totalMinutes - maxAllowedMinutes);
				}).asConstraint("Max hours per shift type per day");
	}

	private Constraint limitWeeklyShiftTypeCounts(ConstraintFactory factory) {
		Map<ShiftType, Integer> weeklyShiftTypeLimit = ShiftTypeLimitConfig.weeklyShiftTypeLimit();

		return factory.forEachIncludingNullVars(ShiftAssignment.class).filter(sa -> sa.getEmployee() != null)
				.groupBy(sa -> sa.getEmployee(), sa -> sa.getShift().getShiftTemplate().getShiftType(),
						sa -> YearWeek.from(sa.getShift().getShiftStart()), ConstraintCollectors.count())
				.filter((emp, type, week, count) -> count > weeklyShiftTypeLimit.getOrDefault(type, Integer.MAX_VALUE))
				.penalize(HardSoftScore.ONE_HARD,
						(emp, type, week, count) -> count - weeklyShiftTypeLimit.getOrDefault(type, 0))
				.asConstraint("Weekly limit per shift type");
	}

	private Constraint maxShiftsPerLocationPerWeek(ConstraintFactory factory) {
		final int MAX_NON_FLOATING_SHIFTS = 4;

		return factory.forEachIncludingNullVars(ShiftAssignment.class).filter(sa -> {
			Employee emp = sa.getEmployee();
			ShiftType type = sa.getShift().getShiftTemplate().getShiftType();
			return emp != null && type != ShiftType.FLOATING;
		}).groupBy(sa -> sa.getEmployee(), sa -> YearWeek.from(sa.getShift().getShiftStart()),
				sa -> sa.getShift().getShiftTemplate().getLocation(), ConstraintCollectors.count())
				.filter((emp, week, location, count) -> count > MAX_NON_FLOATING_SHIFTS)
				.penalize(HardSoftScore.ofHard(1), (emp, week, location, count) -> count - MAX_NON_FLOATING_SHIFTS)
				.asConstraint("Max non-floating shifts per location per week");
	}

	private Constraint noBackToBack(ConstraintFactory factory) {
		return factory.forEach(ShiftAssignment.class)
				.join(ShiftAssignment.class, Joiners.equal(ShiftAssignment::getEmployee),
						Joiners.lessThan(sa -> sa.getShift().getShiftStart()))
				.filter((sa1, sa2) -> areIncompatibleBackToBack(sa1, sa2)).penalize(HardSoftScore.ofHard(1))
				.asConstraint("No incompatible back-to-back shifts");
	}

	private Constraint maxDaysOnIn4Weeks(ConstraintFactory factory) {
		return factory.forEach(ShiftAssignment.class)
				.filter(sa -> sa.getEmployee() != null && sa.getEmployee().getDaysOn() != null
						&& sa.getEmployee().getDaysOn() > 0)
				.groupBy(ShiftAssignment::getEmployee,
						ConstraintCollectors.countDistinct(sa -> sa.getShift().getShiftStart()))
				.filter((employee, daysWorked) -> daysWorked > employee.getDaysOn())
				.penalize(HardSoftScore.ONE_HARD, (employee, daysWorked) -> daysWorked - employee.getDaysOn())
				.asConstraint("Too many working days in 4-week cycle");
	}

	private Constraint minDaysOffIn4Weeks(ConstraintFactory factory) {
		return factory.forEach(ShiftAssignment.class)
				.filter(sa -> sa.getEmployee() != null && sa.getEmployee().getDaysOff() != null
						&& sa.getEmployee().getDaysOff() > 0)
				.groupBy(ShiftAssignment::getEmployee,
						ConstraintCollectors.countDistinct(sa -> sa.getShift().getShiftStart()))
				.filter((employee, daysWorked) -> {
					int totalDays = 28;
					int daysOff = totalDays - daysWorked;
					return daysOff < employee.getDaysOff();
				}).penalize(HardSoftScore.ONE_HARD, (employee, daysWorked) -> {
					int totalDays = 28;
					int actualDaysOff = totalDays - daysWorked;
					return employee.getDaysOff() - actualDaysOff;
				}).asConstraint("Not enough rest days in 4-week cycle");
	}

	private Constraint maxConsecutiveWeeksOn(ConstraintFactory factory) {
		return factory.forEach(ShiftAssignment.class)
				.filter(sa -> sa.getEmployee() != null && sa.getEmployee().getWeekOn() != null
						&& sa.getEmployee().getWeekOn() > 0)
				.groupBy(ShiftAssignment::getEmployee, sa -> YearWeek.from(sa.getShift().getShiftStart()))
				.groupBy((employee, week) -> employee, ConstraintCollectors.toList((employee, week) -> week))
				.filter((employee, weeksList) -> {
					int maxConsecutive = calculateMaxConsecutiveWeeks(weeksList);
					return maxConsecutive > employee.getWeekOn();
				}).penalize(HardSoftScore.ONE_HARD, (employee, weeksList) -> {
					int maxConsecutive = calculateMaxConsecutiveWeeks(weeksList);
					return maxConsecutive - employee.getWeekOn();
				}).asConstraint("Too many consecutive weeks worked");
	}

	private Constraint minWeeksOffAfterStreak(ConstraintFactory factory) {
		return factory.forEach(ShiftAssignment.class)
				.filter(sa -> sa.getEmployee() != null && sa.getEmployee().getWeekOn() != null
						&& sa.getEmployee().getWeekOff() != null && sa.getEmployee().getWeekOn() > 0
						&& sa.getEmployee().getWeekOff() > 0)
				.groupBy(ShiftAssignment::getEmployee, sa -> YearWeek.from(sa.getShift().getShiftStart()))
				.groupBy((employee, week) -> employee, ConstraintCollectors.toList((employee, week) -> week))
				.filter((employee, weeksList) -> hasInsufficientWeeksOff(employee, weeksList))
				.penalize(HardSoftScore.ONE_HARD).asConstraint("Insufficient weeks off after work streak");
	}

	private Constraint maxMonthlyHoursWithExclusions(ConstraintFactory factory) {
		Set<ShiftType> excludedShiftTypes = Set.of(ShiftType.SLEEP_IN);

		return factory.forEach(ShiftAssignment.class).filter(sa -> sa.getEmployee() != null)
				.filter(sa -> !excludedShiftTypes.contains(sa.getShift().getShiftTemplate().getShiftType()))
				.groupBy(ShiftAssignment::getEmployee,
						ConstraintCollectors.sumLong(sa -> sa.getShift().getDurationInMins()))
				.filter((employee, totalMinutes) -> totalMinutes > 270 * 60L)
				.penalize(HardSoftScore.ofHard(100),
						(employee, totalMinutes) -> (int) Math.min(Integer.MAX_VALUE, (totalMinutes - 270 * 60L) / 60))
				.asConstraint("Exceeds 270 monthly hours (excluding exempt shift types)");
	}

	// ========== SOFT CONSTRAINTS ==========

	private Constraint rewardAssignedShift(ConstraintFactory factory) {
		return factory.forEachIncludingNullVars(ShiftAssignment.class).filter(sa -> sa.getEmployee() != null)
				.reward(HardSoftScore.ONE_SOFT).asConstraint("Assigned shift");
	}

	private Constraint minWeeklyHoursConstraint(ConstraintFactory factory) {
		return factory.forEachIncludingNullVars(ShiftAssignment.class).filter(sa -> sa.getEmployee() != null)
				.groupBy(ShiftAssignment::getEmployee, sa -> YearWeek.from(sa.getShift().getShiftStart()),
						ConstraintCollectors.sumLong(sa -> sa.getShift().getDurationInMins()))
				.filter((employee, week, totalMinutes) -> totalMinutes < (employee.getMinHrs().longValue() * 60))
				.penalize(HardSoftScore.ofSoft(500), // ✅ CHANGED: Up from 100
						(employee, week, totalMinutes) -> {
							long minMinutes = employee.getMinHrs().longValue() * 60;
							return (int) (minMinutes - totalMinutes);
						})
				.asConstraint("Min weekly hours not met");
	}

	private Constraint preferedWorkingDaysConstraint(ConstraintFactory factory) {
		return factory.forEachIncludingNullVars(ShiftAssignment.class).filter(sa -> {
			Employee emp = sa.getEmployee();
			if (emp == null || emp.getPreferredDay() == null)
				return false;
			DayOfWeek shiftDay = sa.getShift().getShiftStart().getDayOfWeek();
			return emp.getPreferredDay().contains(shiftDay);
		}).reward(HardSoftScore.ONE_SOFT).asConstraint("Prefer working on preferred days");
	}

	private Constraint preferedShiftTypeConstraint(ConstraintFactory factory) {
		return factory.forEachIncludingNullVars(ShiftAssignment.class).filter(sa -> {
			Employee emp = sa.getEmployee();
			if (emp == null || emp.getPreferredShift() == null)
				return false;
			ShiftType shiftType = sa.getShift().getShiftTemplate().getShiftType();
			return emp.getPreferredShift().contains(shiftType);
		}).reward(HardSoftScore.ONE_SOFT).asConstraint("Prefer working on preferred shift");
	}

	private Constraint prioritizedAllocation(ConstraintFactory factory) {
		return factory.forEachIncludingNullVars(ShiftAssignment.class).filter(sa -> sa.getEmployee() != null)
				.reward(HardSoftScore.ONE_SOFT, sa -> {
					ShiftType shiftType = sa.getShift().getShiftTemplate().getShiftType();
					int priority = sa.getShift().getShiftTemplate().getPriority();
					String location = sa.getShift().getShiftTemplate().getLocation();

					int shiftWeight = switch (shiftType) {
					case DAY -> 450;
					case WAKING_NIGHT -> 400;
					case LONG_DAY -> 300;
					case SLEEP_IN -> 0;
					case FLOATING -> 10;
					default -> 1;
					};

					int priorityWeight = switch (priority) {
					case 1 -> 1000;
					case 2 -> 500;
					case 3 -> 250;
					default -> 100;
					};

					return shiftWeight * priorityWeight;
				}).asConstraint("Prioritized allocation");
	}

	private Constraint prioritizeHighPriorityLocations(ConstraintFactory factory) {
		return factory.forEachIncludingNullVars(ShiftAssignment.class).filter(sa -> sa.getEmployee() != null)
				.reward(HardSoftScore.ONE_SOFT, sa -> {
					int priority = sa.getShift().getShiftTemplate().getPriority();

					if (priority < 1) {
						return 0; // Invalid priority
					}

					// Cap at 20 to prevent extreme values
					int maxPriority = Math.min(priority, 20);

					// Higher priority (lower number) = more reward
					// Priority 1: 200 points
					// Priority 20: 10 points
					return (21 - maxPriority) * 10;
				}).asConstraint("Prioritize high-priority location assignments");
	}

	private Constraint rewardZeroHoursAssignments(ConstraintFactory factory) {
		return factory.forEach(ShiftAssignment.class).filter(sa -> {
			Employee emp = sa.getEmployee();
			return emp != null && emp.getContractType() == ContractType.ZERO_HOURS;
		}).reward(HardSoftScore.ofSoft(3000)) // ✅ CHANGED: Up from 100
				.asConstraint("Allow zero-hours employee assignments");
	}

	private Constraint employeeSchedulePatternConstraint(ConstraintFactory factory) {
		return factory.forEachIncludingNullVars(ShiftAssignment.class).filter(sa -> {
			Employee emp = sa.getEmployee();
			if (emp == null) {
				return false;
			}

			String location = sa.getShift().getShiftTemplate().getLocation();
			LocalDate date = sa.getShift().getShiftStart();
			ShiftType shiftType = sa.getShift().getShiftTemplate().getShiftType();

			// Check if employee can work this shift based on pattern
			// Now uses PeriodService to get correct week number
			return !emp.canWorkShift(location, date, shiftType);
		}).penalize(HardSoftScore.ONE_HARD).asConstraint("Employee schedule pattern violation");
	}

	// ========== HELPER METHODS ==========

	private boolean areIncompatibleBackToBack(ShiftAssignment sa1, ShiftAssignment sa2) {
		LocalDate date1 = sa1.getShift().getShiftStart();
		LocalDate date2 = sa2.getShift().getShiftStart();

		ShiftType type1 = sa1.getShift().getShiftTemplate().getShiftType();
		ShiftType type2 = sa2.getShift().getShiftTemplate().getShiftType();

		// ========== ALLOWED EXCEPTIONS ==========

		// Exception 1: SLEEP_IN always allowed (live-in care)
		if (type1 == ShiftType.SLEEP_IN || type2 == ShiftType.SLEEP_IN) {
			return false; // ✅ Allow LONG_DAY + SLEEP_IN
		}

		// Exception 2: FLOATING + FLOATING same day allowed
		if (type1 == ShiftType.FLOATING && type2 == ShiftType.FLOATING && date1.equals(date2)) {
			return false; // ✅ Allow multiple FLOATING same day
		}

		// ========== CHECK PROXIMITY ==========

		boolean sameDay = date1.equals(date2);
		boolean nextDay = date1.plusDays(1).equals(date2);

		if (!sameDay && !nextDay) {
			return false; // Not close enough
		}

		// ========== FORBIDDEN COMBINATIONS ==========

		// LONG_DAY cannot be followed by active shifts
		if (type1 == ShiftType.LONG_DAY) {
			if (type2 == ShiftType.DAY || type2 == ShiftType.WAKING_NIGHT || type2 == ShiftType.FLOATING) {
				return true; // ❌ Forbidden
			}
		}

		// DAY cannot be followed by active shifts same day
		if (type1 == ShiftType.DAY && sameDay) {
			if (type2 == ShiftType.DAY || type2 == ShiftType.WAKING_NIGHT || type2 == ShiftType.FLOATING) {
				return true; // ❌ Forbidden
			}
		}

		// DAY cannot be followed by WAKING_NIGHT next day
		if (type1 == ShiftType.DAY && nextDay && type2 == ShiftType.WAKING_NIGHT) {
			return true; // ❌ Forbidden
		}

		// WAKING_NIGHT cannot be followed by DAY/LONG_DAY next morning
		if (type1 == ShiftType.WAKING_NIGHT && nextDay) {
			if (type2 == ShiftType.DAY || type2 == ShiftType.LONG_DAY) {
				return true; // ❌ Forbidden (night to morning)
			}
		}

		return false; // All other combinations allowed
	}

	private Constraint weekOnWeekOffPattern(ConstraintFactory factory) {
		return factory.forEach(ShiftAssignment.class).filter(sa -> sa.getEmployee() != null).filter(sa -> {
			Employee emp = sa.getEmployee();
			return emp.getWeekOn() != null && emp.getWeekOff() != null;
		}).filter(sa -> {
			Employee emp = sa.getEmployee();
			Integer absoluteWeek = sa.getShift().getAbsoluteWeek(); // ✅ Calls transient getter

			if (absoluteWeek == null)
				return false;

			return !emp.shouldBeWorkingInAbsoluteWeek(absoluteWeek);
		}).penalize(HardSoftScore.ONE_HARD).asConstraint("Week-on week-off pattern");
	}

	private Constraint minDaysPerLocationPerWeek(ConstraintFactory factory) {
		return factory.forEach(ShiftAssignment.class).filter(sa -> sa.getEmployee() != null)
				.filter(sa -> sa.getShift().getShiftTemplate().getShiftType() != ShiftType.SLEEP_IN)
				.groupBy(ShiftAssignment::getEmployee, sa -> sa.getShift().getShiftTemplate().getLocation(),
						sa -> getWeekNumber(sa.getShift().getShiftStart()),
						ConstraintCollectors.countDistinct(sa -> sa.getShift().getShiftStart()))
				.filter((emp, location, weekNum, dayCount) -> dayCount == 1).penalize(HardSoftScore.ofSoft(200000)) // ✅
																													// FIXED:
																													// Changed
																													// from
																													// HARD
																													// to
																													// SOFT
				.asConstraint("Min 2 days per location per week (SOFT)");
	}

	private Constraint maxDaysPerLocationPerWeek(ConstraintFactory factory) {
		return factory.forEach(ShiftAssignment.class).filter(sa -> sa.getEmployee() != null)
				.filter(sa -> sa.getShift().getShiftTemplate().getShiftType() != ShiftType.SLEEP_IN)
				.groupBy(ShiftAssignment::getEmployee, sa -> sa.getShift().getShiftTemplate().getLocation(),
						sa -> getWeekNumber(sa.getShift().getShiftStart()),
						ConstraintCollectors.countDistinct(sa -> sa.getShift().getShiftStart()))
				.filter((emp, location, weekNum, dayCount) -> dayCount > 5)
				.penalize(HardSoftScore.ofSoft(10000), (emp, location, weekNum, dayCount) -> (dayCount - 5) * 50)
				.asConstraint("Max 5 days per location per week");
	}

	private Constraint penalizeDailyLocationSwitches(ConstraintFactory factory) {
		return factory.forEach(ShiftAssignment.class).filter(sa -> sa.getEmployee() != null)
				.filter(sa -> sa.getShift().getShiftTemplate().getShiftType() != ShiftType.SLEEP_IN)
				.join(ShiftAssignment.class, Joiners.equal(ShiftAssignment::getEmployee),
						Joiners.filtering((sa1, sa2) -> {
							// Check if shifts are on consecutive days
							LocalDate date1 = sa1.getShift().getShiftStart();
							LocalDate date2 = sa2.getShift().getShiftStart();
							return date2.equals(date1.plusDays(1));
						}))
				.filter((sa1, sa2) -> {
					// Different locations
					String loc1 = sa1.getShift().getShiftTemplate().getLocation();
					String loc2 = sa2.getShift().getShiftTemplate().getLocation();
					return !loc1.equals(loc2);
				}).penalize(HardSoftScore.ofSoft(1500000)).asConstraint("Penalize daily location switches");
	}

	private Constraint rewardConsecutiveDaysAtLocation(ConstraintFactory factory) {
		return factory.forEach(ShiftAssignment.class).filter(sa -> sa.getEmployee() != null)
				.filter(sa -> sa.getShift().getShiftTemplate().getShiftType() != ShiftType.SLEEP_IN)
				.join(ShiftAssignment.class, Joiners.equal(ShiftAssignment::getEmployee),
						Joiners.equal(sa -> sa.getShift().getShiftTemplate().getLocation()),
						Joiners.filtering((sa1, sa2) -> {
							// Check if shifts are on consecutive days at same location
							LocalDate date1 = sa1.getShift().getShiftStart();
							LocalDate date2 = sa2.getShift().getShiftStart();
							return date2.equals(date1.plusDays(1));
						}))
				.reward(HardSoftScore.ofSoft(500000)).asConstraint("Reward consecutive days at same location");
	}

	private Constraint limitLocationChangesPerWeek(ConstraintFactory factory) {
		return factory.forEach(ShiftAssignment.class).filter(sa -> sa.getEmployee() != null)
				.filter(sa -> sa.getShift().getShiftTemplate().getShiftType() != ShiftType.SLEEP_IN)
				.groupBy(ShiftAssignment::getEmployee, sa -> getWeekNumber(sa.getShift().getShiftStart()),
						ConstraintCollectors.countDistinct(sa -> sa.getShift().getShiftTemplate().getLocation()))
				.penalize(HardSoftScore.ofSoft(1000), (emp, weekNum, locationCount) -> { // ✅ REDUCED: From 2000 to 1000
					if (locationCount <= 2) {
						return 0; // Perfect - 1 or 2 locations
					} else if (locationCount == 3) {
						return 50; // OK but not ideal
					} else {
						return (locationCount - 3) * 100; // Heavy penalty for 4+ locations
					}
				}).asConstraint("Limit locations per employee per week");
	}

//	private Constraint preferFewerLocationsPerPeriod(ConstraintFactory factory) {
//	    return factory.forEach(ShiftAssignment.class)
//	        .filter(sa -> sa.getEmployee() != null)
//	        .filter(sa -> sa.getShift().getShiftTemplate().getShiftType() != ShiftType.SLEEP_IN)
//	        .groupBy(ShiftAssignment::getEmployee,
//	                 ConstraintCollectors.countDistinct(sa -> sa.getShift().getShiftTemplate().getLocation()))
//	        .penalize(HardSoftScore.ofSoft(1000000), (emp, locationCount) -> {
//	            if (locationCount == 1) {
//	                return 0;  // Perfect - no penalty
//	            } else if (locationCount == 2) {
//	                return 2;  // 2M penalty (2 × 1M)
//	            } else {
//	                return 5;  // 5M penalty for 3 locations (5 × 1M)
//	            }
//	        }).asConstraint("Prefer fewer locations per period");
//	}
	private Constraint locationPreferences(ConstraintFactory factory) {
		Set<ShiftType> applicableTypes = Set.of(ShiftType.DAY, ShiftType.WAKING_NIGHT, ShiftType.LONG_DAY);

		return factory.forEach(ShiftAssignment.class).filter(sa -> sa.getEmployee() != null)
				.filter(sa -> applicableTypes.contains(sa.getShift().getShiftTemplate().getShiftType()))
				// ✅ KEY FIX: Only filter for employees WITH preferences
				.filter(sa -> sa.getEmployee().hasServicePreferences()).reward(HardSoftScore.ofSoft(100000), sa -> {
					Employee emp = sa.getEmployee();
					String location = sa.getShift().getShiftTemplate().getLocation();
					int weightage = emp.getServiceWeightage(location);

					// Reward based on weightage
					if (weightage >= 50) {
						return weightage; // 50 → 50 points (× 10000 = 500,000!)
					} else if (weightage >= 30) {
						return weightage / 2; // 30 → 15 points (× 10000 = 150,000)
					} else if (weightage > 0) {
						return weightage / 5; // 10 → 2 points (× 10000 = 20,000)
					}
					return 0;
				}).asConstraint("Location preferences (reward only)");
	}

	private Constraint maxLocationsPerEmployeePerPeriod(ConstraintFactory factory) {
		return factory.forEach(ShiftAssignment.class).filter(sa -> sa.getEmployee() != null)
				.filter(sa -> sa.getShift().getShiftTemplate().getShiftType() != ShiftType.SLEEP_IN)
				.groupBy(ShiftAssignment::getEmployee,
						ConstraintCollectors.countDistinct(sa -> sa.getShift().getShiftTemplate().getLocation()))
				.filter((emp, locationCount) -> locationCount > 3)
				.penalize(HardSoftScore.ONE_HARD, (emp, locationCount) -> locationCount - 3)
				.asConstraint("Max 3 locations per period");
	}

	private record YearWeek(int year, int week) {
		public static YearWeek from(LocalDate date) {
			WeekFields wf = WeekFields.ISO;
			return new YearWeek(date.get(wf.weekBasedYear()), date.get(wf.weekOfWeekBasedYear()));
		}
	}

	private int getWeekNumber(LocalDate date) {
		return date.get(WeekFields.ISO.weekOfWeekBasedYear());
	}

	private int calculateMaxConsecutiveWeeks(List<YearWeek> weeksList) {
		if (weeksList == null || weeksList.isEmpty()) {
			return 0;
		}

		List<YearWeek> sortedWeeks = weeksList.stream().distinct()
				.sorted(Comparator.comparingInt(YearWeek::year).thenComparingInt(YearWeek::week)).toList();

		if (sortedWeeks.size() == 1) {
			return 1;
		}

		int maxConsecutive = 1;
		int currentStreak = 1;

		for (int i = 1; i < sortedWeeks.size(); i++) {
			YearWeek prev = sortedWeeks.get(i - 1);
			YearWeek curr = sortedWeeks.get(i);

			if (areConsecutiveWeeks(prev, curr)) {
				currentStreak++;
				maxConsecutive = Math.max(maxConsecutive, currentStreak);
			} else {
				currentStreak = 1;
			}
		}

		return maxConsecutive;
	}

	private boolean areConsecutiveWeeks(YearWeek week1, YearWeek week2) {
		if (week1.year() == week2.year()) {
			return week2.week() == week1.week() + 1;
		}

		if (week2.year() == week1.year() + 1) {
			return week1.week() >= 52 && week2.week() == 1;
		}

		return false;
	}

	private boolean hasInsufficientWeeksOff(Employee employee, List<YearWeek> weeksList) {
		if (weeksList == null || weeksList.size() < employee.getWeekOn()) {
			return false;
		}

		List<YearWeek> sortedWeeks = weeksList.stream().distinct()
				.sorted(Comparator.comparingInt(YearWeek::year).thenComparingInt(YearWeek::week)).toList();

		int currentStreak = 1;

		for (int i = 1; i < sortedWeeks.size(); i++) {
			YearWeek prev = sortedWeeks.get(i - 1);
			YearWeek curr = sortedWeeks.get(i);

			if (areConsecutiveWeeks(prev, curr)) {
				currentStreak++;
			} else {
				if (currentStreak >= employee.getWeekOn()) {
					int gapWeeks = weeksBetween(prev, curr) - 1;
					if (gapWeeks < employee.getWeekOff()) {
						return true;
					}
				}
				currentStreak = 1;
			}
		}

		return false;
	}

	private int weeksBetween(YearWeek w1, YearWeek w2) {
		if (w1.year() == w2.year()) {
			return w2.week() - w1.week();
		}

		// Cross year boundary
		int weeksInYear1 = 52; // Simplified - could be 52 or 53
		return (weeksInYear1 - w1.week()) + w2.week();
	}
}