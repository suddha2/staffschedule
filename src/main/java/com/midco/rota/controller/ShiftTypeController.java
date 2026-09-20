package com.midco.rota.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.midco.rota.model.ShiftTypeDef;
import com.midco.rota.repository.ShiftTypeDefRepository;

/**
 * Read access to the data-driven shift types, so the template builder's dropdown is
 * fed from the {@code shift_type} table instead of a hard-coded list. Adding a type
 * is a row, not a deploy.
 */
@RestController
@RequestMapping("/api/shift-types")
public class ShiftTypeController {

    private final ShiftTypeDefRepository repo;

    public ShiftTypeController(ShiftTypeDefRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public ResponseEntity<List<ShiftTypeDef>> list(@RequestParam(required = false) Boolean active) {
        List<ShiftTypeDef> types = Boolean.TRUE.equals(active)
                ? repo.findByActiveTrueOrderByDisplayNameAsc()
                : repo.findAll();
        return ResponseEntity.ok(types);
    }
}
