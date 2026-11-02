package com.midco.rota.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.midco.rota.dto.StandingPin;
import com.midco.rota.service.StandingPinService;

/** The Standing Pins screen: a region's pins, removing them, and undoing a removal. */
@RestController
@RequestMapping("/api/standing-pins")
public class StandingPinController {

	public record RemoveRequest(String region, List<Long> pinIds) {
	}

	private final StandingPinService pins;

	public StandingPinController(StandingPinService pins) {
		this.pins = pins;
	}

	@GetMapping
	public ResponseEntity<List<StandingPin>> list(@RequestParam String region) {
		return ResponseEntity.ok(pins.list(region));
	}

	@GetMapping("/removals")
	public ResponseEntity<List<Map<String, Object>>> history(@RequestParam String region) {
		return ResponseEntity.ok(pins.history(region));
	}

	@GetMapping("/pin-all-impact/{rotaId}")
	public ResponseEntity<Map<String, Object>> pinAllImpact(@PathVariable long rotaId) {
		return ResponseEntity.ok(pins.pinAllImpact(rotaId));
	}

	@PreAuthorize("hasAnyRole('ADMIN','OPS_MANAGER')")
	@PostMapping("/remove")
	public ResponseEntity<?> remove(@RequestBody RemoveRequest body, Authentication auth) {
		try {
			return ResponseEntity.ok(Map.of("removalId", pins.remove(body.region(), body.pinIds(), auth.getName())));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
	}

	@PreAuthorize("hasAnyRole('ADMIN','OPS_MANAGER')")
	@PostMapping("/remove-skipped/{skippedPinId}")
	public ResponseEntity<?> removeSkipped(@PathVariable long skippedPinId, Authentication auth) {
		return ResponseEntity.ok(Map.of("removed", pins.removeSkipped(skippedPinId, auth.getName())));
	}

	@PreAuthorize("hasAnyRole('ADMIN','OPS_MANAGER')")
	@PostMapping("/removals/{id}/undo")
	public ResponseEntity<?> undo(@PathVariable long id, Authentication auth) {
		try {
			return ResponseEntity.ok(Map.of("restored", pins.undo(id, auth.getName())));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
	}
}
