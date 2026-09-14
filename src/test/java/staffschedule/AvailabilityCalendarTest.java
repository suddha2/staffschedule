package staffschedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.midco.rota.model.Employee;
import com.midco.rota.model.EmployeeAvailability;
import com.midco.rota.util.AvailabilityCalendar;
import com.midco.rota.util.AvailabilitySource;
import com.midco.rota.util.AvailabilityType;

/**
 * The pre-solve unavailability map: spans -> per-employee date set clipped to
 * the window, and the O(1) lookup the constraint uses.
 */
class AvailabilityCalendarTest {

	private EmployeeAvailability span(int empId, LocalDate from, LocalDate to) {
		return new EmployeeAvailability(empId, from, to, AvailabilityType.PLANNED_LEAVE,
				AvailabilitySource.PP_API, "ref-" + empId + "-" + from);
	}

	@Test
	void expandsSpanToEveryDayInclusive() {
		Map<Integer, Set<LocalDate>> map = AvailabilityCalendar.build(
				List.of(span(7, LocalDate.of(2025, 7, 21), LocalDate.of(2025, 7, 23))),
				LocalDate.of(2025, 7, 21), LocalDate.of(2025, 7, 27));

		assertEquals(Set.of(LocalDate.of(2025, 7, 21), LocalDate.of(2025, 7, 22), LocalDate.of(2025, 7, 23)),
				map.get(7));
	}

	@Test
	void clipsToWindowAndDropsNonOverlappingSpans() {
		Map<Integer, Set<LocalDate>> map = AvailabilityCalendar.build(
				List.of(
						span(7, LocalDate.of(2025, 7, 19), LocalDate.of(2025, 7, 22)), // starts before window
						span(8, LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 5))),  // entirely after window
				LocalDate.of(2025, 7, 21), LocalDate.of(2025, 7, 27));

		assertEquals(Set.of(LocalDate.of(2025, 7, 21), LocalDate.of(2025, 7, 22)), map.get(7),
				"span clipped to window start");
		assertFalse(map.containsKey(8), "span outside the window contributes nothing");
	}

	@Test
	void multipleSpansForOneEmployeeUnion() {
		Map<Integer, Set<LocalDate>> map = AvailabilityCalendar.build(
				List.of(span(7, LocalDate.of(2025, 7, 21), LocalDate.of(2025, 7, 21)),
						span(7, LocalDate.of(2025, 7, 25), LocalDate.of(2025, 7, 25))),
				LocalDate.of(2025, 7, 21), LocalDate.of(2025, 7, 27));

		assertEquals(Set.of(LocalDate.of(2025, 7, 21), LocalDate.of(2025, 7, 25)), map.get(7));
	}

	@Test
	void employeeLookupIsO1AndDateAware() {
		Employee e = new Employee();
		e.setUnavailableDates(Set.of(LocalDate.of(2025, 7, 22)));

		assertTrue(e.isUnavailableOn(LocalDate.of(2025, 7, 22)));
		assertFalse(e.isUnavailableOn(LocalDate.of(2025, 7, 23)));
		assertFalse(e.isUnavailableOn(null));
	}

	@Test
	void employeeWithNoMapIsAlwaysAvailable() {
		Employee e = new Employee(); // unavailableDates never set
		assertFalse(e.isUnavailableOn(LocalDate.of(2025, 7, 22)));
	}
}
