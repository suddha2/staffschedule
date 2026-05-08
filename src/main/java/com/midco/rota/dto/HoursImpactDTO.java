package com.midco.rota.dto;

import java.math.BigDecimal;

public class HoursImpactDTO {

	private Integer weekNumber;
	private BigDecimal currentHours;
	private BigDecimal shiftHours;
	private BigDecimal afterApprovalHours;
	private BigDecimal minHrs;
	private BigDecimal maxHrs;
	private FitDTO.State state;

	public HoursImpactDTO() {
	}

	public HoursImpactDTO(Integer weekNumber, BigDecimal currentHours, BigDecimal shiftHours,
			BigDecimal afterApprovalHours, BigDecimal minHrs, BigDecimal maxHrs, FitDTO.State state) {
		this.weekNumber = weekNumber;
		this.currentHours = currentHours;
		this.shiftHours = shiftHours;
		this.afterApprovalHours = afterApprovalHours;
		this.minHrs = minHrs;
		this.maxHrs = maxHrs;
		this.state = state;
	}

	public Integer getWeekNumber() {
		return weekNumber;
	}

	public void setWeekNumber(Integer weekNumber) {
		this.weekNumber = weekNumber;
	}

	public BigDecimal getCurrentHours() {
		return currentHours;
	}

	public void setCurrentHours(BigDecimal currentHours) {
		this.currentHours = currentHours;
	}

	public BigDecimal getShiftHours() {
		return shiftHours;
	}

	public void setShiftHours(BigDecimal shiftHours) {
		this.shiftHours = shiftHours;
	}

	public BigDecimal getAfterApprovalHours() {
		return afterApprovalHours;
	}

	public void setAfterApprovalHours(BigDecimal afterApprovalHours) {
		this.afterApprovalHours = afterApprovalHours;
	}

	public BigDecimal getMinHrs() {
		return minHrs;
	}

	public void setMinHrs(BigDecimal minHrs) {
		this.minHrs = minHrs;
	}

	public BigDecimal getMaxHrs() {
		return maxHrs;
	}

	public void setMaxHrs(BigDecimal maxHrs) {
		this.maxHrs = maxHrs;
	}

	public FitDTO.State getState() {
		return state;
	}

	public void setState(FitDTO.State state) {
		this.state = state;
	}
}
