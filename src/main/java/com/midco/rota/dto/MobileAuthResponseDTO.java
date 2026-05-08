package com.midco.rota.dto;

public class MobileAuthResponseDTO {

	private String token;
	private Integer employeeId;
	private String firstName;
	private String lastName;

	public MobileAuthResponseDTO() {
	}

	public MobileAuthResponseDTO(String token, Integer employeeId, String firstName, String lastName) {
		this.token = token;
		this.employeeId = employeeId;
		this.firstName = firstName;
		this.lastName = lastName;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public Integer getEmployeeId() {
		return employeeId;
	}

	public void setEmployeeId(Integer employeeId) {
		this.employeeId = employeeId;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
}
