package com.midco.rota.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A numeric threshold used inside a constraint, for example the weekly shift
 * cap or the maximum number of locations per period.
 *
 * <p>These are separate from {@link ConstraintSetting} because they are not
 * score weights: they are values a constraint compares against, so they have to
 * be read when the constraint streams are built rather than looked up by
 * OptaPlanner. {@code SolverConfigService} loads them into
 * {@code com.midco.rota.opt.SolverTuning} before a solve starts.
 */
@Entity
@Table(name = "solver_tuning")
public class SolverTuningSetting {

	@Id
	@Column(name = "setting_key", length = 80, nullable = false)
	private String settingKey;

	@Column(name = "int_value", nullable = false)
	private int intValue;

	@Column(name = "description", length = 500)
	private String description;

	public SolverTuningSetting() {
	}

	public SolverTuningSetting(String settingKey, int intValue, String description) {
		this.settingKey = settingKey;
		this.intValue = intValue;
		this.description = description;
	}

	public String getSettingKey() {
		return settingKey;
	}

	public void setSettingKey(String settingKey) {
		this.settingKey = settingKey;
	}

	public int getIntValue() {
		return intValue;
	}

	public void setIntValue(int intValue) {
		this.intValue = intValue;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
