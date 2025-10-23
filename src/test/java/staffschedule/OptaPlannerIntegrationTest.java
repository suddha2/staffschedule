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
import com.midco.rota.model.Employee;
import com.midco.rota.model.Rota;
import com.midco.rota.repository.DeferredSolveRequestRepository;
import com.midco.rota.repository.EmployeeRepository;
import com.midco.rota.service.SolverService;
import com.midco.rota.util.ContractType;

import jakarta.transaction.Transactional;

@SpringBootTest(classes = RotaServiceApplication.class)
@TestPropertySource(locations = "classpath:application-dev.properties")
@Transactional
public class OptaPlannerIntegrationTest {

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
	void optaIntegrationsTest() {

		Optional<DeferredSolveRequest> deferredSolveRequest = deferredSolveRequestRepository.findById(31L);
		System.out.println("Has data to process : " + !deferredSolveRequest.isEmpty());
		long rand = new Random().nextLong();
		Rota problem = solverTrigger.loadData(deferredSolveRequest.get());

		Rota testingProblem = new Rota(problem.getEmployeeList(), problem.getShiftAssignmentList().stream().toList());

		System.out.println("Emplist count=" + testingProblem.getEmployeeList().size());

//		Optional<Employee> emp = empRep.findById(294);
//		problem.getShiftAssignmentList().forEach(sa->{sa.setEmployee(emp.get());});
		problem.setPlanningId(rand);

		solverService.solve(testingProblem, rand);

	}
}
