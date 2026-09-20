package com.midco.rota.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.midco.rota.model.ConstraintSetting;
import com.midco.rota.model.SolverTuningSetting;
import com.midco.rota.repository.ConstraintSettingRepository;
import com.midco.rota.repository.SolverTuningSettingRepository;
import com.midco.rota.service.SolverConfigService;

/**
 * Admin access to the solver "levers": per-constraint weights and severity
 * ({@code constraint_setting}) and the numeric thresholds the constraints compare
 * against ({@code solver_tuning}), including the min-rest hours. These were
 * SQL-only; this exposes them so an operator can enable/disable a rule, retune a
 * weight or set a threshold from the app.
 *
 * <p>Constraint weights are read fresh at the start of every solve
 * ({@code SolverConfigService.buildConstraintConfiguration}), so an edit here
 * applies to the next solve with no restart. Tuning values live in a process-wide
 * holder, so a tuning edit calls {@link SolverConfigService#refreshTuning()} to
 * push it into the running process. Rows are seeded on startup, so the lists are
 * populated for editing but new keys are never created here.
 */
@RestController
@RequestMapping("/api/solver-settings")
public class SolverSettingsController {

    private final ConstraintSettingRepository constraintRepo;
    private final SolverTuningSettingRepository tuningRepo;
    private final SolverConfigService solverConfigService;

    public SolverSettingsController(ConstraintSettingRepository constraintRepo,
            SolverTuningSettingRepository tuningRepo, SolverConfigService solverConfigService) {
        this.constraintRepo = constraintRepo;
        this.tuningRepo = tuningRepo;
        this.solverConfigService = solverConfigService;
    }

    // ------------------------------------------------------------- constraints

    @GetMapping("/constraints")
    public ResponseEntity<List<ConstraintSetting>> listConstraints() {
        List<ConstraintSetting> all = constraintRepo.findAll();
        all.sort((a, b) -> a.getConstraintName().compareToIgnoreCase(b.getConstraintName()));
        return ResponseEntity.ok(all);
    }

    @PreAuthorize("hasAnyRole('ADMIN','OPS_MANAGER')")
    @PutMapping("/constraints/{name}")
    public ResponseEntity<?> updateConstraint(@PathVariable String name, @RequestBody ConstraintSetting body) {
        ConstraintSetting row = constraintRepo.findById(name).orElse(null);
        if (row == null) {
            return ResponseEntity.notFound().build();
        }
        if (body.getScoreWeight() < 0L) {
            return badRequest("score weight cannot be negative");
        }
        if (body.getConstraintType() == null) {
            return badRequest("constraint type must be HARD or SOFT");
        }
        row.setScoreWeight(body.getScoreWeight());
        row.setConstraintType(body.getConstraintType());
        row.setEnabled(body.isEnabled());
        // Description is seed metadata, not operator-owned; leave it untouched.
        ConstraintSetting saved = constraintRepo.save(row);
        // No reload: buildConstraintConfiguration() reads constraint_setting fresh per solve.
        return ResponseEntity.ok(saved);
    }

    // ----------------------------------------------------------------- tuning

    @GetMapping("/tuning")
    public ResponseEntity<List<SolverTuningSetting>> listTuning() {
        List<SolverTuningSetting> all = tuningRepo.findAll();
        all.sort((a, b) -> a.getSettingKey().compareToIgnoreCase(b.getSettingKey()));
        return ResponseEntity.ok(all);
    }

    @PreAuthorize("hasAnyRole('ADMIN','OPS_MANAGER')")
    @PutMapping("/tuning/{key}")
    public ResponseEntity<?> updateTuning(@PathVariable String key, @RequestBody SolverTuningSetting body) {
        SolverTuningSetting row = tuningRepo.findById(key).orElse(null);
        if (row == null) {
            return ResponseEntity.notFound().build();
        }
        if (body.getIntValue() < 0) {
            return badRequest("value cannot be negative (0 switches a threshold off where that applies)");
        }
        row.setIntValue(body.getIntValue());
        tuningRepo.save(row);
        solverConfigService.refreshTuning(); // push into the running process for the next solve
        return ResponseEntity.ok(row);
    }

    // ----------------------------------------------------------------- helpers

    private static ResponseEntity<Map<String, String>> badRequest(String message) {
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }
}
