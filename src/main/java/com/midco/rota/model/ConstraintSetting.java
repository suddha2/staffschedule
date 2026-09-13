package com.midco.rota.model;

import org.optaplanner.core.api.score.buildin.hardsoftlong.HardSoftLongScore;

import com.midco.rota.util.ConstraintType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One tunable constraint: how much it counts, and whether breaking it is
 * forbidden or merely undesirable.
 *
 * <p>Severity and magnitude are separate columns rather than a hard weight and
 * a soft weight, because a constraint is one or the other. None of the rules in
 * this system is partly hard and partly soft, and storing two weights would let
 * someone configure that meaningless state by accident.
 *
 * <p>Setting {@code enabled} to false switches the rule off without losing its
 * type or weight, so turning it back on restores the previous configuration.
 *
 * <p>The primary key is the constraint name exactly as it appears in
 * {@code asConstraint(..)} in {@code RotaConstraintProvider}, which is also the
 * value of the matching {@code @ConstraintWeight} annotation.
 */
@Entity
@Table(name = "constraint_setting")
public class ConstraintSetting {

	@Id
	@Column(name = "constraint_name", length = 200, nullable = false)
	private String constraintName;

	@Column(name = "score_weight", nullable = false)
	private long scoreWeight;

	@Enumerated(EnumType.STRING)
	@Column(name = "constraint_type", length = 10, nullable = false)
	private ConstraintType constraintType = ConstraintType.SOFT;

	@Column(name = "enabled", nullable = false)
	private boolean enabled = true;

	@Column(name = "description", length = 500)
	private String description;

	public ConstraintSetting() {
	}

	public ConstraintSetting(String constraintName, long scoreWeight, ConstraintType constraintType,
			String description) {
		this.constraintName = constraintName;
		this.scoreWeight = scoreWeight;
		this.constraintType = constraintType;
		this.enabled = true;
		this.description = description;
	}

	/**
	 * The score OptaPlanner should use for this constraint. A disabled setting
	 * yields {@link HardSoftLongScore#ZERO}, which switches the constraint off.
	 */
	public HardSoftLongScore toScore() {
		if (!enabled) {
			return HardSoftLongScore.ZERO;
		}
		return constraintType == ConstraintType.HARD
				? HardSoftLongScore.ofHard(scoreWeight)
				: HardSoftLongScore.ofSoft(scoreWeight);
	}

	/**
	 * Splits a code default into severity and magnitude for seeding. A score
	 * with a hard component is treated as hard, otherwise soft.
	 *
	 * @return the setting, or null if the score mixes hard and soft components,
	 *         which this schema deliberately cannot represent
	 */
	public static ConstraintSetting fromScore(String constraintName, HardSoftLongScore score, String description) {
		boolean hard = score.hardScore() != 0L;
		boolean soft = score.softScore() != 0L;
		if (hard && soft) {
			return null;
		}
		return hard
				? new ConstraintSetting(constraintName, score.hardScore(), ConstraintType.HARD, description)
				: new ConstraintSetting(constraintName, score.softScore(), ConstraintType.SOFT, description);
	}

	public String getConstraintName() {
		return constraintName;
	}

	public void setConstraintName(String constraintName) {
		this.constraintName = constraintName;
	}

	public long getScoreWeight() {
		return scoreWeight;
	}

	public void setScoreWeight(long scoreWeight) {
		this.scoreWeight = scoreWeight;
	}

	public ConstraintType getConstraintType() {
		return constraintType;
	}

	public void setConstraintType(ConstraintType constraintType) {
		this.constraintType = constraintType;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
