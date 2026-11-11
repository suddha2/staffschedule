package staffschedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.optaplanner.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import org.optaplanner.core.api.solver.SolutionManager;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.config.solver.SolverConfig;

import com.midco.rota.model.Employee;
import com.midco.rota.model.Rota;
import com.midco.rota.model.Shift;
import com.midco.rota.model.ShiftAssignment;
import com.midco.rota.model.ShiftAssignmentFactory;
import com.midco.rota.model.ShiftTemplate;
import com.midco.rota.model.WorkShiftAssignment;
import com.midco.rota.opt.RotaConstraintConfiguration;
import com.midco.rota.opt.RotaConstraintProvider;
import com.midco.rota.opt.SolverTuning;
import com.midco.rota.util.ShiftType;

/**
 * Minimum 11 hours' rest (V043), on Barnet's own examples: 9 Claydon days back to back, a Claydon
 * day into a 07:00 long day, a sleep-in finishing 07:00 then an 08:00 day elsewhere, and the
 * live-in pattern (long day, sleep-in, long day at one house) which must stay allowed.
 */
class MinimumRestRuleTest {

	private static final LocalDate MON = LocalDate.of(2026, 11, 9);
	private static final long ELEVEN_HOURS = 11 * 60;
	private SolverTuning before;

	@BeforeEach
	void elevenHours() {
		before = SolverTuning.current();
		SolverTuning t = new SolverTuning();
		t.setMinRestHours(11);
		SolverTuning.apply(t);
	}

	@AfterEach
	void restore() {
		SolverTuning.apply(before);
	}

	private static ShiftTemplate tpl(String house, ShiftType type, int fromH, int toH) {
		ShiftTemplate t = new ShiftTemplate();
		t.setRegion("BARNET");
		t.setLocation(house);
		t.setShiftType(type);
		t.setShiftTypeCode(type.name());
		t.setStartTime(LocalTime.of(fromH, 0));
		t.setEndTime(LocalTime.of(toH, 0));
		t.setEmpCount(1);
		return t;
	}

	/** Shifts for one carer: a long day at a house gets its paired sleep-in when {@code sleepIn}. */
	private static List<ShiftAssignment> shifts(Employee carer, Object[]... specs) {
		List<ShiftAssignment> all = new ArrayList<>();
		for (Object[] s : specs) {
			LocalDate d = MON.plusDays((Integer) s[0]);
			ShiftTemplate t = tpl((String) s[1], (ShiftType) s[2], (Integer) s[3], (Integer) s[4]);
			all.add(ShiftAssignmentFactory.create(new Shift(d, t, 1)));
		}
		ShiftAssignmentFactory.linkFollowerPairs(all);
		all.forEach(sa -> sa.setEmployee(carer));
		return all;
	}

	private static Object[] at(int day, String house, ShiftType type, int fromH, int toH) {
		return new Object[] { day, house, type, fromH, toH };
	}

	private static Employee carer() {
		Employee e = new Employee();
		e.setId(1);
		e.setFirstName("Test");
		e.setLastName("Carer");
		return e;
	}

	private static long breaches(List<ShiftAssignment> assignments, Employee carer) {
		RotaConstraintConfiguration config = new RotaConstraintConfiguration();
		config.setMinRestBetweenShifts(HardSoftLongScore.ofHard(1000));
		Rota rota = new Rota(new ArrayList<>(List.of(carer)), assignments);
		rota.setConstraintConfiguration(config);
		SolutionManager<Rota, HardSoftLongScore> manager = SolutionManager.create(
				SolverFactory.create(SolverConfig.createFromXmlResource("solverConfig.xml")));
		return manager.explain(rota).getConstraintMatchTotalMap().values().stream()
				.filter(t -> t.getConstraintName().equals("Minimum rest between shifts"))
				.mapToLong(t -> t.getConstraintMatchCount()).sum();
	}

	@Test
	void claydonDaysBackToBackGiveTenHoursAndBreak() {
		Employee c = carer();
		List<ShiftAssignment> s = shifts(c, at(0, "9 CLAYDON", ShiftType.DAY, 8, 22), at(1, "9 CLAYDON", ShiftType.DAY, 8, 22));
		assertTrue(RotaConstraintProvider.shortRestPair(s.get(0), s.get(1), ELEVEN_HOURS));
		assertEquals(1, breaches(s, c));
	}

	@Test
	void claydonDayIntoA7amLongDayElsewhereBreaks() {
		Employee c = carer();
		List<ShiftAssignment> s = shifts(c, at(0, "9 CLAYDON", ShiftType.DAY, 8, 22), at(1, "RIVERDALE", ShiftType.LONG_DAY, 7, 22));
		assertEquals(1, breaches(s, c));
	}

	@Test
	void sleepInFinishingAt7ThenAnotherHouseAt8Breaks() {
		Employee c = carer();
		List<ShiftAssignment> s = shifts(c, at(0, "4 AUDLEY", ShiftType.LONG_DAY, 7, 22), at(0, "4 AUDLEY", ShiftType.SLEEP_IN, 22, 7),
				at(1, "8 BARFORD", ShiftType.DAY, 8, 20));
		assertEquals(1, breaches(s, c));
		// pre-solve check: the Barford day next to the Audley long day + sleep-in
		assertTrue(RotaConstraintProvider.shortRestNextTo(List.of(s.get(0), s.get(1)), s.get(2), ELEVEN_HOURS));
	}

	@Test
	void liveInLongDaySleepInLongDayAtOneHouseIsAllowed() {
		Employee c = carer();
		List<ShiftAssignment> s = shifts(c, at(0, "20 GLEBE", ShiftType.LONG_DAY, 7, 22), at(0, "20 GLEBE", ShiftType.SLEEP_IN, 22, 7),
				at(1, "20 GLEBE", ShiftType.LONG_DAY, 7, 22), at(1, "20 GLEBE", ShiftType.SLEEP_IN, 22, 7),
				at(2, "20 GLEBE", ShiftType.LONG_DAY, 7, 22));
		assertTrue(((WorkShiftAssignment) s.get(0)).getPairedFollower() != null, "sleep-in paired to its long day");
		assertEquals(0, breaches(s, c));
		assertFalse(RotaConstraintProvider.shortRestNextTo(List.of(s.get(0), s.get(1)), s.get(2), ELEVEN_HOURS));
	}

	@Test
	void twelveHoursBetweenDaysIsFineAndTheRuleIsOffAtZero() {
		Employee c = carer();
		List<ShiftAssignment> s = shifts(c, at(0, "61 VICTORIA", ShiftType.DAY, 8, 20), at(1, "61 VICTORIA", ShiftType.DAY, 8, 20));
		assertEquals(0, breaches(s, c));
		List<ShiftAssignment> claydon = shifts(c, at(0, "9 CLAYDON", ShiftType.DAY, 8, 22), at(1, "9 CLAYDON", ShiftType.DAY, 8, 22));
		assertFalse(RotaConstraintProvider.shortRestPair(claydon.get(0), claydon.get(1), 0));
	}

	@Test
	void weightComesFromV043AsHard() throws Exception {
		try (var in = MinimumRestRuleTest.class.getResourceAsStream("/db/migration/V043__minimum_rest_11_hours.sql")) {
			String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			assertTrue(sql.contains("('Minimum rest between shifts', 1000, 'HARD', true"));
			assertTrue(sql.contains("SET int_value = 11"));
		}
	}
}
