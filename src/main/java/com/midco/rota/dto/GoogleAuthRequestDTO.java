package com.midco.rota.dto;

import jakarta.validation.constraints.NotBlank;

public class GoogleAuthRequestDTO {

	@NotBlank
	private String idToken;

	public GoogleAuthRequestDTO() {
	}

	public String getIdToken() {
		return idToken;
	}

	public void setIdToken(String idToken) {
		this.idToken = idToken;
	}
}
