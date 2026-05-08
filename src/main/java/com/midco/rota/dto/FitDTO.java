package com.midco.rota.dto;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FitDTO {

	public enum State { MATCH, NEUTRAL, MISMATCH }

	public enum Summary { STRONG, OK, WEAK }

	private Summary summary;

	// Keys: "service", "day", "shiftType", "gender", "skills", "region"
	private Map<String, State> criteria = new LinkedHashMap<>();

	private HoursImpactDTO hoursImpact;

	private List<String> notes;

	public FitDTO() {
	}

	public FitDTO(Summary summary, Map<String, State> criteria, HoursImpactDTO hoursImpact, List<String> notes) {
		this.summary = summary;
		this.criteria = criteria;
		this.hoursImpact = hoursImpact;
		this.notes = notes;
	}

	public Summary getSummary() {
		return summary;
	}

	public void setSummary(Summary summary) {
		this.summary = summary;
	}

	public Map<String, State> getCriteria() {
		return criteria;
	}

	public void setCriteria(Map<String, State> criteria) {
		this.criteria = criteria;
	}

	public HoursImpactDTO getHoursImpact() {
		return hoursImpact;
	}

	public void setHoursImpact(HoursImpactDTO hoursImpact) {
		this.hoursImpact = hoursImpact;
	}

	public List<String> getNotes() {
		return notes;
	}

	public void setNotes(List<String> notes) {
		this.notes = notes;
	}
}
