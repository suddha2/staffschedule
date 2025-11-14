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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.midco.rota.model.DeferredSolveRequest;
import com.midco.rota.model.Employee;
import com.midco.rota.model.Rota;
import com.midco.rota.model.Shift;
import com.midco.rota.model.ShiftAssignment;
import com.midco.rota.model.ShiftTemplate;
import com.midco.rota.repository.DeferredSolveRequestRepository;
import com.midco.rota.repository.EmployeeRepository;
import com.midco.rota.repository.RotaRepository;
import com.midco.rota.repository.ShiftRepository;
import com.midco.rota.repository.ShiftTemplateRepository;
import com.midco.rota.service.ConstraintExplanationService;
import com.midco.rota.service.PayCycleDataService;
import com.midco.rota.service.RosterAnalysisService;
import com.midco.rota.service.RosterUpdateService;
import com.midco.rota.util.PayCycleRow;

@RestController
@RequestMapping("/api")
public class RotaController {

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
	// private final ExecutorService securityExecutorService;
	@Autowired
	private SimpUserRegistry simpUserRegistry;

	public RotaController(SolverManager<Rota, Long> solverManager, RosterUpdateService updateService,
			ConstraintExplanationService explanationService, RosterAnalysisService rosterAnalysisService,
			EmployeeRepository employeeRepository, ShiftTemplateRepository shiftTemplateRepository,
			DeferredSolveRequestRepository deferredSolveRequestRepository, AuthController authController,
			RotaRepository rotaRepository, ShiftRepository shiftRepository, PayCycleDataService payCycleDataService) {
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
	public ResponseEntity<?> saveSchedule(@RequestBody Map<String, Object> payload, Authentication auth) {

		String bodyJson = (String) payload.get("body");

		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> bodyMap;
		try {
			bodyMap = mapper.readValue(bodyJson, new TypeReference<>() {
			});
		} catch (JsonProcessingException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(Map.of("error", "Json format error: " + e.toString()));
		}

		@SuppressWarnings("unchecked")
		Map<String, List<Map<String, Object>>> assignments = (Map<String, List<Map<String, Object>>>) bodyMap
				.get("assignments");

		Long rotaId = Long.valueOf(bodyMap.get("rota").toString());

		List<ShiftAssignment> allAssignments = new ArrayList<>();

		for (Map.Entry<String, List<Map<String, Object>>> entry : assignments.entrySet()) {
			String slotKey = entry.getKey(); // e.g. "8-Barford|LONG_DAY|2025-09-01|07:00:00"
			List<Map<String, Object>> employeeList = entry.getValue();

			String[] parts = slotKey.split("|");
			if (parts.length != 4)
				continue; // skip malformed keys

			String location = parts[0];// .contains("-") ? parts[0].split("-")[1] : parts[0];
			String shiftType = parts[1];
			LocalDate date = LocalDate.parse(parts[2]);
			LocalTime startTime = LocalTime.parse(parts[3]);

			// Lookup or create ShiftTemplate
			ShiftTemplate template = shiftTemplateRepository.findByLocationAndShiftTypeAndStartTime(location, shiftType,
					startTime);

			if (template == null) {
				// Optionally create or skip
				continue;
			}

			Shift shift = shiftRepository.findByShiftTemplateAndStartTime(template, date);

			// Create ShiftAssignments
			for (Map<String, Object> empMap : employeeList) {
				Integer empId = Integer.valueOf(empMap.get("id").toString());
				Employee emp = employeeRepository.findById(empId).orElseThrow();

				ShiftAssignment assignment = new ShiftAssignment();
				assignment.setShift(shift);
				assignment.setEmployee(emp);
				allAssignments.add(assignment);

			}
		}
		Optional<Rota> rotaOpt = rotaRepository.findById(rotaId);

		if (rotaOpt.isPresent()) {
			Rota rota = rotaOpt.get();
			// rota.getShiftAssignmentList().clear();
			for (ShiftAssignment assignment : allAssignments) {
				assignment.setRota(rota);
				rota.getShiftAssignmentList().add(assignment);
			}
			rotaRepository.save(rota);
			return ResponseEntity.ok("Success");
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(Map.of("error", "Rota not found for ID: " + rotaId));
		}

	}

	@GetMapping("/solved")
	public ResponseEntity<?> solvedSolution(@RequestParam Long id) {
		Optional<Rota> rota = rotaRepository.findById(id);

		if (rota.isPresent()) {

			return ResponseEntity.ok(rota.get());
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
					instances.add(new Shift(current, template));

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

}
