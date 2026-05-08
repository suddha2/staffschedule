package com.midco.rota.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.midco.rota.dto.DeviceRegistrationDTO;
import com.midco.rota.dto.ShiftRequestDTO;
import com.midco.rota.dto.ShiftRequestSubmissionDTO;
import com.midco.rota.dto.UnallocatedShiftDTO;
import com.midco.rota.service.ShiftRequestService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/mobile")
public class MobileShiftController {

	private final ShiftRequestService shiftRequestService;

	public MobileShiftController(ShiftRequestService shiftRequestService) {
		this.shiftRequestService = shiftRequestService;
	}

	@PostMapping("/device/register")
	public ResponseEntity<Void> registerDevice(@RequestBody @Valid DeviceRegistrationDTO dto, Authentication auth) {
		shiftRequestService.registerDevice(auth.getName(), dto);
		return ResponseEntity.ok().build();
	}

	@GetMapping("/shifts/available/{rotaId}")
	public ResponseEntity<List<UnallocatedShiftDTO>> available(
			@PathVariable Long rotaId,
			@RequestParam(required = false) String service) {
		return ResponseEntity.ok(shiftRequestService.listAvailable(rotaId, service));
	}

	@PostMapping("/shifts/request")
	public ResponseEntity<List<ShiftRequestDTO>> submitRequest(
			@RequestBody @Valid ShiftRequestSubmissionDTO dto, Authentication auth) {
		return ResponseEntity.ok(shiftRequestService.submitRequest(auth.getName(), dto));
	}
}
