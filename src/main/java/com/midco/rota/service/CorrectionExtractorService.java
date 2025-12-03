package com.midco.rota.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.midco.rota.model.Employee;
import com.midco.rota.model.RotaCorrection;
import com.midco.rota.model.RotaFeeder;
import com.midco.rota.repository.EmployeeRepository;
import com.midco.rota.repository.RotaCorrectionRepository;
import com.midco.rota.repository.RotaFeederRepository;
import com.midco.rota.util.ShiftType;

/**
 * Extracts corrections from rota_feeder table by comparing Auto vs Manual entries
 * 
 * EXCLUSIONS: SLEEP_IN shifts are excluded from learning because they are 
 * always manually assigned as a post-processing step after OptaPlanner runs.
 */
@Service
public class CorrectionExtractorService {
    
    @Autowired
    private RotaFeederRepository rotaFeederRepository;
    
    @Autowired
    private RotaCorrectionRepository correctionRepository;
    
    @Autowired
    private EmployeeRepository employeeRepository;
    
    // ============================================================================
    // CONFIGURABLE EXCLUSIONS
    // ============================================================================
    
    /**
     * Shift types to exclude from learning
     * SLEEP_IN: Always manually assigned after solving, not a real correction
     */
    private static final List<ShiftType> EXCLUDED_SHIFT_TYPES = List.of(
        ShiftType.SLEEP_IN  // Add more shift types here if needed: ShiftType.WAKING_NIGHT, etc.
    );
    
    /**
     * Extract corrections from the last month
     */
    @Transactional
    public List<RotaCorrection> extractCorrectionsFromLastMonth() {
        LocalDate today = LocalDate.now();
        LocalDate oneMonthAgo = today.minusMonths(1);
        return extractCorrections(oneMonthAgo, today);
    }
    
    /**
     * Extract corrections from rota_feeder for a specific date range
     * Compares Auto (OptaPlanner) vs Manual (user corrected) entries
     */
    @Transactional
    public List<RotaCorrection> extractCorrections(LocalDate startDate, LocalDate endDate) {
        System.out.println("\n=== EXTRACTING CORRECTIONS FROM ROTA_FEEDER ===");
        System.out.println("Date range: " + startDate + " to " + endDate);
        
        // Get all Auto and Manual entries
        List<RotaFeeder> autoEntries = rotaFeederRepository.findByEmpSourceAndDateRange("Auto", startDate, endDate);
        List<RotaFeeder> manualEntries = rotaFeederRepository.findByEmpSourceAndDateRange("Manual", startDate, endDate);
        
        System.out.println("Auto-generated entries: " + autoEntries.size());
        System.out.println("Manual entries: " + manualEntries.size());
        
        // Create map for quick lookup: location+date+shiftType -> RotaFeeder
        Map<String, RotaFeeder> autoMap = new HashMap<>();
        for (RotaFeeder auto : autoEntries) {
            String key = makeShiftKey(auto);
            autoMap.put(key, auto);
        }
        
        // Process each manual entry and create corrections
        List<RotaCorrection> corrections = new ArrayList<>();
        int excludedCount = 0;
        
        for (RotaFeeder manual : manualEntries) {
            // ✅ EXCLUDE SLEEP_IN and other configured shift types
            if (EXCLUDED_SHIFT_TYPES.contains(manual.getShiftType())) {
                excludedCount++;
                continue; // Skip this shift - it's always manually assigned
            }
            
            String key = makeShiftKey(manual);
            RotaFeeder auto = autoMap.get(key);
            
            // Find employees
            Employee originalEmployee = null;
            if (auto != null && auto.getFirstName() != null && auto.getLastName() != null) {
                try {
                    originalEmployee = employeeRepository.findByFirstNameAndLastName(
                        auto.getFirstName(), auto.getLastName()
                    );
                } catch (Exception e) {
                    // Employee not found - leave as null
                }
            }
            
            Employee correctedEmployee = null;
            if (manual.getFirstName() != null && manual.getLastName() != null) {
                try {
                    correctedEmployee = employeeRepository.findByFirstNameAndLastName(
                        manual.getFirstName(), manual.getLastName()
                    );
                } catch (Exception e) {
                    // Employee not found - leave as null
                    System.out.println("⚠️  Employee not found: " + manual.getFirstName() + " " + manual.getLastName());
                }
            }
            
            // Create correction record
            RotaCorrection correction = new RotaCorrection();
            correction.setOriginalEmployee(originalEmployee);
            correction.setCorrectedEmployee(correctedEmployee);
            correction.setLocation(manual.getLocation());
            correction.setShiftType(manual.getShiftType());
            
            // ✅ FIX: Extract dayOfWeek from shiftStart date (manual.getDay() may not exist)
            if (manual.getShiftStart() != null) {
                DayOfWeek dayOfWeek = manual.getShiftStart().getDayOfWeek();
                correction.setDayOfWeek(dayOfWeek);
            }
            
            correction.setShiftDate(manual.getShiftStart());
            correction.setWeek(manual.getWeek());
            correction.setSource("rota_feeder");
            correction.setCorrectionDate(LocalDateTime.now());
            
            // Determine reason if possible
            if (originalEmployee == null && correctedEmployee != null) {
                correction.setCorrectionReason("Filled unassigned shift");
            } else if (originalEmployee != null && correctedEmployee != null 
                      && !originalEmployee.getId().equals(correctedEmployee.getId())) {
                correction.setCorrectionReason("Reassigned to different employee");
            }
            
            corrections.add(correction);
        }
        
        // Save all corrections
        correctionRepository.saveAll(corrections);
        
        System.out.println("=== EXTRACTION COMPLETE ===");
        System.out.println("Extracted " + corrections.size() + " corrections");
        if (excludedCount > 0) {
            System.out.println("Excluded " + excludedCount + " shifts (SLEEP_IN - always manually assigned)");
        }
        System.out.println();
        
        return corrections;
    }
    
    /**
     * Get correction statistics for a date range
     */
    public Map<String, Object> getCorrectionStats(LocalDate startDate, LocalDate endDate) {
        Long autoCount = rotaFeederRepository.countByEmpSourceAndDateRange("Auto", startDate, endDate);
        Long manualCount = rotaFeederRepository.countByEmpSourceAndDateRange("Manual", startDate, endDate);
        
        // Count assigned vs unassigned in Auto entries
        List<RotaFeeder> autoEntries = rotaFeederRepository.findByEmpSourceAndDateRange("Auto", startDate, endDate);
        long autoAssigned = autoEntries.stream()
            .filter(rf -> rf.getFirstName() != null && rf.getLastName() != null)
            .count();
        long autoUnassigned = autoEntries.size() - autoAssigned;
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalShifts", autoCount + manualCount);
        stats.put("autoGenerated", autoCount);
        stats.put("manualCorrections", manualCount);
        stats.put("autoAssigned", autoAssigned);
        stats.put("autoUnassigned", autoUnassigned);
        
        // Calculate allocation rate
        if (autoCount > 0) {
            double allocationRate = (autoAssigned * 100.0) / autoCount;
            stats.put("allocationRate", String.format("%.1f", allocationRate));
        } else {
            stats.put("allocationRate", "0.0");
        }
        
        // Calculate correction rate
        if ((autoCount + manualCount) > 0) {
            double correctionRate = (manualCount * 100.0) / (autoCount + manualCount);
            stats.put("correctionRate", String.format("%.1f", correctionRate));
        } else {
            stats.put("correctionRate", "0.0");
        }
        
        return stats;
    }
    
    /**
     * Create unique key for matching shifts
     * Format: location|shiftType|shiftStart|day
     */
    private String makeShiftKey(RotaFeeder rf) {
        return String.format("%s|%s|%s|%s",
            rf.getLocation(),
            rf.getShiftType(),
            rf.getShiftStart(),
            rf.getDay()
        );
    }
    
    /**
     * Check if two RotaFeeder entries represent the same shift
     */
    private boolean shiftsMatch(RotaFeeder rf1, RotaFeeder rf2) {
        if (rf1 == null || rf2 == null) return false;
        
        return rf1.getLocation().equals(rf2.getLocation()) &&
               rf1.getShiftType() == rf2.getShiftType() &&
               rf1.getShiftStart().equals(rf2.getShiftStart()) &&
               rf1.getDay() == rf2.getDay();
    }
}