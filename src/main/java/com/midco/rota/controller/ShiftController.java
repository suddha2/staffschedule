package com.midco.rota.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.midco.rota.model.ShiftTemplate;
import com.midco.rota.repository.ShiftTemplateRepository;

@RestController
@RequestMapping("/api/shifts")
public class ShiftController {

	private final ShiftTemplateRepository shiftRepository;

	public ShiftController(ShiftTemplateRepository shiftRepository) {
		this.shiftRepository = shiftRepository;
	}

	@GetMapping
	public ResponseEntity<List<ShiftTemplate>> getAllShits() {
		List<ShiftTemplate> shiftTemplate = shiftRepository.findAll();
		return ResponseEntity.ok(shiftTemplate);
	}
}
