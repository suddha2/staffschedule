package com.midco.rota.controller;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

import org.optaplanner.core.api.score.constraint.ConstraintMatchTotal;
import org.optaplanner.core.api.solver.SolverJob;
import org.optaplanner.core.api.solver.SolverManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.javaparser.Problem;
import com.midco.rota.ConstraintExplanationService;
import com.midco.rota.RosterAnalysisService;
import com.midco.rota.RosterUpdateService;
import com.midco.rota.model.DeferredSolveRequest;
import com.midco.rota.model.Employee;
import com.midco.rota.model.Rota;
import com.midco.rota.model.Shift;
import com.midco.rota.model.ShiftAssignment;
import com.midco.rota.repository.DeferredSolveRequestRepository;
import com.midco.rota.repository.EmployeeRepository;
import com.midco.rota.repository.ShiftRepository;

@RestController
@RequestMapping("/api")
public class RotaController {

	private final SolverManager<Rota, Long> solverManager;
	private final RosterUpdateService updateService;
	private final ConstraintExplanationService explanationService;
	private final RosterAnalysisService rosterAnalysisService;
	private final EmployeeRepository employeeRepository;
	private final ShiftRepository shiftRepository;

	private final DeferredSolveRequestRepository deferredSolveRequestRepository;

	public RotaController(SolverManager<Rota, Long> solverManager, RosterUpdateService updateService,
			ConstraintExplanationService explanationService, RosterAnalysisService rosterAnalysisService,
			EmployeeRepository employeeRepository, ShiftRepository shiftRepository,
			DeferredSolveRequestRepository deferredSolveRequestRepository) {
		this.solverManager = solverManager;
		this.updateService = updateService;
		this.explanationService = explanationService;
		this.rosterAnalysisService = rosterAnalysisService;
		this.employeeRepository = employeeRepository;
		this.shiftRepository = shiftRepository;
		this.deferredSolveRequestRepository = deferredSolveRequestRepository;
	}

	@GetMapping("/regions")
	public ResponseEntity<List<Map<String, Object>>> getRegions() {
		List<String> regions = shiftRepository.findAllRegion();
		List<Map<String, Object>> result = new ArrayList<>();

		for (int i = 0; i < regions.size(); i++) {
			Map<String, Object> entry = new HashMap<>();
			entry.put("id", i + 1);
			entry.put("region", regions.get(i));
			result.add(entry);
		}

		return ResponseEntity.ok(result);
		

	}

	@PostMapping("/enqueueSolve")
	public ResponseEntity<?> enqueueSolve(@RequestBody Map<String, Object> payload) {

		String startDate = (String) payload.get("startDate");
		String endDate = (String) payload.get("endDate");
		String region = (String) payload.get("location");

		DeferredSolveRequest request = new DeferredSolveRequest();
		try {
			request.setStartDate(LocalDate.parse(startDate));
			request.setEndDate(LocalDate.parse(endDate));
			request.setRegion(region);
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
		deferredSolveRequestRepository.save(request);

		return ResponseEntity.ok(Map.of("requestId", request.getId()));
	}
	@PostMapping("/solveAsync-test")
	public String solveAsyncTes(@RequestBody Map<String, Object> payload) {
		return "-------------------------------------";
	}

	@PostMapping("/solveAsync")
	public String solveAsync(@RequestBody Map<String, Object> payload) {
		
		
		String startDate = (String) payload.get("startDate");
		String endDate = (String) payload.get("endDate");

//		LocalDate startDate = LocalDate.parse((String)payload.get("startDate"));
//		LocalDate endDate = LocalDate.parse((String)payload.get("endDate"));
		Rota problem = loadData(LocalDate.parse(startDate), LocalDate.parse(endDate));

		SolverJob<Rota,Long> solvJob= solverManager.solve(problem.getId(), id -> problem, bestSolution -> {
			List<ConstraintMatchTotal<?>> violations = explanationService.getConstraintViolations(bestSolution);
			updateService.pushUpdate(bestSolution, violations, solverManager.getSolverStatus(problem.getId()));
//			rosterAnalysisService.printHighImpactViolations(bestSolution);
		});
		try {
			Rota solution = solvJob.getFinalBestSolution();
			
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
//		solverManager.solveAndListen(
//			    problem.getId(),
//			    id -> problem, // Or ideally load from DB: loadProblem(id)
//			    bestSolution -> {
//			        List<ConstraintMatchTotal<?>> violations = explanationService.getConstraintViolations(bestSolution);
//			        SolverStatus status = solverManager.getSolverStatus(problem.getId());
//			        updateService.pushUpdate(bestSolution, violations, status);
//			    }
//			);

		return "Solving started asynchronously.";
	}

	private Rota loadData(LocalDate startDate, LocalDate endDate) {

		final AtomicLong idGenerator = new AtomicLong();

		// Query all employees
		List<Employee> employees = employeeRepository.findAll();
//		System.out.println("Employees -" + employees.size());
		List<Shift> shifts = shiftRepository.findAll();
//		System.out.println("Shifts -" + shifts.size());
		List<ShiftAssignment> shiftAssignments = new ArrayList<>();

		shiftAssignments = this.generateShiftInstances(startDate, endDate, shifts);
//		System.out.println("shiftAssignments - " + shiftAssignments.size());
		long id = idGenerator.incrementAndGet();
		Rota solution = new Rota(employees, shiftAssignments, id);

		return solution;

	}

	private List<ShiftAssignment> generateShiftInstances(LocalDate startDate, LocalDate endDate,
			List<Shift> templates) {

		List<ShiftAssignment> results = new ArrayList<>();

		for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
			results.addAll(generateShiftsForDate(date, templates));
		}

		return results;
	}

	private List<ShiftAssignment> generateShiftsForDate(LocalDate targetDate, List<Shift> templates) {
		DayOfWeek targetDay = targetDate.getDayOfWeek();
		return templates.stream().filter(t -> (t.getDay()) == targetDay).map(t -> {

			ShiftAssignment instance = new ShiftAssignment(targetDate, new Random().nextLong(), t.getLocation(),
					t.getShiftType(), t.getDay(), t.getStartTime(), t.getEndTime(), t.getTotalHours(), t.getGender(),
					null);
			return instance;

		}).toList();
	}

}
