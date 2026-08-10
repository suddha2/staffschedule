package com.midco.rota.model;

import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.variable.PlanningVariable;

import com.midco.rota.opt.ShiftAssignmentDifficultyComparator;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;

/**
 * A work shift assignment (DAY / LONG_DAY / WAKING_NIGHT / FLOATING): its
 * {@code employee} is the genuine {@link PlanningVariable} the solver assigns.
 */
@Entity
@DiscriminatorValue("WORK")
@PlanningEntity(difficultyComparatorClass = ShiftAssignmentDifficultyComparator.class)
public class WorkShiftAssignment extends ShiftAssignment {

	@ManyToOne
	@JoinColumn(name = "employee_id")
	@PlanningVariable(valueRangeProviderRefs = "employeeRange", nullable = true)
	private Employee employee;

	/**
	 * For LONG_DAY assignments, the SLEEP_IN slot paired to this one (same
	 * location + date). Transient; linked at load time so the shadow-variable
	 * listener can mirror this assignment's employee onto its sleep-in. Null for
	 * non-LONG_DAY work shifts or when no sleep-in is paired.
	 */
	@Transient
	private SleepInShiftAssignment pairedSleepIn;

	public WorkShiftAssignment() {
	}

	public WorkShiftAssignment(Shift shift) {
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

	public SleepInShiftAssignment getPairedSleepIn() {
		return pairedSleepIn;
	}

	public void setPairedSleepIn(SleepInShiftAssignment pairedSleepIn) {
		this.pairedSleepIn = pairedSleepIn;
	}
}
