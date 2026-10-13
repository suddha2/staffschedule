package staffschedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.midco.rota.dto.ConflictError;
import com.midco.rota.model.Employee;
import com.midco.rota.model.Shift;
import com.midco.rota.model.ShiftAssignment;
import com.midco.rota.model.ShiftTemplate;
import com.midco.rota.model.WorkShiftAssignment;
import com.midco.rota.repository.EmployeeRepository;
import com.midco.rota.service.PinValidationService;
import com.midco.rota.util.ContractType;
import com.midco.rota.util.Gender;

/**
 * Save-time rules (normal save and pin-all save): overlaps, wrong gender / missing skill,
 * and 6:1 for permanent carers only. A saved rota becomes standing pins, so these must not
 * get through.
 */
class SaveTimeValidationTest {

	private static final LocalDate MON = LocalDate.of(2026, 10, 12);
	private final PinValidationService svc = new PinValidationService(mock(EmployeeRepository.class));
	private long shiftIds = 1;

	private Employee carer(int id, Gender g, ContractType ct) {
		Employee e = new Employee();
		e.setId(id);
		e.setFirstName("C" + id);
		e.setLastName("Test");
		e.setGender(g);
		e.setContractType(ct);
		return e;
	}

	private ShiftAssignment shift(Employee e, LocalDate d, String type, int startH, int endH, Gender required) {
		ShiftTemplate t = new ShiftTemplate();
		t.setLocation("House");
		t.setShiftTypeCode(type);
		t.setDayOfWeek(DayOfWeek.MONDAY);
		t.setStartTime(LocalTime.of(startH, 0));
		t.setEndTime(LocalTime.of(endH, 0));
		t.setRequiredGender(required);
		Shift s = new Shift(d, t, 1);
		s.setId(shiftIds++);
		ShiftAssignment sa = new WorkShiftAssignment(s);
		sa.setEmployee(e);
		return sa;
	}

	private List<ShiftAssignment> days(Employee e, int n) {
		List<ShiftAssignment> list = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			list.add(shift(e, MON.plusDays(i), "DAY", 8, 20, Gender.ANY));
		}
		return list;
	}

	@Test
	void wrongGenderIsRejectedWithAReasonAndAShiftToHighlight() {
		ShiftAssignment sa = shift(carer(1, Gender.FEMALE, ContractType.PERMANENT), MON, "LONG_DAY", 7, 22, Gender.MALE);
		List<ConflictError> c = svc.validateAssignments(List.of(sa));
		assertEquals(1, c.size());
		assertTrue(c.get(0).getReason().startsWith("Wrong gender"), c.get(0).getReason());
		assertEquals(sa.getShift().getId().longValue(), c.get(0).getShiftId());
	}

	@Test
	void permanentSeventhAndEighthDaysInARowAreRejected() {
		List<ConflictError> c = svc.validateAssignments(days(carer(2, Gender.FEMALE, ContractType.PERMANENT), 8));
		assertEquals(2, c.size());
		assertEquals(MON.plusDays(6), c.get(0).getDate());
		assertTrue(c.get(0).getReason().contains("6:1"));
	}

	@Test
	void sixDaysInARowIsFine() {
		assertTrue(svc.validateAssignments(days(carer(3, Gender.MALE, ContractType.PERMANENT), 6)).isEmpty());
	}

	@Test
	void sixOneDoesNotApplyToZeroHoursStaff() {
		assertTrue(svc.validateAssignments(days(carer(4, Gender.MALE, ContractType.ZERO_HOURS), 8)).isEmpty());
	}

	@Test
	void overlappingSameDayShiftsAreStillRejected() {
		Employee e = carer(5, Gender.MALE, ContractType.ZERO_HOURS);
		List<ConflictError> c = svc.validateAssignments(List.of(
				shift(e, MON, "DAY", 8, 20, Gender.ANY), shift(e, MON, "DAY", 10, 18, Gender.ANY)));
		assertEquals(1, c.size());
		assertTrue(c.get(0).getReason().startsWith("Overlapping"));
	}

	@Test
	void womanOnAMaleOnlyOwnCarerSleepInIsRejected() {
		// An own-carer sleep-in is a work shift the solver fills itself; SLEEP_IN doesn't count as
		// worked hours, which used to make the gender check skip it.
		ShiftAssignment sa = shift(carer(6, Gender.FEMALE, ContractType.ZERO_HOURS), MON, "SLEEP_IN", 22, 7, Gender.MALE);
		List<ConflictError> c = svc.validateAssignments(List.of(sa));
		assertEquals(1, c.size());
		assertTrue(c.get(0).getReason().startsWith("Wrong gender"), c.get(0).getReason());
	}
}
