package com.midco.rota;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.midco.rota.model.DeferredSolveRequest;
import com.midco.rota.model.Employee;
import com.midco.rota.model.Rota;
import com.midco.rota.model.Shift;
import com.midco.rota.model.ShiftAssignment;
import com.midco.rota.repository.DeferredSolveRequestRepository;
import com.midco.rota.repository.EmployeeRepository;
import com.midco.rota.repository.ShiftRepository;

@Component

public class SolverTrigger {

	private static final Logger logger = LoggerFactory.getLogger(SolverTrigger.class);

	private final SolverService solverService;
	private final DeferredSolveRequestRepository deferredSolveRequestRepository;
	private final EmployeeRepository employeeRepository;
	private final ShiftRepository shiftRepository;

	public SolverTrigger(SolverService solverService, DeferredSolveRequestRepository deferredSolveRequestRepository,
			EmployeeRepository employeeRepository, ShiftRepository shiftRepository) {
		this.solverService = solverService;
		this.deferredSolveRequestRepository = deferredSolveRequestRepository;
		this.employeeRepository = employeeRepository;
		this.shiftRepository = shiftRepository;
	}

	@Scheduled(cron = "0 */2 * * * *") // Every hour

	public void triggerSolver() {

		deferredSolveRequestRepository.findFirstByCompletedFalse().ifPresentOrElse(deferredSolveRequest -> {
			Rota problem = loadData(deferredSolveRequest); 
			logger.info("triggerSolver=== ");
			solverService.solveAsync(problem, problem.getId(), deferredSolveRequest);
		}, () -> {
			logger.info("no sovler request available to process");
		});

	}

	private Rota loadData(DeferredSolveRequest deferredSolveRequest) {

		// Query all employees
		List<Employee> employees = employeeRepository.findAll();
		List<Shift> shifts = shiftRepository.findAllByRegion(deferredSolveRequest.getRegion());
		List<ShiftAssignment> shiftAssignments = new ArrayList<>();
		shiftAssignments = this.generateShiftInstances(deferredSolveRequest.getStartDate(),
				deferredSolveRequest.getEndDate(), shifts);
		Rota problem = new Rota(employees, shiftAssignments, deferredSolveRequest.getId());

		return problem;
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