package com.midco.rota.opt;

import java.util.Objects;

import org.optaplanner.core.api.score.director.ScoreDirector;
import org.optaplanner.core.impl.heuristic.move.Move;

import com.midco.rota.model.Employee;
import com.midco.rota.model.Rota;
import com.midco.rota.model.ShiftAssignment;

public class SwapEmployeeMove implements Move<Rota> {
	private final ShiftAssignment assignment;
	private final Employee fromEmployee;
	private final Employee toEmployee;

	public SwapEmployeeMove(ShiftAssignment assignment2, Employee toEmployee2, Employee fromEmployee2) {

		this.assignment = assignment2;
		this.fromEmployee = fromEmployee2;
		this.toEmployee = toEmployee2;
	}

	// Constructor, getters, etc.

	@Override
	public boolean isMoveDoable(ScoreDirector<Rota> scoreDirector) {
		return !Objects.equals(fromEmployee, toEmployee);
	}

	@Override
	public Move<Rota> doMove(ScoreDirector<Rota> scoreDirector) {
		scoreDirector.beforeVariableChanged(assignment, "employee");
		assignment.setEmployee(toEmployee);
		scoreDirector.afterVariableChanged(assignment, "employee");
		return new SwapEmployeeMove(assignment, toEmployee, fromEmployee); // undo move
	}

	// Implement equals(), hashCode(), and toString() for debugging
}
