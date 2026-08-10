package com.midco.rota.opt;

import org.optaplanner.core.api.domain.variable.VariableListener;
import org.optaplanner.core.api.score.director.ScoreDirector;

import com.midco.rota.model.Employee;
import com.midco.rota.model.Rota;
import com.midco.rota.model.SleepInShiftAssignment;
import com.midco.rota.model.WorkShiftAssignment;

/**
 * Keeps each SLEEP_IN's shadow {@code employee} equal to its paired LONG_DAY's
 * {@code employee}, live inside the solver. Fires whenever a
 * {@link WorkShiftAssignment}'s employee changes (or the entity is added), and
 * mirrors the value onto the LONG_DAY's paired {@link SleepInShiftAssignment}.
 *
 * <p>Pairing links ({@code WorkShiftAssignment.pairedSleepIn}) are established at
 * load time (see PairingLinker). Non-LONG_DAY work shifts, and LONG_DAYs with no
 * paired sleep-in, have a null link and are ignored.
 */
public class SleepInEmployeeVariableListener implements VariableListener<Rota, WorkShiftAssignment> {

	private void sync(ScoreDirector<Rota> scoreDirector, WorkShiftAssignment work) {
		SleepInShiftAssignment sleepIn = work.getPairedSleepIn();
		if (sleepIn == null) {
			return;
		}
		Employee desired = work.getEmployee();
		if (sleepIn.getEmployee() == desired) {
			return; // already mirrored
		}
		scoreDirector.beforeVariableChanged(sleepIn, "employee");
		sleepIn.setEmployee(desired);
		scoreDirector.afterVariableChanged(sleepIn, "employee");
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
