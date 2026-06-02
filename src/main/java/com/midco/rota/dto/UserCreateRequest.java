package com.midco.rota.dto;

import java.util.List;

public class UserCreateRequest {
	private String username;
	private String password;
	private Boolean active;          // optional; defaults to true on the server
	private List<String> roles;      // role names, e.g. ["OPS_MANAGER"]

	public String getUsername() { return username; }
	public void setUsername(String username) { this.username = username; }
	public String getPassword() { return password; }
	public void setPassword(String password) { this.password = password; }
	public Boolean getActive() { return active; }
	public void setActive(Boolean active) { this.active = active; }
	public List<String> getRoles() { return roles; }
	public void setRoles(List<String> roles) { this.roles = roles; }
}
