package com.midco.rota.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.optaplanner.core.api.score.constraint.ConstraintMatchTotal;
import org.optaplanner.core.api.solver.SolverJob;
import org.optaplanner.core.api.solver.SolverManager;
import org.optaplanner.core.api.solver.SolverStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.midco.rota.PasetoAuthenticationFilter;
import com.midco.rota.SolverTrigger;
import com.midco.rota.controller.AuthController;
import com.midco.rota.controller.RotaController;
import com.midco.rota.model.DeferredSolveRequest;
import com.midco.rota.model.Rota;
import com.midco.rota.repository.DeferredSolveRequestRepository;
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
			ConstraintExplanationService explanationService,
			RosterAnalysisService rosterAnalysisService,
			RotaRepository rotaRepository, 
			AuthController authController) {
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
	public void solveAsync(Rota schedule, Long problemId, DeferredSolveRequest deferredSolveRequest) {
		solverManager.solve(problemId, id -> schedule, bestSolution -> {
			List<ConstraintMatchTotal<?>> violations = explanationService.getConstraintViolations(bestSolution);
			// Update request as completed .
			deferredSolveRequest.setCompleted(true);
			deferredSolveRequest.setCompletedAt(LocalDateTime.now());
			rosterUpdateService.persistSolvedRota(bestSolution, deferredSolveRequest);
			rosterAnalysisService.printHighImpactViolations(bestSolution);
		});
	}

	public void solve(Rota schedule, Long problemId) {

		SolverJob<Rota, Long> solverJob = solverManager.solve(problemId, schedule  );

		try {
			Rota solvedRota = solverJob.getFinalBestSolution(); // blocks until solving is complete

			rosterAnalysisService.printHighImpactViolations(solvedRota);
			
			logger.info(" Employee count for this run :  " + solvedRota.getEmployeeList().size());
			logger.info("✅ Solving completed.");
			logger.info("⏱️ Time taken to complete: " + solverJob.getSolvingDuration());
			logger.info("Unssigned shift count "+solvedRota.getShiftAssignmentList().stream().filter(sa->sa.getEmployee()==null).count());
			solvedRota.shiftAssignmentStats().forEach((key, value) -> logger.info(key + " → " + value));
			
			// You can now inspect or persist the solvedRota
		} catch (InterruptedException | ExecutionException e) {
			System.err.println("❌ Solver failed: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
