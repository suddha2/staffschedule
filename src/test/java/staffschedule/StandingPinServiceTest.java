package staffschedule;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.midco.rota.service.StandingPinService;

/** Removing standing pins: what is refused before anything changes. */
class StandingPinServiceTest {

	private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
	private final StandingPinService svc = new StandingPinService(jdbc);

	private void assertRefused(String message, org.junit.jupiter.api.function.Executable call) {
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, call);
		assertTrue(e.getMessage().contains(message), e.getMessage());
		verify(jdbc, never()).update(anyString(), any(Object[].class));
	}

	@Test
	void nothingChosen() {
		assertRefused("Choose the pins", () -> svc.remove("BARNET", List.of(), "ops"));
		assertRefused("Choose the pins", () -> svc.remove(" ", List.of(1L), "ops"));
	}

	@Test
	void anUnknownRemovalCannotBeUndone() {
		when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(List.of());
		assertRefused("not found", () -> svc.undo(9, "ops"));
	}

	@Test
	void aRemovalCanOnlyBeUndoneOnce() {
		when(jdbc.queryForList(anyString(), any(Object[].class)))
				.thenReturn(List.of(Map.of("region", "BARNET", "undone_at", "2026-11-02")));
		assertRefused("already been undone", () -> svc.undo(9, "ops"));
	}

	@Test
	void onlyTheLatestRemovalCanBeUndone() {
		java.util.Map<String, Object> run = new java.util.HashMap<>();
		run.put("region", "BARNET");
		run.put("undone_at", null);
		when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(List.of(run));
		when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(12L);
		assertRefused("later removals", () -> svc.undo(9, "ops"));
	}
}
