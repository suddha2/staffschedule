package com.midco.rota;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
import com.midco.rota.model.PinnedTemplateAssignment;
import com.midco.rota.model.Rota;
import com.midco.rota.model.Shift;
import com.midco.rota.model.ShiftAssignment;
import com.midco.rota.model.ShiftTemplate;
import com.midco.rota.repository.DeferredSolveRequestRepository;
import com.midco.rota.repository.EmployeeRepository;
import com.midco.rota.repository.PinnedTemplateAssignmentRepository;
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
	private final PinnedTemplateAssignmentRepository pinnedTemplateAssignmentRepository;

	public SolverTrigger(SolverService solverService, DeferredSolveRequestRepository deferredSolveRequestRepository,
			EmployeeRepository employeeRepository, ShiftTemplateRepository shiftTemplateRepository,
			PeriodService periodService, PinnedTemplateAssignmentRepository pinnedTemplateAssignmentRepository) {
		this.solverService = solverService;
		this.deferredSolveRequestRepository = deferredSolveRequestRepository;
		this.employeeRepository = employeeRepository;
		this.shiftTemplateRepository = shiftTemplateRepository;
		this.periodService = periodService;
		this.pinnedTemplateAssignmentRepository = pinnedTemplateAssignmentRepository;
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

		applyTemplateBasedPinning(shiftAssignments, employees);
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

	/**
	 * Apply pins from pinned_template_assignment table
	 */
	private void applyTemplateBasedPinning(List<ShiftAssignment> shiftAssignments, List<Employee> employees) {
		int pinnedCount = 0;
		int skippedInactive = 0;
		int skippedConflict = 0;

		logger.info("Starting template-based pinning for {} assignments", shiftAssignments.size());

		// Load all pins
		List<PinnedTemplateAssignment> allPins = pinnedTemplateAssignmentRepository.findAll();
		logger.info("Found {} pinned template assignments", allPins.size());

		// Build employee lookup map
		Map<Integer, Employee> employeeMap = new HashMap<>();
		for (Employee emp : employees) {
			employeeMap.put(emp.getId(), emp);
		}

		// Group pins by template ID for faster lookup
		Map<Long, List<PinnedTemplateAssignment>> pinsByTemplate = new HashMap<>();
		for (PinnedTemplateAssignment pin : allPins) {
			pinsByTemplate.computeIfAbsent(pin.getShiftTemplateId(), k -> new ArrayList<>()).add(pin);
		}

		// Track assignments per employee per day (for conflict detection)
		Map<String, Set<ShiftType>> employeeDayAssignments = new HashMap<>();

		// Apply pins to matching shift assignments
		for (ShiftAssignment assignment : shiftAssignments) {
			Shift shift = assignment.getShift();
			Long templateId = shift.getShiftTemplate().getId().longValue();

			// Check if this template has any pins
			List<PinnedTemplateAssignment> pinsForTemplate = pinsByTemplate.get(templateId);
			if (pinsForTemplate == null || pinsForTemplate.isEmpty()) {
				continue; // No pins for this template
			}

			// Try each pinned employee for this template
			boolean assigned = false;
			for (PinnedTemplateAssignment pin : pinsForTemplate) {
				Integer empId = pin.getEmployeeId().intValue();
				Employee employee = employeeMap.get(empId);

				// Check 1: Employee exists and is active
				if (employee == null || !employee.isActive()) {
					skippedInactive++;
					continue;
				}

				// Check 2: Same-day conflict detection
				String conflictKey = empId + "-" + shift.getShiftStart();
				Set<ShiftType> existingShifts = employeeDayAssignments.get(conflictKey);

				if (existingShifts != null && !existingShifts.isEmpty()) {
					// Employee already has a shift this day - check if allowed
					List<ShiftType> todaysShifts = new ArrayList<>(existingShifts);
					todaysShifts.add(shift.getShiftTemplate().getShiftType());

					if (!isAllowedDayShiftTypes(todaysShifts)) {
						logger.debug("⚠️ Skipping pin: {} already assigned conflicting shift on {}", employee.getName(),
								shift.getShiftStart());
						skippedConflict++;
						continue;
					}
				}

				// ✅ All checks passed - apply pin
				assignment.setEmployee(employee);
				assignment.setPinned(true);
				pinnedCount++;
				assigned = true;

				// Track for conflict detection
				employeeDayAssignments.computeIfAbsent(conflictKey, k -> new HashSet<>())
						.add(shift.getShiftTemplate().getShiftType());

				logger.debug("✅ PINNED: {} to {} {} {}", employee.getName(), shift.getShiftTemplate().getLocation(),
						shift.getShiftStart(), shift.getShiftTemplate().getShiftType());

				break; // Stop after first successful pin
			}
		}

		logger.info("Template pinning complete:");
		logger.info("  ✅ Successfully pinned: {}", pinnedCount);
		logger.info("  ⚠️ Skipped (inactive employee): {}", skippedInactive);
		logger.info("  ⚠️ Skipped (same-day conflict): {}", skippedConflict);
		logger.info("  📋 Unassigned (for solver): {}", shiftAssignments.size() - pinnedCount);
	}

	/**
	 * Check if shift types are allowed on same day Uses same business rules as
	 * PinValidationService
	 */
	private boolean isAllowedDayShiftTypes(List<ShiftType> types) {
		if (types == null || types.isEmpty())
			return true;
		if (types.size() == 1)
			return true;

		// All FLOATING - check would need location info (skip for now, assume valid)
		boolean allFloating = types.stream().allMatch(t -> t == ShiftType.FLOATING);
		if (allFloating) {
			return true; // Can't check locations here
		}

		// No mixing FLOATING with non-FLOATING
		boolean hasFloating = types.stream().anyMatch(t -> t == ShiftType.FLOATING);
		boolean hasNonFloating = types.stream().anyMatch(t -> t == ShiftType.DAY || t == ShiftType.LONG_DAY
				|| t == ShiftType.WAKING_NIGHT || t == ShiftType.SLEEP_IN);

		if (hasFloating && hasNonFloating) {
			return false;
		}

		// Exactly 2: LONG_DAY + SLEEP_IN allowed
		if (types.size() == 2) {
			boolean hasLongDay = types.contains(ShiftType.LONG_DAY);
			boolean hasSleepIn = types.contains(ShiftType.SLEEP_IN);
			return hasLongDay && hasSleepIn;
		}

		// Any other 2+ non-floating combo is invalid
		return false;
	}

	private record YearWeek(int year, int week) {
		public static YearWeek from(LocalDate date) {
			java.time.temporal.WeekFields wf = java.time.temporal.WeekFields.ISO;
			return new YearWeek(date.get(wf.weekBasedYear()), date.get(wf.weekOfWeekBasedYear()));
		}
	}
}