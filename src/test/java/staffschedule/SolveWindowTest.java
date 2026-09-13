package staffschedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.midco.rota.util.SolveWindow;

/** Week-snapping for ad-hoc solve ranges. */
class SolveWindowTest {

	@Test
	void adHocMidWeekRangeExpandsToEnclosingWholeWeeks() {
		// Wed 2025-09-03 .. Fri 2025-09-12
		SolveWindow w = SolveWindow.snapToWholeWeeks(LocalDate.of(2025, 9, 3), LocalDate.of(2025, 9, 12));

		assertEquals(LocalDate.of(2025, 9, 1), w.start(), "start moves back to Monday");
		assertEquals(LocalDate.of(2025, 9, 14), w.end(), "end moves forward to Sunday");
		assertEquals(DayOfWeek.MONDAY, w.start().getDayOfWeek());
		assertEquals(DayOfWeek.SUNDAY, w.end().getDayOfWeek());
		assertTrue(w.isWholeWeeks());
	}

	@Test
	void mondayAlignedPeriodSnapsToItself() {
		// A 28-day pay period: Mon 2025-07-21 .. Sun 2025-08-17
		LocalDate start = LocalDate.of(2025, 7, 21);
		LocalDate end = LocalDate.of(2025, 8, 17);
		assertEquals(DayOfWeek.MONDAY, start.getDayOfWeek());
		assertEquals(DayOfWeek.SUNDAY, end.getDayOfWeek());

		SolveWindow w = SolveWindow.snapToWholeWeeks(start, end);

		assertEquals(start, w.start(), "already Monday — unchanged");
		assertEquals(end, w.end(), "already Sunday — unchanged");
	}

	@Test
	void singleDayExpandsToItsWholeWeek() {
		SolveWindow w = SolveWindow.snapToWholeWeeks(LocalDate.of(2025, 9, 10), LocalDate.of(2025, 9, 10));

		assertEquals(LocalDate.of(2025, 9, 8), w.start());
		assertEquals(LocalDate.of(2025, 9, 14), w.end());
	}

	@Test
	void isWholeWeeksFalseForPartialRange() {
		assertFalse(new SolveWindow(LocalDate.of(2025, 9, 3), LocalDate.of(2025, 9, 12)).isWholeWeeks());
	}
}
