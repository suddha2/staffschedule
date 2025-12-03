package com.midco.rota.service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

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

		// Group assignments by employee → weekStart → shiftType
		Map<String, Map<LocalDate, Map<ShiftType, ShiftSummaryDTO>>> empWeekMap = new HashMap<>();

		for (ShiftAssignment a : assignments) {
			Employee emp = a.getEmployee();
			if (emp == null)
				continue;

			String name = emp.getName();
			Shift shift = a.getShift();
			ShiftType type = shift.getShiftTemplate().getShiftType();

			// ✅ CHANGE 2: Exclude SLEEP_IN from employee statistics
			if (type == ShiftType.SLEEP_IN) {
				continue;
			}

			BigDecimal hours = shift.getDurationInHours(); // must be BigDecimal

			LocalDate shiftDate = shift.getShiftStart();
			LocalDate weekStart = shiftDate.with(DayOfWeek.MONDAY);
			LocalDate weekEnd = weekStart.plusDays(6);

			empWeekMap.computeIfAbsent(name, k -> new HashMap<>()).computeIfAbsent(weekStart, k -> new HashMap<>())
					.computeIfAbsent(type, k -> new ShiftSummaryDTO()).add(hours);
		}

		// Collect all unique weekStart dates and assign sequential week numbers
		AtomicInteger counter = new AtomicInteger(1);

		Map<LocalDate, Integer> weekNumberMap = empWeekMap.values().stream()
				.flatMap(weekMap -> weekMap.keySet().stream()).distinct().sorted()
				.collect(Collectors.toMap(Function.identity(), // key: weekStart
						key -> counter.getAndIncrement(), // value: sequential week number
						(a, b) -> a, // merge function (not needed here)
						LinkedHashMap::new // preserve insertion order
				));

		// Build final DTO list
		List<EmployeeShiftStatDTO> result = new ArrayList<>();

		for (Employee emp : employees) {
			String name = emp.getName();
			Map<LocalDate, Map<ShiftType, ShiftSummaryDTO>> weekMap = empWeekMap.getOrDefault(name, new HashMap<>());

			List<WeeklyShiftStatDTO> weeklyStats = new ArrayList<>();
			for (Map.Entry<LocalDate, Map<ShiftType, ShiftSummaryDTO>> entry : weekMap.entrySet()) {
				LocalDate start = entry.getKey();
				LocalDate end = start.plusDays(6);
				int weekNumber = weekNumberMap.get(start);

				weeklyStats.add(new WeeklyShiftStatDTO(weekNumber, start, end, entry.getValue()));
			}

			result.add(new EmployeeShiftStatDTO(name, emp.getContractType(), deferredSolveRequest.getRegion(),
					emp.getRateCode(), weeklyStats));
		}

		return result;
	}

	public DeferredSolveRequest getRegionPeriodDetailForRotaID(Long rotaId) {
		DeferredSolveRequest deferredSolveRequest = deferredSolveRequestRepository.findByRotaId(rotaId);
		return deferredSolveRequest;
	}
}