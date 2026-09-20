package com.midco.rota.opt;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintCollectors;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;
import org.optaplanner.core.api.score.stream.Joiners;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

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

	/**
	 * Anchor for week-of-period maths: the first Monday of the very first pay
	 * period on record (Period 1 = 2025-03-31 → 2025-04-27). Every pay period
	 * in the system is a Mon-aligned 28-day block from this anchor, so
	 * {@code ((daysFromAnchor / 7) % 4) + 1} yields week 1..4 within whichever
	 * period any date falls into. This is used by the permanent-employee
	 * alternating cap so we don't need to inject the rota's period start as a
	 * problem fact.
	 */
	private static final LocalDate PERIOD_ANCHOR = LocalDate.of(2025, 3, 31);

	/**
	 * 1..4 week-of-period for the ISO Monday of any date, anchored at
	 * {@link #PERIOD_ANCHOR}. Pre-anchor dates clamp to week 1 (defensive; not
	 * expected in normal use).
	 */
	private static int weekOfPeriod(LocalDate weekMonday) {
		long days = ChronoUnit.DAYS.between(PERIOD_ANCHOR, weekMonday);
		if (days < 0) {
			return 1;
		}
		return (int) ((days / 7) % 4) + 1;
	}

	/**
	 * SLEEP_IN is now a shadow assignment (mirrors its paired LONG_DAY) and is
	 * therefore visible to the constraint streams. Most rules were written when
	 * SLEEP_IN was invisible (null during solving), so they must exclude it or a
	 * SLEEP_IN region becomes massively hard-infeasible. This predicate marks the
	 * assignments those rules still apply to (everything except SLEEP_IN).
	 */
	private static boolean countsAsWork(ShiftAssignment sa) {
		// Data-driven "counts as work" (was: type != SLEEP_IN). A new non-work type
		// (e.g. a salaried shift-lead) is excluded from these rules by seeding its
		// shift_type row with counts_as_work=false — no code change.
		if (sa.getShift() == null || sa.getShift().getShiftTemplate() == null
				|| sa.getShift().getShiftTemplate().getShiftTypeCode() == null) {
			return true;
		}
		return ShiftTypeMeta.countsAsWork(sa.getShift().getShiftTemplate().getShiftTypeCode());
	}

	/**
	 * A slot needs a carer unless it's a follower (shadow) shift, which is filled
	 * automatically by mirroring its leader. Used by the coverage (unassigned)
	 * penalty so genuine slots — including new types like a shift-lead — are pushed
	 * to be filled, while SLEEP_IN followers are not spuriously penalised. For the
	 * six built-ins this matches the old {@code countsAsWork} filter exactly (SLEEP_IN
	 * is the only follower), so it is behaviour-neutral for existing data.
	 */
	private static boolean coverageRequired(ShiftAssignment sa) {
		if (sa.getShift() == null || sa.getShift().getShiftTemplate() == null) {
			return true;
		}
		return !sa.getShift().getShiftTemplate().isEffectiveFollower();
	}

	/**
	 * Every constraint the system knows about is registered here, unconditionally.
	 * Whether a constraint actually runs is decided by its weight in the
	 * {@code constraint_setting} table, not by membership of this array.
	 *
	 * <p>That is safe because OptaPlanner compares each constraint weight against
	 * zero when it builds the scoring session and skips zero-weight constraints
	 * before they enter the node network, so a disabled rule costs nothing to
	 * leave registered. Thirteen of these have never been in force and seed as
	 * {@code enabled = false}; see
	 * {@link RotaConstraintConfiguration#INACTIVE_BY_DEFAULT}.
	 *
	 * <p>Do not comment entries out to disable a rule — set its weight to zero, or
	 * {@code enabled = false}, in the database instead. Removing an entry here
	 * while its {@code @ConstraintWeight} remains is harmless, but the reverse
	 * (registering a constraint with no weight) fails at bootstrap.
	 */
	@Override
	public Constraint[] defineConstraints(ConstraintFactory factory) {
		return new Constraint[] {
				// Physical impossibilities
				preventDuplicateAssignments(factory), tooManyEmployeesPerShift(factory),
				noInvalidSameDayShifts(factory), // MOVED: Before noBackToBack
				noBackToBack(factory), noOverlappingShifts(factory),

				// Eligibility
				genderConstraint(factory), restrictedDayOfWeekConstraint(factory),
				restrictedShiftTypeConstraint(factory), restrictedServiceConstraint(factory),
				requiredSkillsConstraint(factory), employeeSchedulePatternConstraint(factory),
				weekOnWeekOffPattern(factory), employeeUnavailableConstraint(factory),

				// Working-time limits
				maxHoursPerShiftTypePerDay(factory), limitWeeklyShiftTypeCounts(factory),
				maxWeeklyHoursConstraint(factory), employeeMaxHours(factory),
				maxDaysOnIn4Weeks(factory), minDaysOffIn4Weeks(factory),
				maxConsecutiveWeeksOn(factory), minWeeksOffAfterStreak(factory),
				maxMonthlyHoursWithExclusions(factory),
				maxShiftsPerLocationPerWeek(factory), maxLocationsPerEmployeePerPeriod(factory),

				// Contractual weekly shape (soft since the constraint gap analysis)
				permanentWeeklyAlternatingCap(factory), permanentWeeklyMinimum(factory),

				// Coverage and hours
				unassignedShiftConstraint(factory), rewardAssignedShift(factory),
				minWeeklyHoursConstraint(factory), penalizeOverloading(factory),
				encourageBalancedHours(factory), rewardZeroHoursAssignments(factory),

				// Preferences
				preferedWorkingDaysConstraint(factory), preferedShiftTypeConstraint(factory),
				prioritizedAllocation(factory), prioritizeHighPriorityLocations(factory),
				locationPreferences(factory),

				// Continuity of place
				minDaysPerLocationPerWeek(factory), maxDaysPerLocationPerWeek(factory),
				limitLocationChangesPerWeek(factory), penalizeDailyLocationSwitches(factory),
				rewardConsecutiveDaysAtLocation(factory),

				// Continuity of person (seed from prior period)
				continuityKeepSeededCarer(factory), };
	}

	private Constraint penalizeOverloading(ConstraintFactory factory) {
		return factory.forEach(ShiftAssignment.class).filter(sa -> sa.getEmployee() != null)
				.filter(sa -> sa.getEmployee().getMinHrs() != null)
				.filter(RotaConstraintProvider::countsAsWork)
				.groupBy(ShiftAssignment::getEmployee,
						ConstraintCollectors.sumLong(sa -> sa.getShift().getDurationInMins()))
				.filter((emp, totalMins) -> {
					long minMins = emp.getMinHrs().longValue() * 60;
					return totalMins > (minMins * 1.3);
				}).penalizeConfigurable((emp, totalMins) -> {
					long minMins = emp.getMinHrs().longValue() * 60;
					long threshold = (long) (minMins * 1.3);
					long excess = totalMins - threshold;
					int excessHours = (int) (excess / 60);
					return excessHours * excessHours;
				}).asConstraint("Penalize overloading individual employees");
	}

	private Constraint encourageBalancedHours(ConstraintFactory factory) {
		return factory.forEach(ShiftAssignment.class).filter(sa -> sa.getEmployee() != null)
				.filter(RotaConstraintProvider::countsAsWork)
				.groupBy(ShiftAssignment::getEmployee,
						ConstraintCollectors.sumLong(sa -> sa.getShift().getDurationInMins()))
				.rewardConfigurable((emp, totalMins) -> {
					double hours = totalMins / 60.0;
					double min = emp.getMinHrs() != null ? emp.getMinHrs().doubleValue() : 0;
					double max = emp.getMaxHrs() != null ? emp.getMaxHrs().doubleValue() : 999;
					double target = (min + max) / 2.0;
					double distanceFromTarget = Math.abs(hours - target);

					if (distanceFromTarget <= 5) {
						return 100;
					} else if (distanceFromTarget <= 10) {
						return 50;
					} else if (distanceFromTarget <= 20) {
						return 20;
					} else {
						return 5;
					}
				}).asConstraint("Encourage balanced hours around midpoint");
	}

	// ========== HARD CONSTRAINTS ==========

	private Constraint employeeMaxHours(ConstraintFactory factory) {
		return factory.forEach(ShiftAssignment.class).filter(sa -> sa.getEmployee() != null)
				.filter(sa -> sa.getEmployee().getMaxHrs() != null)
				.filter(RotaConstraintProvider::countsAsWork)
				.groupBy(ShiftAssignment::getEmployee, sa -> YearWeek.from(sa.getShift().getShiftStart()),
						ConstraintCollectors.sumLong(sa -> sa.getShift().getDurationInMins()))
				.filter((emp, week, totalMins) -> {
					long maxMins = emp.getMaxHrs().longValue() * 60;
					return totalMins > maxMins;
				}).penalizeConfigurable((emp, week, totalMins) -> {
					long maxMins = emp.getMaxHrs().longValue() * 60;
					long excessMins = totalMins - maxMins;
					return (int) (excessMins / 60);
				}).asConstraint("Max hours per week");
	}

	private Constraint unassignedShiftConstraint(ConstraintFactory factory) {
		return factory.forEachIncludingNullVars(ShiftAssignment.class).filter(sa -> sa.getEmployee() == null)
				.filter(RotaConstraintProvider::coverageRequired)
				.penalizeConfigurable().asConstraint("Unassigned shift");
	}

	private Constraint requiredSkillsConstraint(ConstraintFactory factory) {
		// Hard constraint: an assigned employee must possess every skill the shift template requires.
		// Skill matching is case-insensitive and trim-tolerant. Templates with no required skills are
		// ignored (the common case), so this does not degrade allocation for the bulk of the data.
		return factory.forEachIncludingNullVars(ShiftAssignment.class).filter(sa -> {
			Employee emp = sa.getEmployee();
			if (emp == null) return false;
			List<String> required = sa.getShift().getShiftTemplate().getRequiredSkills();
			if (required == null || required.isEmpty()) return false;
			List<String> empSkills = emp.getSkills();
			Set<String> have = new HashSet<>();
			if (empSkills != null) {
				for (String s : empSkills) {
					if (s != null) have.add(s.trim().toLowerCase());
				}
			}
			for (String r : required) {
				if (r == null) continue;
				if (!have.contains(r.trim().toLowerCase())) {
					return true; // missing at least one required skill
				}
			}
			return false;
		}).penalizeConfigurable().asConstraint("Missing required skill");
	}

	/**
	 * HARD: an employee must not be assigned to a shift that starts on a day they
	 * are unavailable (booked leave, sickness, ...). Availability spans are loaded
	 * as problem facts for the solve window and joined by employee id; the span is
	 * whole-day and inclusive. This is the fix for staff being allocated onto
	 * booked leave.
	 */
	/**
	 * SOFT: keep each carer in the slot they held last period. The pre-solve
	 * continuity seed records {@code seededEmployeeId} per assignment; this
	 * penalises assigning anyone else, so the solver carries the prior period
	 * forward unless a change scores better. This is the person-level continuity
	 * that house-level affinity cannot express. Pure O(1) filter.
	 */
	private Constraint continuityKeepSeededCarer(ConstraintFactory factory) {
		return factory.forEach(ShiftAssignment.class)
				.filter(sa -> sa.getEmployee() != null
						&& sa.getSeededEmployeeId() != null
						&& !sa.getSeededEmployeeId().equals(sa.getEmployee().getId()))
				.penalizeConfigurable()
				.asConstraint("Continuity - keep carer in seeded slot");
	}

	private Constraint employeeUnavailableConstraint(ConstraintFactory factory) {
		// The pre-solve step (SolverTrigger/RotaController loadData) builds each
		// employee's unavailable-date set from EmployeeAvailability spans clipped
		// to the solve window, so this is an O(1) lookup rather than a stream join.
		return factory.forEach(ShiftAssignment.class)
				.filter(sa -> sa.getEmployee() != null && sa.getShift() != null
						&& sa.getShift().getShiftStart() != null
						&& sa.getEmployee().isUnavailableOn(sa.getShift().getShiftStart()))
				.penalizeConfigurable()
				.asConstraint("Employee unavailable (leave)");
	}

	private Constraint preventDuplicateAssignments(ConstraintFactory factory) {
		return factory.forEachIncludingNullVars(ShiftAssignment.class)
				.filter(assignment -> assignment.getEmployee() != null)
				.groupBy(assignment -> assignment.getShift(), assignment -> assignment.getEmployee(),
						ConstraintCollectors.count())
				.filter((shift, employee, count) -> count > 1).penalizeConfigurable()
				.asConstraint("Duplicate assignment of employee to same shift");
	}

	private Constraint genderConstraint(ConstraintFactory factory) {
		return factory.forEachIncludingNullVars(ShiftAssignment.class).filter(RotaConstraintProvider::countsAsWork).filter(sa -> {
			Employee employee = sa.getEmployee();
			Gender required = sa.getShift().getShiftTemplate().getGender();
			if (required == Gender.ANY || employee == null) {
				return false;
			}
			return employee.getGender() != required;
		}).penalizeConfigurable().asConstraint("Gender mismatch");
	}

	private Constraint restrictedDayOfWeekConstraint(ConstraintFactory factory) {
		return factory.forEachIncludingNullVars(ShiftAssignment.class).filter(RotaConstraintProvider::countsAsWork).filter(sa -> {
			Employee emp = sa.getEmployee();
			return emp != null && emp.getRestrictedDays() != null
					&& emp.getRestrictedDays().contains(sa.getShift().getShiftTemplate().getDay());
		}).penalizeConfigurable().asConstraint("Restricted day of week");
	}

	private Constraint restrictedShiftTypeConstraint(ConstraintFactory factory) {
		return factory.forEachIncludingNullVars(ShiftAssignment.class).filter(RotaConstraintProvider::countsAsWork).filter(sa -> {
			Employee emp = sa.getEmployee();
			return emp != null && emp.getRestrictedShifts() != null
					&& emp.getRestrictedShifts().contains(sa.getShift().getShiftTemplate().getShiftType());
		}).penalizeConfigurable().asConstraint("Restricted Shift Type");
	}

	private Constraint restrictedServiceConstraint(ConstraintFactory factory) {
		return factory.forEachIncludingNullVars(ShiftAssignment.class).filter(RotaConstraintProvider::countsAsWork).filter(sa -> {
			Employee emp = sa.getEmployee();
			return emp != null && emp.getRestrictedService() != null
					&& emp.getRestrictedService().contains(sa.getShift().getShiftTemplate().getLocation());
		}).penalizeConfigurable().asConstraint("Restricted Service");
	}

	private Constraint maxWeeklyHoursConstraint(ConstraintFactory factory) {
		return factory.forEachIncludingNullVars(ShiftAssignment.class).filter(sa -> sa.getEmployee() != null)
				.filter(RotaConstraintProvider::countsAsWork)
				.groupBy(ShiftAssignment::getEmployee, sa -> YearWeek.from(sa.getShift().getShiftStart()),
						ConstraintCollectors.sumLong(sa -> sa.getShift().getDurationInMins()))
				.filter((employee, week, totalMinutes) -> totalMinutes > (employee.getMaxHrs().longValue() * 60))
				.penalizeConfigurable(
						(employee, week,
								totalMinutes) -> (int) (totalMinutes - (employee.getMaxHrs().longValue() * 60)))
				.asConstraint("Max weekly hours exceeded");
	}

	private Constraint tooManyEmployeesPerShift(ConstraintFactory factory) {
		return factory.forEachIncludingNullVars(ShiftAssignment.class)
				.groupBy(ShiftAssignment::getShift, ConstraintCollectors.count())
				.filter((shift, count) -> count > shift.getShiftTemplate().getEmpCount())
				.penalizeConfigurable().asConstraint("Too many employees for shift");
	}

	private Constraint maxHoursPerShiftTypePerDay(ConstraintFactory factory) {
		// Per-type daily hour caps are data-driven (shift_type.max_hours_per_day); null = no cap.
		return factory.forEachIncludingNullVars(ShiftAssignment.class).filter(sa -> sa.getEmployee() != null)
				.filter(RotaConstraintProvider::countsAsWork)
				.groupBy(sa -> sa.getEmployee(), sa -> sa.getShift().getShiftStart(),
						sa -> sa.getShift().getShiftTemplate().getShiftTypeCode(),
						ConstraintCollectors.sumLong(sa -> sa.getShift().getDurationInMins()))
				.filter((employee, date, code, totalMinutes) -> {
					Integer maxHours = ShiftTypeMeta.maxHoursPerDay(code);
					return maxHours != null && totalMinutes > maxHours * 60L;
				}).penalizeConfigurable((employee, date, code, totalMinutes) -> {
					Integer maxHours = ShiftTypeMeta.maxHoursPerDay(code);
					long maxMins = (maxHours == null ? 0 : maxHours) * 60L;
					return (int) (totalMinutes - maxMins);
				}).asConstraint("Max hours per shift type per day");
	}

	private Constraint limitWeeklyShiftTypeCounts(ConstraintFactory factory) {
		// Per-type weekly count caps are data-driven (shift_type.max_per_week); null = no cap.
		return factory.forEachIncludingNullVars(ShiftAssignment.class).filter(sa -> sa.getEmployee() != null)
				.filter(RotaConstraintProvider::countsAsWork)
				.groupBy(sa -> sa.getEmployee(), sa -> sa.getShift().getShiftTemplate().getShiftTypeCode(),
						sa -> YearWeek.from(sa.getShift().getShiftStart()), ConstraintCollectors.count())
				.filter((emp, code, week, count) -> {
					Integer lim = ShiftTypeMeta.maxPerWeek(code);
					return lim != null && count > lim;
				})
				.penalizeConfigurable((emp, code, week, count) -> {
					Integer lim = ShiftTypeMeta.maxPerWeek(code);
					return count - (lim == null ? 0 : lim);
				})
				.asConstraint("Weekly limit per shift type");
	}

	/**
	 * HARD upper-bound: for every <b>Permanent</b> employee, the combined count
	 * of shifts that aren't LONG_DAY / SLEEP_IN / FLOATING is capped at <b>6 in
	 * odd weeks (1, 3) and 5 in even weeks (2, 4)</b> of the pay period. This
	 * encodes the 6-5-6-5 alternating roster pattern as a strict ceiling.
	 *
	 * <p>The grouping key is the Monday of the shift's ISO week; the
	 * {@link #weekOfPeriod(LocalDate)} helper turns that into 1..4 relative to
	 * the global {@link #PERIOD_ANCHOR}, so the rule works without injecting a
	 * per-rota period start as a problem fact.
	 *
	 * <p>Non-Permanent (ZERO_HOURS etc.) employees are intentionally excluded:
	 * they are governed only by their contract's max-hours / fit, not by this
	 * weekly cap. This replaces the prior flat ≤6 cap entirely.
	 */
	private Constraint permanentWeeklyAlternatingCap(ConstraintFactory factory) {
		final SolverTuning tuning = SolverTuning.current();
		return factory.forEachIncludingNullVars(ShiftAssignment.class)
				.filter(sa -> sa.getEmployee() != null)
				.filter(sa -> sa.getEmployee().getContractType() == ContractType.PERMANENT)
				.filter(sa -> {
					String code = sa.getShift().getShiftTemplate().getShiftTypeCode();
					return code != null && ShiftTypeMeta.countsTowardWeeklyCap(code);
				})
				.groupBy(ShiftAssignment::getEmployee,
						sa -> sa.getShift().getShiftStart().with(DayOfWeek.MONDAY),
						ConstraintCollectors.count())
				.filter((emp, weekMonday, count) -> count > maxForWeek(weekMonday, tuning))
				.penalizeConfigurable(
						(emp, weekMonday, count) -> count - maxForWeek(weekMonday, tuning))
				.asConstraint("Permanent weekly shift cap");
	}

	/**
	 * HARD lower-bound: for every <b>Permanent</b> employee with at least one
	 * qualifying shift in a given week, that count must be ≥ 5. Combined with
	 * the alternating cap above, this enforces a tight {5, 6} range in odd
	 * weeks and {5} in even weeks.
	 *
	 * <p><b>Important — conditional minimum.</b> This constraint fires only
	 * when the employee already has ≥1 qualifying shift in the week. A
	 * Permanent employee with zero qualifying shifts in a week (e.g. on
	 * holiday, restricted from working any day in that week, or simply not yet
	 * allocated by the solver) does <i>not</i> incur a hard violation here. An
	 * unconditional minimum would otherwise create unsatisfiable hard score on
	 * any week where the staffing fit genuinely doesn't allow 5 — exactly the
	 * trap the previous flat-6 cap fell into when it counted pinned shifts.
	 * The existing {@code minWeeklyHoursConstraint} (soft) continues to pull
	 * the solver toward filling such weeks.
	 */
	private Constraint permanentWeeklyMinimum(ConstraintFactory factory) {
		final int MIN_PER_WEEK = SolverTuning.current().getWeeklyMinPerAllocatedWeek();
		return factory.forEachIncludingNullVars(ShiftAssignment.class)
				.filter(sa -> sa.getEmployee() != null)
				.filter(sa -> sa.getEmployee().getContractType() == ContractType.PERMANENT)
				.filter(sa -> {
					String code = sa.getShift().getShiftTemplate().getShiftTypeCode();
					return code != null && ShiftTypeMeta.countsTowardWeeklyCap(code);
				})
				.groupBy(ShiftAssignment::getEmployee,
						sa -> sa.getShift().getShiftStart().with(DayOfWeek.MONDAY),
						ConstraintCollectors.count())
				.filter((emp, weekMonday, count) -> count < MIN_PER_WEEK)
				.penalizeConfigurable(
						(emp, weekMonday, count) -> MIN_PER_WEEK - count)
				.asConstraint("Permanent weekly shift minimum");
	}

	/**
	 * Upper bound for a given week's Monday, taken from the supplied tuning:
	 * the odd-week cap in weeks 1 and 3 of the period, the even-week cap in
	 * weeks 2 and 4. Both values come from the {@code solver_tuning} table.
	 */
	private static int maxForWeek(LocalDate weekMonday, SolverTuning tuning) {
		return tuning.capForWeekOfPeriod(weekOfPeriod(weekMonday));
	}

	private Constraint maxShiftsPerLocationPerWeek(ConstraintFactory factory) {
		final int MAX_NON_FLOATING_SHIFTS = SolverTuning.current().getMaxNonFloatingShiftsPerLocationPerWeek();

		return factory.forEachIncludingNullVars(ShiftAssignment.class).filter(sa -> {
			Employee emp = sa.getEmployee();
			ShiftType type = sa.getShift().getShiftTemplate().getShiftType();
			return emp != null && type != ShiftType.FLOATING;
		}).groupBy(sa -> sa.getEmployee(), sa -> YearWeek.from(sa.getShift().getShiftStart()),
				sa -> sa.getShift().getShiftTemplate().getLocation(), ConstraintCollectors.count())
				.filter((emp, week, location, count) -> count > MAX_NON_FLOATING_SHIFTS)
				.penalizeConfigurable((emp, week, location, count) -> count - MAX_NON_FLOATING_SHIFTS)
				.asConstraint("Max non-floating shifts per location per week");
	}

	// ✅ FIXED: Same-day constraint (handles all same-day logic)
	private Constraint noInvalidSameDayShifts(ConstraintFactory factory) {
		return factory.forEach(ShiftAssignment.class).filter(sa -> sa.getEmployee() != null && sa.getShift() != null)
				.filter(RotaConstraintProvider::countsAsWork)
				.groupBy(ShiftAssignment::getEmployee, sa -> sa.getShift().getShiftStart(),
						ConstraintCollectors.toList())
				.filter((emp, date, dayAssignments) -> !isAllowedDayAssignments(dayAssignments))
				.penalizeConfigurable().asConstraint("No invalid same-day shift combinations");
	}

	/**
	 * Data-driven overlap: no carer works two shifts whose time windows intersect.
	 * Works for ANY shift type (unlike the enum-keyed same-day/back-to-back rules),
	 * so it catches overlaps those miss — e.g. a shift-lead 09:00–17:00 clashing with a
	 * day 08:00–20:00. Contiguous shifts (LONG_DAY 08:00–20:00 then SLEEP_IN 20:00–08:00)
	 * do NOT overlap, so pairing is unaffected — no exemption needed.
	 */
	private Constraint noOverlappingShifts(ConstraintFactory factory) {
		return factory.forEach(ShiftAssignment.class)
				.filter(sa -> sa.getEmployee() != null && sa.getShift() != null
						&& sa.getShift().getShiftTemplate() != null)
				.join(ShiftAssignment.class,
						Joiners.equal(ShiftAssignment::getEmployee),
						Joiners.lessThan(ShiftAssignment::getPlanningId))
				.filter((a, b) -> shiftsOverlap(a, b))
				.penalizeConfigurable()
				.asConstraint("Overlapping shifts");
	}

	/** True if the two assignments' actual date-time windows intersect (handles overnight). */
	private static boolean shiftsOverlap(ShiftAssignment a, ShiftAssignment b) {
		java.time.LocalDateTime aStart = startOf(a), aEnd = endOf(a);
		java.time.LocalDateTime bStart = startOf(b), bEnd = endOf(b);
		if (aStart == null || aEnd == null || bStart == null || bEnd == null) {
			return false;
		}
		return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
	}

	private static java.time.LocalDateTime startOf(ShiftAssignment sa) {
		if (sa.getShift() == null || sa.getShift().getShiftStart() == null
				|| sa.getShift().getShiftTemplate() == null || sa.getShift().getShiftTemplate().getStartTime() == null) {
			return null;
		}
		return sa.getShift().getShiftStart().atTime(sa.getShift().getShiftTemplate().getStartTime());
	}

	private static java.time.LocalDateTime endOf(ShiftAssignment sa) {
		if (sa.getShift() == null || sa.getShift().getShiftTemplate() == null
				|| sa.getShift().getShiftTemplate().getEndTime() == null) {
			return null;
		}
		// shiftEnd is the (possibly next-day) date for overnight shifts; fall back to start date.
		java.time.LocalDate endDate = sa.getShift().getShiftEnd() != null
				? sa.getShift().getShiftEnd() : sa.getShift().getShiftStart();
		if (endDate == null) {
			return null;
		}
		return endDate.atTime(sa.getShift().getShiftTemplate().getEndTime());
	}

	// ✅ FIXED: Back-to-back constraint (handles ONLY next-day transitions)
	private Constraint noBackToBack(ConstraintFactory factory) {
		return factory.forEach(ShiftAssignment.class)
				.join(ShiftAssignment.class, Joiners.equal(ShiftAssignment::getEmployee),
						Joiners.lessThan(sa -> sa.getShift().getShiftStart()))
				.filter((sa1, sa2) -> countsAsWork(sa1) && countsAsWork(sa2) && areIncompatibleBackToBack(sa1, sa2))
				.penalizeConfigurable()
				.asConstraint("No incompatible back-to-back shifts");
	}

	private Constraint maxDaysOnIn4Weeks(ConstraintFactory factory) {
		return factory.forEach(ShiftAssignment.class)
				.filter(sa -> sa.getEmployee() != null && sa.getEmployee().getDaysOn() != null
						&& sa.getEmployee().getDaysOn() > 0)
				.groupBy(ShiftAssignment::getEmployee,
						ConstraintCollectors.countDistinct(sa -> sa.getShift().getShiftStart()))
				.filter((employee, daysWorked) -> daysWorked > employee.getDaysOn())
				.penalizeConfigurable((employee, daysWorked) -> daysWorked - employee.getDaysOn())
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
				}).penalizeConfigurable((employee, daysWorked) -> {
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
				}).penalizeConfigurable((employee, weeksList) -> {
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
				.penalizeConfigurable().asConstraint("Insufficient weeks off after work streak");
	}

	private Constraint maxMonthlyHoursWithExclusions(ConstraintFactory factory) {
		final long capMinutes = SolverTuning.current().getMonthlyHoursCap() * 60L;

		return factory.forEach(ShiftAssignment.class).filter(sa -> sa.getEmployee() != null)
				.filter(RotaConstraintProvider::countsAsWork)
				.groupBy(ShiftAssignment::getEmployee,
						ConstraintCollectors.sumLong(sa -> sa.getShift().getDurationInMins()))
				.filter((employee, totalMinutes) -> totalMinutes > capMinutes)
				.penalizeConfigurable(
						(employee, totalMinutes) -> (int) Math.min(Integer.MAX_VALUE, (totalMinutes - capMinutes) / 60))
				.asConstraint("Monthly hours cap");
	}

	// ========== SOFT CONSTRAINTS ==========

	private Constraint rewardAssignedShift(ConstraintFactory factory) {
		return factory.forEachIncludingNullVars(ShiftAssignment.class).filter(sa -> sa.getEmployee() != null)
				.rewardConfigurable().asConstraint("Assigned shift");
	}

	private Constraint minWeeklyHoursConstraint(ConstraintFactory factory) {
		return factory.forEachIncludingNullVars(ShiftAssignment.class).filter(sa -> sa.getEmployee() != null)
				.filter(RotaConstraintProvider::countsAsWork)
				.groupBy(ShiftAssignment::getEmployee, sa -> YearWeek.from(sa.getShift().getShiftStart()),
						ConstraintCollectors.sumLong(sa -> sa.getShift().getDurationInMins()))
				.filter((employee, week, totalMinutes) -> totalMinutes < (employee.getMinHrs().longValue() * 60))
				.penalizeConfigurable((employee, week, totalMinutes) -> {
					long minMinutes = employee.getMinHrs().longValue() * 60;
					return (int) (minMinutes - totalMinutes);
				}).asConstraint("Min weekly hours not met");
	}

	private Constraint preferedWorkingDaysConstraint(ConstraintFactory factory) {
		return factory.forEachIncludingNullVars(ShiftAssignment.class).filter(sa -> {
			Employee emp = sa.getEmployee();
			if (emp == null || emp.getPreferredDays() == null)
				return false;
			DayOfWeek shiftDay = sa.getShift().getShiftStart().getDayOfWeek();
			return emp.getPreferredDays().contains(shiftDay);
		}).rewardConfigurable().asConstraint("Prefer working on preferred days");
	}

	private Constraint preferedShiftTypeConstraint(ConstraintFactory factory) {
		return factory.forEachIncludingNullVars(ShiftAssignment.class).filter(sa -> {
			Employee emp = sa.getEmployee();
			if (emp == null || emp.getPreferredShifts() == null)
				return false;
			ShiftType shiftType = sa.getShift().getShiftTemplate().getShiftType();
			// shiftType is null for a new data-driven type; List.of(...).contains(null) NPEs,
			// and a new type is never in a carer's built-in preferred list anyway.
			return shiftType != null && emp.getPreferredShifts().contains(shiftType);
		}).rewardConfigurable().asConstraint("Prefer working on preferred shift");
	}

	private Constraint prioritizedAllocation(ConstraintFactory factory) {
		return factory.forEachIncludingNullVars(ShiftAssignment.class).filter(sa -> sa.getEmployee() != null)
				.rewardConfigurable(sa -> {
					ShiftType shiftType = sa.getShift().getShiftTemplate().getShiftType();
					int priority = sa.getShift().getShiftTemplate().getPriority();

					// shiftType is null for a new data-driven type (not a built-in enum) —
					// give it the same low default the switch's default branch used.
					int shiftWeight = shiftType == null ? 1 : switch (shiftType) {
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
				.rewardConfigurable(sa -> {
					int priority = sa.getShift().getShiftTemplate().getPriority();

					if (priority < 1) {
						return 0;
					}

					int maxPriority = Math.min(priority, 20);
					return (21 - maxPriority) * 10;
				}).asConstraint("Prioritize high-priority location assignments");
	}

	private Constraint rewardZeroHoursAssignments(ConstraintFactory factory) {
		return factory.forEach(ShiftAssignment.class).filter(sa -> {
			Employee emp = sa.getEmployee();
			return emp != null && emp.getContractType() == ContractType.ZERO_HOURS;
		}).rewardConfigurable().asConstraint("Allow zero-hours employee assignments");
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

			return !emp.canWorkShift(location, date, shiftType);
		}).penalizeConfigurable().asConstraint("Employee schedule pattern violation");
	}

	// ========== HELPER METHODS ==========

	private boolean isAllowedDayAssignments(List<ShiftAssignment> dayAssignments) {
		if (dayAssignments == null || dayAssignments.isEmpty())
			return true; // nothing assigned is fine at this stage
		if (dayAssignments.size() == 1)
			return true; // any single shift is OK

		// Extract type and location
		List<ShiftType> types = dayAssignments.stream().map(sa -> sa.getShift().getShiftTemplate().getShiftType())
				.toList();
		List<String> locations = dayAssignments.stream().map(sa -> sa.getShift().getShiftTemplate().getLocation())
				.toList();

		boolean allFloating = types.stream().allMatch(t -> t == ShiftType.FLOATING);
		if (allFloating) {
			// Allow multiple FLOATING but enforce all at DIFFERENT locations
			long distinctLocs = locations.stream().distinct().count();
			return distinctLocs == locations.size();
		}

		// Disallow mixing FLOATING with any non-floating
		boolean containsFloating = types.stream().anyMatch(t -> t == ShiftType.FLOATING);
		boolean containsNonFloating = types.stream().anyMatch(t -> t == ShiftType.DAY || t == ShiftType.LONG_DAY
				|| t == ShiftType.WAKING_NIGHT || t == ShiftType.SLEEP_IN);
		if (containsFloating && containsNonFloating) {
			return false;
		}

		// Non-floating combos:
		if (dayAssignments.size() == 2) {
			// Allow exactly LONG_DAY + SLEEP_IN at the SAME location
			ShiftType t1 = types.get(0);
			ShiftType t2 = types.get(1);
			boolean ldSiPair = (t1 == ShiftType.LONG_DAY && t2 == ShiftType.SLEEP_IN)
					|| (t1 == ShiftType.SLEEP_IN && t2 == ShiftType.LONG_DAY);
			boolean sameLocation = locations.get(0) != null && locations.get(0).equals(locations.get(1));
			return ldSiPair && sameLocation;
		}

		// Any other case with 2+ non-floating shifts is invalid
		return false;
	}

	

	// ✅ FIXED: Only handles NEXT-DAY transitions (skips same-day)
	private boolean areIncompatibleBackToBack(ShiftAssignment sa1, ShiftAssignment sa2) {
		LocalDate date1 = sa1.getShift().getShiftStart();
		LocalDate date2 = sa2.getShift().getShiftStart();

		ShiftType type1 = sa1.getShift().getShiftTemplate().getShiftType();
		ShiftType type2 = sa2.getShift().getShiftTemplate().getShiftType();

		boolean sameDay = date1.equals(date2);
		boolean nextDay = date1.plusDays(1).equals(date2);

		// ✅ CRITICAL FIX: Skip same-day checks - let noInvalidSameDayShifts handle them
		if (sameDay) {
			return false;
		}

		if (!nextDay) {
			return false; // Not close enough
		}

		// ========== NEXT DAY TRANSITION RULES ==========

		// LONG_DAY cannot be followed by active shifts next day
		if (type1 == ShiftType.LONG_DAY) {
			if (type2 == ShiftType.DAY || type2 == ShiftType.WAKING_NIGHT || type2 == ShiftType.FLOATING) {
				return true; // ❌ Forbidden
			}
		}

		// DAY cannot be followed by WAKING_NIGHT next day
		if (type1 == ShiftType.DAY && type2 == ShiftType.WAKING_NIGHT) {
			return true; // ❌ Forbidden
		}

		// WAKING_NIGHT cannot be followed by DAY/LONG_DAY next morning
		if (type1 == ShiftType.WAKING_NIGHT) {
			if (type2 == ShiftType.DAY || type2 == ShiftType.LONG_DAY) {
				return true; // ❌ Forbidden
			}
		}

		// SLEEP_IN ending in morning cannot be followed by active shifts
		if (type1 == ShiftType.SLEEP_IN) {
			if (type2 == ShiftType.DAY || type2 == ShiftType.LONG_DAY || type2 == ShiftType.WAKING_NIGHT) {
				return true; // ❌ Forbidden
			}
		}

		return false; // All other next-day combinations allowed
	}

	private Constraint weekOnWeekOffPattern(ConstraintFactory factory) {
		return factory.forEach(ShiftAssignment.class).filter(sa -> sa.getEmployee() != null).filter(sa -> {
			Employee emp = sa.getEmployee();
			return emp.getWeekOn() != null && emp.getWeekOff() != null;
		}).filter(sa -> {
			Employee emp = sa.getEmployee();
			Integer absoluteWeek = sa.getShift().getAbsoluteWeek();

			if (absoluteWeek == null)
				return false;

			return !emp.shouldBeWorkingInAbsoluteWeek(absoluteWeek);
		}).penalizeConfigurable().asConstraint("Week-on week-off pattern");
	}

	private Constraint minDaysPerLocationPerWeek(ConstraintFactory factory) {
		return factory.forEach(ShiftAssignment.class).filter(sa -> sa.getEmployee() != null)
				.filter(RotaConstraintProvider::countsAsWork)
				.groupBy(ShiftAssignment::getEmployee, sa -> sa.getShift().getShiftTemplate().getLocation(),
						sa -> getWeekNumber(sa.getShift().getShiftStart()),
						ConstraintCollectors.countDistinct(sa -> sa.getShift().getShiftStart()))
				.filter((emp, location, weekNum, dayCount) -> dayCount == 1).penalizeConfigurable()
				.asConstraint("Min 2 days per location per week (SOFT)");
	}

	private Constraint maxDaysPerLocationPerWeek(ConstraintFactory factory) {
		final int maxDays = SolverTuning.current().getMaxDaysPerLocationPerWeek();
		return factory.forEach(ShiftAssignment.class).filter(sa -> sa.getEmployee() != null)
				.filter(RotaConstraintProvider::countsAsWork)
				.groupBy(ShiftAssignment::getEmployee, sa -> sa.getShift().getShiftTemplate().getLocation(),
						sa -> getWeekNumber(sa.getShift().getShiftStart()),
						ConstraintCollectors.countDistinct(sa -> sa.getShift().getShiftStart()))
				.filter((emp, location, weekNum, dayCount) -> dayCount > maxDays)
				.penalizeConfigurable((emp, location, weekNum, dayCount) -> (dayCount - maxDays) * 50)
				.asConstraint("Max days per location per week");
	}

	private Constraint penalizeDailyLocationSwitches(ConstraintFactory factory) {
		return factory.forEach(ShiftAssignment.class).filter(sa -> sa.getEmployee() != null)
				.filter(RotaConstraintProvider::countsAsWork)
				.join(ShiftAssignment.class, Joiners.equal(ShiftAssignment::getEmployee),
						Joiners.filtering((sa1, sa2) -> {
							LocalDate date1 = sa1.getShift().getShiftStart();
							LocalDate date2 = sa2.getShift().getShiftStart();
							return date2.equals(date1.plusDays(1));
						}))
				.filter((sa1, sa2) -> {
					String loc1 = sa1.getShift().getShiftTemplate().getLocation();
					String loc2 = sa2.getShift().getShiftTemplate().getLocation();
					return !loc1.equals(loc2);
				}).penalizeConfigurable().asConstraint("Penalize daily location switches");
	}

	private Constraint rewardConsecutiveDaysAtLocation(ConstraintFactory factory) {
		return factory.forEach(ShiftAssignment.class).filter(sa -> sa.getEmployee() != null)
				.filter(RotaConstraintProvider::countsAsWork)
				.join(ShiftAssignment.class, Joiners.equal(ShiftAssignment::getEmployee),
						Joiners.equal(sa -> sa.getShift().getShiftTemplate().getLocation()),
						Joiners.filtering((sa1, sa2) -> {
							LocalDate date1 = sa1.getShift().getShiftStart();
							LocalDate date2 = sa2.getShift().getShiftStart();
							return date2.equals(date1.plusDays(1));
						}))
				.rewardConfigurable().asConstraint("Reward consecutive days at same location");
	}

	private Constraint limitLocationChangesPerWeek(ConstraintFactory factory) {
		return factory.forEach(ShiftAssignment.class).filter(sa -> sa.getEmployee() != null)
				.filter(RotaConstraintProvider::countsAsWork)
				.groupBy(ShiftAssignment::getEmployee, sa -> getWeekNumber(sa.getShift().getShiftStart()),
						ConstraintCollectors.countDistinct(sa -> sa.getShift().getShiftTemplate().getLocation()))
				.penalizeConfigurable((emp, weekNum, locationCount) -> {
					if (locationCount <= 2) {
						return 0;
					} else if (locationCount == 3) {
						return 50;
					} else {
						return (locationCount - 3) * 100;
					}
				}).asConstraint("Limit locations per employee per week");
	}

	private Constraint locationPreferences(ConstraintFactory factory) {
		return factory.forEach(ShiftAssignment.class).filter(sa -> sa.getEmployee() != null)
				.filter(sa -> {
					String tc = sa.getShift().getShiftTemplate().getShiftTypeCode();
					return tc != null && ShiftTypeMeta.countsAsLocationCoverage(tc);
				})
				.filter(sa -> sa.getEmployee().hasServicePreferences()).rewardConfigurable(sa -> {
					Employee emp = sa.getEmployee();
					String location = sa.getShift().getShiftTemplate().getLocation();
					int weightage = emp.getServiceWeightage(location);

					if (weightage >= 50) {
						return weightage;
					} else if (weightage >= 30) {
						return weightage / 2;
					} else if (weightage > 0) {
						return weightage / 5;
					}
					return 0;
				}).asConstraint("Location preferences (reward only)");
	}

	private Constraint maxLocationsPerEmployeePerPeriod(ConstraintFactory factory) {
		final int maxLocations = SolverTuning.current().getMaxLocationsPerPeriod();
		return factory.forEach(ShiftAssignment.class).filter(sa -> sa.getEmployee() != null)
				.filter(RotaConstraintProvider::countsAsWork)
				.groupBy(ShiftAssignment::getEmployee,
						ConstraintCollectors.countDistinct(sa -> sa.getShift().getShiftTemplate().getLocation()))
				.filter((emp, locationCount) -> locationCount > maxLocations)
				.penalizeConfigurable((emp, locationCount) -> locationCount - maxLocations)
				.asConstraint("Max locations per period");
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

		int weeksInYear1 = 52;
		return (weeksInYear1 - w1.week()) + w2.week();
	}
}