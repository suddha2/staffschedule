package staffschedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.midco.rota.dto.DashboardSummary;
import com.midco.rota.repository.RegionRepository;
import com.midco.rota.service.DashboardService;
import com.midco.rota.service.ServiceBulkUpdateService;

/** The home screen copes with a region that has no rota and no periods set up. */
class DashboardServiceTest {

	@Test
	void aRegionWithNoRotaShowsAsNotStarted() {
		JdbcTemplate jdbc = mock(JdbcTemplate.class);
		RegionRepository regions = mock(RegionRepository.class);
		ServiceBulkUpdateService services = mock(ServiceBulkUpdateService.class);
		when(regions.findOperatingRegions()).thenReturn(List.of("KENT"));
		when(services.namesToFix("KENT")).thenReturn(List.of());
		when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(List.of());
		when(jdbc.queryForObject(anyString(), eq(Integer.class), any(Object[].class))).thenReturn(66);
		when(jdbc.queryForObject(anyString(), eq(Integer.class))).thenReturn(2);

		DashboardSummary summary = new DashboardService(jdbc, regions, services).summary();

		assertNull(summary.nextPeriod());
		assertEquals(2, summary.pendingRequests());
		DashboardSummary.RegionRow kent = summary.regions().get(0);
		assertEquals(66, kent.activeCarers());
		assertEquals("NOT_STARTED", kent.next().status());
		assertNull(kent.next().rotaId());
		assertEquals(0, kent.pinsNotApplied());
	}
}
