package staffschedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.midco.rota.model.EmployeeAvailability;
import com.midco.rota.opt.RotaConstraintConfiguration;
import com.midco.rota.util.AvailabilitySource;
import com.midco.rota.util.AvailabilityType;

/**
 * Unit tests for the leave / unavailability rule. The date-cover logic is the
 * heart of the constraint; the config test guards that the rule is on and hard.
 */
class EmployeeAvailabilityConstraintTest {

	private EmployeeAvailability leave(LocalDate start, LocalDate end) {
		return new EmployeeAvailability(7, start, end, AvailabilityType.PLANNED_LEAVE,
				AvailabilitySource.PP_API, "PP-123");
	}

	@Test
	void coversDateIsInclusiveOfBothEnds() {
		EmployeeAvailability a = leave(LocalDate.of(2025, 7, 21), LocalDate.of(2025, 7, 25));

		assertTrue(a.coversDate(LocalDate.of(2025, 7, 21)), "start day is covered");
		assertTrue(a.coversDate(LocalDate.of(2025, 7, 25)), "end day is covered");
		assertTrue(a.coversDate(LocalDate.of(2025, 7, 23)), "a day inside is covered");
	}

	@Test
	void coversDateExcludesOutsideAndNull() {
		EmployeeAvailability a = leave(LocalDate.of(2025, 7, 21), LocalDate.of(2025, 7, 25));

		assertFalse(a.coversDate(LocalDate.of(2025, 7, 20)), "day before is not covered");
		assertFalse(a.coversDate(LocalDate.of(2025, 7, 26)), "day after is not covered");
		assertFalse(a.coversDate(null), "null date is not covered");
	}

	@Test
	void singleDayLeaveCoversOnlyThatDay() {
		EmployeeAvailability a = leave(LocalDate.of(2025, 7, 21), LocalDate.of(2025, 7, 21));

		assertTrue(a.coversDate(LocalDate.of(2025, 7, 21)));
		assertFalse(a.coversDate(LocalDate.of(2025, 7, 22)));
	}

	@Test
	void theLeaveRuleIsActiveAndHardByDefault() {
		RotaConstraintConfiguration config = new RotaConstraintConfiguration();

		assertTrue(config.getEmployeeUnavailable().hardScore() > 0,
				"leave must be a hard constraint so no-one is allocated onto booked leave");
		assertFalse(RotaConstraintConfiguration.INACTIVE_BY_DEFAULT.contains("Employee unavailable (leave)"),
				"the leave rule must be active by default, not seeded off");
	}
}
