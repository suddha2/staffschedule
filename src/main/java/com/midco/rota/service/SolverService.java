package com.midco.rota.service;

import java.time.LocalDateTime;
import java.util.List;

import org.optaplanner.core.api.score.constraint.ConstraintMatchTotal;
import org.optaplanner.core.api.solver.SolverManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.midco.rota.PasetoAuthenticationFilter;
import com.midco.rota.model.DeferredSolveRequest;
import com.midco.rota.model.Rota;
import com.midco.rota.repository.DeferredSolveRequestRepository;
import com.midco.rota.repository.RotaRepository;

@Service
public class SolverService {

    private final PasetoAuthenticationFilter pasetoAuthenticationFilter;
	private static final Logger logger = LoggerFactory.getLogger(SolverService.class);

	private final SolverManager<Rota, Long> solverManager;
	private final RosterUpdateService rosterUpdateService;
	private final ConstraintExplanationService explanationService;
	private final DeferredSolveRequestRepository deferredSolveRequestRepository;
	private final RosterAnalysisService rosterAnalysisService;
	private final RotaRepository rotaRepository;

	public SolverService(SolverManager<Rota, Long> solverManager, RosterUpdateService rosterUpdateService,
			ConstraintExplanationService explanationService,
			DeferredSolveRequestRepository deferredSolveRequestRepository, RosterAnalysisService rosterAnalysisService,
			RotaRepository rotaRepository, PasetoAuthenticationFilter pasetoAuthenticationFilter) {
		this.solverManager = solverManager;
		this.rosterUpdateService = rosterUpdateService;
		this.explanationService = explanationService;
		this.deferredSolveRequestRepository = deferredSolveRequestRepository;
		this.rosterAnalysisService = rosterAnalysisService;
		this.rotaRepository = rotaRepository;
		this.pasetoAuthenticationFilter = pasetoAuthenticationFilter;
	}

	public void solveAsync(Rota schedule, Long problemId, DeferredSolveRequest deferredSolveRequest) {
		logger.info("solveAsync===== " + problemId);
		solverManager.solve(problemId, id -> schedule, bestSolution -> {
			List<ConstraintMatchTotal<?>> violations = explanationService.getConstraintViolations(bestSolution);
			// Update request as completed .
			deferredSolveRequest.setCompleted(true);
			deferredSolveRequest.setCompletedAt(LocalDateTime.now());
			deferredSolveRequest.setRotaId(problemId);
//			DeferredSolveRequest deferredSolveRequestObj = deferredSolveRequestRepository.save(deferredSolveRequest);
//			rotaRepository.saveAndFlush(bestSolution);
			rosterUpdateService.persistSolvedRota(bestSolution,problemId,deferredSolveRequest);
//			rosterAnalysisService.printHighImpactViolations(bestSolution);
		});
	}
}
