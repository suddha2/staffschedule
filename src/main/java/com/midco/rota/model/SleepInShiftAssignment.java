package com.midco.rota.model;

import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.variable.ShadowVariable;

import com.midco.rota.opt.SleepInEmployeeVariableListener;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * A SLEEP_IN shift assignment: its {@code employee} is a {@link ShadowVariable}
 * that mirrors its paired LONG_DAY ({@link WorkShiftAssignment}) continuously
 * inside the solver, via {@link SleepInEmployeeVariableListener}. This replaces
 * the old post-solve SleepInPairingService pairing.
 */
@Entity
@DiscriminatorValue("SLEEP_IN")
@PlanningEntity
public class SleepInShiftAssignment extends ShiftAssignment {

	@ManyToOne
	@JoinColumn(name = "employee_id")
	@ShadowVariable(variableListenerClass = SleepInEmployeeVariableListener.class, sourceEntityClass = WorkShiftAssignment.class, sourceVariableName = "employee")
	private Employee employee;

	public SleepInShiftAssignment() {
	}

	public SleepInShiftAssignment(Shift shift) {
		super(shift);
	}

	@Override
	public Employee getEmployee() {
		return employee;
	}

	@Override
	public void setEmployee(Employee employee) {
		this.employee = employee;
	}
}
