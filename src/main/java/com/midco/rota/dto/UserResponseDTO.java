package com.midco.rota.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.midco.rota.model.Role;
import com.midco.rota.model.User;

/**
 * Admin-facing user view. Never includes the password hash; only role NAMES
 * (the frontend doesn't need the Role entity shape).
 */
public class UserResponseDTO {
	private Long id;
	private String username;
	private boolean active;
	private LocalDateTime createdAt;
	private List<String> roles;

	public static UserResponseDTO fromEntity(User u) {
		UserResponseDTO dto = new UserResponseDTO();
		dto.id = u.getId();
		dto.username = u.getUsername();
		dto.active = u.isActive();
		dto.createdAt = u.getCreatedAt();
		dto.roles = u.getRoles() == null ? List.of()
				: u.getRoles().stream().map(Role::getName).sorted().collect(Collectors.toList());
		return dto;
	}

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public String getUsername() { return username; }
	public void setUsername(String username) { this.username = username; }
	public boolean isActive() { return active; }
	public void setActive(boolean active) { this.active = active; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
	public List<String> getRoles() { return roles; }
	public void setRoles(List<String> roles) { this.roles = roles; }
}
