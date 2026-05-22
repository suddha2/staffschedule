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
import com.midco.rota.repository.RotaRepository;
import com.midco.rota.util.ShiftType;

@Service
public class PaycycleStatsService {

	@Autowired
	private RotaRepository rotaRepository;

	@Autowired
	private DeferredSolveRequestRepository deferredSolveRequestRepository;

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
}