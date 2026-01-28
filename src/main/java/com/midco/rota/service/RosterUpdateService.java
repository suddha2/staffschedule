package com.midco.rota.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.optaplanner.core.api.score.constraint.ConstraintMatchTotal;
import org.optaplanner.core.api.solver.SolverStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.midco.rota.RotaUpdatePayload;
import com.midco.rota.dto.SaveScheduleRequest;
import com.midco.rota.model.DeferredSolveRequest;
import com.midco.rota.model.Rota;
import com.midco.rota.repository.DeferredSolveRequestRepository;
import com.midco.rota.repository.RotaRepository;
import com.midco.rota.util.PayCycleRow;

import jakarta.transaction.Transactional;

@Service
public class RosterUpdateService {

	private static final Logger logger = LoggerFactory.getLogger(RosterUpdateService.class);

	@Autowired
	private SimpMessagingTemplate messagingTemplate;
	@Autowired
	private RotaRepository rotaRepository;
	@Autowired
	private DeferredSolveRequestRepository deferredSolveRequestRepository;

	@Autowired
	private PayCycleDataService payCycleDataService;
	@Autowired
	private RosterAnalysisService rosterAnalysisService;
	@Autowired
	private ScheduleVersionService scheduleVersionService;

	public RosterUpdateService(SimpMessagingTemplate messagingTemplate, RotaRepository rotaRepository,
			DeferredSolveRequestRepository deferredSolveRequestRepository, RosterAnalysisService rosterAnalysisService,
			PayCycleDataService payCycleDataService, ScheduleVersionService scheduleVersionService) {
		this.messagingTemplate = messagingTemplate;
		this.rotaRepository = rotaRepository;
		this.deferredSolveRequestRepository = deferredSolveRequestRepository;
		this.rosterAnalysisService = rosterAnalysisService;
		this.payCycleDataService = payCycleDataService;
		this.scheduleVersionService = scheduleVersionService;
	}

	public void pushUpdate(Rota rota, List<ConstraintMatchTotal<?>> violations, SolverStatus status, String user) {
		RotaUpdatePayload payload = new RotaUpdatePayload();
		payload.setRota(rota);
		payload.setViolations(violations);
		payload.setStatus("SOLVED"); // TODO:

		logger.info(" Rota ", payload.toString());
		messagingTemplate.convertAndSendToUser(user, "/queue/rotaUpdate", rota);

	}

	public void pushUpdate(DeferredSolveRequest deferredSolveRequest) {
		messagingTemplate.convertAndSendToUser(deferredSolveRequest.getCreatedBy(), "/queue/req-update",
				deferredSolveRequest);
	}

	public void pushUpdate(List<PayCycleRow> pcr, String user) {
		messagingTemplate.convertAndSendToUser(user, "/queue/req-update", pcr);
	}

	public void pushUpdate(String link, String msg, String user) {
		messagingTemplate.convertAndSendToUser(user, link, msg);
	}

	@Transactional
	public void persistSolvedRota(Rota bestSolution, DeferredSolveRequest deferredSolveRequest) {

		Rota managedRota = rotaRepository.save(bestSolution);
		deferredSolveRequest.setScheduleSummary(managedRota.rotaSummaryStats());
		deferredSolveRequest.setCompleted(true);
		deferredSolveRequest.setCompletedAt(LocalDateTime.now());
		deferredSolveRequest.setRotaId(managedRota.getId());
		deferredSolveRequestRepository.save(deferredSolveRequest);

		// ✅ Create Version 1 immediately after generation
		SaveScheduleRequest versionRequest = new SaveScheduleRequest();
		versionRequest.setRotaId(deferredSolveRequest.getRotaId());
		versionRequest.setVersionLabel("Initial Schedule");
		versionRequest.setComment("Auto-generated initial version");
		versionRequest.setUsername(deferredSolveRequest.getCreatedBy());
		versionRequest.setChanges(new ArrayList<>()); // Empty changes

		scheduleVersionService.createVersion(versionRequest);

		this.pushUpdate(payCycleDataService.fetchRows(deferredSolveRequest), deferredSolveRequest.getCreatedBy());

		// Push updates and log violations
		// this.pushUpdate(deferredSolveRequest);
		// rosterAnalysisService.printHighImpactViolations(bestSolution);
	}
}
