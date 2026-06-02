package com.midco.rota.controller;

import com.midco.rota.model.Period;
import com.midco.rota.repository.PeriodRepository;
import com.midco.rota.service.PeriodGenerationService;
import com.midco.rota.service.PayCycleDataService;
import com.midco.rota.util.PayCycleRow;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/periods")
public class PeriodController {

    private final PeriodGenerationService periodGenerationService;
    private final PeriodRepository periodRepository;
    private final PayCycleDataService payCycleDataService;

    public PeriodController(
            PeriodGenerationService periodGenerationService, 
            PeriodRepository periodRepository,
            PayCycleDataService payCycleDataService) {
        this.periodGenerationService = periodGenerationService;
        this.periodRepository = periodRepository;
        this.payCycleDataService = payCycleDataService;
    }

    @PreAuthorize("hasAnyRole('ADMIN','OPS_MANAGER')")
    @PostMapping("/generate")
    public ResponseEntity<String> generate() {
        try {
            return ResponseEntity.ok(periodGenerationService.manualGenerate());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed: " + e.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','OPS_MANAGER')")
    @PostMapping("/archive")
    public ResponseEntity<String> archive() {
        try {
            return ResponseEntity.ok(periodGenerationService.manualArchive());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed: " + e.getMessage());
        }
    }

    @GetMapping("/status")
    public ResponseEntity<String> status() {
        return ResponseEntity.ok(periodGenerationService.getStatus());
    }

    /**
     * Get archived periods grouped by year range (initial call - summary only)
     * GET /api/periods/archived
     */
    @GetMapping("/archived")
    public ResponseEntity<Map<String, Object>> getArchivedPeriods() {
        List<Period> archivedPeriods = periodRepository.findAllArchived();
        
        if (archivedPeriods.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyMap());
        }
        
        // Group into cycles based on date continuity
        List<List<Period>> cycles = new ArrayList<>();
        List<Period> currentCycle = new ArrayList<>();
        
        for (int i = 0; i < archivedPeriods.size(); i++) {
            Period period = archivedPeriods.get(i);
            currentCycle.add(period);
            
            boolean isEndOfCycle = false;
            
            if (i == archivedPeriods.size() - 1) {
                isEndOfCycle = true;
            } else {
                Period nextPeriod = archivedPeriods.get(i + 1);
                LocalDate expectedNextStart = period.getEndDate().plusDays(1);
                
                if (!nextPeriod.getStartDate().equals(expectedNextStart)) {
                    isEndOfCycle = true;
                }
            }
            
            if (isEndOfCycle) {
                cycles.add(new ArrayList<>(currentCycle));
                currentCycle.clear();
            }
        }
        
        // Convert cycles to summary format
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (List<Period> cycle : cycles) {
            if (cycle.isEmpty()) continue;
            
            int startYear = cycle.get(0).getStartDate().getYear();
            int endYear = cycle.get(cycle.size() - 1).getEndDate().getYear();
            
            String cycleYearRange;
            if (startYear == endYear) {
                cycleYearRange = String.valueOf(startYear);
            } else {
                cycleYearRange = startYear + "-" + endYear;
            }
            
            Map<String, Object> group = new LinkedHashMap<>();
            group.put("yearRange", cycleYearRange);
            group.put("periodCount", cycle.size());
            group.put("startDate", cycle.get(0).getStartDate());
            group.put("endDate", cycle.get(cycle.size() - 1).getEndDate());
            
            result.add(group);
        }
        
        // Sort by year range (most recent first)
        result.sort((a, b) -> ((String) b.get("yearRange")).compareTo((String) a.get("yearRange")));
        
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("archivedGroups", result);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get detailed archived period data (same format as payCycleSchedule)
     * GET /api/periods/archived/details?yearRange=2025-2026&location=X
     */
    @GetMapping("/archived/details")
    public ResponseEntity<List<PayCycleRow>> getArchivedPeriodDetails(
            @RequestParam String yearRange,
            @RequestParam(required = false, defaultValue = "") String location) {
        
        // Fetch archived periods in PayCycleRow format (same as active periods)
        List<PayCycleRow> rows = payCycleDataService.fetchArchivedRows(yearRange, location);
        
        if (rows.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(rows);
    }
}