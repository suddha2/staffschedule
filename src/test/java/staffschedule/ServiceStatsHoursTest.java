package staffschedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.midco.rota.model.DeferredSolveRequest;
import com.midco.rota.model.Employee;
import com.midco.rota.model.Rota;
import com.midco.rota.model.Shift;
import com.midco.rota.model.ShiftAssignment;
import com.midco.rota.model.ShiftAssignmentFactory;
import com.midco.rota.model.ShiftTemplate;
import com.midco.rota.model.ShiftTypeStatsDTO;
import com.midco.rota.repository.DeferredSolveRequestRepository;
import com.midco.rota.repository.RotaRepository;
import com.midco.rota.service.PaycycleStatsService;
import com.midco.rota.util.ShiftType;

/** Service stats: a 2-staff shift is two seats of one carer's hours each, not double that. */
class ServiceStatsHoursTest {

	@Test
	void aFullyStaffedTwoPersonShiftShowsNoUnallocatedHours() {
		LocalDate monday = LocalDate.of(2026, 11, 9);
		ShiftTemplate t = new ShiftTemplate();
		t.setRegion("BARNET");
		t.setLocation("Alexandra Flat G -AA");
		t.setShiftType(ShiftType.DAY);
		t.setShiftTypeCode("DAY");
		t.setStartTime(LocalTime.of(8, 0));
		t.setEndTime(LocalTime.of(20, 0));
		t.setEmpCount(2);

		List<ShiftAssignment> seats = ShiftAssignmentFactory.createAll(List.of(new Shift(monday, t, 1)));
		assertEquals(2, seats.size());
		Employee a = new Employee();
		a.setId(1);
		Employee b = new Employee();
		b.setId(2);
		seats.get(0).setEmployee(a);
		seats.get(1).setEmployee(b);

		RotaRepository rotas = mock(RotaRepository.class);
		DeferredSolveRequestRepository requests = mock(DeferredSolveRequestRepository.class);
		DeferredSolveRequest req = new DeferredSolveRequest();
		req.setStartDate(monday);
		req.setEndDate(monday.plusDays(27));
		when(requests.findByRotaId(7L)).thenReturn(req);
		when(rotas.findById(7L)).thenReturn(Optional.of(new Rota(new ArrayList<>(List.of(a, b)), seats)));

		PaycycleStatsService service = new PaycycleStatsService();
		ReflectionTestUtils.setField(service, "rotaRepository", rotas);
		ReflectionTestUtils.setField(service, "deferredSolveRequestRepository", requests);

		ShiftTypeStatsDTO day = service.generateServiceSummary(7L).get(0).services.get(0).weeks.get(0).shiftStats.get(0);
		assertEquals(0, new BigDecimal("24").compareTo(day.totalHours));
		assertEquals(0, new BigDecimal("24").compareTo(day.allocatedHours));
		assertEquals(0, BigDecimal.ZERO.compareTo(day.unallocatedHours));
		assertEquals(2, day.shiftCount);
		assertEquals(2, day.allocationCount);

		seats.get(1).setEmployee(null); // one seat open: half the hours unallocated
		day = service.generateServiceSummary(7L).get(0).services.get(0).weeks.get(0).shiftStats.get(0);
		assertEquals(0, new BigDecimal("12").compareTo(day.unallocatedHours));
	}
}
