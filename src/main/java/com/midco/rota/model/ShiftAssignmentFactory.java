package com.midco.rota.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.midco.rota.util.ShiftType;

/**
 * Creates the right {@link ShiftAssignment} subtype for a shift and wires the
 * LONG_DAY → SLEEP_IN pairing links the shadow-variable listener relies on.
 * Replaces the pre/post-solve pairing that {@code SleepInPairingService} used to
 * do — pairing is now continuous inside the solver.
 */
public final class ShiftAssignmentFactory {

	private ShiftAssignmentFactory() {
	}

	/** SLEEP_IN → shadow entity; everything else → genuine work entity. */
	public static ShiftAssignment create(Shift shift) {
		if (shift != null && shift.getShiftTemplate() != null
				&& shift.getShiftTemplate().getShiftType() == ShiftType.SLEEP_IN) {
			return new SleepInShiftAssignment(shift);
		}
		return new WorkShiftAssignment(shift);
	}

	/**
	 * Link each LONG_DAY {@link WorkShiftAssignment} to a SLEEP_IN
	 * {@link SleepInShiftAssignment} sharing its {@code pairId}, zipping by
	 * position (LONG_DAY[i] ↔ SLEEP_IN[i]) exactly as the old pairing did. Must be
	 * called after the full assignment list is built (batch load / live load) and
	 * before solving, so the listener can mirror employees.
	 */
	public static void linkSleepInPairs(List<? extends ShiftAssignment> assignments) {
		Map<String, List<WorkShiftAssignment>> longDaysByPair = new HashMap<>();
		Map<String, List<SleepInShiftAssignment>> sleepInsByPair = new HashMap<>();

		for (ShiftAssignment sa : assignments) {
			if (sa.getShift() == null || sa.getShift().getShiftTemplate() == null) {
				continue;
			}
			String pairId = sa.getShift().getPairId();
			if (pairId == null) {
				continue;
			}
			ShiftType type = sa.getShift().getShiftTemplate().getShiftType();
			if (type == ShiftType.LONG_DAY && sa instanceof WorkShiftAssignment w) {
				longDaysByPair.computeIfAbsent(pairId, k -> new ArrayList<>()).add(w);
			} else if (type == ShiftType.SLEEP_IN && sa instanceof SleepInShiftAssignment s) {
				sleepInsByPair.computeIfAbsent(pairId, k -> new ArrayList<>()).add(s);
			}
		}

		for (Map.Entry<String, List<WorkShiftAssignment>> entry : longDaysByPair.entrySet()) {
			List<WorkShiftAssignment> works = entry.getValue();
			List<SleepInShiftAssignment> sleeps = sleepInsByPair.getOrDefault(entry.getKey(), List.of());
			int n = Math.min(works.size(), sleeps.size());
			for (int i = 0; i < n; i++) {
				works.get(i).setPairedSleepIn(sleeps.get(i));
			}
		}
	}
}
