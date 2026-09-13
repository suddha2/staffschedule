package com.midco.rota.opt;

/**
 * Numeric thresholds consumed by {@link RotaConstraintProvider}.
 *
 * <p>Constraint streams are built once when the score director factory is
 * created, so a threshold used inside a stream lambda cannot be read from the
 * planning solution — it has to be available at stream-build time. This class
 * therefore follows the same pattern the codebase already uses for
 * {@code ShiftTypeLimitConfig} and {@code Employee.setPeriodService(..)}: a
 * process-wide holder that is populated from the {@code solver_tuning} table
 * before a solve starts.
 *
 * <p>The instance is immutable and swapped wholesale via {@link #apply}, so a
 * running solve always sees a consistent set of values. Changing tuning while a
 * solve is in flight is not supported — the affected solve should be restarted.
 *
 * <p>Defaults below are calibrated against 209,514 real shifts from the
 * People Planner history (2025 onward). The percentage in each comment is the
 * share of observed reality that satisfies the value.
 */
public final class SolverTuning {

	private static volatile SolverTuning current = new SolverTuning();

	/** Soft target: capped shifts allowed in odd weeks of the pay period. Covers 62% of observed weeks. */
	private int weeklyCapOddWeek = 6;

	/** Soft target: capped shifts allowed in even weeks of the pay period. */
	private int weeklyCapEvenWeek = 5;

	/** Soft target: minimum capped shifts in a week the employee is allocated at all. */
	private int weeklyMinPerAllocatedWeek = 5;

	/** Hard ceiling on non-FLOATING shifts at one location in one week. 10 covers 94% of reality (was 4 = 66%). */
	private int maxNonFloatingShiftsPerLocationPerWeek = 10;

	/** Soft ceiling on distinct working days at one location in one week. */
	private int maxDaysPerLocationPerWeek = 5;

	/** Hard ceiling on distinct locations per employee per period. 6 covers 95% of reality (was 3 = 68%). */
	private int maxLocationsPerPeriod = 6;

	/** Hard ceiling on monthly hours, excluding exempt shift types. */
	private int monthlyHoursCap = 270;

	/** Locations per week an employee may work before the soft switching penalty starts. */
	private int freeLocationsPerWeek = 2;

	/** The tuning currently in force. Never null. */
	public static SolverTuning current() {
		return current;
	}

	/** Replaces the tuning in force. Pass a fully populated instance. */
	public static void apply(SolverTuning tuning) {
		if (tuning != null) {
			current = tuning;
		}
	}

	/** Upper bound for a given week-of-period (1..4): odd weeks get the odd cap, even weeks the even cap. */
	public int capForWeekOfPeriod(int weekOfPeriod) {
		return (weekOfPeriod % 2 == 1) ? weeklyCapOddWeek : weeklyCapEvenWeek;
	}

	public int getWeeklyCapOddWeek() {
		return weeklyCapOddWeek;
	}

	public void setWeeklyCapOddWeek(int weeklyCapOddWeek) {
		this.weeklyCapOddWeek = weeklyCapOddWeek;
	}

	public int getWeeklyCapEvenWeek() {
		return weeklyCapEvenWeek;
	}

	public void setWeeklyCapEvenWeek(int weeklyCapEvenWeek) {
		this.weeklyCapEvenWeek = weeklyCapEvenWeek;
	}

	public int getWeeklyMinPerAllocatedWeek() {
		return weeklyMinPerAllocatedWeek;
	}

	public void setWeeklyMinPerAllocatedWeek(int weeklyMinPerAllocatedWeek) {
		this.weeklyMinPerAllocatedWeek = weeklyMinPerAllocatedWeek;
	}

	public int getMaxNonFloatingShiftsPerLocationPerWeek() {
		return maxNonFloatingShiftsPerLocationPerWeek;
	}

	public void setMaxNonFloatingShiftsPerLocationPerWeek(int v) {
		this.maxNonFloatingShiftsPerLocationPerWeek = v;
	}

	public int getMaxDaysPerLocationPerWeek() {
		return maxDaysPerLocationPerWeek;
	}

	public void setMaxDaysPerLocationPerWeek(int maxDaysPerLocationPerWeek) {
		this.maxDaysPerLocationPerWeek = maxDaysPerLocationPerWeek;
	}

	public int getMaxLocationsPerPeriod() {
		return maxLocationsPerPeriod;
	}

	public void setMaxLocationsPerPeriod(int maxLocationsPerPeriod) {
		this.maxLocationsPerPeriod = maxLocationsPerPeriod;
	}

	public int getMonthlyHoursCap() {
		return monthlyHoursCap;
	}

	public void setMonthlyHoursCap(int monthlyHoursCap) {
		this.monthlyHoursCap = monthlyHoursCap;
	}

	public int getFreeLocationsPerWeek() {
		return freeLocationsPerWeek;
	}

	public void setFreeLocationsPerWeek(int freeLocationsPerWeek) {
		this.freeLocationsPerWeek = freeLocationsPerWeek;
	}
}
