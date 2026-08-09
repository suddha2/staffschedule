package com.midco.rota.opt;

import com.midco.rota.model.Employee;
import com.midco.rota.model.Rota;
import com.midco.rota.model.ShiftAssignment;

/**
 * Lookup helpers shared by the live {@link org.optaplanner.core.api.solver.change.ProblemChange}
 * implementations (P3). Resolves the <em>working</em> entity/fact from the
 * solver's working solution by id, so changes are applied to the solver's own
 * instances rather than detached copies.
 */
final class ProblemChangeSupport {

	private ProblemChangeSupport() {
	}

	/**
	 * Find the working {@link ShiftAssignment} whose {@code @PlanningId} (the DB
	 * id anchored at load time) equals {@code assignmentId}, or null if absent.
	 */
	static ShiftAssignment findAssignment(Rota workingSolution, long assignmentId) {
		String planningId = String.valueOf(assignmentId);
		for (ShiftAssignment sa : workingSolution.getShiftAssignmentList()) {
			if (planningId.equals(sa.getPlanningId())) {
				return sa;
			}
		}
		return null;
	}

	/** Find the working {@link Employee} (value-range fact) by id, or null. */
	static Employee findEmployee(Rota workingSolution, Integer employeeId) {
		if (employeeId == null) {
			return null;
		}
		for (Employee e : workingSolution.getEmployeeList()) {
			if (employeeId.equals(e.getId())) {
				return e;
			}
		}
		return null;
	}
}
