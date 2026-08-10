package com.midco.rota.model;

import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.variable.ShadowVariable;

import com.midco.rota.opt.SleepInEmployeeVariableListener;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * A SLEEP_IN shift assignment: its {@code employee} is a {@link ShadowVariable}
 * that mirrors its paired LONG_DAY ({@link WorkShiftAssignment}) continuously
 * inside the solver, via {@link SleepInEmployeeVariableListener}. This replaces
 * the old post-solve SleepInPairingService pairing. The backing field lives on
 * {@link ShiftAssignment}; the annotation is on the overridden getter so both
 * subclasses share the {@code employee_id} column.
 */
@Entity
@DiscriminatorValue("SLEEP_IN")
@PlanningEntity
public class SleepInShiftAssignment extends ShiftAssignment {

	public SleepInShiftAssignment() {
	}

	public SleepInShiftAssignment(Shift shift) {
		super(shift);
	}

	@Override
	@ShadowVariable(variableListenerClass = SleepInEmployeeVariableListener.class, sourceEntityClass = WorkShiftAssignment.class, sourceVariableName = "employee")
	public Employee getEmployee() {
		return super.getEmployee();
	}
}
