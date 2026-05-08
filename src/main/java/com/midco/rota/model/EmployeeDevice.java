package com.midco.rota.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "employee_device")
public class EmployeeDevice {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "employee_id", nullable = false)
	private Employee employee;

	@Column(name = "fcm_token", length = 512, nullable = false, unique = true)
	private String fcmToken;

	@Column(name = "platform", length = 20, nullable = false)
	private String platform = "ANDROID";

	@Column(name = "registered_at", nullable = false)
	private LocalDateTime registeredAt = LocalDateTime.now();

	@Column(name = "last_seen_at")
	private LocalDateTime lastSeenAt;

	@Column(name = "active", nullable = false)
	private boolean active = true;

	public EmployeeDevice() {
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Employee getEmployee() {
		return employee;
	}

	public void setEmployee(Employee employee) {
		this.employee = employee;
	}

	public String getFcmToken() {
		return fcmToken;
	}

	public void setFcmToken(String fcmToken) {
		this.fcmToken = fcmToken;
	}

	public String getPlatform() {
		return platform;
	}

	public void setPlatform(String platform) {
		this.platform = platform;
	}

	public LocalDateTime getRegisteredAt() {
		return registeredAt;
	}

	public void setRegisteredAt(LocalDateTime registeredAt) {
		this.registeredAt = registeredAt;
	}

	public LocalDateTime getLastSeenAt() {
		return lastSeenAt;
	}

	public void setLastSeenAt(LocalDateTime lastSeenAt) {
		this.lastSeenAt = lastSeenAt;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
}
