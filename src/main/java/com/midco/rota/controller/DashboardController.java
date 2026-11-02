package com.midco.rota.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.midco.rota.dto.DashboardSummary;
import com.midco.rota.service.DashboardService;

/** The home screen. Open to any signed-in user, like the rota pages it links to. */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

	private final DashboardService dashboard;

	public DashboardController(DashboardService dashboard) {
		this.dashboard = dashboard;
	}

	@GetMapping
	public ResponseEntity<DashboardSummary> summary() {
		return ResponseEntity.ok(dashboard.summary());
	}
}
