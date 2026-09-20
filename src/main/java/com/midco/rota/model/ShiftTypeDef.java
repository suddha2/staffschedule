package com.midco.rota.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Data-driven shift type: a label plus behaviour DEFAULTS a {@link ShiftTemplate}
 * inherits. Adding a new type is a row here — no code change. The actual behaviour
 * for a given service is set on the template when it's built (pairing, rate,
 * overrides); this row only supplies the sensible starting point.
 *
 * <p>Distinct from the legacy {@link com.midco.rota.util.ShiftType} enum, which is
 * being retired in favour of this table.
 */
@Entity
@Table(name = "shift_type")
public class ShiftTypeDef {

    @Id
    @Column(name = "code", length = 40)
    private String code;

    @Column(name = "display_name", length = 80, nullable = false)
    private String displayName;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "counts_toward_weekly_cap", nullable = false)
    private boolean countsTowardWeeklyCap = true;

    @Column(name = "counts_as_work", nullable = false)
    private boolean countsAsWork = true;

    @Column(name = "counts_as_location_coverage", nullable = false)
    private boolean countsAsLocationCoverage = true;

    @Column(name = "paid_hours", nullable = false)
    private boolean paidHours = true;

    @Column(name = "mineable", nullable = false)
    private boolean mineable = true;

    @Column(name = "default_rate_basis", length = 20, nullable = false)
    private String defaultRateBasis = "HOURLY";

    @Column(name = "default_is_follower", nullable = false)
    private boolean defaultIsFollower = false;

    @Column(name = "default_pairs_with", length = 40)
    private String defaultPairsWith;

    /** Flat rate override for this type (null = use the region/level rate card). */
    @Column(name = "rate", precision = 8, scale = 2)
    private java.math.BigDecimal rate;

    public ShiftTypeDef() {
    }

    public java.math.BigDecimal getRate() { return rate; }
    public void setRate(java.math.BigDecimal rate) { this.rate = rate; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isCountsTowardWeeklyCap() { return countsTowardWeeklyCap; }
    public void setCountsTowardWeeklyCap(boolean v) { this.countsTowardWeeklyCap = v; }

    public boolean isCountsAsWork() { return countsAsWork; }
    public void setCountsAsWork(boolean v) { this.countsAsWork = v; }

    public boolean isCountsAsLocationCoverage() { return countsAsLocationCoverage; }
    public void setCountsAsLocationCoverage(boolean v) { this.countsAsLocationCoverage = v; }

    public boolean isPaidHours() { return paidHours; }
    public void setPaidHours(boolean v) { this.paidHours = v; }

    public boolean isMineable() { return mineable; }
    public void setMineable(boolean v) { this.mineable = v; }

    public String getDefaultRateBasis() { return defaultRateBasis; }
    public void setDefaultRateBasis(String v) { this.defaultRateBasis = v; }

    public boolean isDefaultIsFollower() { return defaultIsFollower; }
    public void setDefaultIsFollower(boolean v) { this.defaultIsFollower = v; }

    public String getDefaultPairsWith() { return defaultPairsWith; }
    public void setDefaultPairsWith(String v) { this.defaultPairsWith = v; }
}
