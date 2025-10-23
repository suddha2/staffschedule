package com.midco.rota.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.midco.rota.model.EmployeeShiftStatDTO;
import com.midco.rota.model.PaycycleStatsDTO;
import com.midco.rota.service.PaycycleStatsService;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

	@Autowired
	private final PaycycleStatsService statsService;

    public StatsController(PaycycleStatsService statsService) {
        this.statsService = statsService;
    }
	
	@GetMapping("/serviceStats")
	public ResponseEntity<List<PaycycleStatsDTO>> serviceStats(@RequestParam Long id) {

		List<PaycycleStatsDTO> summary = statsService.generateServiceSummary(id);

		return ResponseEntity.ok(summary);
	}
	@GetMapping("/empStats")
	public ResponseEntity<List<EmployeeShiftStatDTO>> empStats(@RequestParam Long id) {

		List<EmployeeShiftStatDTO> summary = statsService.generateEmpSummary(id);

		return ResponseEntity.ok(summary);
	}
}
