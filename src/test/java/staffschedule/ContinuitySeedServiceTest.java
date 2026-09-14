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
}
