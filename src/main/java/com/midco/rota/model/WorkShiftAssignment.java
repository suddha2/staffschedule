package com.midco.rota.model;

import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.variable.PlanningVariable;

import com.midco.rota.opt.ShiftAssignmentDifficultyComparator;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Transient;

/**
 * A work shift assignment (DAY / LONG_DAY / WAKING_NIGHT / FLOATING): its
 * {@code employee} is the genuine {@link PlanningVariable} the solver assigns.
 * The backing field lives on {@link ShiftAssignment}; the annotation is on the
 * overridden getter so both subclasses share the {@code employee_id} column.
 */
@Entity
@DiscriminatorValue("WORK")
@PlanningEntity(difficultyComparatorClass = ShiftAssignmentDifficultyComparator.class)
public class WorkShiftAssignment extends ShiftAssignment {

	/**
	 * For a leader assignment (canonically a LONG_DAY), the follower slot paired to
	 * this one (same location + date). Transient; linked at load time so the
	 * shadow-variable listener can mirror this assignment's employee onto its
	 * follower. Null for work shifts that lead no follower.
	 */
	@Transient
	private FollowerShiftAssignment pairedFollower;

	public WorkShiftAssignment() {
	}

	public WorkShiftAssignment(Shift shift) {
		super(shift);
	}

	@Override
	@PlanningVariable(valueRangeProviderRefs = "employeeRange", nullable = true)
	public Employee getEmployee() {
		return super.getEmployee();
	}

	public FollowerShiftAssignment getPairedFollower() {
		return pairedFollower;
	}

	public void setPairedFollower(FollowerShiftAssignment pairedFollower) {
		this.pairedFollower = pairedFollower;
	}
}
