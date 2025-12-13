package com.midco.rota.controller;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.optaplanner.core.api.score.constraint.ConstraintMatchTotal;
import org.optaplanner.core.api.solver.SolverJob;
import org.optaplanner.core.api.solver.SolverManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.midco.rota.model.DeferredSolveRequest;
import com.midco.rota.model.Employee;
import com.midco.rota.model.Rota;
import com.midco.rota.model.RotaCorrection;
import com.midco.rota.model.Shift;
import com.midco.rota.model.ShiftAssignment;
import com.midco.rota.model.ShiftTemplate;
import com.midco.rota.repository.DeferredSolveRequestRepository;
import com.midco.rota.repository.EmployeeRepository;
import com.midco.rota.repository.RotaCorrectionRepository;
import com.midco.rota.repository.RotaRepository;
import com.midco.rota.repository.ShiftAssignmentRepository;
import com.midco.rota.repository.ShiftRepository;
import com.midco.rota.repository.ShiftTemplateRepository;
import com.midco.rota.service.ConstraintExplanationService;
import com.midco.rota.service.PayCycleDataService;
import com.midco.rota.service.PeriodService;
import com.midco.rota.service.RosterAnalysisService;
import com.midco.rota.service.RosterUpdateService;
import com.midco.rota.util.PayCycleRow;

@RestController
@RequestMapping("/api")
public class RotaController {

	private final PeriodService periodService;

	private final ShiftRepository shiftRepository;

	private final AuthController authController;

	private final SolverManager<Rota, Long> solverManager;
	private final RosterUpdateService updateService;
	private final ConstraintExplanationService explanationService;
	private final RosterAnalysisService rosterAnalysisService;
	private final EmployeeRepository employeeRepository;
	private final ShiftTemplateRepository shiftTemplateRepository;
	private final RotaRepository rotaRepository;
	private final PayCycleDataService payCycleDataService;
	private final DeferredSolveRequestRepository deferredSolveRequestRepository;
	private final RotaCorrectionRepository rotaCorrectionRepository;
	private final ShiftAssignmentRepository shiftAssignmentRepository;
	// private final ExecutorService securityExecutorService;
	@Autowired
	private SimpUserRegistry simpUserRegistry;

	public RotaController(SolverManager<Rota, Long> solverManager, RosterUpdateService updateService,
			ConstraintExplanationService explanationService, RosterAnalysisService rosterAnalysisService,
			EmployeeRepository employeeRepository, ShiftTemplateRepository shiftTemplateRepository,
			DeferredSolveRequestRepository deferredSolveRequestRepository, AuthController authController,
			RotaRepository rotaRepository, ShiftRepository shiftRepository, PayCycleDataService payCycleDataService,
			PeriodService periodService, RotaCorrectionRepository rotaCorrectionRepository,
			ShiftAssignmentRepository shiftAssignmentRepository) {
		this.solverManager = solverManager;
		this.updateService = updateService;
		this.explanationService = explanationService;
		this.rosterAnalysisService = rosterAnalysisService;
		this.employeeRepository = employeeRepository;
		this.shiftTemplateRepository = shiftTemplateRepository;
		this.deferredSolveRequestRepository = deferredSolveRequestRepository;
		this.authController = authController;
		this.rotaRepository = rotaRepository;
		this.shiftRepository = shiftRepository;
		this.payCycleDataService = payCycleDataService;
		this.periodService = periodService;
		this.rotaCorrectionRepository = rotaCorrectionRepository;
		this.shiftAssignmentRepository = shiftAssignmentRepository;
	}

	@GetMapping("/regions")
	public ResponseEntity<List<Map<String, Object>>> getRegions() {
		List<String> regions = shiftTemplateRepository.findAllRegion();
		List<Map<String, Object>> result = new ArrayList<>();
		for (int i = 0; i < regions.size(); i++) {
			Map<String, Object> entry = new HashMap<>();
			entry.put("id", i + 1);
			entry.put("region", regions.get(i));
			result.add(entry);
		}
		return ResponseEntity.ok(result);
	}

	 @GetMapping("/service-locations/{regionName}")
	    public ResponseEntity<List<String>> getServicesForRegion(@PathVariable String regionName) {
	        List<String> services = shiftTemplateRepository.findAllServiceLocation(regionName);
	        return ResponseEntity.ok(services);
	    }
	
	@PostMapping("/enqueueRequest")
	public ResponseEntity<?> enqueueSolve(@RequestBody Map<String, Object> payload, Authentication authentication) {

		String startDate = (String) payload.get("startDate");
		String endDate = (String) payload.get("endDate");
		String region = (String) payload.get("location");

		DeferredSolveRequest request = new DeferredSolveRequest();
		try {
			request.setStartDate(LocalDate.parse(startDate));
			request.setEndDate(LocalDate.parse(endDate));
			request.setRegion(region);
			request.setCreatedBy(authentication.getName());

		} catch (Exception ex) {
			return ResponseEntity.badRequest().body("All fields (startDate, endDate, location) are required.");
		}
		long daysBetween = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate());

		if (daysBetween < 1) {
			return ResponseEntity.badRequest().body("End date must be after start date.");
		}

		if (daysBetween > 30) {
			return ResponseEntity.badRequest().body("Duration is over a month.");
		}
		request = deferredSolveRequestRepository.save(request);

		return ResponseEntity.ok(request);
	}

	@GetMapping("/enqueue/latest")
	public ResponseEntity<List<DeferredSolveRequest>> listRequests() {

		List<DeferredSolveRequest> response = deferredSolveRequestRepository.findTop5ByOrderByCreatedAtDesc();
		return ResponseEntity.ok(response);

	}

	@GetMapping("/download/schedule")
	@Transactional(readOnly = true)
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<StreamingResponseBody> getRotaForCSV(@RequestParam String id) {
		Optional<Rota> rota = rotaRepository.findById(Long.valueOf(id));

		if (rota.isEmpty()) {
			return ResponseEntity.notFound().build();
		}

		DeferredSolveRequest dsr = deferredSolveRequestRepository.findByRotaId(rota.get().getId());

		// ✅ Build filename with null checks
		String outputFileName;
		if (dsr != null && dsr.getRegion() != null && dsr.getStartDate() != null && dsr.getEndDate() != null) {
			outputFileName = dsr.getRegion() + "_" + dsr.getStartDate().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
					+ "_" + dsr.getEndDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".csv";
		} else {
			// Fallback filename
			outputFileName = "Rota_" + id + "_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
					+ ".csv";
			System.err.println("Using fallback filename due to missing data: " + outputFileName);
		}

		System.out.println("Generated filename: " + outputFileName);

		List<ShiftAssignment> saList = rota.get().getShiftAssignmentList();
		System.out.println("Data rows count: " + saList.size());

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

		StreamingResponseBody stream = out -> {
			try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
				writer.write("Location,Shift Type,Start Date,End Date,Duration,First Name,Last Name\n");

				for (ShiftAssignment sa : saList) {

					String firstName = Optional.ofNullable(sa.getEmployee()).map(Employee::getFirstName)
							.filter(s -> !s.isBlank()).orElse("UnAssigned");

					String lastName = Optional.ofNullable(sa.getEmployee()).map(Employee::getLastName)
							.filter(s -> !s.isBlank()).orElse("");

					LocalDateTime shiftStart = LocalDateTime.of(sa.getShift().getShiftStart(),
							sa.getShift().getShiftTemplate().getStartTime());
					LocalDateTime shiftEnd = LocalDateTime.of(sa.getShift().getShiftEnd(),
							sa.getShift().getShiftTemplate().getEndTime());

					Duration duration = Duration.between(shiftStart, shiftEnd);

					writer.write(String.join(",", escapeCsv(sa.getShift().getShiftTemplate().getLocation()),
							escapeCsv(sa.getShift().getShiftTemplate().getShiftType().name()),
							escapeCsv(shiftStart.format(formatter)), escapeCsv(shiftEnd.format(formatter)),
							escapeCsv(String.valueOf(duration.toMinutes() / 60.0)), escapeCsv(firstName),
							escapeCsv(lastName)) + "\n");
				}

				writer.flush();
				System.out.println("Streaming finished successfully");
			} catch (Exception e) {
				System.err.println("Streaming failed: " + e.getMessage());
				e.printStackTrace();
			}
		};

		HttpHeaders headers = new HttpHeaders();
		// ✅ Simple, standard format - no RFC encoding
		headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + outputFileName); // No quotes!
		headers.setContentType(MediaType.parseMediaType("text/csv"));
		headers.setCacheControl("no-store");

		return ResponseEntity.ok().headers(headers).body(stream);
	}

	@PostMapping("/solveAsync")
	public String solveAsync(@RequestBody Map<String, Object> payload, Authentication auth) {

		String startDate = (String) payload.get("startDate");
		String endDate = (String) payload.get("endDate");

		Rota problem = loadData(LocalDate.parse(startDate), LocalDate.parse(endDate));

		SolverJob<Rota, Long> solvJob = solverManager.solve(problem.getId(), id -> problem, bestSolution -> {
			List<ConstraintMatchTotal<?>> violations = explanationService.getConstraintViolations(bestSolution);
			updateService.pushUpdate(bestSolution, violations, solverManager.getSolverStatus(problem.getId()),
					auth.getName());
//			rosterAnalysisService.printHighImpactViolations(bestSolution);
		});
		try {
			Rota solution = solvJob.getFinalBestSolution();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return "Solving started asynchronously.";
	}

	@PostMapping("/updateMsgTest")
	public ResponseEntity<String> updateMsgTest(@RequestBody String rawJson, Authentication authentication) {
		updateService.pushUpdate("/queue/req-update", rawJson, authentication.getName());
		return ResponseEntity.ok("success ");
	}

	@PostMapping("/save")
	@Transactional
	public ResponseEntity<?> saveSchedule(@RequestBody Map<String, Object> payload, Authentication auth) {

		if (!payload.containsKey("rota") || !payload.containsKey("assignments")) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Missing required fields"));
		}

		Long rotaId = Long.valueOf(payload.get("rota").toString());

		@SuppressWarnings("unchecked")
		Map<String, List<Map<String, Object>>> incomingAssignments = (Map<String, List<Map<String, Object>>>) payload
				.get("assignments");

		Rota rota = rotaRepository.findById(rotaId).orElseThrow(() -> new RuntimeException("Rota not found"));

		boolean isFirstSave = rotaCorrectionRepository.countBySourceAndShiftAssignmentRotaId("Auto", rotaId) == 0;

		Map<String, ShiftAssignment> existingAssignmentsByKey = new HashMap<>();
		for (ShiftAssignment sa : rota.getShiftAssignmentList()) {
			if (sa.getShift() != null && sa.getShift().getShiftTemplate() != null) {
				String key = buildShiftKey(sa.getShift().getShiftTemplate().getLocation(),
						sa.getShift().getShiftTemplate().getShiftType().name(), sa.getShift().getShiftStart(),
						sa.getShift().getShiftTemplate().getStartTime());
				existingAssignmentsByKey.put(key, sa);
			}
		}

		List<ShiftAssignment> modifiedAssignments = new ArrayList<>();
		Map<ShiftAssignment, CorrectionInfo> correctionData = new HashMap<>();
		int updatedCount = 0;

		for (Map.Entry<String, List<Map<String, Object>>> entry : incomingAssignments.entrySet()) {
			String slotKey = entry.getKey();
			List<Map<String, Object>> employeeList = entry.getValue();

			ShiftAssignment existingAssignment = existingAssignmentsByKey.get(slotKey);
			if (existingAssignment == null)
				continue;

			Employee originalEmployee = existingAssignment.getEmployee();

			if (employeeList.isEmpty()) {
				if (originalEmployee != null && !isFirstSave) {
					correctionData.put(existingAssignment, new CorrectionInfo(originalEmployee, null, "Manual"));
				}
				existingAssignment.setEmployee(null);
				modifiedAssignments.add(existingAssignment); // ✅ Track this
				updatedCount++;
				continue;
			}

			Integer empId = Integer.valueOf(employeeList.get(0).get("id").toString());
			Employee newEmployee = employeeRepository.findById(empId).orElse(null);
			if (newEmployee == null)
				continue;

			boolean hasChanged = originalEmployee == null || !originalEmployee.getId().equals(newEmployee.getId());

			if (isFirstSave) {
				correctionData.put(existingAssignment, new CorrectionInfo(null, newEmployee, "Auto"));
				existingAssignment.setEmployee(newEmployee);
				modifiedAssignments.add(existingAssignment); // ✅ Track this
				updatedCount++;
			} else if (hasChanged) {
				correctionData.put(existingAssignment, new CorrectionInfo(originalEmployee, newEmployee, "Manual"));
				existingAssignment.setEmployee(newEmployee);
				modifiedAssignments.add(existingAssignment); // ✅ Track this
				updatedCount++;
			}
		}

		System.out.println("=== SAVE PROCESS ===");
		System.out.println("Modified assignments: " + modifiedAssignments.size());
		System.out.println("Corrections to create: " + correctionData.size());

		// ✅ STEP 1: Save modified assignments FIRST
		if (!modifiedAssignments.isEmpty()) {
			System.out.println("Saving " + modifiedAssignments.size() + " modified assignments...");
			shiftAssignmentRepository.saveAll(modifiedAssignments);
			shiftAssignmentRepository.flush(); // Force immediate write
			System.out.println("✅ Assignments saved and flushed");
		}

		// ✅ STEP 2: Now create corrections (assignments have been updated)
		List<RotaCorrection> corrections = new ArrayList<>();
		for (Map.Entry<ShiftAssignment, CorrectionInfo> entry : correctionData.entrySet()) {
			ShiftAssignment assignment = entry.getKey();
			CorrectionInfo info = entry.getValue();
			corrections.add(createCorrection(assignment, info.original, info.corrected, info.source));
		}

		// ✅ STEP 3: Save corrections
		if (!corrections.isEmpty()) {
			System.out.println("Saving " + corrections.size() + " corrections...");
			rotaCorrectionRepository.saveAll(corrections);
			rotaCorrectionRepository.flush();
			System.out.println("✅ Corrections saved and flushed");
		}

		System.out.println("=== SAVE COMPLETE ===");

		return ResponseEntity.ok(Map.of("message", "Success", "saveType", isFirstSave ? "Auto" : "Manual",
				"correctionsCreated", corrections.size(), "assignmentsUpdated", updatedCount));
	}

	@GetMapping("/solved")
	public ResponseEntity<?> solvedSolution(@RequestParam Long id) {
		Optional<Rota> rota = rotaRepository.findById(id);

		if (rota.isPresent()) {

			return ResponseEntity.ok((Rota) rota.get());
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Rota not found for ID: " + id));
		}

	}

	@GetMapping("/payCycle")
	public ResponseEntity<?> payCycleList(@RequestParam String location) {
		List<PayCycleRow> pcr = payCycleDataService.fetchRows(location);

		if (pcr.size() < 1)
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "payCycleList is empty"));

		return ResponseEntity.ok(pcr);
	}

	@GetMapping("/wsUsers")
	public List<String> getWsUsers() {
		return simpUserRegistry.getUsers().stream().map(SimpUser::getName).collect(Collectors.toList());
	}

	private Rota loadData(LocalDate startDate, LocalDate endDate) {

		final AtomicLong idGenerator = new AtomicLong();

		// Query all employees
		List<Employee> employees = employeeRepository.findAll();
//		System.out.println("Employees -" + employees.size());
		List<ShiftTemplate> shiftTemplates = shiftTemplateRepository.findAll();
//		System.out.println("Shifts -" + shifts.size());
		List<ShiftAssignment> shiftAssignments = new ArrayList<>();

		shiftAssignments = this.generateShiftInstances(startDate, endDate, shiftTemplates);
//		System.out.println("shiftAssignments - " + shiftAssignments.size());
		Long id = idGenerator.incrementAndGet();
		Rota solution = new Rota(employees, shiftAssignments);

		return solution;

	}

	private List<ShiftAssignment> generateShiftInstances(LocalDate startDate, LocalDate endDate,
			List<ShiftTemplate> templates) {

		List<ShiftAssignment> assignments = new ArrayList<>();
		List<Shift> instances = new ArrayList<>();

		// Create shift instance for the date range
		AtomicLong id = new AtomicLong();
		for (ShiftTemplate template : templates) {
			LocalDate current = startDate;
			while (!current.isAfter(endDate)) {
				if (current.getDayOfWeek().equals(template.getDayOfWeek())) {
					instances.add(new Shift(current, template, periodService.getAbsoluteWeekNumber(current)));

				}
				current = current.plusDays(1);

			}
		}

		// Add shift assignments per shift to enable multiple employee assignment for
		// each shift ( N to 1 scenario)
		for (Shift shift : instances) {
			for (int i = 0; i < shift.getShiftTemplate().getEmpCount(); i++) {
				ShiftAssignment assignment = new ShiftAssignment(shift);
				assignments.add(assignment);
			}
		}
		return assignments;

	}

	private String escapeCsv(String value) {
		if (value == null) {
			return "";
		}
		// Escape double quotes by doubling them
		String escaped = value.replace("\"", "\"\"");
		// Wrap in quotes
		return "\"" + escaped + "\"";
	}

	private String buildShiftKey(String location, String shiftType, LocalDate date, LocalTime startTime) {
		// ✅ Use HH:mm:ss format to match frontend
		String timeStr = startTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
		return location + "|" + shiftType + "|" + date.toString() + "|" + timeStr;
	}

	private RotaCorrection createCorrection(ShiftAssignment assignment, Employee originalEmployee,
			Employee correctedEmployee, String source) {
		RotaCorrection correction = new RotaCorrection();
		correction.setShiftAssignment(assignment);
		correction.setOriginalEmployee(originalEmployee);
		correction.setCorrectedEmployee(correctedEmployee);

// Extract details from existing assignment
		correction.setLocation(assignment.getShift().getShiftTemplate().getLocation());
		correction.setShiftType(assignment.getShift().getShiftTemplate().getShiftType());
		correction.setDayOfWeek(assignment.getShift().getShiftStart().getDayOfWeek());
		correction.setShiftDate(assignment.getShift().getShiftStart());
		correction.setSource(source);
		correction.setCorrectionDate(LocalDateTime.now());

		if ("Auto".equals(source)) {
			correction.setCorrectionReason("OptaPlanner initial allocation");
		} else {
			correction.setCorrectionReason("Manual correction via UI");
		}

		return correction;
	}

	// Helper class
	private static class CorrectionInfo {
		Employee original;
		Employee corrected;
		String source;

		CorrectionInfo(Employee original, Employee corrected, String source) {
			this.original = original;
			this.corrected = corrected;
			this.source = source;
		}
	}

	@GetMapping("/debug/keys/{rotaId}")
	public ResponseEntity<?> debugKeys(@PathVariable Long rotaId) {
		Rota rota = rotaRepository.findById(rotaId).orElseThrow();

		List<String> keys = rota.getShiftAssignmentList().stream()
				.filter(sa -> sa.getShift() != null && sa.getShift().getShiftTemplate() != null)
				.map(sa -> buildShiftKey(sa.getShift().getShiftTemplate().getLocation(),
						sa.getShift().getShiftTemplate().getShiftType().name(), sa.getShift().getShiftStart(),
						sa.getShift().getShiftTemplate().getStartTime()))
				.limit(10).collect(Collectors.toList());

		return ResponseEntity.ok(Map.of("sampleKeys", keys));
	}
}
