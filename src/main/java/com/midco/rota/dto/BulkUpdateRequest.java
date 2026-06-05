package com.midco.rota.dto;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import com.midco.rota.util.Gender;
import com.midco.rota.util.ShiftType;

/**
 * Body of {@code PUT /api/shift-templates/bulk-update}.
 *
 * <p>Shape mirrors what the FE sends — a flat selector
 * ({@code location}, {@code shiftType}, {@code region}) identifying the cohort
 * of templates to update, plus a nested {@code updates} object holding the
 * field values to propagate across them.
 *
 * <p>The previous implementation took {@code @RequestBody ShiftTemplate} directly,
 * which caused Jackson to silently drop the nested {@code updates} field. Every
 * field on the loaded templates was then overwritten with the primitive
 * default (false for {@code active}, 0 for {@code empCount}/{@code priority},
 * null for everything else) — quietly deactivating and corrupting the cohort.
 * This DTO makes the contract explicit so a mismatched body fails noisily
 * rather than silently.
 *
 * <p>{@code shiftType} on the selector is the <b>existing</b> type of the
 * templates being updated. Bulk update does not change shift type — the FE
 * enforces a read-only dropdown in bulk mode, and the BE here ignores any
 * attempt to do so.
 */
public class BulkUpdateRequest {

	private String location;
	private ShiftType shiftType;
	private String region;
	private TemplateFieldUpdates updates;

	public BulkUpdateRequest() {
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public ShiftType getShiftType() {
		return shiftType;
	}

	public void setShiftType(ShiftType shiftType) {
		this.shiftType = shiftType;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public TemplateFieldUpdates getUpdates() {
		return updates;
	}

	public void setUpdates(TemplateFieldUpdates updates) {
		this.updates = updates;
	}

	/**
	 * The mutable fields the bulk update is allowed to change. A {@code null}
	 * value means "do not touch this field" — only non-null values get applied
	 * to the matched templates. {@code Boolean} (not primitive) for {@code
	 * active} so a missing value doesn't silently deactivate the cohort.
	 */
	public static class TemplateFieldUpdates {
		private LocalTime startTime;
		private LocalTime endTime;
		private LocalTime breakStart;
		private LocalTime breakEnd;
		private BigDecimal totalHours;
		private Gender requiredGender;
		private List<String> requiredSkills;
		private Integer empCount;
		private Integer priority;
		private Boolean active;

		public LocalTime getStartTime() {
			return startTime;
		}

		public void setStartTime(LocalTime startTime) {
			this.startTime = startTime;
		}

		public LocalTime getEndTime() {
			return endTime;
		}

		public void setEndTime(LocalTime endTime) {
			this.endTime = endTime;
		}

		public LocalTime getBreakStart() {
			return breakStart;
		}

		public void setBreakStart(LocalTime breakStart) {
			this.breakStart = breakStart;
		}

		public LocalTime getBreakEnd() {
			return breakEnd;
		}

		public void setBreakEnd(LocalTime breakEnd) {
			this.breakEnd = breakEnd;
		}

		public BigDecimal getTotalHours() {
			return totalHours;
		}

		public void setTotalHours(BigDecimal totalHours) {
			this.totalHours = totalHours;
		}

		public Gender getRequiredGender() {
			return requiredGender;
		}

		public void setRequiredGender(Gender requiredGender) {
			this.requiredGender = requiredGender;
		}

		public List<String> getRequiredSkills() {
			return requiredSkills;
		}

		public void setRequiredSkills(List<String> requiredSkills) {
			this.requiredSkills = requiredSkills;
		}

		public Integer getEmpCount() {
			return empCount;
		}

		public void setEmpCount(Integer empCount) {
			this.empCount = empCount;
		}

		public Integer getPriority() {
			return priority;
		}

		public void setPriority(Integer priority) {
			this.priority = priority;
		}

		public Boolean getActive() {
			return active;
		}

		public void setActive(Boolean active) {
			this.active = active;
		}
	}
}
