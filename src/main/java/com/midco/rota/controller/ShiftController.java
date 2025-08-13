package com.midco.rota.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.midco.rota.model.Shift;
import com.midco.rota.repository.ShiftRepository;

@RestController
@RequestMapping("/api/shifts")
public class ShiftController {

	private final ShiftRepository shiftRepository;

	public ShiftController(ShiftRepository shiftRepository) {
		this.shiftRepository = shiftRepository;
	}

	@GetMapping
	public ResponseEntity<List<Shift>> getAllShits() {
		List<Shift> shift = shiftRepository.findAll();
		return ResponseEntity.ok(shift);
	}
}
