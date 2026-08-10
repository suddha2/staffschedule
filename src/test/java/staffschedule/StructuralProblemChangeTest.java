package staffschedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static staffschedule.support.RotaTestFixtures.assignment;
import static staffschedule.support.RotaTestFixtures.employee;
import static staffschedule.support.RotaTestFixtures.rota;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.midco.rota.model.Employee;
import com.midco.rota.model.Rota;
import com.midco.rota.model.ShiftAssignment;
import com.midco.rota.opt.AddEmployeeToValueRangeProblemChange;
import com.midco.rota.opt.RemoveEmployeeFromValueRangeProblemChange;
import com.midco.rota.util.ShiftType;

import staffschedule.support.RecordingProblemChangeDirector;

/**
 * Unit tests for the structural live edits (P3+): add/remove an employee to/from
 * the value range, via the pass-through {@link RecordingProblemChangeDirector}.
 */
class StructuralProblemChangeTest {

	private final RecordingProblemChangeDirector director = new RecordingProblemChangeDirector();

	@Test
	void addEmployeeExtendsTheValueRange() {
		Employee alice = employee(10, "Alice", "Adams");
		Employee bob = employee(20, "Bob", "Brown");
		Rota rota = rota(List.of(alice), assignment(100, "9 CLAYDON", ShiftType.DAY));

		new AddEmployeeToValueRangeProblemChange(bob).doChange(rota, director);

		assertTrue(rota.getEmployeeList().stream().anyMatch(e -> e.getId() == 20), "bob is now selectable");
		assertEquals(2, rota.getEmployeeList().size());
		assertEquals(1, director.addedFacts);
	}

	@Test
	void addEmployeeIsIdempotent() {
		Employee alice = employee(10, "Alice", "Adams");
		Rota rota = rota(List.of(alice), assignment(100, "9 CLAYDON", ShiftType.DAY));

		new AddEmployeeToValueRangeProblemChange(employee(10, "Alice", "Adams")).doChange(rota, director);

		assertEquals(1, rota.getEmployeeList().size(), "already-present employee not duplicated");
		assertEquals(0, director.addedFacts);
	}

	@Test
	void removeEmployeeUnassignsTheirSlotsAndDropsThemFromRange() {
		Employee alice = employee(10, "Alice", "Adams");
		Employee bob = employee(20, "Bob", "Brown");
		ShiftAssignment slot = assignment(100, "9 CLAYDON", ShiftType.DAY);
		slot.setEmployee(alice);
		Rota rota = rota(List.of(alice, bob), slot);

		new RemoveEmployeeFromValueRangeProblemChange(10).doChange(rota, director);

		assertNull(slot.getEmployee(), "alice's slot is freed");
		assertFalse(rota.getEmployeeList().stream().anyMatch(e -> e.getId() == 10), "alice left the value range");
		assertTrue(rota.getEmployeeList().stream().anyMatch(e -> e.getId() == 20), "bob remains");
		assertTrue(director.changedVariables.contains("employee"));
		assertEquals(1, director.removedFacts);
	}

	@Test
	void removeUnknownEmployeeIsNoOp() {
		Rota rota = rota(List.of(employee(10, "Alice", "Adams")), assignment(100, "9 CLAYDON", ShiftType.DAY));

		new RemoveEmployeeFromValueRangeProblemChange(99).doChange(rota, director);

		assertEquals(1, rota.getEmployeeList().size());
		assertEquals(0, director.removedFacts);
	}
}
