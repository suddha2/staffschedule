package staffschedule;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.midco.rota.model.Shift;
import com.midco.rota.model.ShiftAssignment;
import com.midco.rota.model.ShiftTemplate;
import com.midco.rota.model.WorkShiftAssignment;
import com.midco.rota.opt.ShiftOverlap;

/**
 * Parity guard: the backend same-day rule ({@link ShiftOverlap#allowedSameDay})
 * must agree with the shared fixtures. The frontend runs the SAME fixtures against
 * its mirror ({@code shiftConflicts.js}) via {@code scripts/checkConflictParity.mjs}.
 * If either side's rule drifts, its check fails — so the UI can't silently diverge
 * from what the backend enforces. Keep the fixtures file identical in both repos.
 */
class SameDayRuleParityTest {

	@Test
	void backendRuleMatchesSharedFixtures() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode root;
		try (InputStream in = getClass().getResourceAsStream("/shift-conflict-fixtures.json")) {
			assertNotNull(in, "shift-conflict-fixtures.json must be on the test classpath");
			root = mapper.readTree(in);
		}

		LocalDate day = LocalDate.of(2027, 1, 4); // arbitrary single day
		List<String> failures = new ArrayList<>();

		for (JsonNode c : root.get("cases")) {
			String name = c.get("name").asText();
			boolean expected = c.get("allowed").asBoolean();

			List<ShiftAssignment> assignments = new ArrayList<>();
			for (JsonNode pair : c.get("shifts")) {
				ShiftTemplate t = new ShiftTemplate();
				t.setLocation("X");
				t.setStartTime(LocalTime.parse(pair.get(0).asText()));
				t.setEndTime(LocalTime.parse(pair.get(1).asText()));
				assignments.add(new WorkShiftAssignment(new Shift(day, t, 1)));
			}

			boolean actual = ShiftOverlap.allowedSameDay(assignments);
			if (actual != expected) {
				failures.add("'" + name + "' expected allowed=" + expected + " but got " + actual);
			}
		}

		assertTrue(failures.isEmpty(), "Backend same-day rule disagrees with shared fixtures: " + failures);
	}
}
