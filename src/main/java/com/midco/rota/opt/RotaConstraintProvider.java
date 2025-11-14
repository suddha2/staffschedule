package com.midco.rota.opt;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.WeekFields;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintCollectors;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;
import org.optaplanner.core.api.score.stream.Joiners;
import org.optaplanner.core.api.score.stream.bi.BiConstraintStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.midco.rota.ShiftTypeLimitConfig;
import com.midco.rota.model.Employee;
import com.midco.rota.model.ShiftAssignment;
import com.midco.rota.util.ContractType;
import com.midco.rota.util.Gender;
import com.midco.rota.util.IdealShiftCount;
import com.midco.rota.util.ShiftType;

public class RotaConstraintProvider implements ConstraintProvider {

	private static final Logger logger = LoggerFactory.getLogger(RotaConstraintProvider.class);

	@Override
	public Constraint[] defineConstraints(ConstraintFactory factory) {
		return new Constraint[] {
				// HARD constraints - Must be satisfied
				unassignedShiftConstraint(factory), preventDuplicateAssignments(factory), genderConstraint(factory),
				restrictedDayOfWeekConstraint(factory), restrictedShiftTypeConstraint(factory),
				restrictedServiceConstraint(factory), maxWeeklyHoursConstraint(factory),
				tooManyEmployeesPerShift(factory), maxHoursPerShiftTypePerDay(factory),
				limitWeeklyShiftTypeCounts(factory), maxShiftsPerLocationPerWeek(factory), noBackToBack(factory),
				maxDaysOnIn4Weeks(factory), minDaysOffIn4Weeks(factory), maxConsecutiveWeeksOn(factory),
				minWeeksOffAfterStreak(factory),

				// SOFT constraints - Optimization goals
				rewardAssignedShift(factory), minWeeklyHoursConstraint(factory), evenDistribution(factory),
				preferedWorkingDaysConstraint(factory), preferredLocationConstraint(factory),
				preferedShiftTypeConstraint(factory), prioritizedAllocation(factory),
				prioritizeHighPriorityLocations(factory), permanentEmployeesHighPriority(factory),
				rewardZeroHoursAssignments(factory),

				// ❌ REMOVED: logShiftAssignmentDiagnostics - Too expensive
		};
	}

	// ========== HARD CONSTRAINTS ==========

	private Constraint unassignedShiftConstraint(ConstraintFactory factory) {
		return factory.forEachIncludingNullVars(ShiftAssignment.class).filter(sa -> sa.getEmployee() == null)
				.penalize(HardSoftScore.ofSoft(1000)) // High penalty for unassigned
				.asConstraint("Unassigned shift");
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
				.penalize(HardSoftScore.ONE_HARD, // ✅ CHANGED TO HARD
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
				.penalize(HardSoftScore.ONE_HARD, // ✅ CHANGED TO HARD
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
						Joiners.lessThan(assignment -> assignment.getShift().getShiftStart()))
				.filter((sa1, sa2) -> isBackToBack(sa1, sa2)).penalize(HardSoftScore.ofHard(1))
				.asConstraint("No back-to-back shifts");
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
				.penalize(HardSoftScore.ONE_SOFT, // ✅ CHANGED TO PENALIZE
						(employee, week, totalMinutes) -> {
							long minMinutes = employee.getMinHrs().longValue() * 60;
							return (int) (minMinutes - totalMinutes);
						})
				.asConstraint("Min weekly hours not met");
	}

	private Constraint evenDistribution(ConstraintFactory factory) {
		BiConstraintStream<Employee, Integer> countPerEmp = factory.forEachIncludingNullVars(ShiftAssignment.class)
				.filter(sa -> sa.getEmployee() != null)
				.groupBy(ShiftAssignment::getEmployee, ConstraintCollectors.count());

		return countPerEmp.join(IdealShiftCount.class).reward(HardSoftScore.ONE_SOFT,
				(employee, actualCount, idealFact) -> Math.abs(actualCount.intValue() - idealFact.getIdealCount()))
				.asConstraint("Even distribution");
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

	private Constraint preferredLocationConstraint(ConstraintFactory factory) {
		return factory.forEachIncludingNullVars(ShiftAssignment.class).filter(sa -> {
			Employee emp = sa.getEmployee();
			return emp != null && emp.getPreferredService() != null;
		}).reward(HardSoftScore.ONE_SOFT, sa -> {
			Employee emp = sa.getEmployee();
			String shiftLocation = sa.getShift().getShiftTemplate().getLocation();
			int index = emp.getPreferredService().indexOf(shiftLocation);
			if (index == -1)
				return 0;
			return emp.getPreferredService().size() - index;
		}).asConstraint("Prefer working at preferred service (prioritized)");
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
		return factory.forEachIncludingNullVars(ShiftAssignment.class)
				.filter(assignment -> assignment.getEmployee() != null).reward(HardSoftScore.ONE_SOFT, assignment -> {
					ShiftType shiftType = assignment.getShift().getShiftTemplate().getShiftType();
					ContractType contractType = assignment.getEmployee().getContractType();

					int shiftWeight = switch (shiftType) {
					case WAKING_NIGHT -> 400;
					case LONG_DAY -> 300;
					case DAY -> 200;
					case FLOATING -> 10;
					default -> 1;
					};

					// ✅ FIXED: Massive contract weight difference
					int contractWeight = switch (contractType) {
					case PERMANENT -> 100000; // 100x higher!
					case ZERO_HOURS -> 1000;
					default -> 1;
					};

					return shiftWeight + contractWeight;
				}).asConstraint("Allocated shift weighted by shift type and contract type");
	}

	private Constraint prioritizeHighPriorityLocations(ConstraintFactory factory) {
		return factory.forEachIncludingNullVars(ShiftAssignment.class).filter(sa -> sa.getEmployee() != null)
				.reward(HardSoftScore.ONE_SOFT, sa -> {
					int priority = sa.getShift().getShiftTemplate().getPriority();
//                return switch (priority) {
//                    case 1 -> 100;
//                    case 2 -> 50;
//                    case 3 -> 10;
//                    default -> 0;
//                };
					if (priority < 1 || priority > 12) {
						return 0;
					}
					return (13 - priority) * 10;
				}).asConstraint("Prioritize high-priority location assignments");
	}

	private Constraint permanentEmployeesHighPriority(ConstraintFactory factory) {
		return factory.forEach(ShiftAssignment.class)
				.filter(sa -> sa.getEmployee() != null && sa.getEmployee().getContractType() == ContractType.PERMANENT)
				.reward(HardSoftScore.ofSoft(10000)) // ✅ Increased from 80
				.asConstraint("Permanent employees high priority");
	}

	private Constraint rewardZeroHoursAssignments(ConstraintFactory factory) {
		return factory.forEach(ShiftAssignment.class).filter(sa -> {
			Employee emp = sa.getEmployee();
			return emp != null && emp.getContractType() == ContractType.ZERO_HOURS;
		}).reward(HardSoftScore.ofSoft(100)) // ✅ Increased from 20
				.asConstraint("Allow zero-hours employee assignments");
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

	// 2. Min days_off in the 4-week cycle
	private Constraint minDaysOffIn4Weeks(ConstraintFactory factory) {
		return factory.forEach(ShiftAssignment.class)
				.filter(sa -> sa.getEmployee() != null && sa.getEmployee().getDaysOff() != null
						&& sa.getEmployee().getDaysOff() > 0)
				.groupBy(ShiftAssignment::getEmployee,
						ConstraintCollectors.countDistinct(sa -> sa.getShift().getShiftStart()))
				.filter((employee, daysWorked) -> {
					int totalDays = 28; // 4 weeks = 28 days
					int daysOff = totalDays - daysWorked;
					return daysOff < employee.getDaysOff();
				}).penalize(HardSoftScore.ONE_HARD, (employee, daysWorked) -> {
					int totalDays = 28;
					int actualDaysOff = totalDays - daysWorked;
					return employee.getDaysOff() - actualDaysOff;
				}).asConstraint("Not enough rest days in 4-week cycle");
	}

	// 3. Max consecutive weeks_on in the 4-week cycle
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

	// 4. Min weeks_off after work streak in the 4-week cycle
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

	// ========== HELPER METHODS ==========

	private boolean isBackToBack(ShiftAssignment sa1, ShiftAssignment sa2) {
		LocalDate date1 = sa1.getShift().getShiftStart();
		LocalDate date2 = sa2.getShift().getShiftStart();

		LocalTime start1 = sa1.getShift().getShiftTemplate().getStartTime();
		LocalTime end1 = sa1.getShift().getShiftTemplate().getEndTime();
		LocalTime start2 = sa2.getShift().getShiftTemplate().getStartTime();
		LocalTime end2 = sa2.getShift().getShiftTemplate().getEndTime();

		if (start1 == null || end1 == null || start2 == null || end2 == null) {
			return false;
		}

		ShiftType type1 = sa1.getShift().getShiftTemplate().getShiftType();
		ShiftType type2 = sa2.getShift().getShiftTemplate().getShiftType();

		// Same-day adjacent shift types
		if (date1.equals(date2) && Math.abs(type1.ordinal() - type2.ordinal()) == 1) {
			return true;
		}

		// Overnight transition
		if (date1.plusDays(1).equals(date2) && type1 == ShiftType.WAKING_NIGHT
				&& (type2 == ShiftType.DAY || type2 == ShiftType.LONG_DAY)) {
			return true;
		}

		// Overlapping shifts on same day
		if (date1.equals(date2) && start1.isBefore(end2) && end1.isAfter(start2)) {
			return true;
		}

		// Short rest period (<12 hours)
		LocalDateTime shift1End = sa1.getShift().getShiftEnd().atTime(sa1.getShift().getShiftTemplate().getEndTime());
		LocalDateTime shift2Start = sa2.getShift().getShiftStart()
				.atTime(sa2.getShift().getShiftTemplate().getStartTime());

		if (shift1End.isBefore(shift2Start) || shift1End.equals(shift2Start)) {
			long restHours = Duration.between(shift1End, shift2Start).toHours();
			if (restHours < 12) {
				return true;
			}
		}

		return false;
	}

	private record YearWeek(int year, int week) {
		public static YearWeek from(LocalDate date) {
			WeekFields wf = WeekFields.ISO;
			return new YearWeek(date.get(wf.weekBasedYear()), date.get(wf.weekOfWeekBasedYear()));
		}
	}

	private int calculateMaxConsecutiveWeeks(List<YearWeek> weeksList) {
		if (weeksList == null || weeksList.isEmpty()) {
			return 0;
		}

		// Get unique sorted weeks
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

	/**
	 * Check if two weeks are consecutive
	 */
	private boolean areConsecutiveWeeks(YearWeek week1, YearWeek week2) {
		// Same year
		if (week1.year() == week2.year()) {
			return week2.week() == week1.week() + 1;
		}

		// Year boundary (week 52/53 → week 1)
		if (week2.year() == week1.year() + 1) {
			return week1.week() >= 52 && week2.week() == 1;
		}

		return false;
	}

	/**
	 * Check if employee has insufficient rest weeks after a work streak
	 */
	private boolean hasInsufficientWeeksOff(Employee employee, List<YearWeek> weeksList) {
		if (weeksList == null || weeksList.size() < employee.getWeekOn()) {
			return false; // Not enough weeks worked to form a streak
		}

		// Sort weeks
		List<YearWeek> sortedWeeks = weeksList.stream().distinct()
				.sorted(Comparator.comparingInt(YearWeek::year).thenComparingInt(YearWeek::week)).toList();

		// Find all work streaks and check gaps
		int currentStreak = 1;
		YearWeek streakStart = sortedWeeks.get(0);

		for (int i = 1; i < sortedWeeks.size(); i++) {
			YearWeek prev = sortedWeeks.get(i - 1);
			YearWeek curr = sortedWeeks.get(i);

			if (areConsecutiveWeeks(prev, curr)) {
				currentStreak++;
			} else {
				// End of streak - check if it was long enough and gap is sufficient
				if (currentStreak >= employee.getWeekOn()) {
					int gapWeeks = weeksBetween(prev, curr) - 1;
					if (gapWeeks < employee.getWeekOff()) {
						return true; // Gap too short!
					}
				}
				currentStreak = 1;
				streakStart = curr;
			}
		}

		return false;
	}

	/**
	 * Calculate weeks between two YearWeek instances
	 */
	private int weeksBetween(YearWeek w1, YearWeek w2) {
		if (w1.year() == w2.year()) {
			return w2.week() - w1.week();
		}

		// Cross year boundary
		int weeksInYear1 = 52; // Simplified - could be 52 or 53
		return (weeksInYear1 - w1.week()) + w2.week();
	}
}