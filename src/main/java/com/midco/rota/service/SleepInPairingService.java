package com.midco.rota.service;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.midco.rota.model.Rota;
import com.midco.rota.model.ShiftAssignment;
import com.midco.rota.util.ShiftType;

/**
 * Owns the SLEEP_IN pairing rules that were previously inlined in
 * {@link SolverService#solveAsync}.
 *
 * <p>SLEEP_IN shifts are never chosen by the solver — {@link ShiftAssignment#isPinned()}
 * returns {@code true} for them, so OptaPlanner leaves their {@code employee}
 * untouched. Instead, each SLEEP_IN is <em>derived</em> from the LONG_DAY it
 * shares a {@link com.midco.rota.model.Shift#getPairId() pairId} with: whoever
 * works the long day also does the sleep-in. FLOATING is likewise pinned and
 * reserved for the mobile publish-and-grab flow, so there is no FLOATING logic
 * to run here — it lives entirely in {@code isPinned()}.
 *
 * <p>This was extracted verbatim (behaviour-preserving) so it can be reused both
 * by the batch solve callback and — later — by the continuous/live snapshot
 * path, which has no single "post-solve" moment at which to pair.
 */
@Service
public class SleepInPairingService {

	private static final Logger logger = LoggerFactory.getLogger(SleepInPairingService.class);

	/**
	 * Summary of a pairing pass, returned for assertions/logging.
	 *
	 * @param paired   SLEEP_IN slots successfully matched to a LONG_DAY employee
	 * @param failed   LONG_DAY employees for which no free SLEEP_IN was found
	 * @param unpaired SLEEP_IN slots still unassigned after the pass
	 */
	public record PairingResult(int paired, int failed, long unpaired) {
	}

	/**
	 * Pre-solve step: blank every SLEEP_IN assignment so the solver starts from a
	 * clean slate (they are pinned, so they would otherwise keep any stale
	 * employee). Must run before handing the problem to the solver.
	 */
	public void resetSleepIns(Rota schedule) {
		for (ShiftAssignment sa : schedule.getShiftAssignmentList()) {
			if (sa.getShift().getShiftTemplate().getShiftType() == ShiftType.SLEEP_IN) {
				sa.setEmployee(null);
			}
		}
	}

	/**
	 * Post-solve step: mirror each LONG_DAY's employee onto the SLEEP_IN sharing
	 * its {@code pairId}. Also backfills any missing {@code rota} back-reference so
	 * the rows persist cleanly. Mutates {@code bestSolution} in place.
	 */
	public PairingResult pairSleepIns(Rota bestSolution) {
		int pairedCount = 0;
		int failedPairings = 0;
		Set<ShiftAssignment> pairedSleepIns = new HashSet<>();

		for (ShiftAssignment longDaySa : bestSolution.getShiftAssignmentList()) {
			if (longDaySa.getShift().getShiftTemplate().getShiftType() == ShiftType.LONG_DAY
					&& longDaySa.getEmployee() != null) {

				String pairId = longDaySa.getShift().getPairId();
				if (pairId == null) {
					continue;
				}

				boolean paired = false;

				for (ShiftAssignment sleepInSa : bestSolution.getShiftAssignmentList()) {
					if (sleepInSa.getShift().getShiftTemplate().getShiftType() == ShiftType.SLEEP_IN
							&& pairId.equals(sleepInSa.getShift().getPairId())
							&& !pairedSleepIns.contains(sleepInSa)) {

						sleepInSa.setEmployee(longDaySa.getEmployee());

						// Ensure SLEEP_IN has Rota reference (avoids null rota_id on persist).
						if (sleepInSa.getRota() == null) {
							sleepInSa.setRota(bestSolution);
						}

						pairedSleepIns.add(sleepInSa);
						pairedCount++;
						paired = true;
						break;
					}
				}

				if (!paired) {
					failedPairings++;
					logger.warn("No available SLEEP_IN for LONG_DAY at {} (pairId: {})",
							longDaySa.getShift().getShiftTemplate().getLocation(), pairId);
				}
			}
		}

		logger.info("\n=== SLEEP_IN PAIRING ===");
		logger.info("Successfully paired: {}", pairedCount);

		if (failedPairings > 0) {
			logger.info("Failed pairings: {}", failedPairings);
		}

		long unpairedSleepIn = bestSolution.getShiftAssignmentList().stream()
				.filter(sa -> sa.getShift().getShiftTemplate().getShiftType() == ShiftType.SLEEP_IN)
				.filter(sa -> sa.getEmployee() == null).count();

		if (unpairedSleepIn > 0) {
			logger.warn("{} SLEEP_IN shifts remain unassigned", unpairedSleepIn);
		}

		// Verify ALL shift assignments have a Rota reference before persisting, to
		// catch any assignment the solver produced without one.
		for (ShiftAssignment sa : bestSolution.getShiftAssignmentList()) {
			if (sa.getRota() == null) {
				logger.warn("ShiftAssignment missing Rota reference - fixing: {}",
						sa.getShift().getShiftTemplate().getShiftType());
				sa.setRota(bestSolution);
			}
		}

		return new PairingResult(pairedCount, failedPairings, unpairedSleepIn);
	}
}
