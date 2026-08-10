package com.midco.rota.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.optaplanner.core.api.domain.entity.PlanningPin;
import org.optaplanner.core.api.domain.lookup.PlanningId;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.midco.rota.util.ShiftType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

/**
 * Base of the shift-assignment hierarchy (single-table inheritance on
 * {@code rota_shift_assignment}, discriminator column {@code assignment_type}).
 *
 * <p>Split into two planning entities so SLEEP_IN can carry a genuinely different
 * planning role from work shifts:
 * <ul>
 *   <li>{@link WorkShiftAssignment} (DAY / LONG_DAY / WAKING_NIGHT / FLOATING) —
 *       {@code employee} is a genuine {@code @PlanningVariable}.</li>
 *   <li>{@link SleepInShiftAssignment} — {@code employee} is a
 *       {@code @ShadowVariable} that mirrors its paired LONG_DAY continuously
 *       inside the solver (retires the post-solve SleepInPairingService).</li>
 * </ul>
 * The {@code employee} field lives in the subclasses (each maps the shared
 * {@code employee_id} column) with its own OptaPlanner annotation; the base only
 * declares the abstract accessors, so the ~40 constraints that call
 * {@code sa.getEmployee()} on {@code ShiftAssignment} are unchanged.
 */
@Entity(name = "rota_shift_assignment")
@Table(name = "rota_shift_assignment")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "assignment_type")
public abstract class ShiftAssignment {

	@Transient
	@PlanningId
	private String planningId;

	@OneToOne(cascade = CascadeType.PERSIST)
	private Shift shift;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "rota_id")
	@JsonIgnore
	private Rota rota;

	@Transient
	private List<String> diagnosticReasons = new ArrayList<>();

	@Transient
	private List<String> unassignmentReasons = new ArrayList<>();

	@Column(name = "is_pinned")
	private boolean pinned = false;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	/** True once the response-bound rules have hidden this slot from the mobile
	 *  Available list. Cleared on re-publish. */
	@Column(name = "withheld", nullable = false)
	private boolean withheld = false;

	/** True when this assignment's employee was set by approving a mobile shift
	 *  request (vs solver / manual). Cleared on any manual edit. Drives the
	 *  ViewSchedule highlight. */
	@Column(name = "filled_via_request", nullable = false)
	private boolean filledViaRequest = false;

	@PlanningPin
	public boolean isPinned() {
		if (pinned) return true;
		if (shift == null || shift.getShiftTemplate() == null) return false;
		ShiftType type = shift.getShiftTemplate().getShiftType();
		// FLOATING is reserved for the mobile publish-and-grab flow — solver leaves it null.
		// SLEEP_IN is no longer pinned here: it's a shadow variable (SleepInShiftAssignment)
		// that mirrors its paired LONG_DAY continuously.
		return type == ShiftType.FLOATING;
	}

	protected ShiftAssignment() {
	}

	protected ShiftAssignment(Shift shift) {
		this.shift = shift;
		this.planningId = UUID.randomUUID().toString();
	}

	/** Genuine planning variable in {@link WorkShiftAssignment}; shadow in {@link SleepInShiftAssignment}. */
	public abstract Employee getEmployee();

	public abstract void setEmployee(Employee employee);

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

	@Override
	public String toString() {
		Employee employee = getEmployee();
		return shift.getShiftTemplate().getLocation() + " " + shift.getShiftTemplate().getShiftType().toString() + " "
				+ shift.getShiftStart() + " " + shift.getShiftTemplate().getStartTime() + " -> "
				+ (employee == null ? "UNASSIGNED" : employee.toString());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || !(o instanceof ShiftAssignment))
			return false;
		ShiftAssignment that = (ShiftAssignment) o;

		if (planningId != null && that.planningId != null) {
			return Objects.equals(planningId, that.planningId);
		}
		if (id != null && that.id != null) {
			return Objects.equals(id, that.id);
		}
		return false;
	}

	@Override
	public int hashCode() {
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

	public boolean isWithheld() {
		return withheld;
	}

	public void setWithheld(boolean withheld) {
		this.withheld = withheld;
	}

	public boolean isFilledViaRequest() {
		return filledViaRequest;
	}

	public void setFilledViaRequest(boolean filledViaRequest) {
		this.filledViaRequest = filledViaRequest;
	}
}
