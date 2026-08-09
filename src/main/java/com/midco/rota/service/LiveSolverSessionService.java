package com.midco.rota.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.optaplanner.core.api.solver.SolverManager;
import org.optaplanner.core.api.solver.SolverStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.midco.rota.dto.LiveRotaUpdate;
import com.midco.rota.dto.LiveRotaUpdate.Slot;
import com.midco.rota.model.Employee;
import com.midco.rota.model.Rota;
import com.midco.rota.model.ShiftAssignment;
import com.midco.rota.opt.AssignEmployeeProblemChange;
import com.midco.rota.opt.SetPinProblemChange;

/**
 * Owns the lifecycle of continuous / live real-time planning sessions (P2).
 *
 * <p>A session is a daemon solver (see {@code liveSolverConfig.xml}) running on
 * an existing rota. It streams each new best solution to
 * {@code /topic/rota/{rotaId}} (throttled to ~1 Hz), keeps the latest best in
 * memory, and can snapshot it back to the DB on demand. Sessions are keyed by
 * rota id, capped by the live SolverManager's parallelSolverCount, and evicted
 * when idle to free the daemon thread.
 *
 * <p>P2 scope: start / stream / snapshot / stop. Feeding live edits as
 * ProblemChanges is P3; the frontend live grid is P4.
 */
@Service
public class LiveSolverSessionService {

	private static final Logger logger = LoggerFactory.getLogger(LiveSolverSessionService.class);

	/** Don't push more than one frame per rota per this interval. */
	private static final long MIN_PUSH_INTERVAL_MS = 1000L;
	/** Stop a session that has produced no new best solution for this long. */
	private static final long IDLE_TIMEOUT_MS = 15 * 60 * 1000L;

	private final SolverManager<Rota, Long> liveSolverManager;
	private final LiveRotaPersistenceService persistenceService;
	private final SleepInPairingService sleepInPairingService;
	private final SimpMessagingTemplate messagingTemplate;

	private final Map<Long, LiveSession> sessions = new ConcurrentHashMap<>();

	public LiveSolverSessionService(@Qualifier("liveSolverManager") SolverManager<Rota, Long> liveSolverManager,
			LiveRotaPersistenceService persistenceService, SleepInPairingService sleepInPairingService,
			SimpMessagingTemplate messagingTemplate) {
		this.liveSolverManager = liveSolverManager;
		this.persistenceService = persistenceService;
		this.sleepInPairingService = sleepInPairingService;
		this.messagingTemplate = messagingTemplate;
	}

	private static final class LiveSession {
		volatile Rota latestBest;
		volatile long lastActivityMs;
		volatile long lastPushMs;

		LiveSession(long nowMs) {
			this.lastActivityMs = nowMs;
		}
	}

	/**
	 * Start a live solver for {@code rotaId}. No-op (returns false) if one is
	 * already active for that rota. Synchronized so two concurrent starts for the
	 * same rota can't both call solveAndListen.
	 */
	public synchronized boolean start(Long rotaId) {
		SolverStatus status = liveSolverManager.getSolverStatus(rotaId);
		if (status != SolverStatus.NOT_SOLVING) {
			logger.info("Live solver already active for rota {} ({})", rotaId, status);
			return false;
		}

		Rota rota = persistenceService.loadFullRota(rotaId);
		// Rota.equals/hashCode key on planningId — must be set before solving.
		rota.setPlanningId(rotaId);
		sleepInPairingService.resetSleepIns(rota);

		sessions.put(rotaId, new LiveSession(System.currentTimeMillis()));
		liveSolverManager.solveAndListen(rotaId, id -> rota, best -> onBestSolution(rotaId, best));
		logger.info("Live solver started for rota {}", rotaId);
		return true;
	}

	/** Called on the solver thread for every new best solution. */
	private void onBestSolution(Long rotaId, Rota best) {
		LiveSession session = sessions.get(rotaId);
		if (session == null) {
			return; // stopped/evicted between best solutions
		}
		session.latestBest = best; // planning clone — safe to keep and mutate
		long now = System.currentTimeMillis();
		session.lastActivityMs = now;

		if (now - session.lastPushMs >= MIN_PUSH_INTERVAL_MS) {
			session.lastPushMs = now;
			// Pair SLEEP_INs only on frames we actually stream (keeps it ~1 Hz).
			sleepInPairingService.pairSleepIns(best);
			messagingTemplate.convertAndSend("/topic/rota/" + rotaId, buildUpdate(rotaId, best));
		}
	}

	/**
	 * Persist the current live best solution back to the DB without stopping the
	 * solver. Returns the number of assignments whose employee changed.
	 */
	public int snapshot(Long rotaId) {
		LiveSession session = sessions.get(rotaId);
		if (session == null || session.latestBest == null) {
			throw new IllegalStateException("No live best solution to snapshot for rota " + rotaId);
		}
		Rota best = session.latestBest;
		sleepInPairingService.pairSleepIns(best); // ensure SLEEP_INs are filled before persist
		int changed = persistenceService.applySnapshot(rotaId, best);
		session.lastActivityMs = System.currentTimeMillis();
		logger.info("Snapshotted live rota {} -> {} assignment change(s) persisted", rotaId, changed);
		return changed;
	}

	/**
	 * Live edit (P3): assign (or clear, when {@code employeeId} is null) the
	 * employee on a slot and set its pin state, feeding it to the running solver
	 * as a ProblemChange. The solver re-optimises and streams a new best solution.
	 */
	public void applyAssignment(Long rotaId, long assignmentId, Integer employeeId, boolean pin) {
		requireActive(rotaId);
		liveSolverManager.addProblemChange(rotaId, new AssignEmployeeProblemChange(assignmentId, employeeId, pin));
		touch(rotaId);
	}

	/** Live edit (P3): pin or unpin a slot without changing its employee. */
	public void applyPin(Long rotaId, long assignmentId, boolean pinned) {
		requireActive(rotaId);
		liveSolverManager.addProblemChange(rotaId, new SetPinProblemChange(assignmentId, pinned));
		touch(rotaId);
	}

	private void requireActive(Long rotaId) {
		if (!sessions.containsKey(rotaId) || liveSolverManager.getSolverStatus(rotaId) == SolverStatus.NOT_SOLVING) {
			throw new IllegalStateException("No live solver running for rota " + rotaId);
		}
	}

	private void touch(Long rotaId) {
		LiveSession session = sessions.get(rotaId);
		if (session != null) {
			session.lastActivityMs = System.currentTimeMillis();
		}
	}

	/** Terminate the live solver for {@code rotaId} and forget the session. */
	public boolean stop(Long rotaId) {
		boolean tracked = sessions.remove(rotaId) != null;
		liveSolverManager.terminateEarly(rotaId);
		logger.info("Live solver stopped for rota {} (was tracked: {})", rotaId, tracked);
		return tracked;
	}

	public Map<String, Object> status(Long rotaId) {
		LiveSession session = sessions.get(rotaId);
		SolverStatus solverStatus = liveSolverManager.getSolverStatus(rotaId);
		Map<String, Object> result = new HashMap<>();
		result.put("rotaId", rotaId);
		result.put("solverStatus", solverStatus.name());
		result.put("tracked", session != null);
		result.put("score", session != null && session.latestBest != null && session.latestBest.getScore() != null
				? session.latestBest.getScore().toString()
				: null);
		return result;
	}

	/** Reclaim daemon threads held by sessions that have gone quiet. */
	@Scheduled(fixedDelay = 60_000L)
	public void evictIdleSessions() {
		long now = System.currentTimeMillis();
		for (Map.Entry<Long, LiveSession> entry : sessions.entrySet()) {
			if (now - entry.getValue().lastActivityMs > IDLE_TIMEOUT_MS) {
				logger.info("Evicting idle live solver for rota {}", entry.getKey());
				stop(entry.getKey());
			}
		}
	}

	private LiveRotaUpdate buildUpdate(Long rotaId, Rota best) {
		List<Slot> slots = new ArrayList<>();
		int assigned = 0;
		for (ShiftAssignment sa : best.getShiftAssignmentList()) {
			Employee emp = sa.getEmployee();
			Integer empId = emp == null ? null : emp.getId();
			String empName = emp == null ? null
					: ((emp.getFirstName() == null ? "" : emp.getFirstName()) + " "
							+ (emp.getLastName() == null ? "" : emp.getLastName())).trim();
			if (emp != null) {
				assigned++;
			}
			slots.add(new Slot(sa.getId(), empId, empName, sa.isPinned()));
		}
		String score = best.getScore() == null ? null : best.getScore().toString();
		String solverStatus = liveSolverManager.getSolverStatus(rotaId).name();
		return new LiveRotaUpdate(rotaId, solverStatus, score, assigned, best.getShiftAssignmentList().size(), slots);
	}
}
