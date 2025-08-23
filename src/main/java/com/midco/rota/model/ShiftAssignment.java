package com.midco.rota.model;

import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.lookup.PlanningId;
import org.optaplanner.core.api.domain.variable.PlanningVariable;

@PlanningEntity
public class ShiftAssignment {
	private Shift shift;

	@PlanningId
	private long id;

	@PlanningVariable(valueRangeProviderRefs = "employeeRange", nullable = true)
	private Employee employee; // planning variable

	public ShiftAssignment() {
	}

	public ShiftAssignment(Shift shift, long id) {
		this.shift = shift;

		this.id = id;
	}

	public Shift getShift() {
		return shift;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setShift(Shift shift) {
		this.shift = shift;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Employee getEmployee() {
		return employee;
	}

	public void setEmployee(Employee employee) {
		this.employee = employee;
	}

	@Override
	public String toString() {
		return shift.getShiftTemplate().getLocation() + " " + shift.getShiftStart() + " "
				+ shift.getShiftTemplate().getStartTime() + " -> "
				+ (employee == null ? "UNASSIGNED" : employee.toString());
	}

}
