package com.midco.rota.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.optaplanner.core.api.domain.lookup.PlanningId;
import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty;
import org.optaplanner.core.api.domain.solution.PlanningScore;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.domain.solution.ProblemFactCollectionProperty;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.midco.rota.util.IdealShiftCount;
import com.midco.rota.util.ShiftType;

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
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")

public class Rota {

//	@Transient
//	private static final AtomicLong COUNTER = new AtomicLong();

	@Transient
	@PlanningId
	private Long planningId;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToMany(cascade = CascadeType.MERGE)
	@JoinTable(name = "rota_employee", // your actual join table
			joinColumns = @JoinColumn(name = "rota_id"), inverseJoinColumns = @JoinColumn(name = "employee_id"))
	@ValueRangeProvider(id = "employeeRange")
	@ProblemFactCollectionProperty
	private List<Employee> employeeList;

	@OneToMany(mappedBy = "rota", cascade = CascadeType.ALL, orphanRemoval = true)
	@PlanningEntityCollectionProperty
	@JsonManagedReference
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
		// this.planningId = COUNTER.incrementAndGet();

		for (ShiftAssignment sa : this.shiftAssignmentList) {
			sa.setRota(this); // safe linkage for JPA requirements
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

	public Long getPlanningId() {
		return planningId;
	}

	public void setPlanningId(Long planningId) {
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

	public HashMap<String, Map<String, Integer>> rotaSummaryStats() {
		HashMap<String, Map<String, Integer>> summaryStats = new HashMap<>();

		for (ShiftAssignment sa : this.getShiftAssignmentList()) {

			if (sa.getShift() == null || sa.getShift().getShiftTemplate() == null
					|| sa.getShift().getShiftTemplate().getShiftType() == null) {
				continue; // Skip if any part is null
			}

			String shiftTypeName = sa.getShift().getShiftTemplate().getShiftType().name();
			boolean isAssigned = sa.getEmployee() != null;

			summaryStats.computeIfAbsent(shiftTypeName, k -> {
				Map<String, Integer> counts = new HashMap<>();
				counts.put("assigned", 0);
				counts.put("unassigned", 0);
				return counts;
			});

			String key = isAssigned ? "assigned" : "unassigned";
			summaryStats.get(shiftTypeName).merge(key, 1, Integer::sum);
		}

		return summaryStats;
	}

	public Map<ShiftType, Integer> shiftTypeSummary() {
		Map<ShiftType, Integer> shiftTypeCounts = this.getShiftAssignmentList().stream()
				// ✅ Filter out nulls BEFORE mapping
				.filter(sa -> sa != null && sa.getShift() != null && sa.getShift().getShiftTemplate() != null
						&& sa.getShift().getShiftTemplate().getShiftType() != null)
				.map(sa -> sa.getShift().getShiftTemplate().getShiftType())
				.collect(Collectors.toMap(shiftType -> shiftType, shiftType -> 1, Integer::sum));

		return shiftTypeCounts;
	}

	public Integer loctionCount() {

		Integer locCount = (int) this.getShiftAssignmentList().stream().map(ShiftAssignment::getShift)
				.map(Shift::getShiftTemplate).map(ShiftTemplate::getLocation).filter(Objects::nonNull).distinct()
				.count();

		return locCount;
	}

	public Integer shiftCount() {
		Integer shiftCount = (int) this.getShiftAssignmentList().stream().map(ShiftAssignment::getShift)
				.filter(Objects::nonNull).distinct().count();

		return shiftCount;
	}

	public Integer empCount() {
		Integer empCount = this.employeeList.size();

		return empCount;
	}

	public Map<String, Integer> shiftAssignmentStats() {
		Map<String, Integer> results = new HashMap<>();

		Map<ShiftType, Long> assignedShiftCounts = this.getShiftAssignmentList().stream()
				.filter(sa -> sa != null && sa.getEmployee() != null)
				// ✅ Filter out nulls BEFORE mapping
				.filter(sa -> sa.getShift() != null && sa.getShift().getShiftTemplate() != null
						&& sa.getShift().getShiftTemplate().getShiftType() != null)
				.map(sa -> sa.getShift().getShiftTemplate().getShiftType())
				.collect(Collectors.groupingBy(shiftType -> shiftType, Collectors.counting()));

		// Add counts for all shift types (including 0s)
		for (ShiftType type : ShiftType.values()) {
			Long count = assignedShiftCounts.getOrDefault(type, 0L);
			results.put(type.name(), count.intValue());
		}

		// Add total
		int total = assignedShiftCounts.values().stream().mapToInt(Long::intValue).sum();
		results.put("TotalAssigned", total);

		return results;
	}

}
