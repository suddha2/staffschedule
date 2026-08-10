package com.midco.rota.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.midco.rota.service.LiveCapacityException;
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
	public ResponseEntity<?> start(@PathVariable Long id, Authentication auth) {
		try {
			boolean started = liveSolverSessionService.start(id, auth.getName());
			return ResponseEntity.ok(Map.of("rotaId", id, "started", started,
					"message", started ? "Live solver started" : "Live solver already running"));
		} catch (LiveCapacityException e) {
			return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("error", e.getMessage()));
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
	public ResponseEntity<?> snapshot(@PathVariable Long id, Authentication auth) {
		try {
			int changed = liveSolverSessionService.snapshot(id, auth.getName());
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
	public ResponseEntity<?> stop(@PathVariable Long id, Authentication auth) {
		boolean tracked = liveSolverSessionService.stop(id, auth.getName());
		return ResponseEntity.ok(Map.of("rotaId", id, "stopped", true, "wasTracked", tracked));
	}

	@PreAuthorize("hasAnyRole('ADMIN','OPS_MANAGER','ROTA_EDITOR')")
	@GetMapping("/{id}/live/status")
	public ResponseEntity<?> status(@PathVariable Long id) {
		return ResponseEntity.ok(liveSolverSessionService.status(id));
	}

	/**
	 * Live edit: assign (or clear) the employee on a slot and set its pin state.
	 * Body: {@code {"assignmentId": <long>, "employeeId": <int|null>, "pin": <bool, default true>}}.
	 */
	@PreAuthorize("hasAnyRole('ADMIN','OPS_MANAGER','ROTA_EDITOR')")
	@PostMapping("/{id}/live/assign")
	public ResponseEntity<?> assign(@PathVariable Long id, @RequestBody Map<String, Object> body) {
		if (body.get("assignmentId") == null) {
			return ResponseEntity.badRequest().body(Map.of("error", "assignmentId is required"));
		}
		long assignmentId = Long.parseLong(body.get("assignmentId").toString());
		Integer employeeId = body.get("employeeId") == null ? null : Integer.valueOf(body.get("employeeId").toString());
		boolean pin = body.get("pin") == null || Boolean.parseBoolean(body.get("pin").toString());
		try {
			liveSolverSessionService.applyAssignment(id, assignmentId, employeeId, pin);
			return ResponseEntity.ok(Map.of("rotaId", id, "assignmentId", assignmentId,
					"employeeId", employeeId == null ? "" : employeeId, "pin", pin, "queued", true));
		} catch (IllegalStateException e) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
		}
	}

	/**
	 * Live edit: pin or unpin a slot without changing its employee.
	 * Body: {@code {"assignmentId": <long>, "pinned": <bool>}}.
	 */
	@PreAuthorize("hasAnyRole('ADMIN','OPS_MANAGER','ROTA_EDITOR')")
	@PostMapping("/{id}/live/pin")
	public ResponseEntity<?> pin(@PathVariable Long id, @RequestBody Map<String, Object> body) {
		if (body.get("assignmentId") == null || body.get("pinned") == null) {
			return ResponseEntity.badRequest().body(Map.of("error", "assignmentId and pinned are required"));
		}
		long assignmentId = Long.parseLong(body.get("assignmentId").toString());
		boolean pinned = Boolean.parseBoolean(body.get("pinned").toString());
		try {
			liveSolverSessionService.applyPin(id, assignmentId, pinned);
			return ResponseEntity.ok(Map.of("rotaId", id, "assignmentId", assignmentId, "pinned", pinned, "queued", true));
		} catch (IllegalStateException e) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
		}
	}

	/**
	 * Structural live edit: add an employee to the running solve's value range.
	 * Body: {@code {"employeeId": <int>}}.
	 */
	@PreAuthorize("hasAnyRole('ADMIN','OPS_MANAGER','ROTA_EDITOR')")
	@PostMapping("/{id}/live/employee/add")
	public ResponseEntity<?> addEmployee(@PathVariable Long id, @RequestBody Map<String, Object> body) {
		if (body.get("employeeId") == null) {
			return ResponseEntity.badRequest().body(Map.of("error", "employeeId is required"));
		}
		Integer employeeId = Integer.valueOf(body.get("employeeId").toString());
		try {
			liveSolverSessionService.addEmployee(id, employeeId);
			return ResponseEntity.ok(Map.of("rotaId", id, "employeeId", employeeId, "added", true, "queued", true));
		} catch (IllegalStateException e) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
		}
	}

	/**
	 * Structural live edit: remove an employee from the value range (unassigns
	 * their slots). Body: {@code {"employeeId": <int>}}.
	 */
	@PreAuthorize("hasAnyRole('ADMIN','OPS_MANAGER','ROTA_EDITOR')")
	@PostMapping("/{id}/live/employee/remove")
	public ResponseEntity<?> removeEmployee(@PathVariable Long id, @RequestBody Map<String, Object> body) {
		if (body.get("employeeId") == null) {
			return ResponseEntity.badRequest().body(Map.of("error", "employeeId is required"));
		}
		Integer employeeId = Integer.valueOf(body.get("employeeId").toString());
		try {
			liveSolverSessionService.removeEmployee(id, employeeId);
			return ResponseEntity.ok(Map.of("rotaId", id, "employeeId", employeeId, "removed", true, "queued", true));
		} catch (IllegalStateException e) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
		}
	}
}
