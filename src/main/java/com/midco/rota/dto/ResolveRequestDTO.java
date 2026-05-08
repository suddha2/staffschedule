package com.midco.rota.dto;

import jakarta.validation.constraints.Pattern;

public class ResolveRequestDTO {

	@Pattern(regexp = "APPROVE|REJECT", message = "action must be APPROVE or REJECT")
	private String action;

	public ResolveRequestDTO() {
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}
}
