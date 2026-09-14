package com.midco.rota.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.midco.rota.model.Employee;
import com.midco.rota.model.ShiftAssignment;
import com.midco.rota.repository.ShiftAssignmentRepository;

/**
 * Continuity seeding — the person-level "carry the previous period forward" step.
 *
 * <p>House-level affinity gets a cold solve to ~72% familiar; it cannot express
 * "the same carer keeps this exact slot". This does: before solving, each new
 * shift is pre-assigned to whoever held the equivalent slot one 28-day cycle back
 * in the region's current published rota, and its {@code seededEmployeeId} is
 * recorded. The construction heuristic then only fills the gaps, and the soft
 * "Continuity — keep carer in seeded slot" constraint keeps the seed unless a
 * change scores better (leave, a hard limit, a strong preference).
 *
 * <p>Seeds are skipped when the prior carer has left the pool or is unavailable
 * on the day (so seeding never fights leave), and pinned assignments are left
 * untouched (hard pins win). With no prior published rota the seed is empty and
 * the solve falls back to affinity.
 */
@Service
public class ContinuitySeedService {

	private static final Logger logger = LoggerFactory.getLogger(ContinuitySeedService.class);

	/** Pay-cycle length; the equivalent slot one period back is exactly this many days earlier. */
	private static final int CYCLE_DAYS = 28;

	private final ShiftAssignmentRepository shiftAssignmentRepository;

	public ContinuitySeedService(ShiftAssignmentRepository shiftAssignmentRepository) {
		this.shiftAssignmentRepository = shiftAssignmentRepository;
	}

	/**
	 * Seed the given assignments from the region's prior published period. Safe
	 * no-op when there is no prior rota. {@code windowStart}/{@code windowEnd} are
	 * the (already week-snapped) solve window.
	 *
	 * @return number of assignments seeded
	 */
	public int seedFromPriorPeriod(List<ShiftAssignment> assignments, List<Employee> employees,
			String region, LocalDate windowStart, LocalDate windowEnd) {
		List<Object[]> prior;
		try {
			prior = shiftAssignmentRepository.findPriorPublishedAllocation(
					region, windowStart, windowStart.minusDays(CYCLE_DAYS), windowEnd.minusDays(CYCLE_DAYS));
		} catch (Exception e) {
			logger.warn("Continuity seed lookup failed for region {} ({}); solving without a seed. Cause: {}",
					region, windowStart, e.toString());
			return 0;
		}
		if (prior == null || prior.isEmpty()) {
			logger.info("Continuity seed: no prior published rota for region {} before {}; cold solve", region, windowStart);
			return 0;
		}

		// seedByKey: (templateId|priorDate) -> ordered list of employee ids that held that slot
		Map<String, List<Integer>> seedByKey = new HashMap<>();
		for (Object[] row : prior) {
			Integer templateId = ((Number) row[0]).intValue();
			LocalDate date = ((java.sql.Date) row[1]).toLocalDate();
			Integer empId = ((Number) row[2]).intValue();
			seedByKey.computeIfAbsent(key(templateId, date), k -> new ArrayList<>()).add(empId);
		}

		Map<Integer, Employee> pool = new HashMap<>();
		for (Employee e : employees) {
			pool.put(e.getId(), e);
		}
		int seeded = applySeed(assignments, seedByKey, pool);
		logger.info("Continuity seed: {} of {} assignments seeded from region {}'s prior period",
				seeded, assignments.size(), region);
		return seeded;
	}

	/**
	 * Apply a prebuilt seed map to the assignments. Package-visible and pure (no
	 * DB) so it is unit-testable. The seed key for a new assignment on date D from
	 * template T is (T, D-28). Pinned assignments are skipped; a seed carer that
	 * is not in the pool or is unavailable that day is skipped (leaving the slot
	 * for construction). Handles 2:1 by consuming the prior slot's carer list.
	 */
	public int applySeed(List<ShiftAssignment> assignments, Map<String, List<Integer>> seedByKey,
			Map<Integer, Employee> pool) {
		// Group new assignments by their prior-slot key so a 2:1 slot's carers are shared out.
		Map<String, List<ShiftAssignment>> byKey = new HashMap<>();
		for (ShiftAssignment sa : assignments) {
			if (sa.isPinned() || sa.getShift() == null || sa.getShift().getShiftTemplate() == null
					|| sa.getShift().getShiftStart() == null) {
				continue;
			}
			Integer templateId = sa.getShift().getShiftTemplate().getId();
			if (templateId == null) {
				continue;
			}
			LocalDate priorDate = sa.getShift().getShiftStart().minusDays(CYCLE_DAYS);
			byKey.computeIfAbsent(key(templateId, priorDate), k -> new ArrayList<>()).add(sa);
		}

		int seeded = 0;
		for (Map.Entry<String, List<ShiftAssignment>> entry : byKey.entrySet()) {
			List<Integer> priorCarers = seedByKey.get(entry.getKey());
			if (priorCarers == null || priorCarers.isEmpty()) {
				continue;
			}
			int i = 0;
			for (ShiftAssignment sa : entry.getValue()) {
				// find the next prior carer who is valid + available for this assignment's date
				while (i < priorCarers.size()) {
					Employee carer = pool.get(priorCarers.get(i));
					i++;
					if (carer == null) {
						continue; // left the pool
					}
					if (carer.isUnavailableOn(sa.getShift().getShiftStart())) {
						continue; // on leave that day — don't seed, let construction fill
					}
					sa.setSeededEmployeeId(carer.getId());
					sa.setEmployee(carer); // warm start; construction skips initialised entities
					seeded++;
					break;
				}
			}
		}
		return seeded;
	}

	private static String key(Integer templateId, LocalDate date) {
		return templateId + "|" + date;
	}
}
