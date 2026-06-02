package com.midco.rota.dto;

import java.util.List;

/**
 * All fields optional. Any field left null is left unchanged; an empty roles
 * list explicitly clears the user's roles. Password is not edited here — use
 * the dedicated /password endpoint.
 */
public class UserUpdateRequest {
	private String username;
	private Boolean active;
	private List<String> roles;

	public String getUsername() { return username; }
	public void setUsername(String username) { this.username = username; }
	public Boolean getActive() { return active; }
	public void setActive(Boolean active) { this.active = active; }
	public List<String> getRoles() { return roles; }
	public void setRoles(List<String> roles) { this.roles = roles; }
}
