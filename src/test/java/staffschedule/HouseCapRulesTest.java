package staffschedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.midco.rota.SolverTrigger;
import com.midco.rota.model.Employee;
import com.midco.rota.model.Shift;
import com.midco.rota.model.ShiftAssignment;
import com.midco.rota.model.ShiftTemplate;
import com.midco.rota.model.WorkShiftAssignment;
import com.midco.rota.opt.RotaConstraintProvider;
import com.midco.rota.util.ShiftType;

/**
 * V037 (Barnet rota 477 review): at most 4 houses per carer per period, each flat its own
 * house, lead shifts exempt; pins and carry-overs past the cap are left for the solver.
 */
class HouseCapRulesTest {

	private static final LocalDate MON = LocalDate.of(2026, 11, 9);

	private static WorkShiftAssignment slot(String location, ShiftType type, int dayOffset) {
		return slot(location, type.name(), dayOffset);
	}

	private static WorkShiftAssignment slot(String location, String typeCode, int dayOffset) {
		ShiftTemplate t = new ShiftTemplate();
		t.setLocation(location);
		t.setShiftTypeCode(typeCode);
		t.setDayOfWeek(DayOfWeek.MONDAY);
		Shift s = new Shift();
		s.setShiftTemplate(t);
		s.setShiftStart(MON.plusDays(dayOffset));
		return new WorkShiftAssignment(s);
	}

	@Test
	void eachFlatIsItsOwnHouseButStraySpacesAndCaseDont() {
		assertEquals(RotaConstraintProvider.locationKey(slot("Alexandra Flat B - LC ", ShiftType.WAKING_NIGHT, 0)),
				RotaConstraintProvider.locationKey(slot("Alexandra Flat B - LC", ShiftType.DAY, 1)));
		assertEquals(RotaConstraintProvider.locationKey(slot("20 glebe", ShiftType.DAY, 0)),
				RotaConstraintProvider.locationKey(slot("20 GLEBE", ShiftType.DAY, 1)));
		assertFalse(RotaConstraintProvider.locationKey(slot("Alexandra Flat A - KC", ShiftType.DAY, 0))
				.equals(RotaConstraintProvider.locationKey(slot("Alexandra Flat B - LC", ShiftType.DAY, 0))));
	}

	@Test
	void leadShiftsAndSleepInsDontCount() {
		assertTrue(RotaConstraintProvider.countsTowardLocationRules(slot("9 CLAYDON", ShiftType.DAY, 0)));
		assertFalse(RotaConstraintProvider.countsTowardLocationRules(slot("9 CLAYDON", "SHIFT_LEAD", 0)));
		assertFalse(RotaConstraintProvider.countsTowardLocationRules(slot("9 CLAYDON", ShiftType.SLEEP_IN, 0)));
	}

	@Test
	void aFifthNewHouseIsRefusedButAnotherShiftAtAKnownHouseIsFine() {
		List<ShiftAssignment> held = List.of(slot("29 EDGEWORTH", ShiftType.DAY, 0), slot("61 VICTORIA", ShiftType.DAY, 1),
				slot("8 BARFORD", ShiftType.DAY, 2), slot("9 CLAYDON ", ShiftType.DAY, 3));
		assertTrue(SolverTrigger.wouldExceedLocationCap(held, slot("Alexandra Flat E - RF", ShiftType.DAY, 4), 4));
		assertFalse(SolverTrigger.wouldExceedLocationCap(held, slot("9 CLAYDON", ShiftType.DAY, 4), 4), "same house, stray space");
		assertFalse(SolverTrigger.wouldExceedLocationCap(held, slot("Alexandra Flat E - RF", "SHIFT_LEAD", 4), 4),
				"lead shifts exempt");
		assertFalse(SolverTrigger.wouldExceedLocationCap(held, slot("Alexandra Flat E - RF", ShiftType.DAY, 4), 0),
				"cap 0 = rule off");
	}

	@Test
	void pinsAreKeptAtTheCarersHighestWeightServicesNotTheFirstByDate() {
		Employee e = new Employee();
		e.setPreferredService(List.of("Eddington:60", "LAVENDER:50", "BISHOPS:40", "CHELWOOD:20", "LOWER BARN:10"));
		java.util.Map<String, Integer> pins = java.util.Map.of("EDDINGTON", 5, "LAVENDER", 4, "BISHOPS", 4, "CHELWOOD", 2, "LOWER BARN", 1);
		assertEquals(java.util.Set.of("EDDINGTON", "LAVENDER", "BISHOPS", "CHELWOOD"), SolverTrigger.highestWeightHouses(e, pins, 4));
	}

	@Test
	void equalWeightsFallBackToMostPins() {
		Employee e = new Employee();
		e.setPreferredService(List.of("A:50", "B:50", "C:50"));
		assertEquals(java.util.Set.of("A", "C"), SolverTrigger.highestWeightHouses(e, java.util.Map.of("A", 6, "B", 1, "C", 3), 2));
	}

	@Test
	void servicesDecideWhereACarerWorksAndWeightsSetThePriority() {
		Employee e = new Employee();
		e.setPreferredService(List.of("9 CLAYDON:100", "RIVERDALE:40", "GOLDERTON:0"));
		ShiftTemplate claydon = slot("9 CLAYDON ", ShiftType.DAY, 0).getShift().getShiftTemplate();
		ShiftTemplate riverdale = slot("RIVERDALE", ShiftType.DAY, 0).getShift().getShiftTemplate();
		ShiftTemplate golderton = slot("GOLDERTON", ShiftType.DAY, 0).getShift().getShiftTemplate();
		ShiftTemplate glebe = slot("20 GLEBE", ShiftType.DAY, 0).getShift().getShiftTemplate();
		assertFalse(RotaConstraintProvider.isOutsideServices(e, claydon));
		assertFalse(RotaConstraintProvider.isOutsideServices(e, riverdale));
		assertTrue(RotaConstraintProvider.isOutsideServices(e, golderton), "weight 0 = not a service");
		assertTrue(RotaConstraintProvider.isOutsideServices(e, glebe));
		assertEquals(0, RotaConstraintProvider.serviceWeightGap(e, claydon));
		assertEquals(60, RotaConstraintProvider.serviceWeightGap(e, riverdale));

		Employee noList = new Employee();
		assertFalse(RotaConstraintProvider.isOutsideServices(noList, glebe), "no services listed = anywhere in region");
		assertEquals(0, RotaConstraintProvider.serviceWeightGap(noList, glebe));

		glebe.setRequiredSkills(List.of("SHIFT_LEAD"));
		assertFalse(RotaConstraintProvider.isOutsideServices(e, glebe), "lead shifts exempt");
	}

	@Test
	void v038MakesServicesHardAndSwitchesOffTheHistoryRules() throws Exception {
		try (var in = HouseCapRulesTest.class.getResourceAsStream("/db/migration/V038__services_drive_placement.sql")) {
			String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			assertTrue(sql.contains("('Not one of the carer''s services', 1000, 'HARD', true"));
			assertTrue(sql.contains("('Lower-weight service', 5000, 'SOFT', true"));
			assertTrue(sql.contains("WHERE constraint_name = 'Carer new to this house'"));
			assertTrue(sql.contains("WHERE constraint_name = 'Carer never worked at this house'"));
		}
	}

	@Test
	void preferredServiceLookupIgnoresStraySpaces() {
		Employee e = new Employee();
		e.setPreferredService(List.of("Alexandra Flat B - LC:100", "9 CLAYDON:40"));
		assertEquals(100, e.getServiceWeightage("Alexandra Flat B - LC "));
		assertTrue(e.isPrimaryAtLocation("Alexandra Flat B - LC "));
		assertFalse(e.isPrimaryAtLocation("9 CLAYDON"));
	}

	@Test
	void v037SetsTheAgreedRules() throws Exception {
		try (var in = HouseCapRulesTest.class.getResourceAsStream("/db/migration/V037__barnet_477_rule_changes.sql")) {
			String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			assertTrue(sql.contains("('Max locations per period', 1000, 'HARD', true"));
			assertTrue(sql.contains("('Penalize daily location switches', 100000, 'SOFT', true"));
			assertTrue(sql.contains("SET int_value = 4"));
		}
	}
}
