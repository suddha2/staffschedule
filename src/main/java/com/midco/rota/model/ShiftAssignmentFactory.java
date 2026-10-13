package com.midco.rota.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.midco.rota.opt.ShiftTypeMeta;


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
	 * period generation does. A follower (any type set as a follower in Manage Shift Types,
	 * e.g. SLEEP_IN following LONG_DAY) is created as a shadow only while there is a slot of
	 * ITS leader type at the same house and date to mirror. A follower beyond that, or one
	 * whose leader type isn't configured, has no carer to copy, so it becomes a standalone
	 * work shift the solver fills (and coverage counts) instead of a silently empty one.
	 */
	public static List<ShiftAssignment> createAll(List<Shift> shifts) {
		Map<String, Integer> leaderSlots = new HashMap<>();
		for (Shift shift : shifts) {
			String key = leaderKey(shift);
			if (key != null) {
				leaderSlots.merge(key, shift.getShiftTemplate().getEmpCount(), Integer::sum);
			}
		}
		Map<String, Integer> followersCreated = new HashMap<>();
		List<ShiftAssignment> assignments = new ArrayList<>();
		for (Shift shift : shifts) {
			for (int i = 0; i < shift.getShiftTemplate().getEmpCount(); i++) {
				ShiftAssignment sa = create(shift);
				if (sa instanceof FollowerShiftAssignment) {
					String key = followerKey(shift);
					int n = key == null ? 1 : followersCreated.merge(key, 1, Integer::sum);
					if (key == null || n > leaderSlots.getOrDefault(key, 0)) {
						sa = new WorkShiftAssignment(shift); // no leader of its type left to mirror
					}
				}
				assignments.add(sa);
			}
		}
		return assignments;
	}

	/**
	 * Link each leader {@link WorkShiftAssignment} to a {@link FollowerShiftAssignment} of a
	 * type that follows it, at the same house and date, zipping by position (leader[i] ↔
	 * follower[i]). Pairs are type-level and data-driven: a follower only ever links to its
	 * own configured leader type, so several pairs can share a house/day without crossing.
	 * Must be called after the full assignment list is built (batch load / live load) and
	 * before solving, so the listener can mirror employees.
	 */
	public static void linkFollowerPairs(List<? extends ShiftAssignment> assignments) {
		Map<String, List<WorkShiftAssignment>> leadersByKey = new HashMap<>();
		Map<String, List<FollowerShiftAssignment>> followersByKey = new HashMap<>();

		for (ShiftAssignment sa : assignments) {
			if (sa instanceof FollowerShiftAssignment s) {
				String key = followerKey(sa.getShift());
				if (key != null) {
					followersByKey.computeIfAbsent(key, k -> new ArrayList<>()).add(s);
				}
			} else if (sa instanceof WorkShiftAssignment w) {
				// A standalone follower-type shift is a work shift too, but not a leader.
				String key = leaderKey(sa.getShift());
				if (key != null) {
					leadersByKey.computeIfAbsent(key, k -> new ArrayList<>()).add(w);
				}
			}
		}

		for (Map.Entry<String, List<WorkShiftAssignment>> entry : leadersByKey.entrySet()) {
			List<WorkShiftAssignment> leaders = entry.getValue();
			List<FollowerShiftAssignment> followers = followersByKey.getOrDefault(entry.getKey(), List.of());
			int n = Math.min(leaders.size(), followers.size());
			for (int i = 0; i < n; i++) {
				leaders.get(i).setPairedFollower(followers.get(i));
			}
		}
	}

	/** Pairing key a leader offers: house|date|its own type. Null unless some type follows it. */
	private static String leaderKey(Shift shift) {
		ShiftTemplate t = shift == null ? null : shift.getShiftTemplate();
		if (t == null || shift.getShiftStart() == null || t.isEffectiveFollower()
				|| !ShiftTypeMeta.isLeader(t.getShiftTypeCode())) {
			return null;
		}
		return key(t.getLocation(), shift, t.getShiftTypeCode());
	}

	/** Pairing key a follower looks for: house|date|the leader type it follows. Null if none configured. */
	private static String followerKey(Shift shift) {
		ShiftTemplate t = shift == null ? null : shift.getShiftTemplate();
		if (t == null || shift.getShiftStart() == null) {
			return null;
		}
		String leader = ShiftTypeMeta.pairsWith(t.getShiftTypeCode());
		return leader == null ? null : key(t.getLocation(), shift, leader);
	}

	private static String key(String location, Shift shift, String leaderCode) {
		return location + "|" + shift.getShiftStart() + "|" + leaderCode;
	}
}
