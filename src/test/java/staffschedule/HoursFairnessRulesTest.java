package staffschedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.optaplanner.core.api.score.buildin.hardsoftlong.HardSoftLongScore;

import com.midco.rota.model.Employee;
import com.midco.rota.opt.RotaConstraintConfiguration;
import com.midco.rota.opt.RotaConstraintProvider;
import com.midco.rota.opt.SolveProfile;
import com.midco.rota.opt.SolverTuning;
import com.midco.rota.util.ContractType;

/**
 * Weekly hours fairness (V030): permanents get their contracted minimum, don't drop below
 * 90% of their usual (People Planner) hours, don't grow past their usual hours while
 * zero-hours staff are idle, and zero-hours staff share the rest evenly. Weights and the
 * floor % live in the database; the code only registers the rules with ZERO weights.
 */
class HoursFairnessRulesTest {

	private static final LocalDate MON = LocalDate.of(2026, 10, 12);
	private static final String[] RULES = { "Permanent idle week", "Permanent below weekly minimum",
			"Permanent below usual earnings", "Permanent above usual hours", "Zero-hours fair share" };
	private static String v030;

	@BeforeAll
	static void readMigration() throws Exception {
		try (var in = HoursFairnessRulesTest.class.getResourceAsStream("/db/migration/V030__weekly_hours_fairness.sql")) {
			v030 = new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private static Map<String, Long> v030Weights() {
		Map<String, Long> w = new HashMap<>();
		Matcher m = Pattern.compile("\\('([^']+)',\\s*(\\d+),\\s*'SOFT',\\s*true").matcher(v030);
		while (m.find()) {
			w.put(m.group(1), Long.parseLong(m.group(2)));
		}
		return w;
	}

	private static Employee permanent(double minHrs, Double baseline) {
		Employee e = new Employee();
		e.setActive(true);
		e.setContractType(ContractType.PERMANENT);
		e.setMinHrs(BigDecimal.valueOf(minHrs));
		e.setBaselineWeeklyHours(baseline == null ? null : BigDecimal.valueOf(baseline));
		return e;
	}

	@Test
	void weeklyMinimumIsTheContractPRoRatedForLeave() {
		Employee e = permanent(37.5, null);
		assertEquals(2250, RotaConstraintProvider.weeklyMinimumMinutes(e, MON));
		assertEquals(2250, RotaConstraintProvider.idleWeekMinutes(e, MON));
		e.setUnavailableDates(Set.of(MON, MON.plusDays(1)));
		assertEquals(Math.round(2250 * 5 / 7.0), RotaConstraintProvider.weeklyMinimumMinutes(e, MON));
	}

	@Test
	void permanentRulesSkipZeroHoursSupervisorsInactiveAndFullyBlockedCarers() {
		Employee zh = permanent(0, 40.0);
		zh.setContractType(ContractType.ZERO_HOURS);
		Employee lead = permanent(37.5, 40.0);
		lead.setSupervisor(true);
		Employee left = permanent(37.5, 40.0);
		left.setActive(false);
		Employee blocked = permanent(37.5, 40.0);
		blocked.setRestrictedDays(List.copyOf(EnumSet.allOf(DayOfWeek.class)));
		for (Employee e : List.of(zh, lead, left, blocked)) {
			assertEquals(0, RotaConstraintProvider.weeklyMinimumMinutes(e, MON));
			assertEquals(0, RotaConstraintProvider.earningsFloorMinutes(e, MON, 90));
			assertEquals(0, RotaConstraintProvider.usualCapMinutes(e));
		}
		Employee weekdaysOnly = permanent(37.5, null);
		weekdaysOnly.setRestrictedDays(List.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY));
		assertEquals(2250, RotaConstraintProvider.weeklyMinimumMinutes(weekdaysOnly, MON), "partly restricted still counts");
	}

	@Test
	void earningsFloorIsAPercentOfUsualHours() {
		assertEquals(3240, RotaConstraintProvider.earningsFloorMinutes(permanent(37.5, 60.0), MON, 90), "90% of 60h");
		assertEquals(0, RotaConstraintProvider.earningsFloorMinutes(permanent(37.5, null), MON, 90), "no history");
		assertEquals(0, RotaConstraintProvider.earningsFloorMinutes(permanent(37.5, 60.0), MON, 0), "0% = off");
	}

	@Test
	void usualHoursCapIsTheBaselineButNeverBelowTheMinimum() {
		assertEquals(3600, RotaConstraintProvider.usualCapMinutes(permanent(37.5, 60.0)));
		assertEquals(2250, RotaConstraintProvider.usualCapMinutes(permanent(37.5, 23.0)), "low usual -> minimum");
		assertEquals(2250, RotaConstraintProvider.usualCapMinutes(permanent(37.5, null)), "no history -> minimum");
	}

	@Test
	void zeroHoursCostIsConvexSoWorkSpreads() {
		long even = 2L * RotaConstraintProvider.zeroHoursShareCost(30 * 60);
		long lopsided = RotaConstraintProvider.zeroHoursShareCost(60 * 60) + RotaConstraintProvider.zeroHoursShareCost(0);
		assertTrue(even < lopsided, "two on 30h must cost less than one on 60h and one idle");
	}

	@Test
	void v030SetsEveryWeightAndTheFloorAndDisablesTheBrokenBalanceRules() {
		Map<String, Long> w = v030Weights();
		for (String rule : RULES) {
			assertTrue(w.containsKey(rule), rule + " missing from V030");
		}
		assertTrue(v030.contains("'permanentEarningsFloorPct', 90"));
		for (String old : List.of("Min weekly hours not met", "Penalize overloading individual employees",
				"Encourage balanced hours around midpoint")) {
			assertTrue(v030.contains("'" + old + "'"), old + " should be disabled by V030");
		}
	}

	@Test
	void v030WeightsHaveTheIntendedOrder() {
		Map<String, Long> w = v030Weights();
		long unassignedSlot = 1_000_000L;
		// A permanent left idle on a 37.5h minimum must cost more than an empty slot, so the minimum wins.
		assertTrue(2250 * w.get("Permanent idle week") > unassignedSlot);
		// Going past usual hours by one 12h shift must cost less than leaving that shift empty.
		assertTrue(720 * w.get("Permanent above usual hours") < unassignedSlot);
		// A zero-hours carer on 60h taking another 12h shift must also cost less than an empty slot.
		long zhExtra = w.get("Zero-hours fair share")
				* (RotaConstraintProvider.zeroHoursShareCost(72 * 60) - RotaConstraintProvider.zeroHoursShareCost(60 * 60));
		assertTrue(zhExtra < unassignedSlot);
		// The contract minimum outranks the earnings floor, which outranks the usual-hours cap.
		assertTrue(w.get("Permanent below weekly minimum") > w.get("Permanent below usual earnings"));
		assertTrue(w.get("Permanent below usual earnings") > w.get("Permanent above usual hours"));
	}

	@Test
	void noJavaTuning() {
		RotaConstraintConfiguration c = new RotaConstraintConfiguration();
		for (HardSoftLongScore s : List.of(c.getPermanentIdleWeek(), c.getPermanentBelowWeeklyMinimum(),
				c.getPermanentBelowUsualEarnings(), c.getPermanentAboveUsualHours(), c.getZeroHoursFairShare())) {
			assertEquals(HardSoftLongScore.ZERO, s, "weights come only from constraint_setting");
		}
		assertEquals(0, new SolverTuning().getPermanentEarningsFloorPct(), "floor % comes only from solver_tuning");
		for (SolveProfile p : SolveProfile.values()) {
			for (String rule : RULES) {
				assertFalse(p.weightOverrides().containsKey(rule), p + " must not tune " + rule + " in code");
			}
		}
	}
}
