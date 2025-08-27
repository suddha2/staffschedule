package com.midco.rota.model;

import java.util.List;
import java.util.UUID;

import org.optaplanner.core.api.domain.lookup.PlanningId;
import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty;
import org.optaplanner.core.api.domain.solution.PlanningScore;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.domain.solution.ProblemFactCollectionProperty;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;

import com.midco.rota.util.IdealShiftCount;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Transient;

@Entity
@PlanningSolution
public class Rota {

	@Transient
	@PlanningId
	private String planningId;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToMany(cascade = CascadeType.ALL)
	@JoinTable(name = "rota_employee", // your actual join table
			joinColumns = @JoinColumn(name = "rota_id"), inverseJoinColumns = @JoinColumn(name = "employee_id"))
	@ValueRangeProvider(id = "employeeRange")
	@ProblemFactCollectionProperty
	private List<Employee> employeeList;

	@OneToMany(mappedBy = "rota", cascade = CascadeType.ALL)
	@PlanningEntityCollectionProperty
	private List<ShiftAssignment> shiftAssignmentList;

	@Transient
	@PlanningScore
	private HardSoftScore score;

	@Transient
	@ProblemFactCollectionProperty
	private List<IdealShiftCount> idealShiftCountList;

	public Rota() {
	}

	public Rota(List<Employee> employeeList, List<ShiftAssignment> shiftAssignmentList) {
		this.employeeList = employeeList;
		this.shiftAssignmentList = shiftAssignmentList;
		int ideal = shiftAssignmentList.size() / employeeList.size();
		this.idealShiftCountList = List.of(new IdealShiftCount(ideal));
		this.planningId = UUID.randomUUID().toString();
		
		 for (ShiftAssignment sa : this.shiftAssignmentList) {
	            sa.setRota(this); // safe linkage
	        }
		
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setEmployeeList(List<Employee> employeeList) {
		this.employeeList = employeeList;
	}

	public void setShiftAssignmentList(List<ShiftAssignment> shiftAssignmentList) {
		this.shiftAssignmentList = shiftAssignmentList;
	}

	public List<Employee> getEmployeeList() {
		return employeeList;
	}

	public List<ShiftAssignment> getShiftAssignmentList() {
		return shiftAssignmentList;
	}

	public HardSoftScore getScore() {
		return score;
	}

	public void setScore(HardSoftScore score) {
		this.score = score;
	}

	public List<IdealShiftCount> getIdealShiftCountList() {
		return idealShiftCountList;
	}

	public void setIdealShiftCountList(List<IdealShiftCount> idealShiftCountList) {
		this.idealShiftCountList = idealShiftCountList;
	}

	@Override
	public String toString() {
		return "Rota [id=" + id + ", employeeList=" + employeeList + ", shiftAssignmentList=" + shiftAssignmentList
				+ ", score=" + score + ", idealShiftCountList=" + idealShiftCountList + "]";
	}

	public String getPlanningId() {
		return planningId;
	}

	public void setPlanningId(String planningId) {
		this.planningId = planningId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof Rota))
			return false;
		return planningId.equals(((Rota) o).planningId);
	}

	@Override
	public int hashCode() {
		return planningId.hashCode();
	}

}
