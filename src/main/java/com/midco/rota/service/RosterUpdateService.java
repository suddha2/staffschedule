package com.midco.rota.service;

import java.util.List;

import org.optaplanner.core.api.score.constraint.ConstraintMatchTotal;
import org.optaplanner.core.api.solver.SolverStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.midco.rota.RotaUpdatePayload;
import com.midco.rota.model.Rota;

@Service
public class RosterUpdateService {

	private static final Logger logger = LoggerFactory.getLogger(RosterUpdateService.class);
	
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public RosterUpdateService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void pushUpdate(Rota rota, List<ConstraintMatchTotal<?>> violations,SolverStatus status) {
    	logger.info("pushUpdate======= count of violations "+violations.size());
    	
        RotaUpdatePayload payload = new RotaUpdatePayload();
        payload.setRota(rota);
        payload.setViolations(violations);
        payload.setStatus("SOLVED"); // TODO:

        messagingTemplate.convertAndSend("/topic/rotaUpdate", rota);
    }
}
