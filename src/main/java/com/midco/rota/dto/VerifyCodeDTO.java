package com.midco.rota.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Request body for POST /api/mobile/auth/verify-code. */
public class VerifyCodeDTO {

	@NotBlank
	@Email
	private String email;

	@NotBlank
	private String code;

	public VerifyCodeDTO() {
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}
}
