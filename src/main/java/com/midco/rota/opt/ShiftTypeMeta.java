package com.midco.rota.opt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.midco.rota.model.ShiftTypeDef;
import com.midco.rota.repository.ShiftTypeDefRepository;

/**
 * Static, process-wide view of the data-driven {@code shift_type} table, so entities
 * (Shift, ShiftTemplate, the factory) and constraints can read per-type behaviour
 * without a Spring dependency — same pattern as {@code RateTableProvider}.
 *
 * <p>Retires the hard-coded {@code ShiftType} enum branches: instead of
 * {@code type == SLEEP_IN}, callers ask {@code ShiftTypeMeta.isFollower(code)} etc.
 * Loaded once at startup. A hard-coded fallback matching the V013 seed keeps every
 * answer correct even before the table is loaded (tests, pre-ready), so behaviour is
 * identical whether or not the row is present.
 */
@Component
public class ShiftTypeMeta {

    private static volatile Map<String, ShiftTypeDef> BY_CODE = Map.of();

    private final ShiftTypeDefRepository repo;

    public ShiftTypeMeta(ShiftTypeDefRepository repo) {
        this.repo = repo;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        List<ShiftTypeDef> all = repo.findAll();
        Map<String, ShiftTypeDef> map = new HashMap<>();
        for (ShiftTypeDef d : all) {
            map.put(d.getCode(), d);
        }
        BY_CODE = map;
    }

    private static ShiftTypeDef def(String code) {
        return code == null ? null : BY_CODE.get(code);
    }

    // --- behaviour flags (DB row if present, else the seed-matching fallback) ---

    public static boolean isFollower(String code) {
        ShiftTypeDef d = def(code);
        return d != null ? d.isDefaultIsFollower() : Fallback.isFollower(code);
    }

    public static String pairsWith(String code) {
        ShiftTypeDef d = def(code);
        return d != null ? d.getDefaultPairsWith() : Fallback.pairsWith(code);
    }

    /** True when some active type pairs onto this one (i.e. this type leads a follower). */
    public static boolean isLeader(String code) {
        if (code == null) {
            return false;
        }
        if (!BY_CODE.isEmpty()) {
            for (ShiftTypeDef d : BY_CODE.values()) {
                if (d.isActive() && code.equals(d.getDefaultPairsWith())) {
                    return true;
                }
            }
            return false;
        }
        return Fallback.isLeader(code);
    }

    /** Leaders and followers both need a pairId so the linker can zip them. */
    public static boolean participatesInPairing(String code) {
        return isFollower(code) || isLeader(code);
    }

    public static boolean countsTowardWeeklyCap(String code) {
        ShiftTypeDef d = def(code);
        return d != null ? d.isCountsTowardWeeklyCap() : Fallback.countsTowardWeeklyCap(code);
    }

    public static boolean countsAsWork(String code) {
        ShiftTypeDef d = def(code);
        return d != null ? d.isCountsAsWork() : Fallback.countsAsWork(code);
    }

    public static boolean countsAsLocationCoverage(String code) {
        ShiftTypeDef d = def(code);
        return d != null ? d.isCountsAsLocationCoverage() : Fallback.countsAsLocationCoverage(code);
    }

    public static boolean paidHours(String code) {
        ShiftTypeDef d = def(code);
        return d != null ? d.isPaidHours() : Fallback.paidHours(code);
    }

    public static boolean mineable(String code) {
        ShiftTypeDef d = def(code);
        return d != null ? d.isMineable() : Fallback.mineable(code);
    }

    public static String rateBasis(String code) {
        ShiftTypeDef d = def(code);
        return d != null ? d.getDefaultRateBasis() : Fallback.rateBasis(code);
    }

    /**
     * Hard-coded mirror of the V013 seed. Used only when the row isn't loaded, so
     * behaviour is identical either way during the enum-retirement transition.
     */
    private static final class Fallback {
        static boolean isFollower(String c) { return "SLEEP_IN".equals(c); }
        static String pairsWith(String c) { return "SLEEP_IN".equals(c) ? "LONG_DAY" : null; }
        static boolean isLeader(String c) { return "LONG_DAY".equals(c); }
        static boolean countsTowardWeeklyCap(String c) {
            return !("LONG_DAY".equals(c) || "FLOATING".equals(c) || "SLEEP_IN".equals(c));
        }
        static boolean countsAsWork(String c) { return !"SLEEP_IN".equals(c); }
        static boolean countsAsLocationCoverage(String c) {
            return "DAY".equals(c) || "WAKING_NIGHT".equals(c) || "LONG_DAY".equals(c);
        }
        static boolean paidHours(String c) { return !"SLEEP_IN".equals(c); }
        static boolean mineable(String c) { return !"SLEEP_IN".equals(c); }
        static String rateBasis(String c) {
            if ("LONG_DAY".equals(c)) return "DAILY";
            if ("SLEEP_IN".equals(c)) return "FLAT";
            return "HOURLY";
        }
    }
}
