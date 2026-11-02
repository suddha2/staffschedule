package com.midco.rota.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.midco.rota.dto.ServiceBulkRequest;
import com.midco.rota.dto.ServiceLocationRequest;
import com.midco.rota.dto.ServiceNameToFix;
import com.midco.rota.model.ServiceLocation;
import com.midco.rota.service.ServiceBulkUpdateService;
import com.midco.rota.service.ServiceLocationService;

/**
 * The Manage Services screen: a region's services, adding one, linking a flat to its service,
 * and bulk rename, merge or remove of a service name.
 */
@RestController
@RequestMapping("/api/services")
public class ServiceLocationController {

	private final ServiceLocationService service;
	private final ServiceBulkUpdateService bulk;

	public ServiceLocationController(ServiceLocationService service, ServiceBulkUpdateService bulk) {
		this.service = service;
		this.bulk = bulk;
	}

	@GetMapping("/names-to-fix")
	public ResponseEntity<List<ServiceNameToFix>> namesToFix(@RequestParam String region) {
		return ResponseEntity.ok(bulk.namesToFix(region));
	}

	@GetMapping("/bulk")
	public ResponseEntity<List<Map<String, Object>>> bulkHistory(@RequestParam String region) {
		return ResponseEntity.ok(bulk.history(region));
	}

	@PreAuthorize("hasAnyRole('ADMIN','OPS_MANAGER')")
	@PostMapping("/bulk/preview")
	public ResponseEntity<?> bulkPreview(@RequestBody ServiceBulkRequest body) {
		try {
			return ResponseEntity.ok(bulk.preview(body));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
	}

	@PreAuthorize("hasAnyRole('ADMIN','OPS_MANAGER')")
	@PostMapping("/bulk")
	public ResponseEntity<?> bulkApply(@RequestBody ServiceBulkRequest body, Authentication auth) {
		try {
			return ResponseEntity.ok(bulk.apply(body, auth.getName()));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
	}

	@PreAuthorize("hasAnyRole('ADMIN','OPS_MANAGER')")
	@PostMapping("/bulk/{id}/undo")
	public ResponseEntity<?> bulkUndo(@PathVariable long id, Authentication auth) {
		try {
			bulk.undo(id, auth.getName());
			return ResponseEntity.ok(Map.of("undone", id));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
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
