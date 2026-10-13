package staffschedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.optaplanner.core.api.score.buildin.hardsoftlong.HardSoftLongScore;

import com.midco.rota.model.Employee;
import com.midco.rota.model.ShiftTemplate;
import com.midco.rota.opt.RotaConstraintConfiguration;
import com.midco.rota.service.HouseHistoryService;

/**
 * Regulars first (V033): a shift at a house People Planner tracks goes to the carers for whom
 * it is one of their own houses before anyone for whom it is a sideline, and they before a
 * stranger. Weight lives in the database; the code only registers the rule.
 */
class RegularsFirstRuleTest {

	private static ShiftTemplate house(String location, boolean tracked) {
		ShiftTemplate t = new ShiftTemplate();
		t.setLocation(location);
		t.setHistoryTracked(tracked);
		return t;
	}

	private static Employee carer(Map<String, Integer> familiarity) {
		Employee e = new Employee();
		e.setHouseFamiliarity(familiarity);
		return e;
	}

	@Test
	void ownHouseCostsNothingSidelineCostsMostStrangerCostsAll() {
		ShiftTemplate riverdale = house("Riverdale ", true);
		Employee bongiwe = carer(Map.of("RIVERDALE", 100));   // 28% of her shifts
		Employee loveness = carer(Map.of("RIVERDALE", 4, "3 HIGHFIELDS", 100)); // 2 days in June
		Employee louise = carer(Map.of("ALEXANDRA FLAT A - KC", 100));

		assertEquals(0, HouseHistoryService.newcomerPenalty(bongiwe, riverdale));
		assertEquals(96, HouseHistoryService.newcomerPenalty(loveness, riverdale));
		assertEquals(100, HouseHistoryService.newcomerPenalty(louise, riverdale));
		assertEquals(100, HouseHistoryService.newcomerPenalty(new Employee(), riverdale), "no history = new everywhere tracked");
	}

	@Test
	void untrackedHousesAreNeverJudged() {
		// e.g. 57 STURGESS / 13 EDINBURG, not mapped to People Planner; or before the table is loaded
		assertEquals(0, HouseHistoryService.newcomerPenalty(new Employee(), house("57 STURGESS", false)));
		assertTrue(HouseHistoryService.backsCarryOver(new Employee(), house("57 STURGESS", false)));
	}

	@Test
	void carryOverNeedsTheHouseToBeARealPartOfTheirWork() {
		ShiftTemplate riverdale = house("RIVERDALE", true);
		assertTrue(HouseHistoryService.backsCarryOver(carer(Map.of("RIVERDALE", 50)), riverdale));
		assertFalse(HouseHistoryService.backsCarryOver(carer(Map.of("RIVERDALE", 49)), riverdale));
	}

	@Test
	void weightAndThresholdsComeFromV033() throws Exception {
		assertEquals(HardSoftLongScore.ZERO, new RotaConstraintConfiguration().getCarerNewToHouse(),
				"weight comes only from constraint_setting");
		try (var in = RegularsFirstRuleTest.class.getResourceAsStream("/db/migration/V033__carer_house_history.sql")) {
			String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			assertTrue(sql.contains("'Carer new to this house', 5000, 'SOFT', true"));
			assertTrue(sql.contains("('houseHistoryMinShifts', 4"));
			assertTrue(sql.contains("('houseHistoryFullSharePct', 20"));
		}
	}
}
