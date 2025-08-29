package com.midco.rota;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.midco.rota.model.DeferredSolveRequest;
import com.midco.rota.model.Employee;
import com.midco.rota.model.Rota;
import com.midco.rota.model.Shift;
import com.midco.rota.model.ShiftTemplate;
import com.midco.rota.model.ShiftAssignment;
import com.midco.rota.repository.DeferredSolveRequestRepository;
import com.midco.rota.repository.EmployeeRepository;
 
import com.midco.rota.repository.ShiftTemplateRepository;
import com.midco.rota.service.SolverService;

@Component

public class SolverTrigger {

	private static final Logger logger = LoggerFactory.getLogger(SolverTrigger.class);

	private final SolverService solverService;
	private final DeferredSolveRequestRepository deferredSolveRequestRepository;
	private final EmployeeRepository employeeRepository;
	private final ShiftTemplateRepository  shiftTemplateRepository;

	public SolverTrigger(SolverService solverService, DeferredSolveRequestRepository deferredSolveRequestRepository,
			EmployeeRepository employeeRepository, ShiftTemplateRepository shiftTemplateRepository) {
		this.solverService = solverService;
		this.deferredSolveRequestRepository = deferredSolveRequestRepository;
		this.employeeRepository = employeeRepository;
		this.shiftTemplateRepository = shiftTemplateRepository;
	}

	@Scheduled(cron = "0 */2 * * * *") // Every hour

	public void triggerSolver() {

		deferredSolveRequestRepository.findFirstByCompletedFalse().ifPresentOrElse(deferredSolveRequest -> {
			Rota problem = loadData(deferredSolveRequest); 
			logger.info("triggerSolver=== ");
			solverService.solveAsync(problem, problem.getPlanningId(), deferredSolveRequest);
		}, () -> {
			logger.info("no sovler request available to process");
		});

	}

	private Rota loadData(DeferredSolveRequest deferredSolveRequest) {

		// Query all employees
		List<Employee> employees = employeeRepository.findAll();
		List<ShiftTemplate> shiftTemplates = shiftTemplateRepository.findAllByRegion(deferredSolveRequest.getRegion());
		List<ShiftAssignment> shiftAssignments = new ArrayList<>();
		shiftAssignments = this.generateShiftInstances(deferredSolveRequest.getStartDate(),
				deferredSolveRequest.getEndDate(), shiftTemplates);
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
					instances.add(new Shift( current, template));

				}
				current = current.plusDays(1);

			}
		}
		
		  
		// Add shift assignments to enable multiple employee assignment for each shift ( 2 to 1 scenario)
		for (Shift shift : instances) {
		    for (int i = 0; i < shift.getShiftTemplate().getEmpCount(); i++) {
		        ShiftAssignment assignment = new ShiftAssignment(shift);
		        assignments.add(assignment);
		    }
		}
		return assignments;
		
	}

}