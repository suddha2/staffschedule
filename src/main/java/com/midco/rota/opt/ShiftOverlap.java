package com.midco.rota.opt;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import com.midco.rota.model.ShiftAssignment;

/**
 * The single source of truth for "do two shifts clash in time" and the same-day
 * compatibility rule derived from it. Both the solver constraints
 * ({@code RotaConstraintProvider}: Overlapping shifts, No invalid same-day
 * combinations, Minimum rest) and the save-time validator
 * ({@code PinValidationService}) delegate here, so the rule cannot drift between
 * solving and validation. The frontend mirror ({@code shiftConflicts.js}) is kept
 * in step by the shared-fixture parity test.
 *
 * <p>Times are compared as real date-times and are overnight-aware: an end at or
 * before the start (with no later end date) rolls to the next day.
 */
public final class ShiftOverlap {

    private ShiftOverlap() {
    }

    /** True if the two assignments' date-time windows intersect. */
    public static boolean overlaps(ShiftAssignment a, ShiftAssignment b) {
        LocalDateTime aStart = startOf(a), aEnd = endOf(a);
        LocalDateTime bStart = startOf(b), bEnd = endOf(b);
        if (aStart == null || aEnd == null || bStart == null || bEnd == null) {
            return false;
        }
        return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    }

    /**
     * A carer's shifts on one day are allowed unless two of them overlap in time.
     * Non-overlapping combinations (e.g. a lead day shift plus a waking night) are
     * left to the rest/hour rules.
     */
    public static boolean allowedSameDay(List<ShiftAssignment> dayAssignments) {
        if (dayAssignments == null || dayAssignments.size() < 2) {
            return true;
        }
        for (int i = 0; i < dayAssignments.size(); i++) {
            for (int j = i + 1; j < dayAssignments.size(); j++) {
                if (overlaps(dayAssignments.get(i), dayAssignments.get(j))) {
                    return false;
                }
            }
        }
        return true;
    }

    public static LocalDateTime startOf(ShiftAssignment sa) {
        if (sa == null || sa.getShift() == null || sa.getShift().getShiftStart() == null
                || sa.getShift().getShiftTemplate() == null
                || sa.getShift().getShiftTemplate().getStartTime() == null) {
            return null;
        }
        return sa.getShift().getShiftStart().atTime(sa.getShift().getShiftTemplate().getStartTime());
    }

    public static LocalDateTime endOf(ShiftAssignment sa) {
        if (sa == null || sa.getShift() == null || sa.getShift().getShiftTemplate() == null
                || sa.getShift().getShiftTemplate().getEndTime() == null) {
            return null;
        }
        LocalDate startDate = sa.getShift().getShiftStart();
        LocalTime startTime = sa.getShift().getShiftTemplate().getStartTime();
        LocalTime endTime = sa.getShift().getShiftTemplate().getEndTime();
        // Prefer the persisted end date; else the start date. Then, if the result is
        // not after the start (an overnight shift whose dates don't already say so),
        // roll the end to the next day.
        LocalDate endDate = sa.getShift().getShiftEnd() != null ? sa.getShift().getShiftEnd() : startDate;
        if (endDate == null) {
            return null;
        }
        LocalDateTime end = endDate.atTime(endTime);
        LocalDateTime start = (startDate != null && startTime != null) ? startDate.atTime(startTime) : null;
        if (start != null && !end.isAfter(start)) {
            end = endDate.atTime(endTime).plusDays(1);
        }
        return end;
    }
}
