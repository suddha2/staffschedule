package com.midco.rota.opt;

import java.util.Map;

/**
 * A named objective for a solve: which side of the continuity-vs-hours-fairness
 * trade-off to favour. Each profile overrides a handful of trade-off constraint
 * weights on top of the database base ({@code constraint_setting}); every other
 * constraint is unaffected. Chosen per solve on the {@code DeferredSolveRequest},
 * so a planner can generate both and compare.
 *
 * <ul>
 *   <li>{@link #SPREAD} — even hours, use the whole team, avoid overloading; lower
 *       continuity. The traditional behaviour.</li>
 *   <li>{@link #CONTINUITY} — keep carers in their prior-period slots and stable
 *       weekly patterns; concentrates work on fewer carers (some may fall below
 *       their minimum hours).</li>
 * </ul>
 */
public enum SolveProfile {

    SPREAD(Map.of(
            "Penalize overloading individual employees", 100L,
            "Min weekly hours not met", 500L,
            "Permanent weekly shift minimum", 100_000L,
            "Max hours per shift type per day", 50_000L,
            "Continuity - keep carer in seeded slot", 300_000L,
            "Weekly pattern consistency", 50_000L)),

    CONTINUITY(Map.of(
            "Penalize overloading individual employees", 1L,
            "Min weekly hours not met", 100L,
            "Permanent weekly shift minimum", 20_000L,
            "Max hours per shift type per day", 5_000L,
            "Continuity - keep carer in seeded slot", 800_000L,
            "Weekly pattern consistency", 500_000L));

    private final Map<String, Long> weightOverrides;

    SolveProfile(Map<String, Long> weightOverrides) {
        this.weightOverrides = weightOverrides;
    }

    /** Constraint-name → soft weight overrides this profile applies. */
    public Map<String, Long> weightOverrides() {
        return weightOverrides;
    }

    /** Parse a stored/request value, defaulting to SPREAD for null/blank/unknown. */
    public static SolveProfile fromString(String value) {
        if (value == null || value.isBlank()) {
            return SPREAD;
        }
        try {
            return SolveProfile.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return SPREAD;
        }
    }
}
