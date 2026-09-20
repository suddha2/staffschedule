package com.midco.rota.model;

import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.variable.ShadowVariable;

import com.midco.rota.opt.FollowerEmployeeVariableListener;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * The follower half of a paired shift: its {@code employee} is a
 * {@link ShadowVariable} that mirrors its paired leader
 * ({@link WorkShiftAssignment}) continuously inside the solver, via
 * {@link FollowerEmployeeVariableListener}. A SLEEP_IN following a LONG_DAY is the
 * canonical case, but the role is general — any shift type flagged as a follower
 * is stored here (discriminator {@code FOLLOWER}), decoupled from the shift-type
 * label. The backing field lives on {@link ShiftAssignment}; the annotation is on
 * the overridden getter so both subclasses share the {@code employee_id} column.
 */
@Entity
@DiscriminatorValue("FOLLOWER")
@PlanningEntity
public class FollowerShiftAssignment extends ShiftAssignment {

	public FollowerShiftAssignment() {
	}

	public FollowerShiftAssignment(Shift shift) {
		super(shift);
	}

	@Override
	@ShadowVariable(variableListenerClass = FollowerEmployeeVariableListener.class, sourceEntityClass = WorkShiftAssignment.class, sourceVariableName = "employee")
	public Employee getEmployee() {
		return super.getEmployee();
	}
}
