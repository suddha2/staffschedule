package com.midco.rota.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.entity.PlanningPin;
import org.optaplanner.core.api.domain.lookup.PlanningId;
import org.optaplanner.core.api.domain.variable.PlanningVariable;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.midco.rota.opt.ShiftAssignmentDifficultyComparator;
import com.midco.rota.util.ShiftType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Transient;

@Entity(name = "rota_shift_assignment")
@PlanningEntity(difficultyComparatorClass = ShiftAssignmentDifficultyComparator.class)


public class ShiftAssignment {

	@Transient
	@PlanningId
	private String planningId;

	@OneToOne(cascade = CascadeType.PERSIST)
	private Shift shift;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@PlanningVariable(valueRangeProviderRefs = "employeeRange", nullable = true)
	private Employee employee; // planning variable

	@ManyToOne
	@JoinColumn(name = "rota_id")
	@JsonIgnore
	private Rota rota;

	@Transient
	private List<String> diagnosticReasons = new ArrayList<>();

	@Transient
	private List<String> unassignmentReasons = new ArrayList<>();

	@Transient
	private boolean pinned = false;

	@PlanningPin
	public boolean isPinned() {
		return pinned || (shift != null && shift.getShiftTemplate() != null
				&& shift.getShiftTemplate().getShiftType() == ShiftType.SLEEP_IN);
	}

	public ShiftAssignment() {
	}

	public ShiftAssignment(Shift shift) {
		this.shift = shift;

		this.planningId = UUID.randomUUID().toString();
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

	public Employee getEmployee() {
		return employee;
	}

	public void setEmployee(Employee employee) {
		this.employee = employee;
	}

	@Override
	public String toString() {
		return shift.getShiftTemplate().getLocation() + " " + shift.getShiftTemplate().getShiftType().toString() + " "
				+ shift.getShiftStart() + " " + shift.getShiftTemplate().getStartTime() + " -> "
				+ (employee == null ? "UNASSIGNED" : employee.toString());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		ShiftAssignment that = (ShiftAssignment) o;

		// ✅ Compare planningId if both have it
		if (planningId != null && that.planningId != null) {
			return Objects.equals(planningId, that.planningId);
		}

		// ✅ Fall back to database id
		if (id != null && that.id != null) {
			return Objects.equals(id, that.id);
		}

		// ✅ If neither has id, they're only equal if same instance
		return false;
	}

	@Override
	public int hashCode() {
		// ✅ Use planningId if available, otherwise use id
		if (planningId != null) {
			return Objects.hash(planningId);
		}
		return id != null ? Objects.hash(id) : 0;
	}

	public String getPlanningId() {
		return planningId;
	}

	public void setPlanningId(String planningId) {
		this.planningId = planningId;
	}

	public Rota getRota() {
		return rota;
	}

	public void setRota(Rota rota) {
		this.rota = rota;
	}

	public List<String> getDiagnosticReasons() {
		return diagnosticReasons;
	}

	public void setDiagnosticReasons(List<String> diagnosticReasons) {
		this.diagnosticReasons = diagnosticReasons;
	}

	public List<String> getUnassignmentReasons() {
		return unassignmentReasons;
	}

	public void setUnassignmentReasons(List<String> reasons) {
		this.unassignmentReasons = reasons;
	}

	public void setPinned(boolean pinned) {
		this.pinned = pinned;
	}

	public boolean getPinned() {
		return pinned;
	}
}
