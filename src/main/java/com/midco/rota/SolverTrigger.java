package com.midco.rota;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.optaplanner.core.api.solver.SolverStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.midco.rota.model.DeferredSolveRequest;
import com.midco.rota.model.Employee;
import com.midco.rota.model.EmployeeSchedulePattern;
import com.midco.rota.model.Rota;
import com.midco.rota.model.Shift;
import com.midco.rota.model.ShiftAssignment;
import com.midco.rota.model.ShiftTemplate;
import com.midco.rota.repository.DeferredSolveRequestRepository;
import com.midco.rota.repository.EmployeeRepository;
import com.midco.rota.repository.ShiftTemplateRepository;
import com.midco.rota.service.PeriodService;
import com.midco.rota.service.SolverService;
import com.midco.rota.util.ShiftType;

@Component

public class SolverTrigger {

	private static final Logger logger = LoggerFactory.getLogger(SolverTrigger.class);

	private final SolverService solverService;
	private final DeferredSolveRequestRepository deferredSolveRequestRepository;
	private final EmployeeRepository employeeRepository;
	private final ShiftTemplateRepository shiftTemplateRepository;
	private final PeriodService periodService;

	public SolverTrigger(SolverService solverService, DeferredSolveRequestRepository deferredSolveRequestRepository,
			EmployeeRepository employeeRepository, ShiftTemplateRepository shiftTemplateRepository,
			PeriodService periodService) {
		this.solverService = solverService;
		this.deferredSolveRequestRepository = deferredSolveRequestRepository;
		this.employeeRepository = employeeRepository;
		this.shiftTemplateRepository = shiftTemplateRepository;
		this.periodService = periodService;
	}

	@Scheduled(cron = "0 */2 * * * *") // Every 2 Mins

	public void triggerSolver() {
		deferredSolveRequestRepository.findFirstByCompletedFalse().ifPresentOrElse(deferredSolveRequest -> {
			if (solverService.getSolverStatus(deferredSolveRequest.getId()) == SolverStatus.NOT_SOLVING) {

				Rota problem = loadData(deferredSolveRequest);
				problem.setPlanningId(deferredSolveRequest.getId());
				logger.info("triggerSolver=== ");
				solverService.solveAsync(problem, deferredSolveRequest.getId(), deferredSolveRequest);
			} else {
				logger.info("Solving in progress for reqeust " + deferredSolveRequest.getId());
			}
		}, () -> {
			logger.info("No solver request available to process");
		});
	}

	public Rota loadData(DeferredSolveRequest deferredSolveRequest) {
		logger.info(deferredSolveRequest.toString());
		// Query all employees
		List<Employee> employees = employeeRepository.findByPreferredRegion(deferredSolveRequest.getRegion());
		List<ShiftTemplate> shiftTemplates = shiftTemplateRepository.findAllByRegion(deferredSolveRequest.getRegion());
		List<ShiftAssignment> shiftAssignments = new ArrayList<>();

		shiftAssignments = this.generateShiftInstances(deferredSolveRequest.getStartDate(),
				deferredSolveRequest.getEndDate(), shiftTemplates);

//		applyPatternBasedPinning(shiftAssignments);
		Rota problem = new Rota(employees, shiftAssignments);

		return problem;
	}

	private List<ShiftAssignment> generateShiftInstances(LocalDate startDate, LocalDate endDate,
			List<ShiftTemplate> templates) {

		List<ShiftAssignment> assignments = new ArrayList<>();
		List<Shift> instances = new ArrayList<>();

		// Create shift instance for the date range
		AtomicLong id = new AtomicLong(1L);
		for (ShiftTemplate template : templates) {
			LocalDate current = startDate;
			while (!current.isAfter(endDate)) {
				if (current.getDayOfWeek().equals(template.getDayOfWeek())) {
					instances.add(new Shift(current, template, periodService.getAbsoluteWeekNumber(current)));

				}
				current = current.plusDays(1);

			}
		}

		// Add shift assignments to enable multiple employee assignment for each shift (
		// 2 to 1 scenario)
		for (Shift shift : instances) {
			for (int i = 0; i < shift.getShiftTemplate().getEmpCount(); i++) {
				ShiftAssignment assignment = new ShiftAssignment(shift);
				assignments.add(assignment);
			}
		}
		return assignments;

	}

//	private void applyPatternBasedPinning(List<ShiftAssignment> shiftAssignments) {
//		for (ShiftAssignment assignment : shiftAssignments) {
//
//			// Find employee with matching pattern
//			List<EmployeeSchedulePattern> pattern = employeeRepository.findByPinnedEmp(
//					assignment.getShift().getShiftTemplate().getLocation(),
//					periodService.calculateWeekNumber(assignment.getShift().getShiftStart()),
//					assignment.getShift().getShiftTemplate().getDayOfWeek().name(),
//					assignment.getShift().getShiftTemplate().getShiftType());
//
//
//			if (pattern != null&& !pattern.isEmpty() && Boolean.TRUE.equals(pattern.getFirst().getIsAvailable())) {
//				assignment.setEmployee(pattern.getFirst().getEmployee());
//				assignment.setPinned(true); // ✅ PIN IT!
//			}
//		}
//	}
	private void applyPatternBasedPinning(List<ShiftAssignment> shiftAssignments) {
		int pinnedCount = 0;
		int rejectedDueToConflict = 0;
		int rejectedByMaxHours = 0; // ✅ NEW counter

		// Track what's already pinned per employee per day per location
		Map<String, Set<String>> pinnedEmployeeDayLocation = new HashMap<>();

		// ✅ NEW: Track hours per employee per week
		Map<Integer, Map<YearWeek, Long>> employeeWeeklyHours = new HashMap<>();

		logger.info("Starting pattern-based pinning for {} assignments", shiftAssignments.size());

		for (ShiftAssignment assignment : shiftAssignments) {
			Shift shift = assignment.getShift();
			LocalDate date = shift.getShiftStart();
			String location = shift.getShiftTemplate().getLocation();
			ShiftType shiftType = shift.getShiftTemplate().getShiftType();
			String dayOfWeek = shift.getShiftTemplate().getDayOfWeek().name();

			int weekInCycle = periodService.calculateWeekNumber(date);

			if (weekInCycle == 0) {
				continue;
			}

			List<EmployeeSchedulePattern> patterns = employeeRepository.findByPinnedEmp(location, weekInCycle%4,
					dayOfWeek, shiftType);

			if (patterns == null || patterns.isEmpty()) {
				continue;
			}

			boolean assigned = false;

			for (EmployeeSchedulePattern pattern : patterns) {
				Employee employee = pattern.getEmployee();
				Integer empId = employee.getId();
				String empName = employee.getName();

				// CHECK 1: Availability
				if (!Boolean.TRUE.equals(pattern.getIsAvailable())) {
					continue;
				}

				// CHECK 2: canWorkShift
				if (!employee.canWorkShift(location, date, shiftType)) {
					continue;
				}

				// CHECK 3: Same day/location conflict
				String conflictKey = empId + "-" + date + "-" + location;

				if (pinnedEmployeeDayLocation.containsKey(conflictKey)) {
					logger.debug("⚠️ Skipping {}: already pinned to another shift on {} at {}", empName, date,
							location);
					rejectedDueToConflict++;
					continue;
				}

				// ✅ CHECK 4 (NEW): Max hours check
				if (employee.getMaxHrs() != null) {
					YearWeek week = YearWeek.from(date);

					// Get current hours for this employee this week
					long currentMins = employeeWeeklyHours.getOrDefault(empId, new HashMap<>()).getOrDefault(week, 0L);

					// Calculate what total would be if we pin this shift
					long shiftMins = shift.getDurationInMins();
					long totalMins = currentMins + shiftMins;
					long maxMins = employee.getMaxHrs().longValue() * 60;

					// Would this exceed max hours?
					if (totalMins > maxMins) {
						logger.debug(
								"⚠️ Skipping {}: would exceed max hours "
										+ "({} current + {} shift = {} total > {} max)",
								empName, currentMins / 60, shiftMins / 60, totalMins / 60, maxMins / 60);
						rejectedByMaxHours++;
						continue; // Would exceed max hours, try next employee
					}
				}

				// ✅ All checks passed - pin and track
				assignment.setEmployee(employee);
				assignment.setPinned(true);
				pinnedCount++;
				assigned = true;

				// Track for same-day conflict detection
				pinnedEmployeeDayLocation.put(conflictKey, Set.of(shiftType.toString()));

				// ✅ NEW: Track hours for max hours check
				YearWeek week = YearWeek.from(date);
				employeeWeeklyHours.computeIfAbsent(empId, k -> new HashMap<>()).merge(week, shift.getDurationInMins(),
						Long::sum);

				// Log with current hours
				long totalHours = employeeWeeklyHours.get(empId).get(week) / 60;
				logger.debug("✅ PINNED: {} to {} {} week {} {} (now at {} hours this week)", empName, location,
						dayOfWeek, weekInCycle, shiftType, totalHours);

				break;
			}
		}

		logger.info("Pattern pinning complete:");
		logger.info("  ✅ Successfully pinned: {}", pinnedCount);
		logger.info("  ⚠️ Rejected (same day/location conflict): {}", rejectedDueToConflict);
		logger.info("  ⚠️ Rejected (would exceed max hours): {}", rejectedByMaxHours); // ✅ NEW
		logger.info("  📋 Unassigned (for solver): {}", shiftAssignments.size() - pinnedCount);
	}

	private record YearWeek(int year, int week) {
		public static YearWeek from(LocalDate date) {
			java.time.temporal.WeekFields wf = java.time.temporal.WeekFields.ISO;
			return new YearWeek(date.get(wf.weekBasedYear()), date.get(wf.weekOfWeekBasedYear()));
		}
	}
}