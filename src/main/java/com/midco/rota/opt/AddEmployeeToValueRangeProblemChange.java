package com.midco.rota.opt;

import org.optaplanner.core.api.solver.change.ProblemChange;
import org.optaplanner.core.api.solver.change.ProblemChangeDirector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.midco.rota.model.Employee;
import com.midco.rota.model.Rota;

/**
 * Structural live edit (P3+): add an employee to the running solve's value range
 * so the solver can start assigning them (e.g. an extra carer becomes available
 * mid-period). The employee is a problem fact; adding it via the director makes
 * the solver aware of the new selectable value.
 *
 * <p>{@link com.midco.rota.model.Employee} has no lazy associations, so a
 * detached instance loaded by id is safe to hand to the solver.
 */
public class AddEmployeeToValueRangeProblemChange implements ProblemChange<Rota> {

	private static final Logger logger = LoggerFactory.getLogger(AddEmployeeToValueRangeProblemChange.class);

	private final Employee employee;

	public AddEmployeeToValueRangeProblemChange(Employee employee) {
		this.employee = employee;
	}

	@Override
	public void doChange(Rota workingSolution, ProblemChangeDirector problemChangeDirector) {
		Integer id = employee.getId();
		boolean alreadyPresent = workingSolution.getEmployeeList().stream()
				.anyMatch(e -> e.getId() != null && e.getId().equals(id));
		if (alreadyPresent) {
			logger.info("Employee {} already in value range; add skipped", id);
			return;
		}
		problemChangeDirector.addProblemFact(employee, e -> workingSolution.getEmployeeList().add(e));
	}
}
