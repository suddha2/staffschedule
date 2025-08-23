package com.midco.rota.service;

import java.time.LocalDate;
import java.util.List;

import org.optaplanner.core.api.score.constraint.ConstraintMatchTotal;
import org.optaplanner.core.api.solver.SolverManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.midco.rota.model.DeferredSolveRequest;
import com.midco.rota.model.Rota;
import com.midco.rota.repository.DeferredSolveRequestRepository;

@Service
public class SolverService {
	private static final Logger logger = LoggerFactory.getLogger(SolverService.class);

	private final SolverManager<Rota, Long> solverManager;
	private final RosterUpdateService rosterUpdateService;
	private final ConstraintExplanationService explanationService;
	private final DeferredSolveRequestRepository deferredSolveRequestRepository;
	private final RosterAnalysisService rosterAnalysisService;

	public SolverService(SolverManager<Rota, Long> solverManager, RosterUpdateService rosterUpdateService,
			ConstraintExplanationService explanationService,
			DeferredSolveRequestRepository deferredSolveRequestRepository,RosterAnalysisService rosterAnalysisService) {
		this.solverManager = solverManager;
		this.rosterUpdateService = rosterUpdateService;
		this.explanationService = explanationService;
		this.deferredSolveRequestRepository = deferredSolveRequestRepository;
		this.rosterAnalysisService = rosterAnalysisService;
	}

	public void solveAsync(Rota schedule, Long problemId, DeferredSolveRequest deferredSolveRequest) {
		logger.info("solveAsync===== " + problemId);
		solverManager.solve(problemId, id -> schedule, bestSolution -> {
			List<ConstraintMatchTotal<?>> violations = explanationService.getConstraintViolations(bestSolution);
			// Update request as completed .
			deferredSolveRequest.setCompleted(true);
			deferredSolveRequest.setCompletedAt(LocalDate.now());
			DeferredSolveRequest deferredSolveRequestObj = deferredSolveRequestRepository.save(deferredSolveRequest);
			rosterUpdateService.pushUpdate(deferredSolveRequestObj);
			rosterAnalysisService.printHighImpactViolations(bestSolution);
		});
	}
}
