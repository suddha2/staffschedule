package com.midco.rota.controller;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.hibernate.query.Page;
import org.optaplanner.core.api.score.constraint.ConstraintMatchTotal;
import org.optaplanner.core.api.solver.SolverJob;
import org.optaplanner.core.api.solver.SolverManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.midco.rota.model.DeferredSolveRequest;
import com.midco.rota.model.Employee;
import com.midco.rota.model.Rota;
import com.midco.rota.model.Shift;
import com.midco.rota.model.ShiftAssignment;
import com.midco.rota.model.ShiftTemplate;
import com.midco.rota.repository.DeferredSolveRequestRepository;
import com.midco.rota.repository.EmployeeRepository;
import com.midco.rota.repository.ShiftTemplateRepository;
import com.midco.rota.service.ConstraintExplanationService;
import com.midco.rota.service.RosterAnalysisService;
import com.midco.rota.service.RosterUpdateService;

@RestController
@RequestMapping("/api")
public class RotaController {

    private final AuthController authController;

	private final SolverManager<Rota, Long> solverManager;
	private final RosterUpdateService updateService;
	private final ConstraintExplanationService explanationService;
	private final RosterAnalysisService rosterAnalysisService;
	private final EmployeeRepository employeeRepository;
	private final ShiftTemplateRepository shiftTemplateRepository;

	private final DeferredSolveRequestRepository deferredSolveRequestRepository;

	@Autowired
	private SimpUserRegistry simpUserRegistry;

	public RotaController(SolverManager<Rota, Long> solverManager, RosterUpdateService updateService,
			ConstraintExplanationService explanationService, RosterAnalysisService rosterAnalysisService,
			EmployeeRepository employeeRepository, ShiftTemplateRepository shiftTemplateRepository,
			DeferredSolveRequestRepository deferredSolveRequestRepository, AuthController authController) {
		this.solverManager = solverManager;
		this.updateService = updateService;
		this.explanationService = explanationService;
		this.rosterAnalysisService = rosterAnalysisService;
		this.employeeRepository = employeeRepository;
		this.shiftTemplateRepository = shiftTemplateRepository;
		this.deferredSolveRequestRepository = deferredSolveRequestRepository;
		this.authController = authController;
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
	public ResponseEntity<String> updateMsgTest(Authentication authentication) {
		updateService.pushUpdate("/queue/req-update", "Test message", authentication.getName());
		return ResponseEntity.ok("success ");
	}

	@PostMapping("/save")
	public ResponseEntity<?> saveSchedule(@RequestBody Map<String, Object> payload, Authentication auth) {
		
		System.out.println("======================================== "+payload.toString());
		
		return ResponseEntity.ok("Success"); 
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
		long id = idGenerator.incrementAndGet();
		Rota solution = new Rota(employees, shiftAssignments, id);

		return solution;

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
					instances.add(new Shift(id.getAndIncrement(), current, template));

				}
				current = current.plusDays(1);

			}
		}

		// Add shift assignments per shift to enable multiple employee assignment for
		// each shift ( N to 1 scenario)
		for (Shift shift : instances) {
			for (int i = 0; i < shift.getShiftTemplate().getEmpCount(); i++) {
				ShiftAssignment assignment = new ShiftAssignment(shift, id.getAndIncrement());
				assignments.add(assignment);
			}
		}
		return assignments;

	}

}
