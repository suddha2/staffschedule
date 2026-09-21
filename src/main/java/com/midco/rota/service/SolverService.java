package com.midco.rota.service;

import java.time.LocalDateTime;
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
import com.midco.rota.repository.RotaRepository;

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

	/**
	 * Logs the per-constraint score breakdown of a solved rota (each constraint's hard,
	 * soft and match count, biggest impact first). Diagnostic: shows what actually
	 * dominates the soft score and whether continuity constraints are firing.
	 */
	private void logScoreBreakdown(Rota solution) {
		try {
			java.util.List<org.optaplanner.core.api.score.constraint.ConstraintMatchTotal<?>> totals =
					explanationService.getConstraintViolations(solution);
			logger.info("=== Score breakdown ({} constraints) — name | hard | soft | matches ===", totals.size());
			totals.stream()
					.filter(t -> {
						var s = (org.optaplanner.core.api.score.buildin.hardsoftlong.HardSoftLongScore) t.getScore();
						return s.hardScore() != 0L || s.softScore() != 0L;
					})
					.sorted((a, b) -> {
						var sa = (org.optaplanner.core.api.score.buildin.hardsoftlong.HardSoftLongScore) a.getScore();
						var sb = (org.optaplanner.core.api.score.buildin.hardsoftlong.HardSoftLongScore) b.getScore();
						long ka = Math.abs(sa.hardScore()) * 1_000_000_000L + Math.abs(sa.softScore());
						long kb = Math.abs(sb.hardScore()) * 1_000_000_000L + Math.abs(sb.softScore());
						return Long.compare(kb, ka);
					})
					.forEach(t -> {
						var s = (org.optaplanner.core.api.score.buildin.hardsoftlong.HardSoftLongScore) t.getScore();
						logger.info("  [score] {} | hard={} soft={} matches={}",
								t.getConstraintName(), s.hardScore(), s.softScore(), t.getConstraintMatchCount());
					});
		} catch (Exception e) {
			logger.warn("Score breakdown failed: {}", e.toString());
		}
	}

	public void solveAsync(Rota schedule, Long problemId, DeferredSolveRequest deferredSolveRequest) {

		// SLEEP_IN pairing is now a shadow variable (FollowerShiftAssignment mirrors
		// its paired LONG_DAY continuously inside the solver); links are set at load
		// time. No pre-solve reset or post-solve pairing needed here.

		solverManager.solve(problemId, id -> schedule, bestSolution -> {
			try {

				// ========== PERSIST ==========
				deferredSolveRequest.setCompleted(true);
				deferredSolveRequest.setCompletedAt(LocalDateTime.now());

				rosterUpdateService.persistSolvedRota(bestSolution, deferredSolveRequest);
				logScoreBreakdown(bestSolution);
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
