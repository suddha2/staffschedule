package com.midco.rota.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.midco.rota.dto.ServiceLocationRequest;
import com.midco.rota.model.ServiceLocation;
import com.midco.rota.service.ServiceLocationService;

/** The Manage Services screen: a region's services, adding one, and linking a flat to its service. */
@RestController
@RequestMapping("/api/services")
public class ServiceLocationController {

	private final ServiceLocationService service;

	public ServiceLocationController(ServiceLocationService service) {
		this.service = service;
	}

	@GetMapping
	public ResponseEntity<List<ServiceLocation>> list(@RequestParam String region) {
		return ResponseEntity.ok(service.list(region));
	}

	@PreAuthorize("hasAnyRole('ADMIN','OPS_MANAGER')")
	@PostMapping
	public ResponseEntity<?> create(@RequestBody ServiceLocationRequest body) {
		try {
			return ResponseEntity.status(HttpStatus.CREATED).body(service.create(body));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
	}

	@PreAuthorize("hasAnyRole('ADMIN','OPS_MANAGER')")
	@PutMapping("/{id}")
	public ResponseEntity<?> update(@PathVariable long id, @RequestBody ServiceLocationRequest body) {
		try {
			return ResponseEntity.ok(service.update(id, body));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
	}
}
