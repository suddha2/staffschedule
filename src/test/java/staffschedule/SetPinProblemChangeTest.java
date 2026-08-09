package staffschedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static staffschedule.support.RotaTestFixtures.assignment;
import static staffschedule.support.RotaTestFixtures.employee;
import static staffschedule.support.RotaTestFixtures.rota;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.midco.rota.model.Employee;
import com.midco.rota.model.Rota;
import com.midco.rota.model.ShiftAssignment;
import com.midco.rota.opt.SetPinProblemChange;
import com.midco.rota.util.ShiftType;

import staffschedule.support.RecordingProblemChangeDirector;

/**
 * Unit tests for the live "set pin" ProblemChange (P3).
 */
class SetPinProblemChangeTest {

	private final RecordingProblemChangeDirector director = new RecordingProblemChangeDirector();

	@Test
	void pinsAnUnpinnedSlot() {
		Employee alice = employee(10, "Alice", "Adams");
		ShiftAssignment slot = assignment(100, "9 CLAYDON", ShiftType.DAY);
		slot.setEmployee(alice);
		Rota rota = rota(List.of(alice), slot);

		new SetPinProblemChange(100, true).doChange(rota, director);

		assertTrue(slot.isPinned());
		assertEquals(10, slot.getEmployee().getId(), "pin must not change the employee");
		assertEquals(1, director.problemPropertyChanges);
	}

	@Test
	void unpinsAPinnedSlot() {
		ShiftAssignment slot = assignment(100, "9 CLAYDON", ShiftType.DAY);
		slot.setPinned(true);
		Rota rota = rota(List.of(employee(10, "Alice", "Adams")), slot);

		new SetPinProblemChange(100, false).doChange(rota, director);

		assertFalse(slot.isPinned());
	}

	@Test
	void unknownAssignmentIsIgnored() {
		ShiftAssignment slot = assignment(100, "9 CLAYDON", ShiftType.DAY);
		slot.setPinned(true);
		Rota rota = rota(List.of(employee(10, "Alice", "Adams")), slot);

		new SetPinProblemChange(999, false).doChange(rota, director);

		assertTrue(slot.isPinned(), "unknown id must not touch existing slots");
		assertEquals(0, director.problemPropertyChanges);
	}
}
