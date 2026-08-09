package com.midco.rota.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.midco.rota.model.Employee;
import com.midco.rota.model.Rota;
import com.midco.rota.model.ShiftAssignment;
import com.midco.rota.repository.EmployeeRepository;
import com.midco.rota.repository.RotaRepository;
import com.midco.rota.util.IdealShiftCount;

/**
 * Transactional DB boundary for live solver sessions (P2). Kept as its own bean
 * so {@code @Transactional} is honoured (a self-invoked transactional method on
 * {@link LiveSolverSessionService} would be bypassed by the proxy).
 */
@Service
public class LiveRotaPersistenceService {

	private static final Logger logger = LoggerFactory.getLogger(LiveRotaPersistenceService.class);

	private final RotaRepository rotaRepository;
	private final EmployeeRepository employeeRepository;

	public LiveRotaPersistenceService(RotaRepository rotaRepository, EmployeeRepository employeeRepository) {
		this.rotaRepository = rotaRepository;
		this.employeeRepository = employeeRepository;
	}

	/**
	 * Load a rota with the object graph the solver needs (assignments, their
	 * shift/template, and the employee value range) fully initialised, so the
	 * detached instance can be handed to a solver running off-transaction.
	 */
	@Transactional(readOnly = true)
	public Rota loadFullRota(Long rotaId) {
		Rota rota = rotaRepository.findById(rotaId)
				.orElseThrow(() -> new IllegalArgumentException("Rota not found: " + rotaId));

		// Force-initialise lazy graph before the transaction closes.
		rota.getEmployeeList().size();

		// Transient @ProblemFactCollectionProperty fields are only populated by the
		// Rota(employeeList, shiftAssignmentList) constructor; a JPA load uses the
		// no-arg constructor and leaves idealShiftCountList null, which OptaPlanner
		// rejects ("factCollectionProperty ... should never return null"). Rebuild
		// it exactly as the constructor does.
		if (rota.getIdealShiftCountList() == null) {
			int ideal = rota.getEmployeeList().isEmpty() ? 0
					: rota.getShiftAssignmentList().size() / rota.getEmployeeList().size();
			rota.setIdealShiftCountList(List.of(new IdealShiftCount(ideal)));
		}

		for (ShiftAssignment sa : rota.getShiftAssignmentList()) {
			if (sa.getShift() != null && sa.getShift().getShiftTemplate() != null) {
				sa.getShift().getShiftTemplate().getShiftType();
			}
			if (sa.getEmployee() != null) {
				sa.getEmployee().getId();
			}
			// @PlanningId (transient) is null after a JPA load — the no-arg
			// constructor never sets it. Anchor it to the stable DB id so
			// OptaPlanner has a valid planning id and live ProblemChanges can
			// target a slot by its DB id.
			if (sa.getPlanningId() == null && sa.getId() != null) {
				sa.setPlanningId(String.valueOf(sa.getId()));
			}
		}
		return rota;
	}

	/**
	 * Persist the employee assignments from a live best solution back onto the
	 * managed rota rows, matching by ShiftAssignment id. Only changed slots are
	 * touched. Returns the number of assignments whose employee changed.
	 *
	 * <p>Mutates managed entities inside the transaction so JPA optimistic
	 * locking (@Version) and dirty-checking apply normally.
	 */
	@Transactional
	public int applySnapshot(Long rotaId, Rota best) {
		Rota managed = rotaRepository.findById(rotaId)
				.orElseThrow(() -> new IllegalArgumentException("Rota not found: " + rotaId));

		Map<Long, ShiftAssignment> bestById = best.getShiftAssignmentList().stream()
				.filter(sa -> sa.getId() != null)
				.collect(Collectors.toMap(ShiftAssignment::getId, sa -> sa, (a, b) -> a));

		int changed = 0;
		for (ShiftAssignment m : managed.getShiftAssignmentList()) {
			ShiftAssignment b = bestById.get(m.getId());
			if (b == null) {
				continue;
			}
			Integer newEmpId = b.getEmployee() == null ? null : b.getEmployee().getId();
			Integer curEmpId = m.getEmployee() == null ? null : m.getEmployee().getId();
			if (!Objects.equals(newEmpId, curEmpId)) {
				Employee newEmp = newEmpId == null ? null : employeeRepository.findById(newEmpId).orElse(null);
				m.setEmployee(newEmp);
				changed++;
			}
		}
		logger.info("applySnapshot rota {}: {} assignment change(s)", rotaId, changed);
		return changed; // flushed on commit
	}
}
