package staffschedule;

import java.util.Optional;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.optaplanner.core.config.solver.SolverConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import com.midco.rota.RotaServiceApplication;
import com.midco.rota.SolverTrigger;
import com.midco.rota.model.DeferredSolveRequest;
import com.midco.rota.model.Rota;
import com.midco.rota.repository.DeferredSolveRequestRepository;
import com.midco.rota.repository.EmployeeRepository;
import com.midco.rota.service.SolverService;

import jakarta.transaction.Transactional;

@SpringBootTest(classes = RotaServiceApplication.class)
@TestPropertySource(locations = "classpath:application-dev.properties")
@Transactional
public class OptaPlannerIntegrationTestWithDiagnostics {

	@Autowired
	private SolverService solverService;

	@Autowired
	private DeferredSolveRequestRepository deferredSolveRequestRepository;

	@Autowired
	private EmployeeRepository empRep;

	@Autowired
	private SolverTrigger solverTrigger;

	@Autowired
	private SolverConfig solverConfig;

	@Test
	void optaIntegrationsTestWithDiagnostics() {
		System.out.println("\n========================================");
		System.out.println("OPTAPLANNER DIAGNOSTIC TEST ");
		System.out.println("========================================\n");

		Optional<DeferredSolveRequest> deferredSolveRequest = deferredSolveRequestRepository.findById(174L);
		System.out.println("Has data to process: " + !deferredSolveRequest.isEmpty());
		
		long rand = new Random().nextLong();
		Rota problem = solverTrigger.loadData(deferredSolveRequest.get());

		System.out.println("Employee pool size: " + problem.getEmployeeList().size());
		System.out.println("Total shifts: " + problem.getShiftAssignmentList().size());
		
		// Filter to 9 CLAYDON shifts
//		Rota testingProblem = new Rota(
//			problem.getEmployeeList(), 
//			problem.getShiftAssignmentList().stream()
//				.filter(sa -> sa.getShift().getShiftTemplate().getLocation().equals("9 CLAYDON"))
//				.toList()
//		);
//		
//		System.out.println("9 CLAYDON shifts: " + testingProblem.getShiftAssignmentList().size());
		
		// ✅ PRE-SOLVE DIAGNOSTICS
		System.out.println("\n========== PRE-SOLVE ANALYSIS ==========");
		ShiftAssignmentDiagnostics.diagnoseUnassignedShifts(
				problem.getShiftAssignmentList(), 
				problem.getEmployeeList()
		);
		
		// Count by shift type
		System.out.println("\nShifts by type:");
		problem.getShiftAssignmentList().stream()
			.collect(java.util.stream.Collectors.groupingBy(
				sa -> sa.getShift().getShiftTemplate().getShiftType(),
				java.util.stream.Collectors.counting()
			))
			.forEach((type, count) -> System.out.println("  " + type + ": " + count));
		
		// Check employee restrictions
//		System.out.println("\nEmployee restriction summary:");
//		long empWithDayRestriction = problem.getEmployeeList().stream()
//			.filter(e -> e.getRestrictedShift() != null && 
//				e.getRestrictedShift().contains(com.midco.rota.util.ShiftType.DAY))
//			.count();
//		System.out.println("  Employees with DAY restriction: " + empWithDayRestriction);
		
//		long empWithClaydonRestriction = problem.getEmployeeList().stream()
//			.filter(e -> e.getRestrictedService() != null && 
//				e.getRestrictedService().contains("9 CLAYDON"))
//			.count();
//		System.out.println("  Employees with 9 CLAYDON restriction: " + empWithClaydonRestriction);
		
		// Check if any employees prefer 9 CLAYDON
//		long empPreferClaydon = problem.getEmployeeList().stream()
//			.filter(e -> e.prefersLocation("9 CLAYDON"))
//			.count();
//		System.out.println("  Employees who prefer 9 CLAYDON: " + empPreferClaydon);

		problem.setPlanningId(rand);

		// Solve
		System.out.println("\n========== SOLVING ==========");
		Rota solution = solverService.solve(problem, rand);

		// ✅ POST-SOLVE DIAGNOSTICS
		System.out.println("\n========== POST-SOLVE ANALYSIS ==========");
		ShiftAssignmentDiagnostics.diagnoseUnassignedShifts(
			solution.getShiftAssignmentList(), 
			solution.getEmployeeList()
		);
		
		// Show sample assignments
		System.out.println("\nSample assignments:");
		solution.getShiftAssignmentList().stream()
			.filter(sa -> sa.getEmployee() != null)
			.limit(10)
			.forEach(sa -> System.out.println("  " + sa));
		
		// Check if any DAY shifts got assigned
		long dayAssigned = solution.getShiftAssignmentList().stream()
			.filter(sa -> sa.getShift().getShiftTemplate().getShiftType() == 
				com.midco.rota.util.ShiftType.DAY)
			.filter(sa -> sa.getEmployee() != null)
			.count();
		
		System.out.println("\n========== FINAL STATS ==========");
		System.out.println("DAY shifts assigned: " + dayAssigned);
		System.out.println("Score: " + solution.getScore());
	}
}