package staffschedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.midco.rota.integration.DataEngineClient;
import com.midco.rota.integration.LeaveRecord;
import com.midco.rota.integration.LeaveSource;
import com.midco.rota.integration.LeaveSyncService;
import com.midco.rota.integration.PeopleHrClient;
import com.midco.rota.integration.PeopleHrProperties;
import com.midco.rota.integration.PeoplePlannerProperties;
import com.midco.rota.model.Employee;
import com.midco.rota.model.EmployeeAvailability;
import com.midco.rota.repository.EmployeeAvailabilityRepository;
import com.midco.rota.repository.EmployeeRepository;
import com.midco.rota.util.AvailabilitySource;

/**
 * Multi-source leave sync — no Spring, no network. Guarantees: disabled sources
 * do nothing; an empty/failed fetch never wipes local data; a record is matched
 * to a live employee by the source's own id first (email fallback); unknown
 * employees are skipped (not written); each source's rows carry its own tag.
 */
class LeaveSyncTest {

	private final LocalDate from = LocalDate.of(2025, 7, 1);
	private final LocalDate to = LocalDate.of(2025, 8, 1);

	/** record identified by the source's own employee id (no email). */
	private LeaveRecord byId(String sourceId, String ref) {
		return new LeaveRecord(sourceId, null, LocalDate.of(2025, 7, 10), LocalDate.of(2025, 7, 12),
				"Holiday", ref, null, false);
	}

	/** record identified only by email (external id not populated yet). */
	private LeaveRecord byEmail(String email, String ref) {
		return new LeaveRecord(null, email, LocalDate.of(2025, 7, 10), LocalDate.of(2025, 7, 12),
				"Holiday", ref, null, false);
	}

	private Employee emp(int id, String email, String ppId, String hrId) {
		Employee e = new Employee();
		e.setId(id);
		e.setEmail(email);
		e.setPpEmployeeId(ppId);
		e.setPeopleHrEmployeeId(hrId);
		return e;
	}

	private EmployeeRepository empRepoWith(Employee... employees) {
		EmployeeRepository repo = mock(EmployeeRepository.class);
		when(repo.findAll()).thenReturn(List.of(employees));
		return repo;
	}

	@Test
	void bothClientsReturnEmptyWhenDisabled() {
		PeoplePlannerProperties pp = new PeoplePlannerProperties();
		pp.setEnabled(false);
		pp.setBaseUrl("https://pp.example");
		assertTrue(new DataEngineClient(pp).fetch(from, to).isEmpty());

		PeopleHrProperties hr = new PeopleHrProperties();
		hr.setEnabled(true);
		hr.setBaseUrl("   ");
		assertTrue(!new PeopleHrClient(hr).isEnabled());
	}

	@Test
	void syncAllSkipsDisabledSourcesAndTouchesNothing() {
		LeaveSource off = mock(LeaveSource.class);
		when(off.isEnabled()).thenReturn(false);
		EmployeeAvailabilityRepository repo = mock(EmployeeAvailabilityRepository.class);

		int applied = new LeaveSyncService(List.of(off), repo, empRepoWith(), new PeoplePlannerProperties())
				.syncAll(from, to);

		assertEquals(0, applied);
		verify(off, never()).fetch(any(), any());
		verifyNoInteractions(repo);
	}

	@Test
	void emptyFetchDoesNotWipeLocalData() {
		LeaveSource src = mock(LeaveSource.class);
		when(src.source()).thenReturn(AvailabilitySource.PP_API);
		when(src.fetch(from, to)).thenReturn(List.of());
		EmployeeAvailabilityRepository repo = mock(EmployeeAvailabilityRepository.class);

		int applied = new LeaveSyncService(List.of(src), repo,
				empRepoWith(emp(1, "a@x", "PP1", null)), new PeoplePlannerProperties()).syncOne(src, from, to);

		assertEquals(0, applied);
		verifyNoInteractions(repo);
	}

	@Test
	void matchesBySourceIdPerSourceColumn() {
		// PP feed matches on pp_employee_id; HR feed on peoplehr_employee_id.
		LeaveSource pp = mock(LeaveSource.class);
		when(pp.isEnabled()).thenReturn(true);
		when(pp.source()).thenReturn(AvailabilitySource.PP_API);
		when(pp.fetch(from, to)).thenReturn(List.of(byId("PP-500", "PPU-1")));

		LeaveSource hr = mock(LeaveSource.class);
		when(hr.isEnabled()).thenReturn(true);
		when(hr.source()).thenReturn(AvailabilitySource.HR_API);
		when(hr.fetch(from, to)).thenReturn(List.of(byId("HR-900", "HOL-1")));

		EmployeeAvailabilityRepository repo = mock(EmployeeAvailabilityRepository.class);
		EmployeeRepository emps = empRepoWith(
				emp(11, "alice@care.com", "PP-500", null),   // zero-hours: PP id
				emp(22, "bob@care.com", null, "HR-900"));     // contracted: PeopleHR id

		int applied = new LeaveSyncService(List.of(pp, hr), repo, emps, new PeoplePlannerProperties()).syncAll(from, to);

		assertEquals(2, applied);
		ArgumentCaptor<EmployeeAvailability> saved = ArgumentCaptor.forClass(EmployeeAvailability.class);
		verify(repo, org.mockito.Mockito.times(2)).save(saved.capture());
		EmployeeAvailability ppRow = saved.getAllValues().stream()
				.filter(a -> a.getSource() == AvailabilitySource.PP_API).findFirst().orElseThrow();
		EmployeeAvailability hrRow = saved.getAllValues().stream()
				.filter(a -> a.getSource() == AvailabilitySource.HR_API).findFirst().orElseThrow();
		assertEquals(11, ppRow.getEmployeeId(), "PP id resolved to live 11");
		assertEquals(22, hrRow.getEmployeeId(), "PeopleHR id resolved to live 22");
	}

	@Test
	void fallsBackToEmailWhenExternalIdNotStored() {
		LeaveSource src = mock(LeaveSource.class);
		when(src.isEnabled()).thenReturn(true);
		when(src.source()).thenReturn(AvailabilitySource.PP_API);
		when(src.fetch(from, to)).thenReturn(List.of(byEmail("Carol@Care.com", "PPU-2")));
		EmployeeAvailabilityRepository repo = mock(EmployeeAvailabilityRepository.class);
		EmployeeRepository emps = empRepoWith(emp(33, "carol@care.com", null, null)); // no external id yet

		int applied = new LeaveSyncService(List.of(src), repo, emps, new PeoplePlannerProperties())
				.syncOne(src, from, to);

		assertEquals(1, applied);
		ArgumentCaptor<EmployeeAvailability> saved = ArgumentCaptor.forClass(EmployeeAvailability.class);
		verify(repo).save(saved.capture());
		assertEquals(33, saved.getValue().getEmployeeId(), "email fallback resolved case-insensitively");
	}

	@Test
	void unresolvedEmployeeIsSkippedNotWritten() {
		LeaveSource src = mock(LeaveSource.class);
		when(src.isEnabled()).thenReturn(true);
		when(src.source()).thenReturn(AvailabilitySource.PP_API);
		when(src.fetch(from, to)).thenReturn(List.of(byId("PP-UNKNOWN", "PPU-3")));
		EmployeeAvailabilityRepository repo = mock(EmployeeAvailabilityRepository.class);

		int applied = new LeaveSyncService(List.of(src), repo,
				empRepoWith(emp(1, "real@care.com", "PP-1", null)), new PeoplePlannerProperties())
				.syncOne(src, from, to);

		assertEquals(0, applied);
		verify(repo, never()).save(any());
	}

	@Test
	void cancelledRecordDeletesExistingRow() {
		LeaveSource src = mock(LeaveSource.class);
		when(src.source()).thenReturn(AvailabilitySource.HR_API);
		when(src.fetch(from, to)).thenReturn(List.of(
				new LeaveRecord("HR-900", null, LocalDate.of(2025, 7, 10), LocalDate.of(2025, 7, 12),
						"Holiday", "HOL-5", null, true)));
		EmployeeAvailability existing = new EmployeeAvailability();
		EmployeeAvailabilityRepository repo = mock(EmployeeAvailabilityRepository.class);
		when(repo.findBySourceAndExternalRef(eq(AvailabilitySource.HR_API), eq("HOL-5"))).thenReturn(existing);

		new LeaveSyncService(List.of(src), repo, empRepoWith(emp(22, "bob@care.com", null, "HR-900")),
				new PeoplePlannerProperties()).syncOne(src, from, to);

		verify(repo).delete(existing);
		verify(repo, never()).save(any());
	}
}
