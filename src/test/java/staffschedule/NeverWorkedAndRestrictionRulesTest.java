package staffschedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.optaplanner.core.api.score.buildin.hardsoftlong.HardSoftLongScore;

import com.midco.rota.model.Employee;
import com.midco.rota.model.FollowerShiftAssignment;
import com.midco.rota.model.Shift;
import com.midco.rota.model.ShiftTemplate;
import com.midco.rota.model.WorkShiftAssignment;
import com.midco.rota.opt.RotaConstraintConfiguration;
import com.midco.rota.service.ContinuitySeedService;
import com.midco.rota.service.HouseHistoryService;
import com.midco.rota.util.ShiftType;

/**
 * V034 (rota feedback 2026-10): nobody works solo at a house they've never worked, and a
 * carer's recorded restrictions are hard - for the solver and for pins / carry-overs.
 */
class NeverWorkedAndRestrictionRulesTest {

	private static final LocalDate FRI = LocalDate.of(2026, 10, 30);

	private static ShiftTemplate template(String location, ShiftType type, boolean tracked) {
		ShiftTemplate t = new ShiftTemplate();
		t.setLocation(location);
		t.setShiftType(type);
		t.setDayOfWeek(DayOfWeek.FRIDAY);
		t.setHistoryTracked(tracked);
		return t;
	}

	private static WorkShiftAssignment slot(ShiftTemplate t) {
		Shift s = new Shift();
		s.setShiftTemplate(t);
		s.setShiftStart(FRI);
		return new WorkShiftAssignment(s);
	}

	private static Employee carer(String... worked) {
		Employee e = new Employee();
		e.setWorkedHouses(Set.of(worked));
		return e;
	}

	@Test
	void anyPeoplePlannerShiftThereCountsAsHavingWorkedIt() {
		ShiftTemplate audley = template("FLAT 2 AUDLEY ", ShiftType.LONG_DAY, true);
		assertTrue(HouseHistoryService.neverWorkedAt(carer("ALEXANDRA FLAT A - KC"), audley), "Kofi: never at Audley");
		assertFalse(HouseHistoryService.neverWorkedAt(carer("FLAT 2 AUDLEY"), audley));
		assertTrue(HouseHistoryService.neverWorkedAt(new Employee(), audley), "no history at all");
	}

	@Test
	void untrackedHousesAndLeadShiftsAreExempt() {
		assertFalse(HouseHistoryService.neverWorkedAt(new Employee(), template("57 STURGESS", ShiftType.DAY, false)));
		ShiftTemplate lead = template("20 GLEBE", ShiftType.DAY, true); // a lead shift is one requiring SHIFT_LEAD
		lead.setRequiredSkills(List.of("SHIFT_LEAD"));
		assertFalse(HouseHistoryService.neverWorkedAt(new Employee(), lead));
	}

	@Test
	void pinsAndCarryOversGiveTheReason() {
		WorkShiftAssignment audley = slot(template("FLAT 2 AUDLEY", ShiftType.LONG_DAY, true));
		assertEquals("Carer has never worked at this house (needs a shadow shift first)",
				ContinuitySeedService.hardEligibilityReason(carer("4 AUDLEY"), audley));
		assertNull(ContinuitySeedService.hardEligibilityReason(carer("FLAT 2 AUDLEY"), audley));
	}

	@Test
	void restrictedDayShiftTypeAndServiceBlock() {
		WorkShiftAssignment glebe = slot(template("20 GLEBE", ShiftType.LONG_DAY, false));

		Employee allDays = new Employee();
		allDays.setRestrictedDays(List.of(DayOfWeek.values()));
		assertEquals("Carer is restricted from this day", ContinuitySeedService.hardEligibilityReason(allDays, glebe));

		Employee noLongDays = new Employee();
		noLongDays.setRestrictedShifts(List.of(ShiftType.LONG_DAY));
		assertTrue(ContinuitySeedService.breaksHardEligibility(noLongDays, glebe));

		Employee notGlebe = new Employee();
		notGlebe.setRestrictedService(List.of("20 GLEBE"));
		assertEquals("Carer is restricted from this service", ContinuitySeedService.hardEligibilityReason(notGlebe, glebe));
	}

	@Test
	void aLongDaysPairedSleepInIsCheckedToo() {
		WorkShiftAssignment longDay = slot(template("20 GLEBE", ShiftType.LONG_DAY, false));
		Shift sleep = new Shift();
		sleep.setShiftTemplate(template("20 GLEBE", ShiftType.SLEEP_IN, false));
		sleep.setShiftStart(FRI);
		longDay.setPairedFollower(new FollowerShiftAssignment(sleep));

		Employee noSleepIns = new Employee();
		noSleepIns.setRestrictedShifts(List.of(ShiftType.SLEEP_IN));
		assertEquals("Carer is restricted from this shift type", ContinuitySeedService.hardEligibilityReason(noSleepIns, longDay));
	}

	private static WorkShiftAssignment at(LocalDate date, ShiftType type, String start, String end) {
		ShiftTemplate t = template("29 EDGEWORTH", type, false);
		t.setStartTime(java.time.LocalTime.parse(start));
		t.setEndTime(java.time.LocalTime.parse(end));
		Shift s = new Shift();
		s.setShiftTemplate(t);
		s.setShiftStart(date);
		return new WorkShiftAssignment(s);
	}

	@Test
	void aDayOrLongDayCanOnlyBeFollowedByItsSleepIn() {
		LocalDate tue = LocalDate.of(2026, 10, 13);
		WorkShiftAssignment day = at(tue, ShiftType.DAY, "08:00", "18:00");
		WorkShiftAssignment longDay = at(tue, ShiftType.LONG_DAY, "08:00", "20:00");
		WorkShiftAssignment night = at(tue, ShiftType.WAKING_NIGHT, "20:00", "08:00");
		WorkShiftAssignment sleepIn = at(tue, ShiftType.SLEEP_IN, "20:00", "08:00");
		WorkShiftAssignment floating = at(tue, ShiftType.FLOATING, "13:00", "15:00");

		assertTrue(com.midco.rota.opt.RotaConstraintProvider.dayShiftClash(day, night), "Clinton: Day then waking night");
		assertTrue(com.midco.rota.opt.RotaConstraintProvider.dayShiftClash(night, longDay));
		assertTrue(com.midco.rota.opt.RotaConstraintProvider.dayShiftClash(day, floating));
		assertFalse(com.midco.rota.opt.RotaConstraintProvider.dayShiftClash(longDay, sleepIn), "sleep-in may follow");
		assertFalse(com.midco.rota.opt.RotaConstraintProvider.dayShiftClash(floating, night), "rule is about day shifts only");
		assertFalse(com.midco.rota.opt.RotaConstraintProvider.dayShiftClash(day, at(tue.plusDays(1), ShiftType.WAKING_NIGHT, "20:00", "08:00")),
				"next day is a different date");
	}

	@Test
	void onlyAnotherWakingNightMayFollowAWakingNight() {
		LocalDate tue = LocalDate.of(2026, 10, 13), wed = tue.plusDays(1);
		WorkShiftAssignment night = at(tue, ShiftType.WAKING_NIGHT, "20:00", "08:00");

		assertTrue(com.midco.rota.opt.RotaConstraintProvider.wakingNightClash(night, at(wed, ShiftType.LONG_DAY, "08:00", "20:00")),
				"Clinton: night then Wednesday long day");
		assertTrue(com.midco.rota.opt.RotaConstraintProvider.wakingNightClash(at(wed, ShiftType.DAY, "10:00", "16:00"), night), "either order");
		assertTrue(com.midco.rota.opt.RotaConstraintProvider.wakingNightClash(night, at(wed, ShiftType.FLOATING, "13:00", "15:00")));
		assertFalse(com.midco.rota.opt.RotaConstraintProvider.wakingNightClash(night, at(wed, ShiftType.WAKING_NIGHT, "20:00", "08:00")),
				"consecutive nights are fine");
		assertFalse(com.midco.rota.opt.RotaConstraintProvider.wakingNightClash(night, at(wed, ShiftType.SLEEP_IN, "20:00", "08:00")));
		assertFalse(com.midco.rota.opt.RotaConstraintProvider.wakingNightClash(night, at(wed.plusDays(1), ShiftType.DAY, "08:00", "18:00")),
				"a day off in between");
		assertTrue(com.midco.rota.opt.RotaConstraintProvider.restClash(night, at(wed, ShiftType.LONG_DAY, "08:00", "20:00")));
	}

	@Test
	void wakingNightRuleWeightComesFromV036() throws Exception {
		try (var in = NeverWorkedAndRestrictionRulesTest.class
				.getResourceAsStream("/db/migration/V036__no_shift_straight_after_waking_night.sql")) {
			String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			assertTrue(sql.contains("('Shift straight after a waking night', 1000, 'HARD', true"));
		}
	}

	@Test
	void dayShiftRuleWeightComesFromV035() throws Exception {
		try (var in = NeverWorkedAndRestrictionRulesTest.class
				.getResourceAsStream("/db/migration/V035__day_shift_only_followed_by_sleep_in.sql")) {
			String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			assertTrue(sql.contains("('Day or long day followed by another shift', 1000, 'HARD', true"));
		}
	}

	@Test
	void weightsComeFromV034() throws Exception {
		assertEquals(HardSoftLongScore.ZERO, new RotaConstraintConfiguration().getCarerNeverWorkedAtHouse(),
				"weight comes only from constraint_setting");
		try (var in = NeverWorkedAndRestrictionRulesTest.class
				.getResourceAsStream("/db/migration/V034__hard_restrictions_and_never_worked.sql")) {
			String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			assertTrue(sql.contains("('Carer never worked at this house', 100, 'HARD', true"));
			assertTrue(sql.contains("SET score_weight = 100, constraint_type = 'HARD', enabled = true"));
			assertTrue(sql.contains("WHERE constraint_name = 'Max hours per week'"));
		}
	}
}
