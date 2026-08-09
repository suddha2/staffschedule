package com.midco.rota.opt;

import org.optaplanner.core.api.solver.change.ProblemChange;
import org.optaplanner.core.api.solver.change.ProblemChangeDirector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.midco.rota.model.Rota;
import com.midco.rota.model.ShiftAssignment;

/**
 * Live edit (P3): pin or unpin a slot without changing its employee. Pinning
 * locks the current assignment so the solver won't move it; unpinning returns it
 * to the solver's control.
 *
 * <p>Note: SLEEP_IN and FLOATING are always pinned via
 * {@link ShiftAssignment#isPinned()} regardless of this flag, so this change is a
 * no-op for those types.
 */
public class SetPinProblemChange implements ProblemChange<Rota> {

	private static final Logger logger = LoggerFactory.getLogger(SetPinProblemChange.class);

	private final long assignmentId;
	private final boolean pinned;

	public SetPinProblemChange(long assignmentId, boolean pinned) {
		this.assignmentId = assignmentId;
		this.pinned = pinned;
	}

	@Override
	public void doChange(Rota workingSolution, ProblemChangeDirector problemChangeDirector) {
		ShiftAssignment target = ProblemChangeSupport.findAssignment(workingSolution, assignmentId);
		if (target == null) {
			logger.warn("Live pin skipped: no assignment with id {} in working solution", assignmentId);
			return;
		}
		problemChangeDirector.changeProblemProperty(target, a -> a.setPinned(pinned));
	}
}
