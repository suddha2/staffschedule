package com.midco.rota.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.midco.rota.model.ShiftTypeDef;
import com.midco.rota.opt.ShiftTypeMeta;
import com.midco.rota.repository.ShiftTypeDefRepository;

/**
 * Read + admin management of the data-driven shift types. The template builder's
 * dropdown reads the list; the "Manage shift types" admin screen creates and edits
 * rows here. Adding a type — with its rate, caps, required skill and behaviour flags —
 * is a row, not a deploy.
 *
 * <p>Writes are ADMIN/OPS_MANAGER only and, after committing, refresh the static
 * {@link ShiftTypeMeta} view so a changed rate, cap or flag reaches the next solve
 * and cost report without a restart. Deleting is a soft deactivate: templates and
 * history reference a type by its code, so a row is never physically removed.
 */
@RestController
@RequestMapping("/api/shift-types")
public class ShiftTypeController {

    /** Rate bases the cost report understands. */
    private static final Set<String> RATE_BASES = Set.of("HOURLY", "DAILY", "FLAT");

    private final ShiftTypeDefRepository repo;
    private final ShiftTypeMeta shiftTypeMeta;

    public ShiftTypeController(ShiftTypeDefRepository repo, ShiftTypeMeta shiftTypeMeta) {
        this.repo = repo;
        this.shiftTypeMeta = shiftTypeMeta;
    }

    @GetMapping
    public ResponseEntity<List<ShiftTypeDef>> list(@RequestParam(required = false) Boolean active) {
        List<ShiftTypeDef> types = Boolean.TRUE.equals(active)
                ? repo.findByActiveTrueOrderByDisplayNameAsc()
                : repo.findAll();
        return ResponseEntity.ok(types);
    }

    @PreAuthorize("hasAnyRole('ADMIN','OPS_MANAGER')")
    @PostMapping
    public ResponseEntity<?> create(@RequestBody ShiftTypeDef body) {
        String code = normaliseCode(body.getCode());
        if (code == null) {
            return badRequest("code is required");
        }
        if (!code.matches("[A-Z0-9_]{2,40}")) {
            return badRequest("code must be 2–40 chars, UPPER_CASE letters, digits or underscore");
        }
        if (repo.existsById(code)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "a shift type with code '" + code + "' already exists"));
        }
        String invalid = validateBody(body);
        if (invalid != null) {
            return badRequest(invalid);
        }

        ShiftTypeDef def = new ShiftTypeDef();
        def.setCode(code);
        applyEditableFields(def, body);
        ShiftTypeDef saved = repo.save(def);
        shiftTypeMeta.reload();
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PreAuthorize("hasAnyRole('ADMIN','OPS_MANAGER')")
    @PutMapping("/{code}")
    public ResponseEntity<?> update(@PathVariable String code, @RequestBody ShiftTypeDef body) {
        ShiftTypeDef def = repo.findById(code).orElse(null);
        if (def == null) {
            return ResponseEntity.notFound().build();
        }
        String invalid = validateBody(body);
        if (invalid != null) {
            return badRequest(invalid);
        }
        applyEditableFields(def, body);
        ShiftTypeDef saved = repo.save(def);
        shiftTypeMeta.reload();
        return ResponseEntity.ok(saved);
    }

    /** Soft delete: deactivate rather than remove, since templates/history reference the code. */
    @PreAuthorize("hasAnyRole('ADMIN','OPS_MANAGER')")
    @DeleteMapping("/{code}")
    public ResponseEntity<?> deactivate(@PathVariable String code) {
        ShiftTypeDef def = repo.findById(code).orElse(null);
        if (def == null) {
            return ResponseEntity.notFound().build();
        }
        def.setActive(false);
        repo.save(def);
        shiftTypeMeta.reload();
        return ResponseEntity.ok(def);
    }

    // ------------------------------------------------------------------ helpers

    /** Copies the fields an admin may set; never the code (the immutable @Id). */
    private void applyEditableFields(ShiftTypeDef def, ShiftTypeDef body) {
        def.setDisplayName(body.getDisplayName() != null && !body.getDisplayName().isBlank()
                ? body.getDisplayName().trim() : def.getCode());
        def.setActive(body.isActive());
        def.setCountsTowardWeeklyCap(body.isCountsTowardWeeklyCap());
        def.setCountsAsWork(body.isCountsAsWork());
        def.setCountsAsLocationCoverage(body.isCountsAsLocationCoverage());
        def.setPaidHours(body.isPaidHours());
        def.setMineable(body.isMineable());
        def.setDefaultRateBasis(body.getDefaultRateBasis() == null ? "HOURLY"
                : body.getDefaultRateBasis().trim().toUpperCase());
        def.setDefaultIsFollower(body.isDefaultIsFollower());
        def.setDefaultPairsWith(blankToNull(body.getDefaultPairsWith()));
        def.setRate(body.getRate());
        def.setRequiredSkill(blankToNull(body.getRequiredSkill()));
        def.setMaxHoursPerDay(body.getMaxHoursPerDay());
        def.setMaxPerWeek(body.getMaxPerWeek());
    }

    /** Returns an error message when the body is invalid, or null when it is fine. */
    private String validateBody(ShiftTypeDef body) {
        String basis = body.getDefaultRateBasis();
        if (basis != null && !basis.isBlank() && !RATE_BASES.contains(basis.trim().toUpperCase())) {
            return "rateBasis must be one of HOURLY, DAILY or FLAT";
        }
        BigDecimal rate = body.getRate();
        if (rate != null && rate.signum() < 0) {
            return "rate cannot be negative";
        }
        if (body.getMaxHoursPerDay() != null && body.getMaxHoursPerDay() <= 0) {
            return "maxHoursPerDay must be positive, or empty for no cap";
        }
        if (body.getMaxPerWeek() != null && body.getMaxPerWeek() <= 0) {
            return "maxPerWeek must be positive, or empty for no cap";
        }
        if (body.isDefaultIsFollower() && blankToNull(body.getDefaultPairsWith()) == null) {
            return "a follower type must name the leader type it pairs with";
        }
        return null;
    }

    private static String normaliseCode(String code) {
        if (code == null) {
            return null;
        }
        String c = code.trim().toUpperCase();
        return c.isEmpty() ? null : c;
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static ResponseEntity<Map<String, String>> badRequest(String message) {
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }
}
