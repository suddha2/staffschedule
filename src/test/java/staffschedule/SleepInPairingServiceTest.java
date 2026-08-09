package staffschedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.midco.rota.model.Employee;
import com.midco.rota.model.Rota;
import com.midco.rota.model.Shift;
import com.midco.rota.model.ShiftAssignment;
import com.midco.rota.model.ShiftTemplate;
import com.midco.rota.service.SleepInPairingService;
import com.midco.rota.service.SleepInPairingService.PairingResult;
import com.midco.rota.util.ShiftType;

/**
 * Pure (no Spring / no DB) behaviour test for the SLEEP_IN pairing rules
 * extracted from {@code SolverService.solveAsync} in Phase 0. Locks the
 * batch-equivalent behaviour so the extraction is provably behaviour-preserving.
 */
class SleepInPairingServiceTest {

	private static final LocalDate DATE = LocalDate.of(2026, 1, 5); // a Monday

	private final SleepInPairingService service = new SleepInPairingService();

	private ShiftAssignment assignment(String location, ShiftType type) {
		ShiftTemplate template = new ShiftTemplate();
		template.setLocation(location);
		template.setShiftType(type);
		template.setDayOfWeek(DayOfWeek.MONDAY);
		template.setStartTime(LocalTime.of(8, 0));
		template.setEndTime(LocalTime.of(20, 0));
		return new ShiftAssignment(new Shift(DATE, template, 1));
	}

	private Rota rota(List<ShiftAssignment> assignments) {
		List<Employee> employees = new ArrayList<>();
		employees.add(new Employee()); // non-empty: Rota ctor divides by employee count
		return new Rota(employees, assignments);
	}

	@Test
	void pairsSleepInToLongDayEmployeeOnSameLocationAndDate() {
		Employee alice = new Employee();
		ShiftAssignment longDay = assignment("9 CLAYDON", ShiftType.LONG_DAY);
		longDay.setEmployee(alice);
		ShiftAssignment sleepIn = assignment("9 CLAYDON", ShiftType.SLEEP_IN);

		Rota rota = rota(List.of(longDay, sleepIn));
		PairingResult result = service.pairSleepIns(rota);

		assertSame(alice, sleepIn.getEmployee(), "SLEEP_IN should mirror its LONG_DAY employee");
		assertEquals(1, result.paired());
		assertEquals(0, result.failed());
		assertEquals(0L, result.unpaired());
	}

	@Test
	void leavesSleepInUnassignedWhenNoMatchingLongDay() {
		// LONG_DAY at a different location → different pairId → no match.
		Employee bob = new Employee();
		ShiftAssignment longDayElsewhere = assignment("OTHER HOUSE", ShiftType.LONG_DAY);
		longDayElsewhere.setEmployee(bob);
		ShiftAssignment sleepIn = assignment("9 CLAYDON", ShiftType.SLEEP_IN);

		Rota rota = rota(List.of(longDayElsewhere, sleepIn));
		PairingResult result = service.pairSleepIns(rota);

		assertNull(sleepIn.getEmployee(), "SLEEP_IN with no same-pair LONG_DAY stays unassigned");
		assertEquals(0, result.paired());
		assertEquals(1, result.failed());
		assertEquals(1L, result.unpaired());
	}

	@Test
	void eachSleepInIsPairedAtMostOnce() {
		// Two LONG_DAYs (two carers) and two SLEEP_IN slots at the same house/date.
		Employee alice = new Employee();
		Employee carol = new Employee();
		ShiftAssignment longDay1 = assignment("9 CLAYDON", ShiftType.LONG_DAY);
		longDay1.setEmployee(alice);
		ShiftAssignment longDay2 = assignment("9 CLAYDON", ShiftType.LONG_DAY);
		longDay2.setEmployee(carol);
		ShiftAssignment sleepIn1 = assignment("9 CLAYDON", ShiftType.SLEEP_IN);
		ShiftAssignment sleepIn2 = assignment("9 CLAYDON", ShiftType.SLEEP_IN);

		Rota rota = rota(List.of(longDay1, longDay2, sleepIn1, sleepIn2));
		PairingResult result = service.pairSleepIns(rota);

		assertEquals(2, result.paired());
		assertEquals(0, result.failed());
		assertEquals(0L, result.unpaired());
		// Both sleep-ins assigned, and to distinct carers (no sleep-in reused).
		assertNotNull(sleepIn1.getEmployee());
		assertNotNull(sleepIn2.getEmployee());
		assertNotSame(sleepIn1.getEmployee(), sleepIn2.getEmployee(),
				"the two SLEEP_IN slots must not both be paired to the same carer");
	}

	@Test
	void resetSleepInsBlanksOnlySleepInSlots() {
		Employee alice = new Employee();
		ShiftAssignment longDay = assignment("9 CLAYDON", ShiftType.LONG_DAY);
		longDay.setEmployee(alice);
		ShiftAssignment sleepIn = assignment("9 CLAYDON", ShiftType.SLEEP_IN);
		sleepIn.setEmployee(alice); // stale value from a previous solve

		Rota rota = rota(List.of(longDay, sleepIn));
		service.resetSleepIns(rota);

		assertNull(sleepIn.getEmployee(), "reset should blank SLEEP_IN");
		assertSame(alice, longDay.getEmployee(), "reset must not touch non-SLEEP_IN slots");
	}
}
