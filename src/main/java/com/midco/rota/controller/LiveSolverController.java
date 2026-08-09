package com.midco.rota.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.midco.rota.service.LiveSolverSessionService;

/**
 * Endpoints controlling live / continuous real-time planning sessions (P2).
 *
 * <ul>
 *   <li>{@code POST /api/rota/{id}/live/start}    — start a live solver</li>
 *   <li>{@code POST /api/rota/{id}/live/snapshot} — persist current live best</li>
 *   <li>{@code POST /api/rota/{id}/live/stop}     — terminate the live solver</li>
 *   <li>{@code GET  /api/rota/{id}/live/status}   — session/solver status</li>
 * </ul>
 *
 * While running, best solutions stream to STOMP topic {@code /topic/rota/{id}}.
 */
@RestController
@RequestMapping("/api/rota")
public class LiveSolverController {

	private static final Logger log = LoggerFactory.getLogger(LiveSolverController.class);

	private final LiveSolverSessionService liveSolverSessionService;

	public LiveSolverController(LiveSolverSessionService liveSolverSessionService) {
		this.liveSolverSessionService = liveSolverSessionService;
	}

	@PreAuthorize("hasAnyRole('ADMIN','OPS_MANAGER','ROTA_EDITOR')")
	@PostMapping("/{id}/live/start")
	public ResponseEntity<?> start(@PathVariable Long id) {
		try {
			boolean started = liveSolverSessionService.start(id);
			return ResponseEntity.ok(Map.of("rotaId", id, "started", started,
					"message", started ? "Live solver started" : "Live solver already running"));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
		} catch (Exception e) {
			log.error("Failed to start live solver for rota {}", id, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("error", "Failed to start live solver: " + e.getMessage()));
		}
	}

	@PreAuthorize("hasAnyRole('ADMIN','OPS_MANAGER','ROTA_EDITOR')")
	@PostMapping("/{id}/live/snapshot")
	public ResponseEntity<?> snapshot(@PathVariable Long id) {
		try {
			int changed = liveSolverSessionService.snapshot(id);
			return ResponseEntity.ok(Map.of("rotaId", id, "assignmentsChanged", changed));
		} catch (IllegalStateException e) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
		} catch (Exception e) {
			log.error("Failed to snapshot live rota {}", id, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("error", "Failed to snapshot: " + e.getMessage()));
		}
	}

	@PreAuthorize("hasAnyRole('ADMIN','OPS_MANAGER','ROTA_EDITOR')")
	@PostMapping("/{id}/live/stop")
	public ResponseEntity<?> stop(@PathVariable Long id) {
		boolean tracked = liveSolverSessionService.stop(id);
		return ResponseEntity.ok(Map.of("rotaId", id, "stopped", true, "wasTracked", tracked));
	}

	@PreAuthorize("hasAnyRole('ADMIN','OPS_MANAGER','ROTA_EDITOR')")
	@GetMapping("/{id}/live/status")
	public ResponseEntity<?> status(@PathVariable Long id) {
		return ResponseEntity.ok(liveSolverSessionService.status(id));
	}
}
