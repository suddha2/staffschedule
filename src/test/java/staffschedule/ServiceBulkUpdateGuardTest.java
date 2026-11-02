package staffschedule;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.midco.rota.dto.ServiceBulkRequest;
import com.midco.rota.service.LiveSolverSessionService;
import com.midco.rota.service.ServiceBulkUpdateService;

/** A bulk update must not run while a solve holds the old name in memory. */
class ServiceBulkUpdateGuardTest {

	private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
	private final LiveSolverSessionService live = mock(LiveSolverSessionService.class);
	private final ServiceBulkUpdateService svc = new ServiceBulkUpdateService(jdbc, live);
	private final ServiceBulkRequest rename = new ServiceBulkRequest("BARNET", "RENAME", "57 STURGESS", "55 SEVINGTON");

	@Test
	void refusedWhileASolveIsWaitingOrRunning() {
		when(jdbc.queryForObject(contains("not completed"), eq(Integer.class), any(Object[].class))).thenReturn(1);
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> svc.apply(rename, "ops"));
		assertTrue(e.getMessage().contains("solve"), e.getMessage());
		verify(jdbc, never()).update(anyString(), any(Object[].class));
	}

	@Test
	void refusedWhileALiveSessionIsOpen() {
		when(jdbc.queryForObject(contains("not completed"), eq(Integer.class), any(Object[].class))).thenReturn(0);
		when(live.activeRotaIds()).thenReturn(Set.of(477L));
		when(jdbc.queryForObject(contains("rota_id = ?"), eq(Integer.class), any(Object[].class))).thenReturn(1);
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> svc.apply(rename, "ops"));
		assertTrue(e.getMessage().contains("live session"), e.getMessage());
		verify(jdbc, never()).update(anyString(), any(Object[].class));
	}
}
