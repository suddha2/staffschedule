package com.midco.rota.opt;

import org.optaplanner.core.api.solver.change.ProblemChange;
import org.optaplanner.core.api.solver.change.ProblemChangeDirector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.midco.rota.model.Employee;
import com.midco.rota.model.Rota;
import com.midco.rota.model.ShiftAssignment;
import com.midco.rota.model.WorkShiftAssignment;

/**
 * Structural live edit (P3+): remove an employee from the running solve's value
 * range (e.g. a carer becomes unavailable mid-period). First unassigns every slot
 * they currently hold, then removes them as a problem fact so the solver can no
 * longer select them. On snapshot the freed slots persist as unassigned and the
 * employee leaves the rota's value-range join.
 */
public class RemoveEmployeeFromValueRangeProblemChange implements ProblemChange<Rota> {

	private static final Logger logger = LoggerFactory.getLogger(RemoveEmployeeFromValueRangeProblemChange.class);

	private final Integer employeeId;

	public RemoveEmployeeFromValueRangeProblemChange(Integer employeeId) {
		this.employeeId = employeeId;
	}

	@Override
	public void doChange(Rota workingSolution, ProblemChangeDirector problemChangeDirector) {
		Employee target = workingSolution.getEmployeeList().stream()
				.filter(e -> employeeId.equals(e.getId()))
				.findFirst().orElse(null);
		if (target == null) {
			logger.info("Employee {} not in value range; remove skipped", employeeId);
			return;
		}

		// Unassign every WORK slot held by this employee BEFORE removing the fact, so
		// no genuine variable is left pointing at a removed value. SLEEP_IN slots are
		// shadows — they follow automatically when their LONG_DAY is unassigned.
		for (ShiftAssignment sa : workingSolution.getShiftAssignmentList()) {
			if (sa instanceof WorkShiftAssignment && sa.getEmployee() != null
					&& employeeId.equals(sa.getEmployee().getId())) {
				problemChangeDirector.changeVariable(sa, "employee", a -> a.setEmployee(null));
			}
		}

		problemChangeDirector.removeProblemFact(target,
				t -> workingSolution.getEmployeeList().removeIf(e -> employeeId.equals(e.getId())));
	}
}
