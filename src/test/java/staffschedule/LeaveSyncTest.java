package staffschedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.midco.rota.integration.DataEngineClient;
import com.midco.rota.integration.LeaveSyncService;
import com.midco.rota.integration.PeoplePlannerProperties;
import com.midco.rota.repository.EmployeeAvailabilityRepository;

/**
 * Pure tests for the People Planner leave integration — no Spring, no network.
 * The key guarantees: it is inert until configured, and it never wipes local
 * data when the API returns nothing.
 */
class LeaveSyncTest {

	@Test
	void clientReturnsEmptyAndMakesNoCallWhenDisabled() {
		PeoplePlannerProperties props = new PeoplePlannerProperties();
		props.setEnabled(false);
		props.setBaseUrl("https://example.com"); // even with a URL, disabled = no call

		DataEngineClient client = new DataEngineClient(props);

		// No baseUrl-less/enabled config, so this must return empty without throwing.
		assertTrue(client.fetchLeave(LocalDate.now(), LocalDate.now().plusDays(30)).isEmpty());
	}

	@Test
	void clientReturnsEmptyWhenNoBaseUrl() {
		PeoplePlannerProperties props = new PeoplePlannerProperties();
		props.setEnabled(true);
		props.setBaseUrl("   "); // blank

		DataEngineClient client = new DataEngineClient(props);
		assertTrue(client.fetchLeave(LocalDate.now(), LocalDate.now().plusDays(30)).isEmpty());
	}

	@Test
	void syncAppliesNothingAndDoesNotTouchRepoWhenNoRecords() {
		DataEngineClient client = mock(DataEngineClient.class);
		EmployeeAvailabilityRepository repo = mock(EmployeeAvailabilityRepository.class);
		when(client.fetchLeave(LocalDate.of(2025, 7, 1), LocalDate.of(2025, 8, 1)))
				.thenReturn(List.of());

		LeaveSyncService sync = new LeaveSyncService(client, repo, new PeoplePlannerProperties());
		int applied = sync.syncLeave(LocalDate.of(2025, 7, 1), LocalDate.of(2025, 8, 1));

		assertEquals(0, applied, "no records => nothing applied");
		verifyNoInteractions(repo); // crucially, an empty/failed fetch must not delete good local data
	}
}
