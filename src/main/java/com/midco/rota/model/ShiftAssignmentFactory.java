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
	 * Build the assignments for a batch of shifts ({@code emp_count} per shift), the way
	 * period generation does. Followers (e.g. SLEEP_IN) are created as shadows only while
	 * there is a leader slot (e.g. LONG_DAY) at the same house and date to mirror; any
	 * follower beyond that has no carer to copy, so it becomes a standalone work shift the
	 * solver fills (and coverage counts). Two sleep-ins against one long day therefore give
	 * one paired sleep-in and one with its own carer, instead of a silently empty one.
	 */
	public static List<ShiftAssignment> createAll(List<Shift> shifts) {
		Map<String, Integer> leaderSlots = new HashMap<>();
		for (Shift shift : shifts) {
			if (isLeader(shift)) {
				leaderSlots.merge(shift.getPairId(), shift.getShiftTemplate().getEmpCount(), Integer::sum);
			}
		}
		Map<String, Integer> followersCreated = new HashMap<>();
		List<ShiftAssignment> assignments = new ArrayList<>();
		for (Shift shift : shifts) {
			for (int i = 0; i < shift.getShiftTemplate().getEmpCount(); i++) {
				ShiftAssignment sa = create(shift);
				if (sa instanceof FollowerShiftAssignment && shift.getPairId() != null) {
					int n = followersCreated.merge(shift.getPairId(), 1, Integer::sum);
					if (n > leaderSlots.getOrDefault(shift.getPairId(), 0)) {
						sa = new WorkShiftAssignment(shift); // no leader left to mirror
					}
				}
				assignments.add(sa);
			}
		}
		return assignments;
	}

	/** A shift whose carer a follower can mirror: a leader type (e.g. LONG_DAY), not itself a follower. */
	private static boolean isLeader(Shift shift) {
		ShiftTemplate t = shift.getShiftTemplate();
		return t != null && shift.getPairId() != null && !t.isEffectiveFollower()
				&& com.midco.rota.opt.ShiftTypeMeta.isLeader(t.getShiftTypeCode());
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
			// Leaders are leader-type work shifts only: a standalone sleep-in (or one left
			// without a long day) is a work shift too, but nothing may pair onto it.
			if (sa instanceof FollowerShiftAssignment s) {
				followersByPair.computeIfAbsent(pairId, k -> new ArrayList<>()).add(s);
			} else if (sa instanceof WorkShiftAssignment w && isLeader(sa.getShift())) {
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
