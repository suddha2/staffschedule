package com.midco.rota.model;

import java.util.UUID;

import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.lookup.PlanningId;
import org.optaplanner.core.api.domain.variable.PlanningVariable;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
@PlanningEntity
public class ShiftAssignment {
	
	@Transient
	@PlanningId
    private String planningId ; 
	
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
		return shift.getShiftTemplate().getLocation() + " " + shift.getShiftStart() + " "
				+ shift.getShiftTemplate().getStartTime() + " -> "
				+ (employee == null ? "UNASSIGNED" : employee.toString());
	}
	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ShiftAssignment)) return false;
        return planningId.equals(((ShiftAssignment) o).planningId);
    }

    @Override
    public int hashCode() {
        return planningId.hashCode();
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
}
