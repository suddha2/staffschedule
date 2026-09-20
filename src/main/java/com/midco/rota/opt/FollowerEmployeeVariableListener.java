package com.midco.rota.opt;

import org.optaplanner.core.api.domain.variable.VariableListener;
import org.optaplanner.core.api.score.director.ScoreDirector;

import com.midco.rota.model.Employee;
import com.midco.rota.model.FollowerShiftAssignment;
import com.midco.rota.model.Rota;
import com.midco.rota.model.WorkShiftAssignment;

/**
 * Keeps each follower's shadow {@code employee} equal to its paired leader's
 * {@code employee}, live inside the solver (the canonical case being a SLEEP_IN
 * mirroring its LONG_DAY). Fires whenever a {@link WorkShiftAssignment}'s employee
 * changes (or the entity is added), and mirrors the value onto the leader's paired
 * {@link FollowerShiftAssignment}.
 *
 * <p>Pairing links ({@code WorkShiftAssignment.pairedFollower}) are established at
 * load time (see PairingLinker). Work shifts that lead no follower have a null link
 * and are ignored.
 */
public class FollowerEmployeeVariableListener implements VariableListener<Rota, WorkShiftAssignment> {

	private void sync(ScoreDirector<Rota> scoreDirector, WorkShiftAssignment work) {
		FollowerShiftAssignment follower = work.getPairedFollower();
		if (follower == null) {
			return;
		}
		Employee desired = work.getEmployee();
		if (follower.getEmployee() == desired) {
			return; // already mirrored
		}
		scoreDirector.beforeVariableChanged(follower, "employee");
		follower.setEmployee(desired);
		scoreDirector.afterVariableChanged(follower, "employee");
	}

	@Override
	public void afterVariableChanged(ScoreDirector<Rota> scoreDirector, WorkShiftAssignment work) {
		sync(scoreDirector, work);
	}

	@Override
	public void afterEntityAdded(ScoreDirector<Rota> scoreDirector, WorkShiftAssignment work) {
		sync(scoreDirector, work);
	}

	@Override
	public void beforeVariableChanged(ScoreDirector<Rota> scoreDirector, WorkShiftAssignment work) {
		// no-op
	}

	@Override
	public void beforeEntityAdded(ScoreDirector<Rota> scoreDirector, WorkShiftAssignment work) {
		// no-op
	}

	@Override
	public void beforeEntityRemoved(ScoreDirector<Rota> scoreDirector, WorkShiftAssignment work) {
		// no-op
	}

	@Override
	public void afterEntityRemoved(ScoreDirector<Rota> scoreDirector, WorkShiftAssignment work) {
		// no-op
	}
}
