package com.midco.rota.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.optaplanner.core.api.solver.SolverJob;
import org.optaplanner.core.api.solver.SolverManager;
import org.optaplanner.core.api.solver.SolverStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.midco.rota.controller.AuthController;
import com.midco.rota.model.DeferredSolveRequest;
import com.midco.rota.model.Rota;
import com.midco.rota.model.ShiftAssignment;
import com.midco.rota.repository.RotaRepository;
import com.midco.rota.util.ShiftType;

@Service
public class SolverService {

//	private final PasetoAuthenticationFilter pasetoAuthenticationFilter;
	private static final Logger logger = LoggerFactory.getLogger(SolverService.class);

	private final SolverManager<Rota, Long> solverManager;
	private final RosterUpdateService rosterUpdateService;
	private final ConstraintExplanationService explanationService;
//	private final DeferredSolveRequestRepository deferredSolveRequestRepository;
	private final RosterAnalysisService rosterAnalysisService;
//	private final RotaRepository rotaRepository;

	public SolverService(SolverManager<Rota, Long> solverManager, RosterUpdateService rosterUpdateService,
			ConstraintExplanationService explanationService, RosterAnalysisService rosterAnalysisService,
			RotaRepository rotaRepository, AuthController authController) {
		this.solverManager = solverManager;
		this.rosterUpdateService = rosterUpdateService;
		this.explanationService = explanationService;
//		this.deferredSolveRequestRepository = deferredSolveRequestRepository;
		this.rosterAnalysisService = rosterAnalysisService;
//		this.rotaRepository = rotaRepository;
//		this.pasetoAuthenticationFilter = pasetoAuthenticationFilter;

	}

	public SolverStatus getSolverStatus(long id) {
		return this.solverManager.getSolverStatus(id);
	}

//	public void solveAsync(Rota schedule, Long problemId, DeferredSolveRequest deferredSolveRequest) {
//		solverManager.solve(problemId, id -> schedule, bestSolution -> {
//			List<ConstraintMatchTotal<?>> violations = explanationService.getConstraintViolations(bestSolution);
//			// Update request as completed .
//			deferredSolveRequest.setCompleted(true);
//			deferredSolveRequest.setCompletedAt(LocalDateTime.now());
//			rosterUpdateService.persistSolvedRota(bestSolution, deferredSolveRequest);
//			rosterAnalysisService.printHighImpactViolations(bestSolution);
//			logger.info("Solve complete for " + problemId);
//		});
//	}
	public void solveAsync(Rota schedule, Long problemId, DeferredSolveRequest deferredSolveRequest) {

		// Initialize SLEEP_IN as unassigned (pinned via isPinned())
		for (ShiftAssignment sa : schedule.getShiftAssignmentList()) {
			if (sa.getShift().getShiftTemplate().getShiftType() == ShiftType.SLEEP_IN) {
				sa.setEmployee(null);
			}
		}

		solverManager.solve(problemId, id -> schedule, bestSolution -> {
			try {

				// ========== SLEEP_IN PAIRING ==========
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
								
								// ✅ FIX 1: Ensure SLEEP_IN has Rota reference
								// This is critical to avoid the null rota_id error
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

				// ✅ FIX 2: Verify ALL shift assignments have Rota reference
				// Double-check before persisting to catch any missed assignments
				for (ShiftAssignment sa : bestSolution.getShiftAssignmentList()) {
					if (sa.getRota() == null) {
						logger.warn("ShiftAssignment missing Rota reference - fixing: {}", 
							sa.getShift().getShiftTemplate().getShiftType());
						sa.setRota(bestSolution);
					}
				}
				
				// ========== PERSIST ==========
				deferredSolveRequest.setCompleted(true);
				deferredSolveRequest.setCompletedAt(LocalDateTime.now());

				rosterUpdateService.persistSolvedRota(bestSolution, deferredSolveRequest);
				logger.info("Solve complete for problemId: {}", problemId);

			} catch (Exception e) {
				logger.error("Error processing solved rota for problemId: {}", problemId, e);
				deferredSolveRequest.setCompleted(false);
				deferredSolveRequest.setCompletedAt(LocalDateTime.now());
				throw new RuntimeException("Failed to process solved rota", e);
			}
		});
	}

	public Rota solve(Rota schedule, Long problemId) {

		SolverJob<Rota, Long> solverJob = solverManager.solve(problemId, schedule);

		try {
			Rota solvedRota = solverJob.getFinalBestSolution(); // blocks until solving is complete

//			rosterAnalysisService.printHighImpactViolations(solvedRota);

//			logger.info(" Employee count for this run :  " + solvedRota.getEmployeeList().size());
//			logger.info("✅ Solving completed.");
//			logger.info("⏱️ Time taken to complete: " + solverJob.getSolvingDuration());
//			logger.info("Unssigned shift count "
//					+ solvedRota.getShiftAssignmentList().stream().filter(sa -> sa.getEmployee() == null).count());
//			solvedRota.shiftAssignmentStats().forEach((key, value) -> logger.info(key + " → " + value));

			// You can now inspect or persist the solvedRota
			return solvedRota;
		} catch (InterruptedException | ExecutionException e) {
			System.err.println("❌ Solver failed: " + e.getMessage());
			e.printStackTrace();
		}
		return null;
	}
}
