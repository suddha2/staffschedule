package com.midco.rota.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.midco.rota.model.Learning;
import com.midco.rota.model.RotaCorrection;
import com.midco.rota.service.CorrectionExtractorService;
import com.midco.rota.service.LearningApplicationService;
import com.midco.rota.service.LearningOrchestrator;

/**
 * REST API for managing the learning system
 */
@RestController
@RequestMapping("/api/learning")
public class LearningController {
    
    @Autowired
    private LearningOrchestrator orchestrator;
    
    @Autowired
    private CorrectionExtractorService correctionExtractor;
    
    @Autowired
    private LearningApplicationService learningApplication;
    
    /**
     * Run the complete monthly learning cycle
     * POST /api/learning/cycle
     */
    @PostMapping("/cycle")
    public ResponseEntity<String> runLearningCycle() {
        try {
            orchestrator.runMonthlyLearningCycle();
            return ResponseEntity.ok("Learning cycle completed successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body("Error running learning cycle: " + e.getMessage());
        }
    }
    
    /**
     * Run learning cycle for custom date range
     * POST /api/learning/cycle?startDate=2024-10-01&endDate=2024-11-01
     */
    @PostMapping("/cycle/custom")
    public ResponseEntity<String> runCustomLearningCycle(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            orchestrator.runLearningCycle(startDate, endDate);
            return ResponseEntity.ok("Learning cycle completed successfully for " + startDate + " to " + endDate);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body("Error running learning cycle: " + e.getMessage());
        }
    }
    
    /**
     * Extract corrections from rota_feeder (without running full cycle)
     * POST /api/learning/extract?startDate=2024-10-01&endDate=2024-11-01
     */
    @PostMapping("/extract")
    public ResponseEntity<List<RotaCorrection>> extractCorrections(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            if (startDate == null || endDate == null) {
                // Default to last month
                endDate = LocalDate.now();
                startDate = endDate.minusMonths(1);
            }
            List<RotaCorrection> corrections = correctionExtractor.extractCorrections(startDate, endDate);
            return ResponseEntity.ok(corrections);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    /**
     * Get correction statistics
     * GET /api/learning/stats?startDate=2024-10-01&endDate=2024-11-01
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCorrectionStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            if (startDate == null || endDate == null) {
                // Default to last month
                endDate = LocalDate.now();
                startDate = endDate.minusMonths(1);
            }
            Map<String, Object> stats = correctionExtractor.getCorrectionStats(startDate, endDate);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    /**
     * Get all pending learnings
     * GET /api/learning/pending
     */
    @GetMapping("/pending")
    public ResponseEntity<List<Learning>> getPendingLearnings() {
        try {
            List<Learning> pending = learningApplication.getPendingLearnings();
            return ResponseEntity.ok(pending);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    /**
     * Get high confidence learnings (>= 80%)
     * GET /api/learning/high-confidence
     */
    @GetMapping("/high-confidence")
    public ResponseEntity<List<Learning>> getHighConfidenceLearnings() {
        try {
            List<Learning> highConfidence = learningApplication.getHighConfidenceLearnings();
            return ResponseEntity.ok(highConfidence);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    /**
     * Apply a specific learning
     * POST /api/learning/apply/123
     */
    @PostMapping("/apply/{learningId}")
    public ResponseEntity<String> applyLearning(@PathVariable Long learningId) {
        try {
            boolean success = learningApplication.applyLearning(learningId);
            if (success) {
                return ResponseEntity.ok("Learning applied successfully");
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                   .body("Failed to apply learning");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body("Error applying learning: " + e.getMessage());
        }
    }
    
    /**
     * Reject a learning
     * POST /api/learning/reject/123
     * Body: {"reason": "Employee confirmed different preference"}
     */
    @PostMapping("/reject/{learningId}")
    public ResponseEntity<String> rejectLearning(
            @PathVariable Long learningId,
            @RequestBody Map<String, String> request) {
        try {
            String reason = request.getOrDefault("reason", "Rejected by user");
            learningApplication.rejectLearning(learningId, reason);
            return ResponseEntity.ok("Learning rejected");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body("Error rejecting learning: " + e.getMessage());
        }
    }
    
    /**
     * Get learning statistics
     * GET /api/learning/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getLearningStatistics() {
        try {
            Map<String, Object> stats = learningApplication.getLearningStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    /**
     * Auto-apply all high confidence learnings
     * POST /api/learning/auto-apply
     */
    @PostMapping("/auto-apply")
    public ResponseEntity<String> autoApplyHighConfidence() {
        try {
            int applied = learningApplication.autoApplyHighConfidenceLearnings();
            return ResponseEntity.ok("Auto-applied " + applied + " high-confidence learnings");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body("Error auto-applying learnings: " + e.getMessage());
        }
    }
}