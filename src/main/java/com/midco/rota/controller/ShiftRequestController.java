package com.midco.rota.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.midco.rota.dto.ResolveRequestDTO;
import com.midco.rota.dto.ShiftRequestDTO;
import com.midco.rota.service.ShiftRequestService;
import com.midco.rota.util.ShiftRequestStatus;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/shift-requests")
public class ShiftRequestController {

	private final ShiftRequestService shiftRequestService;

	public ShiftRequestController(ShiftRequestService shiftRequestService) {
		this.shiftRequestService = shiftRequestService;
	}

	@GetMapping
	public ResponseEntity<List<ShiftRequestDTO>> listForRota(
			@RequestParam(required = false) Long rotaId,
			@RequestParam(required = false) ShiftRequestStatus status) {
		return ResponseEntity.ok(shiftRequestService.listRequestsForRota(rotaId, status));
	}

	@PutMapping("/{requestId}/resolve")
	public ResponseEntity<ShiftRequestDTO> resolve(
			@PathVariable Long requestId,
			@RequestBody @Valid ResolveRequestDTO dto,
			Authentication auth) {
		return ResponseEntity.ok(shiftRequestService.resolveRequest(requestId, auth.getName(), dto.getAction()));
	}
}
