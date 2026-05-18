package com.midco.rota.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Request body for POST /api/mobile/auth/request-code. */
public class RequestCodeDTO {

	@NotBlank
	@Email
	private String email;

	public RequestCodeDTO() {
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}
}
