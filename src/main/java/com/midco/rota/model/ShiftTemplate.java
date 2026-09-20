package com.midco.rota.model;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import com.midco.rota.converter.StringListConverter;
import com.midco.rota.util.Gender;
import com.midco.rota.util.ShiftType;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

@Entity
@Table(name = "shift_templates")
@NamedQuery(name = "ShiftTemplate.findAllRegion", query = "select distinct s.region from ShiftTemplate s where active=true order by 1")

public class ShiftTemplate {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(length = 50)
	private String location;

	@Column(length = 50)
	private String region;

	// Data-driven shift type: stored as a free-text code (matches shift_type.code) so a
	// new type needs no enum change. getShiftType() still returns the built-in enum for
	// the six known codes (null otherwise) so existing callers keep working unchanged.
	@Column(name = "shift_type", length = 40)
	private String shiftTypeCode;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "day_of_week", length = 15)
	private DayOfWeek dayOfWeek;

	@Column(name = "start_time")
	private LocalTime startTime;

	@Column(name = "end_time")
	private LocalTime endTime;

	@Column(name = "break_start")
	private LocalTime breakStart;

	@Column(name = "break_end")
	private LocalTime breakEnd;

	@Column(name = "total_hours", precision = 4, scale = 2)
	private BigDecimal totalHours;

	@Enumerated(EnumType.STRING)
	@Column(name = "required_gender", length = 10)
	private Gender requiredGender;

	@Convert(converter = StringListConverter.class)
	@Column(name = "required_skills", columnDefinition = "TEXT")
	private List<String> requiredSkills;

	@Column(name = "emp_count")
	private int empCount;

	@Column(name = "allocation_priority")
	private int priority;

	@Column(name="active")
	private boolean active;

	// --- Data-driven shift-type config (V013). Set per service when the template is
	// built; null/false means "inherit the shift_type default". Nothing reads these
	// yet beyond accessors — the constraint/factory migration lands in later installments.

	/** Flat rate that overrides the rate card for this shift (e.g. complex 20, sleep-in 0). Null = use rate card. */
	@Column(name = "rate", precision = 8, scale = 2)
	private BigDecimal rate;

	/** HOURLY | DAILY | FLAT; null = inherit the shift type's default basis. */
	@Column(name = "rate_basis", length = 20)
	private String rateBasis;

	/** True when this template mirrors a leader's carer (the generalised SLEEP_IN shadow). */
	@Column(name = "is_follower", nullable = false)
	private boolean follower = false;

	/** The leader template this one follows (its carer is mirrored onto this shift). Null unless {@link #follower}. */
	@Column(name = "paired_with_template_id")
	private Integer pairedWithTemplateId;

	public ShiftTemplate() {
	}

	public ShiftTemplate(String location, String region, DayOfWeek day_of_week, LocalTime start_time,
			LocalTime end_time, Gender required_gender, List<String> required_skills, int empCount, int priority) {

		this.location = location;
		this.region = region;
		this.dayOfWeek = day_of_week;
		this.startTime = start_time;
		this.endTime = end_time;
		this.requiredGender = required_gender;
		this.requiredSkills = required_skills;
		this.empCount = empCount;
		this.priority = priority;
		this.active=true;

	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public void setShiftType(ShiftType shiftType) {
		this.shiftTypeCode = (shiftType == null) ? null : shiftType.name();
	}

	/** Raw shift-type code (JSON {@code shiftType}); any value, including new data-driven types. */
	@com.fasterxml.jackson.annotation.JsonProperty("shiftType")
	public String getShiftTypeCode() {
		return shiftTypeCode;
	}

	@com.fasterxml.jackson.annotation.JsonProperty("shiftType")
	public void setShiftTypeCode(String shiftTypeCode) {
		this.shiftTypeCode = shiftTypeCode;
	}

//	public void setDay(DayOfWeek day) {
//		this.dayOfWeek = day;
//	}

	public void setStartTime(LocalTime startTime) {
		this.startTime = startTime;
	}

	public void setEndTime(LocalTime endTime) {
		this.endTime = endTime;
	}

	public void setTotalHours(BigDecimal totalHours) {
		this.totalHours = totalHours;
	}

	public void setGender(Gender gender) {
		this.requiredGender = gender;
	}

	public String getLocation() {
		return location;
	}

	/** Legacy enum view: the built-in {@link ShiftType} for the six known codes, else null. */
	@com.fasterxml.jackson.annotation.JsonIgnore
	public ShiftType getShiftType() {
		if (shiftTypeCode == null) {
			return null;
		}
		try {
			return ShiftType.valueOf(shiftTypeCode);
		} catch (IllegalArgumentException e) {
			return null; // a new data-driven type with no enum constant
		}
	}

	public DayOfWeek getDay() {
		return dayOfWeek;
	}

	public LocalTime getStartTime() {
		return startTime;
	}

	public LocalTime getEndTime() {
		return endTime;
	}

	public BigDecimal getTotalHours() {
		return totalHours;
	}

	public Gender getGender() {
		return requiredGender;
	}

	@Override
	public String toString() {
		return "Shift [id=" + id + ", region=" + region + ", location=" + location + ", shiftType=" + shiftTypeCode
				+ ", day=" + dayOfWeek + ", startTime=" + startTime + ", endTime=" + endTime + ", totalHours="
				+ totalHours + ", gender=" + requiredGender + "skills = " + requiredSkills + "empcount =" + empCount +" priority ="+priority
				+ "]";
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public DayOfWeek getDayOfWeek() {
		return dayOfWeek;
	}

	public void setDayOfWeek(DayOfWeek dayOfWeek) {
		this.dayOfWeek = dayOfWeek;
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

	public int getEmpCount() {
		return empCount;
	}

	public void setEmpCount(int empCount) {
		this.empCount = empCount;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public BigDecimal getRate() {
		return rate;
	}

	public void setRate(BigDecimal rate) {
		this.rate = rate;
	}

	public String getRateBasis() {
		return rateBasis;
	}

	public void setRateBasis(String rateBasis) {
		this.rateBasis = rateBasis;
	}

	public boolean isFollower() {
		return follower;
	}

	public void setFollower(boolean follower) {
		this.follower = follower;
	}

	public Integer getPairedWithTemplateId() {
		return pairedWithTemplateId;
	}

	public void setPairedWithTemplateId(Integer pairedWithTemplateId) {
		this.pairedWithTemplateId = pairedWithTemplateId;
	}

	/**
	 * True when this template's shift mirrors a leader's carer (the generalised
	 * SLEEP_IN shadow): the per-template {@code is_follower} flag if set, otherwise
	 * the shift type's seeded default. Drives which assignment subtype the factory
	 * creates and how the linker pairs shifts.
	 */
	public boolean isEffectiveFollower() {
		if (follower) {
			return true;
		}
		return shiftTypeCode != null && com.midco.rota.opt.ShiftTypeMeta.isFollower(shiftTypeCode);
	}

}
