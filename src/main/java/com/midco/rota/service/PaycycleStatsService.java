package com.midco.rota.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.midco.rota.model.DeferredSolveRequest;
import com.midco.rota.model.Employee;
import com.midco.rota.model.EmployeeShiftStatDTO;
import com.midco.rota.model.EmployeeWithSummaryDTO;
import com.midco.rota.model.PaycycleStatsDTO;
import com.midco.rota.model.Rota;
import com.midco.rota.model.ServiceStatsDTO;
import com.midco.rota.model.Shift;
import com.midco.rota.model.ShiftAssignment;
import com.midco.rota.model.ShiftSummaryDTO;
import com.midco.rota.model.ShiftTemplate;
import com.midco.rota.model.ShiftTypeStatsDTO;
import com.midco.rota.model.WeekStatsDTO;
import com.midco.rota.model.WeeklyShiftStatDTO;
import com.midco.rota.repository.DeferredSolveRequestRepository;
import com.midco.rota.repository.EmployeeRepository;
import com.midco.rota.repository.RotaRepository;
import com.midco.rota.util.ShiftType;

@Service
public class PaycycleStatsService {

	@Autowired
	private RotaRepository rotaRepository;

	@Autowired
	private DeferredSolveRequestRepository deferredSolveRequestRepository;

	@Autowired
	private EmployeeRepository employeeRepository;

//    @Autowired
//    private ShiftAssignmentRepository assignmentRepository;

	public List<PaycycleStatsDTO> generateServiceSummary(Long rotaId) {

		DeferredSolveRequest deferredSolveRequest = deferredSolveRequestRepository.findByRotaId(rotaId);
		Optional<Rota> rotaOpt = rotaRepository.findById(rotaId);
		if (rotaOpt.isEmpty()) {
			return Collections.emptyList();
		}

		Rota rota = rotaOpt.get();
		List<ShiftAssignment> assignments = rota.getShiftAssignmentList();
		LocalDate startDate = deferredSolveRequest.getStartDate();

		LocalDate endDate = deferredSolveRequest.getEndDate();

		String periodId = "PC-" + startDate.getYear() + "-" + startDate.getMonthValue();

		Map<String, PaycycleStatsDTO> regionMap = new HashMap<>();

		for (ShiftAssignment assignment : assignments) {
			Shift shift = assignment.getShift();
			ShiftTemplate template = shift.getShiftTemplate();
			if (template == null)
				continue;

			// ✅ CHANGE 1: Exclude SLEEP_IN from service statistics
			if (template.getShiftType() == ShiftType.SLEEP_IN) {
				continue;
			}

			String region = template.getRegion();
			String location = template.getLocation();
			ShiftType type = template.getShiftType();
			BigDecimal hours = shift.getDurationInHours();
//			int weekIndex = (int) ChronoUnit.WEEKS.between(startDate, shift.getShiftStart());

			// Region DTO
			PaycycleStatsDTO regionStats = regionMap.computeIfAbsent(region, r -> {
				PaycycleStatsDTO dto = new PaycycleStatsDTO();
				dto.region = r;
				dto.period = startDate + ":" + endDate;
				dto.periodId = periodId;
				return dto;
			});

			// Service DTO
			ServiceStatsDTO serviceStats = regionStats.services.stream().filter(s -> s.location.equals(location))
					.findFirst().orElseGet(() -> {
						ServiceStatsDTO s = new ServiceStatsDTO();
						s.location = location;
						regionStats.services.add(s);
						return s;
					});

			// Week DTO
			int weekIndex = (int) ChronoUnit.WEEKS.between(startDate, shift.getShiftStart());
			int weekNumber = weekIndex + 1;

			WeekStatsDTO weekStats = serviceStats.weeks.stream().filter(w -> w.weekNumber == weekNumber).findFirst()
					.orElseGet(() -> {
						WeekStatsDTO ws = new WeekStatsDTO();
						ws.weekNumber = weekNumber;
						ws.start = startDate.plusWeeks(weekIndex);
						ws.end = ws.start.plusDays(6);
						serviceStats.weeks.add(ws);
						return ws;
					});

			// ShiftType DTO
			List<ShiftTypeStatsDTO> shiftStatsList = weekStats.shiftStats;

			ShiftTypeStatsDTO shiftStats = shiftStatsList.stream().filter(st -> st.shiftType.equals(type)).findFirst()
					.orElseGet(() -> {
						ShiftTypeStatsDTO st = new ShiftTypeStatsDTO();
						st.shiftType = type;
						shiftStatsList.add(st);
						return st;
					});

			BigDecimal total = hours.multiply(BigDecimal.valueOf(template.getEmpCount()));
			shiftStats.totalHours = shiftStats.totalHours.add(total);
			shiftStats.shiftCount += 1;

			if (assignment.getEmployee() != null) {
				shiftStats.allocatedHours = shiftStats.allocatedHours.add(hours);
				shiftStats.allocationCount += 1;
			}

			shiftStats.unallocatedHours = shiftStats.totalHours.subtract(shiftStats.allocatedHours);
		}

		return new ArrayList<>(regionMap.values());
	}

	public List<EmployeeShiftStatDTO> generateEmpSummary(Long rotaId) {

		DeferredSolveRequest deferredSolveRequest = deferredSolveRequestRepository.findByRotaId(rotaId);
		Optional<Rota> rotaOpt = rotaRepository.findById(rotaId);
		if (rotaOpt.isEmpty()) {
			return Collections.emptyList();
		}

		Rota rota = rotaOpt.get();
		List<Employee> employees = rota.getEmployeeList();
		List<ShiftAssignment> assignments = rota.getShiftAssignmentList();

		// Pay-cycle start anchors the week numbering. A pay cycle is a fixed 28-day
		// (4-week) period, so weeks are 1..4 measured from this date — NOT calendar
		// Mondays. Snapping to Monday would spill a mid-week-starting cycle into a
		// phantom 5th week and split the counts.
		LocalDate startDate = deferredSolveRequest.getStartDate();

		// Group assignments by employee id → weekNumber → shiftType. Keying by id
		// (not name) keeps two employees who share a name from merging.
		Map<Integer, Map<Integer, Map<ShiftType, ShiftSummaryDTO>>> empWeekMap = new HashMap<>();

		for (ShiftAssignment a : assignments) {
			Employee emp = a.getEmployee();
			if (emp == null)
				continue;

			Shift shift = a.getShift();
			ShiftType type = shift.getShiftTemplate().getShiftType();

			// Exclude SLEEP_IN from employee statistics
			if (type == ShiftType.SLEEP_IN) {
				continue;
			}

			BigDecimal hours = shift.getDurationInHours();

			int weekIndex = (int) ChronoUnit.WEEKS.between(startDate, shift.getShiftStart());
			int weekNumber = weekIndex + 1;

			empWeekMap.computeIfAbsent(emp.getId(), k -> new HashMap<>())
					.computeIfAbsent(weekNumber, k -> new HashMap<>())
					.computeIfAbsent(type, k -> new ShiftSummaryDTO()).add(hours);
		}

		// Build final DTO list
		List<EmployeeShiftStatDTO> result = new ArrayList<>();

		for (Employee emp : employees) {
			Map<Integer, Map<ShiftType, ShiftSummaryDTO>> weekMap = empWeekMap.getOrDefault(emp.getId(),
					Collections.emptyMap());

			List<WeeklyShiftStatDTO> weeklyStats = new ArrayList<>();
			for (Map.Entry<Integer, Map<ShiftType, ShiftSummaryDTO>> entry : weekMap.entrySet()) {
				int weekNumber = entry.getKey();
				LocalDate start = startDate.plusWeeks(weekNumber - 1L);
				LocalDate end = start.plusDays(6);
				weeklyStats.add(new WeeklyShiftStatDTO(weekNumber, start, end, entry.getValue()));
			}
			// Stable week order for the table/export.
			weeklyStats.sort(Comparator.comparingInt(w -> w.weekNumber));

			result.add(new EmployeeShiftStatDTO(emp.getId(), emp.getName(), emp.getContractType(),
					deferredSolveRequest.getRegion(), emp.getRateCode(), weeklyStats));
		}

		return result;
	}

	public DeferredSolveRequest getRegionPeriodDetailForRotaID(Long rotaId) {
		DeferredSolveRequest deferredSolveRequest = deferredSolveRequestRepository.findByRotaId(rotaId);
		return deferredSolveRequest;
	}

	/**
	 * Active employees whose preferred region differs from the given rota's,
	 * each with their shift-type totals (count + hours) aggregated from
	 * sibling rotas in the SAME paycycle period (same start/end date, completed).
	 *
	 * <p>An employee whose region has no completed sibling rota still appears,
	 * with an empty summary — the floating panel's Other-Regions tab uses this
	 * to let admins drag in staff from any other region.
	 *
	 * <p>SLEEP_IN shifts are excluded from totals to match
	 * {@link #generateEmpSummary} (sleep-ins don't count toward weekly limits).
	 */
	public List<EmployeeWithSummaryDTO> getOutOfRegionEmployees(Long rotaId) {
		DeferredSolveRequest current = deferredSolveRequestRepository.findByRotaId(rotaId);
		if (current == null) {
			return Collections.emptyList();
		}

		String currentRegion = current.getRegion();

		// Aggregate (employeeId -> shiftType -> {count, hours}) across all
		// other-region rotas that share this paycycle period. One employee can
		// only appear in one region's rota for a given period, so no merge
		// conflicts — but using a map keyed by id keeps it robust if that ever
		// changes.
		Map<Integer, Map<ShiftType, ShiftSummaryDTO>> summaryByEmp = new HashMap<>();

		List<DeferredSolveRequest> siblings = deferredSolveRequestRepository
				.findByStartDateAndEndDateAndCompletedAndRegionNot(
						current.getStartDate(), current.getEndDate(), true, currentRegion);

		for (DeferredSolveRequest sibling : siblings) {
			Optional<Rota> rotaOpt = rotaRepository.findById(sibling.getRotaId());
			if (rotaOpt.isEmpty()) continue;

			for (ShiftAssignment a : rotaOpt.get().getShiftAssignmentList()) {
				Employee emp = a.getEmployee();
				if (emp == null) continue;
				ShiftType type = a.getShift().getShiftTemplate().getShiftType();
				if (type == ShiftType.SLEEP_IN) continue;

				summaryByEmp
						.computeIfAbsent(emp.getId(), k -> new HashMap<>())
						.computeIfAbsent(type, k -> new ShiftSummaryDTO())
						.add(a.getShift().getDurationInHours());
			}
		}

		// Start from the full out-of-region employee roster so admins can also
		// see (and drag in) staff whose region has no sibling rota this period.
		// Those employees just get an empty summary.
		List<Employee> roster = employeeRepository.findActiveOutOfRegion(currentRegion);
		List<EmployeeWithSummaryDTO> result = new ArrayList<>(roster.size());
		for (Employee emp : roster) {
			result.add(new EmployeeWithSummaryDTO(
					emp.getId(),
					emp.getFirstName(),
					emp.getLastName(),
					emp.getContractType(),
					emp.getPreferredRegion(),
					summaryByEmp.getOrDefault(emp.getId(), Collections.emptyMap())));
		}

		// Stable order: by region, then by name — keeps the tab from shuffling
		// between renders.
		result.sort(Comparator
				.comparing((EmployeeWithSummaryDTO e) -> e.getPreferredRegion() == null ? "" : e.getPreferredRegion())
				.thenComparing(e -> (e.getFirstName() == null ? "" : e.getFirstName()) + " "
						+ (e.getLastName() == null ? "" : e.getLastName())));

		return result;
	}
}