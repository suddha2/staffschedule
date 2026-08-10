package com.midco.rota;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import com.midco.rota.model.ShiftAssignmentFactory;
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

	// Serialises solve scheduling. Without it, two near-simultaneous enqueues
	// could both pass the getSolverStatus guard for the same (oldest) request
	// and call SolverManager.solve twice with the same problemId, which throws.
	private final java.util.concurrent.locks.ReentrantLock triggerLock = new java.util.concurrent.locks.ReentrantLock();

	/**
	 * Fallback / backlog drain. Runs every 2 minutes and picks up anything the
	 * immediate {@link #triggerSolverAsync()} kick missed — e.g. a request
	 * enqueued while another was still solving, an async failure, or requests
	 * left pending across a restart.
	 */
	@Scheduled(cron = "0 */2 * * * *") // Every 2 Mins
	public void triggerSolver() {
		processNextPendingRequest();
	}

	/**
	 * Immediate kick, invoked right after a request is enqueued so a solve
	 * starts without waiting for the ≤2-minute cron tick. Runs off the HTTP
	 * request thread via the shared security-aware async executor; the cron
	 * above remains the safety net.
	 */
	@org.springframework.scheduling.annotation.Async("applicationTaskExecutor")
	public void triggerSolverAsync() {
		processNextPendingRequest();
	}

	private void processNextPendingRequest() {
		// If a scheduling pass is already running, skip: the in-flight pass (or
		// the next cron tick) will handle whatever is still pending.
		if (!triggerLock.tryLock()) {
			logger.info("Solve scheduling already in progress; skipping this trigger");
			return;
		}
		try {
			deferredSolveRequestRepository.findFirstByCompletedFalse().ifPresentOrElse(deferredSolveRequest -> {
			if (solverService.getSolverStatus(deferredSolveRequest.getId()) != SolverStatus.NOT_SOLVING) {
				logger.info("Solving in progress for request {}", deferredSolveRequest.getId());
				return;
			}

			// Fail-fast: a region with zero employees crashes the Rota
			// constructor with ArithmeticException (/ by zero) and would
			// otherwise keep the request stuck on completed=false, so the
			// scheduler picks it up on every 2-minute tick and loops
			// forever. Close it with an explanatory summary instead.
			List<Employee> employees = employeeRepository.findByPreferredRegion(deferredSolveRequest.getRegion());
			if (employees.isEmpty()) {
				// scheduleSummary is structured (Map<String,Map<String,Integer>>) and
				// can't carry a free-text reason; we log the explanation instead and
				// rely on completed=true + a null summary to mark this as
				// "closed without producing a rota".
				logger.warn("Refusing to solve request {} ({}): no employees with preferred_region='{}'. " +
						"Marking the request as completed without producing a rota.",
						deferredSolveRequest.getId(),
						deferredSolveRequest.getRegion(),
						deferredSolveRequest.getRegion());
				deferredSolveRequest.setCompleted(true);
				deferredSolveRequest.setCompletedAt(LocalDateTime.now());
				deferredSolveRequestRepository.save(deferredSolveRequest);
				return;
			}

			Rota problem = loadData(deferredSolveRequest);
			problem.setPlanningId(deferredSolveRequest.getId());
			logger.info("triggerSolver=== ");
			solverService.solveAsync(problem, deferredSolveRequest.getId(), deferredSolveRequest);
		}, () -> {
				logger.info("No solver request available to process");
			});
		} finally {
			triggerLock.unlock();
		}
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
		// Link LONG_DAY → SLEEP_IN so the shadow-variable listener mirrors employees.
		ShiftAssignmentFactory.linkSleepInPairs(shiftAssignments);
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
				ShiftAssignment assignment = ShiftAssignmentFactory.create(shift);
				assignments.add(assignment);
			}
		}
		return assignments;

	}

	/**
	 * Apply pins from pinned_template_assignment table.
	 *
	 * <p>Pins are looked up by {@code (shift_template_id, week_of_period)} — see
	 * PinnedTemplateAssignment for why this is week-specific rather than flat.
	 * The period anchor (week 1's Monday) is the earliest shift date in the
	 * batch being solved; week numbers are derived per shift by integer division
	 * of (days since periodStart) by 7.
	 */
	private void applyTemplateBasedPinning(List<ShiftAssignment> shiftAssignments, List<Employee> employees) {
		int pinnedCount = 0;
		int skippedInactive = 0;
		int skippedConflict = 0;

		logger.info("Starting template-based pinning for {} assignments", shiftAssignments.size());

		// Determine the period anchor for this solve batch. Periods are
		// Mon-aligned 28-day blocks in this dataset, so the earliest shift date
		// is week 1's Monday.
		LocalDate periodStart = shiftAssignments.stream()
				.map(ShiftAssignment::getShift)
				.filter(Objects::nonNull)
				.map(Shift::getShiftStart)
				.filter(Objects::nonNull)
				.min(LocalDate::compareTo)
				.orElse(null);

		if (periodStart == null) {
			logger.warn("No dated shifts in batch - skipping template-based pinning");
			return;
		}
		logger.info("Period anchor for pinning: {} (week 1 Monday)", periodStart);

		// Load all pins
		List<PinnedTemplateAssignment> allPins = pinnedTemplateAssignmentRepository.findAll();
		logger.info("Found {} pinned template assignments", allPins.size());

		// Build employee lookup map
		Map<Integer, Employee> employeeMap = new HashMap<>();
		for (Employee emp : employees) {
			employeeMap.put(emp.getId(), emp);
		}

		// Group pins by (templateId, weekOfPeriod) for direct lookup. Pins
		// missing weekOfPeriod (legacy rows before the migration) default to
		// week 1, which is a safe fallback that preserves the old "apply on
		// every occurrence" intent only for the week 1 occurrence — admins
		// will need to resave each period once to populate proper week pins.
		Map<String, List<PinnedTemplateAssignment>> pinsByTemplateAndWeek = new HashMap<>();
		for (PinnedTemplateAssignment pin : allPins) {
			Short week = pin.getWeekOfPeriod() != null ? pin.getWeekOfPeriod() : (short) 1;
			String key = pin.getShiftTemplateId() + ":" + week;
			pinsByTemplateAndWeek.computeIfAbsent(key, k -> new ArrayList<>()).add(pin);
		}

		// Track assignments per employee per day (for conflict detection)
		Map<String, Set<ShiftType>> employeeDayAssignments = new HashMap<>();

		// Apply pins to matching shift assignments
		for (ShiftAssignment assignment : shiftAssignments) {
			Shift shift = assignment.getShift();
			if (shift == null || shift.getShiftTemplate() == null || shift.getShiftStart() == null) {
				continue;
			}
			Long templateId = shift.getShiftTemplate().getId().longValue();
			Short shiftWeek = weekOfPeriod(periodStart, shift.getShiftStart());
			String lookupKey = templateId + ":" + shiftWeek;

			// Check if this template/week combination has any pins
			List<PinnedTemplateAssignment> pinsForTemplate = pinsByTemplateAndWeek.get(lookupKey);
			if (pinsForTemplate == null || pinsForTemplate.isEmpty()) {
				continue; // No pins for this (template, week)
			}

			// Try each pinned employee for this template/week
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
	 * 1-based week index of {@code shiftDate} within a period anchored at
	 * {@code periodStart} (which should be week 1's Monday). Integer-divides
	 * (shiftDate - periodStart) by 7 so any date in week K returns K. Negative
	 * deltas (shift before periodStart) clamp to week 1.
	 */
	private static Short weekOfPeriod(LocalDate periodStart, LocalDate shiftDate) {
		long days = ChronoUnit.DAYS.between(periodStart, shiftDate);
		if (days < 0) {
			return (short) 1;
		}
		return (short) ((days / 7) + 1);
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