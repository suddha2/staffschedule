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
import com.midco.rota.opt.AssignEmployeeProblemChange;
import com.midco.rota.util.ShiftType;

import staffschedule.support.RecordingProblemChangeDirector;

/**
 * Unit tests for the live "assign employee" ProblemChange (P3), driven through
 * the {@link RecordingProblemChangeDirector} test double — no solver, no DB.
 */
class AssignEmployeeProblemChangeTest {

	private final RecordingProblemChangeDirector director = new RecordingProblemChangeDirector();

	@Test
	void assignsEmployeeAndPinsSlot() {
		Employee alice = employee(10, "Alice", "Adams");
		ShiftAssignment slot = assignment(100, "9 CLAYDON", ShiftType.DAY);
		Rota rota = rota(List.of(alice), slot);

		new AssignEmployeeProblemChange(100, 10, true).doChange(rota, director);

		assertEquals(10, slot.getEmployee().getId());
		assertTrue(slot.isPinned(), "manually assigned slot should be pinned");
		assertTrue(director.changedVariables.contains("employee"));
		assertEquals(1, director.problemPropertyChanges);
	}

	@Test
	void unassignsAndUnpinsWhenEmployeeIdNull() {
		Employee alice = employee(10, "Alice", "Adams");
		ShiftAssignment slot = assignment(100, "9 CLAYDON", ShiftType.DAY);
		slot.setEmployee(alice);
		slot.setPinned(true);
		Rota rota = rota(List.of(alice), slot);

		new AssignEmployeeProblemChange(100, null, false).doChange(rota, director);

		assertNull(slot.getEmployee(), "null employeeId should clear the slot");
		assertFalse(slot.isPinned(), "unpinned slot returns to the solver");
	}

	@Test
	void unknownAssignmentIsIgnored() {
		Employee alice = employee(10, "Alice", "Adams");
		ShiftAssignment slot = assignment(100, "9 CLAYDON", ShiftType.DAY);
		Rota rota = rota(List.of(alice), slot);

		// assignmentId 999 is not in the rota → no change, no exception
		new AssignEmployeeProblemChange(999, 10, true).doChange(rota, director);

		assertNull(slot.getEmployee());
		assertTrue(director.changedVariables.isEmpty());
		assertEquals(0, director.problemPropertyChanges);
	}

	@Test
	void employeeNotInValueRangeLeavesSlotUnassigned() {
		Employee alice = employee(10, "Alice", "Adams");
		ShiftAssignment slot = assignment(100, "9 CLAYDON", ShiftType.DAY);
		Rota rota = rota(List.of(alice), slot);

		// employee 77 is not in the value range → resolves to null (unassigned)
		new AssignEmployeeProblemChange(100, 77, true).doChange(rota, director);

		assertNull(slot.getEmployee());
	}
}
