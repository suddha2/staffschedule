package com.midco.rota.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Creates the right {@link ShiftAssignment} subtype for a shift and wires the
 * leader → follower pairing links the shadow-variable listener relies on (the
 * canonical case being LONG_DAY → SLEEP_IN). Replaces the pre/post-solve pairing
 * that {@code SleepInPairingService} used to do — pairing is now continuous
 * inside the solver.
 */
public final class ShiftAssignmentFactory {

	private ShiftAssignmentFactory() {
	}

	/** Follower template → shadow entity; everything else → genuine work entity.
	 *  Data-driven (was: type == SLEEP_IN). */
	public static ShiftAssignment create(Shift shift) {
		if (shift != null && shift.getShiftTemplate() != null
				&& shift.getShiftTemplate().isEffectiveFollower()) {
			return new FollowerShiftAssignment(shift);
		}
		return new WorkShiftAssignment(shift);
	}

	/**
	 * Link each leader {@link WorkShiftAssignment} to a
	 * {@link FollowerShiftAssignment} sharing its {@code pairId}, zipping by
	 * position (leader[i] ↔ follower[i]) exactly as the old pairing did (the
	 * canonical case being LONG_DAY ↔ SLEEP_IN). Must be called after the full
	 * assignment list is built (batch load / live load) and before solving, so the
	 * listener can mirror employees.
	 */
	public static void linkFollowerPairs(List<? extends ShiftAssignment> assignments) {
		// Data-driven (was: type == LONG_DAY / SLEEP_IN). Within a pairId group
		// (location + date), zip each leader (non-follower work shift) to a follower
		// (shadow shift) by position, exactly as before.
		Map<String, List<WorkShiftAssignment>> leadersByPair = new HashMap<>();
		Map<String, List<FollowerShiftAssignment>> followersByPair = new HashMap<>();

		for (ShiftAssignment sa : assignments) {
			if (sa.getShift() == null || sa.getShift().getShiftTemplate() == null) {
				continue;
			}
			String pairId = sa.getShift().getPairId();
			if (pairId == null) {
				continue;
			}
			boolean follower = sa.getShift().getShiftTemplate().isEffectiveFollower();
			if (follower && sa instanceof FollowerShiftAssignment s) {
				followersByPair.computeIfAbsent(pairId, k -> new ArrayList<>()).add(s);
			} else if (!follower && sa instanceof WorkShiftAssignment w) {
				leadersByPair.computeIfAbsent(pairId, k -> new ArrayList<>()).add(w);
			}
		}

		for (Map.Entry<String, List<WorkShiftAssignment>> entry : leadersByPair.entrySet()) {
			List<WorkShiftAssignment> leaders = entry.getValue();
			List<FollowerShiftAssignment> followers = followersByPair.getOrDefault(entry.getKey(), List.of());
			int n = Math.min(leaders.size(), followers.size());
			for (int i = 0; i < n; i++) {
				leaders.get(i).setPairedFollower(followers.get(i));
			}
		}
	}
}
