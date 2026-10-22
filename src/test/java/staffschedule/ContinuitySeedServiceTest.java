package staffschedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.midco.rota.model.Employee;
import com.midco.rota.model.Shift;
import com.midco.rota.model.ShiftAssignment;
import com.midco.rota.model.ShiftTemplate;
import com.midco.rota.model.WorkShiftAssignment;
import com.midco.rota.service.ContinuitySeedService;

/**
 * Pure tests for the seed-application logic (no DB). The seed key for a new
 * assignment on date D from template T is (T, D-28).
 */
class ContinuitySeedServiceTest {

	private final ContinuitySeedService svc = new ContinuitySeedService(null);
	private static final LocalDate D = LocalDate.of(2025, 7, 21);      // a Monday in the window
	private static final LocalDate PRIOR = D.minusDays(28);            // the seed key date

	private ShiftAssignment slot(int templateId, LocalDate date) {
		ShiftTemplate t = new ShiftTemplate();
		t.setId(templateId);
		Shift shift = new Shift();
		shift.setShiftTemplate(t);
		shift.setShiftStart(date);
		return new WorkShiftAssignment(shift);
	}

	private Employee emp(int id) {
		Employee e = new Employee();
		e.setId(id);
		return e;
	}

	@Test
	void seedsFromThePriorCycleSlot() {
		ShiftAssignment a = slot(100, D);
		Map<String, List<Integer>> seed = Map.of("100|" + PRIOR, List.of(5));

		int n = svc.applySeed(List.of(a), seed, Map.of(5, emp(5)));

		assertEquals(1, n);
		assertEquals(5, a.getSeededEmployeeId());
		assertEquals(5, a.getEmployee().getId(), "seed is also warm-started onto the assignment");
	}

	@Test
	void twoToOneSlotSharesThePriorCarers() {
		ShiftAssignment a1 = slot(100, D);
		ShiftAssignment a2 = slot(100, D); // same slot, 2:1
		Map<String, List<Integer>> seed = Map.of("100|" + PRIOR, List.of(5, 6));

		int n = svc.applySeed(List.of(a1, a2), seed, Map.of(5, emp(5), 6, emp(6)));

		assertEquals(2, n);
		assertEquals(Set.of(5, 6), Set.of(a1.getSeededEmployeeId(), a2.getSeededEmployeeId()));
	}

	@Test
	void skipsSeedCarerWhoLeftThePool() {
		ShiftAssignment a = slot(100, D);
		Map<String, List<Integer>> seed = Map.of("100|" + PRIOR, List.of(99)); // 99 not in pool

		int n = svc.applySeed(List.of(a), seed, Map.of(5, emp(5)));

		assertEquals(0, n);
		assertNull(a.getSeededEmployeeId());
		assertNull(a.getEmployee());
	}

	@Test
	void skipsSeedCarerOnLeaveThatDay() {
		ShiftAssignment a = slot(100, D);
		Employee e = emp(5);
		e.setUnavailableDates(Set.of(D)); // on leave on the shift date
		Map<String, List<Integer>> seed = Map.of("100|" + PRIOR, List.of(5));

		int n = svc.applySeed(List.of(a), seed, Map.of(5, e));

		assertEquals(0, n, "seed carer on leave is not seeded; slot left for construction");
		assertNull(a.getEmployee());
	}

	@Test
	void doesNotTouchPinnedAssignments() {
		ShiftAssignment a = slot(100, D);
		a.setPinned(true);
		Map<String, List<Integer>> seed = Map.of("100|" + PRIOR, List.of(5));

		int n = svc.applySeed(List.of(a), seed, Map.of(5, emp(5)));

		assertEquals(0, n);
		assertNull(a.getSeededEmployeeId());
	}

	@Test
	void noSeedForSlotWithNoPriorCarer() {
		ShiftAssignment a = slot(200, D); // no seed entry for template 200
		Map<String, List<Integer>> seed = Map.of("100|" + PRIOR, List.of(5));

		assertEquals(0, svc.applySeed(List.of(a), seed, Map.of(5, emp(5))));
		assertNull(a.getSeededEmployeeId());
	}

	@Test
	void doesNotCarryAGenderMismatchForward() {
		ShiftAssignment a = slot(100, D);
		a.getShift().getShiftTemplate().setRequiredGender(com.midco.rota.util.Gender.MALE);
		Employee woman = emp(5);
		woman.setGender(com.midco.rota.util.Gender.FEMALE);
		Map<String, List<Integer>> seed = Map.of("100|" + PRIOR, List.of(5));

		assertEquals(0, svc.applySeed(List.of(a), seed, Map.of(5, woman)), "a past rule break must not be re-locked");
		assertNull(a.getEmployee());
	}

	@Test
	void fallsThroughToTheNextEligiblePriorCarer() {
		ShiftAssignment a = slot(100, D);
		a.getShift().getShiftTemplate().setRequiredGender(com.midco.rota.util.Gender.MALE);
		Employee woman = emp(5), man = emp(6);
		woman.setGender(com.midco.rota.util.Gender.FEMALE);
		man.setGender(com.midco.rota.util.Gender.MALE);
		Map<String, List<Integer>> seed = Map.of("100|" + PRIOR, List.of(5, 6));

		assertEquals(1, svc.applySeed(List.of(a), seed, Map.of(5, woman, 6, man)));
		assertEquals(6, a.getSeededEmployeeId());
	}

	/** A slot at a house People Planner tracks (somebody is a regular there) unless tracked=false. */
	private ShiftAssignment slotAt(String house, boolean tracked) {
		ShiftAssignment a = slot(100, D);
		a.getShift().getShiftTemplate().setLocation(house);
		a.getShift().getShiftTemplate().setHistoryTracked(tracked);
		return a;
	}

	/** Carer whose own houses (familiarity 100, carer_house_history) are these. */
	private Employee regularAt(int id, String... houses) {
		Employee e = emp(id);
		Map<String, Integer> f = new java.util.HashMap<>();
		for (String x : houses) f.put(x.trim().toUpperCase(), 100);
		e.setHouseFamiliarity(f);
		e.setWorkedHouses(f.keySet()); // a regular has worked there
		return e;
	}

	@Test
	void aHouseThatIsASmallSidelineForTheCarerIsNotCarriedOver() {
		// Loveness case: two days at Riverdale in June (1% of her work); her own house is Highfields
		ShiftAssignment a = slotAt("RIVERDALE", true);
		Employee loveness = emp(5);
		loveness.setHouseFamiliarity(Map.of("3 HIGHFIELDS", 100, "RIVERDALE", 4));

		assertEquals(0, svc.applySeed(List.of(a), Map.of("100|" + PRIOR, List.of(5)), Map.of(5, loveness)));
	}

	@Test
	void doesNotCarryACarerIntoAHouseTheyAreNotARegularAt() {
		// Louise Tripp case: only ever worked Alexandra, carried over at Riverdale from one
		// system-generated rota to the next
		ShiftAssignment a = slotAt("RIVERDALE", true);
		Employee louise = regularAt(5, "Alexandra Flat A - KC");

		assertEquals(0, svc.applySeed(List.of(a), Map.of("100|" + PRIOR, List.of(5)), Map.of(5, louise)));
		assertNull(a.getEmployee(), "left open for the solver");
	}

	@Test
	void keepsARegular() {
		ShiftAssignment a = slotAt(" Riverdale ", true);
		Employee bongiwe = regularAt(5, "9 CLAYDON", "RIVERDALE");

		assertEquals(1, svc.applySeed(List.of(a), Map.of("100|" + PRIOR, List.of(5)), Map.of(5, bongiwe)));
	}

	@Test
	void aCarerWithNoPeoplePlannerHistoryIsNotCarriedIntoATrackedHouse() {
		// the prior rota is a solver output, so it isn't evidence on its own
		ShiftAssignment a = slotAt("RIVERDALE", true);

		assertEquals(0, svc.applySeed(List.of(a), Map.of("100|" + PRIOR, List.of(5)), Map.of(5, emp(5))));
	}

	@Test
	void keepsCarryOversAtAnUntrackedHouse() {
		// e.g. a house People Planner can't see: nobody can be judged there
		ShiftAssignment a = slotAt("57 STURGESS", false);
		Employee carer = regularAt(5, "4 AUDLEY");

		assertEquals(1, svc.applySeed(List.of(a), Map.of("100|" + PRIOR, List.of(5)), Map.of(5, carer)));
	}

	@Test
	void pairedSleepInsAreNotSeededSeparatelyFromTheirLongDay() {
		// 2:1 sleep-in: seeding its seats on their own could lock one carer into both seats
		// (or into the seat whose long day is pinned to someone else). They follow the long day.
		Shift shift = new Shift();
		ShiftTemplate t = new ShiftTemplate();
		t.setId(300);
		shift.setShiftTemplate(t);
		shift.setShiftStart(D);
		ShiftAssignment sleepIn = new com.midco.rota.model.FollowerShiftAssignment(shift);

		assertEquals(0, svc.applySeed(List.of(sleepIn), Map.of("300|" + PRIOR, List.of(5)), Map.of(5, emp(5))));
		assertNull(sleepIn.getEmployee());
	}

	@Test
	void doesNotSeedACarerMissingARequiredSkill() {
		ShiftAssignment a = slot(100, D);
		a.getShift().getShiftTemplate().setRequiredSkills(List.of("SHIFT_LEAD"));
		Employee carer = emp(5);
		carer.setSkills(List.of("Carer"));

		assertEquals(0, svc.applySeed(List.of(a), Map.of("100|" + PRIOR, List.of(5)), Map.of(5, carer)));
	}
}
