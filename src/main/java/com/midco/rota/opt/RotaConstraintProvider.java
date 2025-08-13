package com.midco.rota.opt;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintCollectors;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;
import org.optaplanner.core.api.score.stream.Joiners;
import org.optaplanner.core.api.score.stream.bi.BiConstraintStream;

import com.midco.rota.model.Employee;
import com.midco.rota.model.ShiftAssignment;
import com.midco.rota.util.Gender;
import com.midco.rota.util.IdealShiftCount;
import com.midco.rota.util.ShiftType;


public class RotaConstraintProvider implements ConstraintProvider {
	@Override
	public Constraint[] defineConstraints(ConstraintFactory factory) {

		return new Constraint[] { rewardOneShiftPerDay(factory), unassignedShiftConstraint(factory),
				allEmployeesHaveAtLeastOneShift(factory), noBackToBack(factory), oneShiftPerDay(factory),
				evenDistribution(factory), genderConstraint(factory), preferedWorkingDaysConstraint(factory),
				preferedLocationConstraint(factory), preferedShiftTypeConstraint(factory),
				restrictedDayOfWeekConstraint(factory), restrictedShiftTypeConstraint(factory),
				restrictedServiceConstraint(factory), maxWeeklyHoursConstraint(factory),
				minWeeklyHoursConstraint(factory) };
	}

	private Constraint rewardOneShiftPerDay(ConstraintFactory factory) {
		return factory.forEach(ShiftAssignment.class).filter(sa -> sa.getEmployee() != null)
				.groupBy(ShiftAssignment::getEmployee, ShiftAssignment::getDate, ConstraintCollectors.count())
				.filter((emp, date, count) -> count == 1).reward(HardSoftScore.ONE_SOFT)
				.asConstraint("Exactly one shift per day");
	}

//	private Constraint everyShiftMustBeAssigned(ConstraintFactory factory) {
//		return factory.forEachIncludingNullVars(ShiftAssignment.class).filter(sa -> sa.getEmployee() == null)
//				.penalize(HardSoftScore.ONE_HARD).asConstraint("Unassigned shift (hard)");
//	}

	private Constraint oneShiftPerDay(ConstraintFactory factory) {
		return factory.forEach(ShiftAssignment.class).filter(sa -> sa.getEmployee() != null)
				.groupBy(ShiftAssignment::getEmployee, ShiftAssignment::getDate, ConstraintCollectors.count())
				.filter((emp, date, count) -> count > 1)
				.penalize(HardSoftScore.ONE_HARD, (emp, date, count) -> count - 1).asConstraint("One shift per day");

	}

	private Constraint noBackToBack(ConstraintFactory factory) {
		return factory.forEachUniquePair(ShiftAssignment.class, Joiners.equal(ShiftAssignment::getEmployee))
				.filter((sa1, sa2) -> isBackToBack(sa1, sa2)).penalize(HardSoftScore.ONE_HARD)
				.asConstraint("No back-to-back shifts");

	}

	private Constraint genderConstraint(ConstraintFactory factory) {
		return factory.forEach(ShiftAssignment.class).filter((ShiftAssignment sa) -> {
			Employee emp = sa.getEmployee();
			Gender required = sa.getGender();
			return emp != null && required != Gender.ANY && emp.getGender() != required;
		}).penalize(HardSoftScore.ONE_HARD, sa -> 1).asConstraint("Gender mismatch");
	}

	private boolean isBackToBack(ShiftAssignment sa1, ShiftAssignment sa2) {
//		DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("H:mm");

		// Safely parse dates and times
		LocalDate date1 = sa1.getDate();
		LocalDate date2 = sa2.getDate();

		LocalTime start1 = sa1.getStartTime();
		LocalTime end1 =  sa1.getEndTime();
		LocalTime start2 = sa2.getStartTime();
		LocalTime end2 = sa2.getEndTime();

		if (start1 == null || end1 == null || start2 == null || end2 == null) {
			return false; // Invalid time format
		}

		LocalDateTime shift1End = LocalDateTime.of(date1, end1);
		LocalDateTime shift2Start = LocalDateTime.of(date2, start2);

		ShiftType type1 = sa1.getShiftType();
		ShiftType type2 = sa2.getShiftType();

		// 1. Same-day adjacent shift types (ordinal difference = 1)
		if (date1.equals(date2) && Math.abs(type1.ordinal() - type2.ordinal()) == 1) {
			return true;
		}

		// 2. Overnight transition: WAKING_NIGHT → DAY or LONG_DAY
		if (date1.plusDays(1).equals(date2) && type1 == ShiftType.WAKING_NIGHT
				&& (type2 == ShiftType.DAY || type2 == ShiftType.LONG_DAY)) {
			return true;
		}

		// 3. Reverse overnight: DAY or LONG_DAY → WAKING_NIGHT
		if (date2.plusDays(1).equals(date1) && type2 == ShiftType.WAKING_NIGHT
				&& (type1 == ShiftType.DAY || type1 == ShiftType.LONG_DAY)) {
			return true;
		}

		// 4. Overlapping or touching shifts on same day
		if (date1.equals(date2) && !end1.isBefore(start2) && !start1.isAfter(end2)) {
			return true;
		}

		// 5. Short rest period between shifts (<12 hours)
		long restHours = Duration.between(shift1End, shift2Start).toHours();
		if (restHours >= 0 && restHours < 12) {
			return true;
		}

		return false;
	}

	// Helper to safely parse time
//	private LocalTime parseTime(String timeStr, DateTimeFormatter formatter) {
//		try {
//			return LocalTime.parse(timeStr, formatter);
//		} catch (DateTimeParseException e) {
//			return null;
//		}
//	}

	private Constraint allEmployeesHaveAtLeastOneShift(ConstraintFactory factory) {
		return factory.forEach(Employee.class)
				.ifNotExists(ShiftAssignment.class, Joiners.equal(emp -> emp, ShiftAssignment::getEmployee))
				.reward(HardSoftScore.ONE_SOFT, emp -> 1).asConstraint("employee must have one shift");
	}

	private Constraint evenDistribution(ConstraintFactory factory) {
		// 1) Count shifts per employee
		BiConstraintStream<Employee, Integer> countPerEmp = factory.forEach(ShiftAssignment.class)
				.filter(sa -> sa.getEmployee() != null)
				.groupBy(ShiftAssignment::getEmployee, ConstraintCollectors.count());

		// 2) Join that with our single IdealShiftCount fact
		return countPerEmp.join(IdealShiftCount.class)
				// 3) Penalize deviation
				.reward(HardSoftScore.ONE_SOFT,
						(employee, actualCount, idealFact) -> Math
								.abs(actualCount.intValue() - idealFact.getIdealCount()))
				.asConstraint("Even distribution");
	}

	private Constraint preferedWorkingDaysConstraint(ConstraintFactory factory) {
		return factory.forEach(ShiftAssignment.class).filter(sa -> {
			Employee emp = sa.getEmployee();
			if (emp == null || emp.getPreferredDay() == null)
				return false;

			DayOfWeek shiftDay = sa.getDate().getDayOfWeek();
			return emp.getPreferredDay().contains(shiftDay);
		}).reward(HardSoftScore.ONE_SOFT).asConstraint("Prefer working on preferred days");
	}

	private Constraint preferedLocationConstraint(ConstraintFactory factory) {
		return factory.forEach(ShiftAssignment.class).filter(sa -> {
			Employee emp = sa.getEmployee();
			if (emp == null || emp.getPreferredService() == null)
				return false;

			String shiftLocation = sa.getLocation();
			return emp.getPreferredService().contains(shiftLocation);
		}).reward(HardSoftScore.ONE_SOFT).asConstraint("Prefer working at preferred service");
	}

	private Constraint preferedShiftTypeConstraint(ConstraintFactory factory) {
		return factory.forEach(ShiftAssignment.class).filter(sa -> {
			Employee emp = sa.getEmployee();
			if (emp == null || emp.getPreferredShift() == null)
				return false;

			ShiftType shiftType = sa.getShiftType();
			return emp.getPreferredShift().contains(shiftType);
		}).reward(HardSoftScore.ONE_SOFT).asConstraint("Prefer working on preferred shift");
	}

	public Constraint restrictedDayOfWeekConstraint(ConstraintFactory factory) {
		return factory.forEach(ShiftAssignment.class).filter(sa -> {
			Employee emp = sa.getEmployee();
			return emp != null && emp.getRestrictedDay() != null
					&& emp.getRestrictedDay().contains(sa.getDay());
		}).penalize(HardSoftScore.ONE_HARD).asConstraint("Restricted day of week");
	}

	private Constraint restrictedShiftTypeConstraint(ConstraintFactory factory) {
		return factory.forEach(ShiftAssignment.class).filter(sa -> {
			Employee emp = sa.getEmployee();
			return emp != null && emp.getRestrictedShift() != null
					&& emp.getRestrictedShift().contains(sa.getShiftType());
		}).penalize(HardSoftScore.ONE_HARD).asConstraint("Restricted Shift Type");
	}

	private Constraint restrictedServiceConstraint(ConstraintFactory factory) {
		return factory.forEach(ShiftAssignment.class).filter(sa -> {
			Employee emp = sa.getEmployee();
			return emp != null && emp.getRestrictedService() != null
					&& emp.getRestrictedService().contains(sa.getLocation());
		}).penalize(HardSoftScore.ONE_HARD).asConstraint("Restricted Service");
	}

	private Constraint weeklyHoursRangeConstraint(ConstraintFactory factory) {
		return factory.forEachIncludingNullVars(ShiftAssignment.class).filter(shift -> shift.getEmployee() != null)
				.groupBy(ShiftAssignment::getEmployee, shift -> shift.getDate().with(DayOfWeek.MONDAY),
						ConstraintCollectors.sumLong(shift -> {
							LocalTime start = shift.getStartTime();
							LocalTime end =shift.getEndTime()
									;
							return Duration.between(start, end).toMinutes();
						}))
				.filter((employee, weekStart, totalMinutes) -> totalMinutes <  (employee.getMinHrs().longValue() * 60)
						|| totalMinutes > (employee.getMaxHrs().longValue() * 60))
				.penalize(HardSoftLongScore.ONE_HARD, (employee, weekStart, totalMinutes) -> {
					long minMinutes = (employee.getMinHrs().longValue() * 60);
					long maxMinutes = (employee.getMaxHrs().longValue() * 60);
					if (totalMinutes < minMinutes) {
						return Long.valueOf(minMinutes - totalMinutes).intValue();
					} else {
						return Long.valueOf(totalMinutes - maxMinutes).intValue();
					}
				}).asConstraint("Weekly hours outside allowed range");
	}

	private Constraint maxWeeklyHoursConstraint(ConstraintFactory factory) {
		return factory.forEachIncludingNullVars(ShiftAssignment.class).filter(sa -> sa.getEmployee() != null)
				.groupBy(ShiftAssignment::getEmployee, sa -> sa.getDate().with(DayOfWeek.MONDAY),
						ConstraintCollectors.sumLong(sa -> {
							LocalTime start = sa.getStartTime();
							LocalTime end = sa.getEndTime();
							return Duration.between(start, end).toMinutes();
						}))
				.filter((employee, weekStart, totalMinutes) -> totalMinutes > (employee.getMaxHrs().longValue() * 60))
				.penalize(HardSoftScore.ONE_HARD,
						(employee, weekStart, totalMinutes) -> Long
								.valueOf(totalMinutes - (employee.getMaxHrs().longValue() * 60)).intValue())
				.asConstraint("Max weekly hours exceeded");
	}

	private Constraint minWeeklyHoursConstraint(ConstraintFactory factory) {

		return factory.forEachIncludingNullVars(ShiftAssignment.class).filter(sa -> sa.getEmployee() != null)
				.groupBy(ShiftAssignment::getEmployee, sa -> sa.getDate().with(DayOfWeek.MONDAY),
						ConstraintCollectors.sumLong(sa -> {
							LocalTime start = sa.getStartTime();
							LocalTime end = sa.getEndTime();
							return Duration.between(start, end).toMinutes();
						}))
				.filter((employee, weekStart, totalMinutes) -> totalMinutes < (employee.getMinHrs().longValue() * 60))
				.penalize(HardSoftScore.ONE_HARD,
						(employee, weekStart,
								totalMinutes) -> (int) Math.max(0, (employee.getMinHrs().longValue() - totalMinutes * 60)))
				.asConstraint("Min weekly hours not met");
	}

	private Constraint unassignedShiftConstraint(ConstraintFactory factory) {
		return factory.forEach(ShiftAssignment.class).filter(sa -> {
			if (sa.getEmployee() == null) {
				System.out.println("⚠️ Unassigned shift: " + sa.getId() + " on " + sa.getDate() + " ("
						+ sa.getStartTime() + " - " + sa.getEndTime() + ")");
			}
			return sa.getEmployee() == null;
		}).penalize(HardSoftScore.ONE_HARD).asConstraint("Unassigned shift");
	}

}
