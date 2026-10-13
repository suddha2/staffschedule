package staffschedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.optaplanner.core.api.score.buildin.hardsoftlong.HardSoftLongScore;

import com.midco.rota.opt.RotaConstraintConfiguration;
import com.midco.rota.opt.RotaConstraintProvider;

/** Compliance 6:1 - no permanent carer works 7 days in a row, counted across week boundaries. */
class SixOneRuleTest {

	private static final LocalDate MON = LocalDate.of(2026, 10, 12);

	private static SortedSet<LocalDate> run(int fromOffset, int days) {
		SortedSet<LocalDate> s = new TreeSet<>();
		for (int i = 0; i < days; i++) {
			s.add(MON.plusDays(fromOffset + i));
		}
		return s;
	}

	@Test
	void sixInARowIsFineSevenIsOneBreachEightIsTwo() {
		assertEquals(0, RotaConstraintProvider.sevenDayRuns(run(0, 6)));
		assertEquals(1, RotaConstraintProvider.sevenDayRuns(run(0, 7)));
		assertEquals(2, RotaConstraintProvider.sevenDayRuns(run(0, 8)));
	}

	@Test
	void aDayOffResetsTheCount() {
		SortedSet<LocalDate> d = run(0, 6);
		d.addAll(run(7, 6)); // day 6 off, then 6 more
		assertEquals(0, RotaConstraintProvider.sevenDayRuns(d));
	}

	@Test
	void runsAcrossAWeekBoundaryAreCaught() {
		// Off on Monday of week 1, then Tuesday week 1 to Sunday week 2 = 13 days in a row.
		assertEquals(7, RotaConstraintProvider.sevenDayRuns(run(1, 13)));
	}

	@Test
	void weightComesFromV031AsHard() throws Exception {
		assertEquals(HardSoftLongScore.ZERO, new RotaConstraintConfiguration().getMaxConsecutiveWorkingDays(),
				"weight comes only from constraint_setting");
		try (var in = SixOneRuleTest.class.getResourceAsStream("/db/migration/V031__six_one_working_pattern.sql")) {
			String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			assertTrue(sql.contains("'Max 6 consecutive working days', 1000, 'HARD', true"));
		}
	}

	@Test
	void appliesToPermanentCarersOnly() {
		com.midco.rota.model.Employee perm = new com.midco.rota.model.Employee();
		perm.setContractType(com.midco.rota.util.ContractType.PERMANENT);
		com.midco.rota.model.Employee zh = new com.midco.rota.model.Employee();
		zh.setContractType(com.midco.rota.util.ContractType.ZERO_HOURS);
		assertTrue(RotaConstraintProvider.sixOneApplies(perm));
		assertTrue(!RotaConstraintProvider.sixOneApplies(zh), "zero-hours staff are not covered by 6:1");
	}
}
