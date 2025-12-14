package com.midco.rota.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA Entity for schedule_change table NO LOMBOK VERSION
 */
@Entity
@Table(name = "schedule_change")
public class ScheduleChange {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "from_version_id", nullable = false)
	private Long fromVersionId;

	@Column(name = "to_version_id", nullable = false)
	private Long toVersionId;

	@Column(name = "shift_id", nullable = false)
	private Long shiftId;

	@Enumerated(EnumType.STRING)
	@Column(name = "change_type", nullable = false, length = 20)
	private ChangeType changeType;

	@Column(name = "old_employee_id")
	private Integer oldEmployeeId;

	@Column(name = "new_employee_id")
	private Integer newEmployeeId;

	@Enumerated(EnumType.STRING)
	@Column(name = "change_reason", length = 50)
	private ChangeReason changeReason;

	@Column(name = "changed_at")
	private LocalDateTime changedAt = LocalDateTime.now();

	@Column(name = "changed_by", nullable = false, length = 100)
	private String changedBy;

	// Enums

	public enum ChangeType {
		ASSIGNED, UNASSIGNED, REASSIGNED, ADDED, REMOVED
	}

	public enum ChangeReason {
		MANUAL_ASSIGN, MANUAL_REMOVE, MANUAL_DRAG_DROP, AUTO_SOLVER, ROLLBACK, IMPORT, SYSTEM, OTHER
	}

	// Constructors

	public ScheduleChange() {
	}

	// Getters and Setters

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getFromVersionId() {
		return fromVersionId;
	}

	public void setFromVersionId(Long fromVersionId) {
		this.fromVersionId = fromVersionId;
	}

	public Long getToVersionId() {
		return toVersionId;
	}

	public void setToVersionId(Long toVersionId) {
		this.toVersionId = toVersionId;
	}

	public Long getShiftId() {
		return shiftId;
	}

	public void setShiftId(Long shiftId) {
		this.shiftId = shiftId;
	}

	public ChangeType getChangeType() {
		return changeType;
	}

	public void setChangeType(ChangeType changeType) {
		this.changeType = changeType;
	}

	public Integer getOldEmployeeId() {
		return oldEmployeeId;
	}

	public void setOldEmployeeId(Integer oldEmployeeId) {
		this.oldEmployeeId = oldEmployeeId;
	}

	public Integer getNewEmployeeId() {
		return newEmployeeId;
	}

	public void setNewEmployeeId(Integer newEmployeeId) {
		this.newEmployeeId = newEmployeeId;
	}

	public ChangeReason getChangeReason() {
		return changeReason;
	}

	public void setChangeReason(ChangeReason changeReason) {
		this.changeReason = changeReason;
	}

	public LocalDateTime getChangedAt() {
		return changedAt;
	}

	public void setChangedAt(LocalDateTime changedAt) {
		this.changedAt = changedAt;
	}

	public String getChangedBy() {
		return changedBy;
	}

	public void setChangedBy(String changedBy) {
		this.changedBy = changedBy;
	}

	// Builder pattern

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private Long fromVersionId;
		private Long toVersionId;
		private Long shiftId;
		private ChangeType changeType;
		private Integer oldEmployeeId;
		private Integer newEmployeeId;
		private ChangeReason changeReason;
		private String changedBy;

		public Builder fromVersionId(Long fromVersionId) {
			this.fromVersionId = fromVersionId;
			return this;
		}

		public Builder toVersionId(Long toVersionId) {
			this.toVersionId = toVersionId;
			return this;
		}

		public Builder shiftId(Long shiftId) {
			this.shiftId = shiftId;
			return this;
		}

		public Builder changeType(ChangeType changeType) {
			this.changeType = changeType;
			return this;
		}

		public Builder oldEmployeeId(Integer oldEmployeeId) {
			this.oldEmployeeId = oldEmployeeId;
			return this;
		}

		public Builder newEmployeeId(Integer newEmployeeId) {
			this.newEmployeeId = newEmployeeId;
			return this;
		}

		public Builder changeReason(ChangeReason changeReason) {
			this.changeReason = changeReason;
			return this;
		}

		public Builder changedBy(String changedBy) {
			this.changedBy = changedBy;
			return this;
		}

		public ScheduleChange build() {
			ScheduleChange change = new ScheduleChange();
			change.setFromVersionId(this.fromVersionId);
			change.setToVersionId(this.toVersionId);
			change.setShiftId(this.shiftId);
			change.setChangeType(this.changeType);
			change.setOldEmployeeId(this.oldEmployeeId);
			change.setNewEmployeeId(this.newEmployeeId);
			change.setChangeReason(this.changeReason);
			change.setChangedBy(this.changedBy);
			return change;
		}
	}
}