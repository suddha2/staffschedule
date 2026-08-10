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
 * Live edit (P3): assign a specific employee to a shift-assignment slot (or clear
 * it when {@code employeeId} is null) and set its pin state, then let the running
 * solver re-optimise everything else around it.
 *
 * <p>The slot is identified by its DB id, which {@code LiveRotaPersistenceService}
 * anchors as the {@code @PlanningId}. Pinning a manual assignment keeps the solver
 * from moving it; unassigning-and-unpinning hands the slot back to the solver.
 */
public class AssignEmployeeProblemChange implements ProblemChange<Rota> {

	private static final Logger logger = LoggerFactory.getLogger(AssignEmployeeProblemChange.class);

	private final long assignmentId;
	private final Integer employeeId; // null => unassign
	private final boolean pin;

	public AssignEmployeeProblemChange(long assignmentId, Integer employeeId, boolean pin) {
		this.assignmentId = assignmentId;
		this.employeeId = employeeId;
		this.pin = pin;
	}

	@Override
	public void doChange(Rota workingSolution, ProblemChangeDirector problemChangeDirector) {
		ShiftAssignment target = ProblemChangeSupport.findAssignment(workingSolution, assignmentId);
		if (target == null) {
			logger.warn("Live assign skipped: no assignment with id {} in working solution", assignmentId);
			return;
		}
		// SLEEP_IN's employee is a shadow variable (mirrors its LONG_DAY); it can't be
		// changed directly. Assign the paired LONG_DAY instead.
		if (!(target instanceof WorkShiftAssignment)) {
			logger.info("Live assign skipped for slot {}: SLEEP_IN employee follows its LONG_DAY", assignmentId);
			return;
		}

		Employee employee = null;
		if (employeeId != null) {
			employee = ProblemChangeSupport.findEmployee(workingSolution, employeeId);
			if (employee == null) {
				logger.warn("Live assign for slot {}: employee {} not in value range; leaving unassigned",
						assignmentId, employeeId);
			}
		}

		final Employee resolved = employee;
		problemChangeDirector.changeVariable(target, "employee", a -> a.setEmployee(resolved));
		problemChangeDirector.changeProblemProperty(target, a -> a.setPinned(pin));
	}
}
