package com.midco.rota.service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.midco.rota.model.Employee;
import com.midco.rota.model.Learning;
import com.midco.rota.model.Learning.LearningType;
import com.midco.rota.model.RotaCorrection;
import com.midco.rota.repository.EmployeeRepository;
import com.midco.rota.repository.LearningRepository;
import com.midco.rota.repository.RotaCorrectionRepository;
import com.midco.rota.util.ShiftType;

/**
 * Analyzes manual corrections to extract patterns and generate learnings
 * Works with Employee's List<String> preferredService format: ["LOCATION:60", "LOCATION2:40"]
 * 
 * EXCLUSIONS: SLEEP_IN shifts are excluded from analysis because they are 
 * always manually assigned as a post-processing step, not real preference corrections.
 */
@Service
public class PatternAnalyzer {
    
    @Autowired
    private RotaCorrectionRepository correctionRepository;
    
    @Autowired
    private LearningRepository learningRepository;
    
    @Autowired
    private EmployeeRepository employeeRepository;
    
    // Configurable thresholds
    private static final int MIN_CORRECTIONS_FOR_PATTERN = 5;
    private static final double LOCATION_AFFINITY_THRESHOLD = 0.50; // 50% of corrections at same location
    private static final double DAY_PREFERENCE_THRESHOLD = 0.25; // 25% of corrections on same day (was 0.60 - too high!)
    private static final double SHIFT_TYPE_THRESHOLD = 0.50; // 50% of corrections for same shift type
    private static final double PREFERENCE_CHANGE_THRESHOLD = 0.20; // 20% change to suggest update
    private static final int MIN_UNASSIGNED_FOR_FLAG = 5; // Minimum unassigned at location to flag
    
    /**
     * Shift types to exclude from analysis (should match CorrectionExtractorService exclusions)
     */
    private static final List<ShiftType> EXCLUDED_SHIFT_TYPES = List.of(
        ShiftType.SLEEP_IN  // Always manually assigned, not a preference indicator
    );
    
    /**
     * Analyze corrections from the last month
     */
    public List<Learning> analyzeLastMonth() {
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        return analyzeCorrections(oneMonthAgo, LocalDateTime.now());
    }
    
    /**
     * Analyze corrections in a specific date range
     */
    public List<Learning> analyzeCorrections(LocalDateTime startDate, LocalDateTime endDate) {
        System.out.println("\n=== PATTERN ANALYSIS START ===");
        
        List<RotaCorrection> corrections = correctionRepository.findByCorrectionDateBetween(startDate, endDate);
        System.out.println("Analyzing " + corrections.size() + " corrections from " + 
                          startDate.toLocalDate() + " to " + endDate.toLocalDate());
        
        return analyzeCorrections(corrections);
    }
    
    /**
     * Analyze a provided list of corrections (RECOMMENDED - faster, no DB query)
     * This is the main analysis method that should be called with pre-fetched corrections
     */
    public List<Learning> analyzeCorrections(List<RotaCorrection> corrections) {
        System.out.println("\n=== PATTERN ANALYSIS START ===");
        
        // ✅ FILTER OUT EXCLUDED SHIFT TYPES (safety measure - should already be filtered)
        int originalCount = corrections.size();
        corrections = corrections.stream()
            .filter(c -> !EXCLUDED_SHIFT_TYPES.contains(c.getShiftType()))
            .collect(Collectors.toList());
        
        int excludedCount = originalCount - corrections.size();
        if (excludedCount > 0) {
            System.out.println("Filtered out " + excludedCount + " excluded shift types (SLEEP_IN)");
        }
        
        System.out.println("Analyzing " + corrections.size() + " corrections");
        
        List<Learning> allLearnings = new ArrayList<>();
        
        // Pattern 1: Location Affinity Analysis
        allLearnings.addAll(analyzeLocationAffinity(corrections));
        
        // Pattern 2: Day Preferences
        allLearnings.addAll(analyzeDayPreferences(corrections));
        
        // Pattern 3: Shift Type Preferences
        allLearnings.addAll(analyzeShiftTypePreferences(corrections));
        
        // Pattern 4: Unassigned Patterns
        allLearnings.addAll(analyzeUnassignedPatterns(corrections));
        
        // Save all learnings
        learningRepository.saveAll(allLearnings);
        
        System.out.println("\n=== PATTERN ANALYSIS COMPLETE ===");
        System.out.println("Discovered " + allLearnings.size() + " learnings");
        
        return allLearnings;
    }
    
    /**
     * Pattern 1: Location Affinity Analysis
     * If employee is consistently assigned to same location, increase their preference weightage
     * Works with List<String> preferredService format: ["LOCATION:60", "LOCATION2:40"]
     */
    private List<Learning> analyzeLocationAffinity(List<RotaCorrection> corrections) {
        System.out.println("\n--- Analyzing Location Affinity ---");
        List<Learning> learnings = new ArrayList<>();
        
        // Group corrections by employee
        Map<Integer, List<RotaCorrection>> byEmployee = corrections.stream()
            .filter(rc -> rc.getCorrectedEmployee() != null)
            .collect(Collectors.groupingBy(rc -> rc.getCorrectedEmployee().getId()));
        
        for (Map.Entry<Integer, List<RotaCorrection>> entry : byEmployee.entrySet()) {
            Integer employeeId = entry.getKey();
            List<RotaCorrection> employeeCorrections = entry.getValue();
            
            if (employeeCorrections.size() < MIN_CORRECTIONS_FOR_PATTERN) {
                continue; // Not enough data
            }
            
            // Find most frequent location for this employee
            Map<String, Long> locationCounts = employeeCorrections.stream()
                .collect(Collectors.groupingBy(RotaCorrection::getLocation, Collectors.counting()));
            
            for (Map.Entry<String, Long> locationEntry : locationCounts.entrySet()) {
                String location = locationEntry.getKey();
                Long count = locationEntry.getValue();
                
                double frequency = (double) count / employeeCorrections.size();
                
                if (frequency >= LOCATION_AFFINITY_THRESHOLD) {
                    // This employee has strong affinity for this location
                    Employee employee = employeeRepository.findById(employeeId).orElse(null);
                    if (employee == null) continue;
                    
                    // Calculate suggested weightage
                    int suggestedWeightage = (int) (frequency * 100);
                    
                    // Get current weightage using Employee's helper method
                    int currentWeightage = employee.getServiceWeightage(location);
                    
                    // Only suggest if change is significant
                    if (Math.abs(suggestedWeightage - currentWeightage) >= (PREFERENCE_CHANGE_THRESHOLD * 100)) {
                        Learning learning = new Learning();
                        learning.setType(LearningType.EMPLOYEE_PREFERENCE);
                        learning.setEmployeeId(employeeId);
                        learning.setParameter(location);
                        learning.setOldValue(location + ":" + currentWeightage);
                        learning.setNewValue(location + ":" + suggestedWeightage);
                        learning.setConfidence(frequency);
                        learning.setSupportingEvidence(count.intValue());
                        learning.setDescription(String.format(
                            "%s %s was manually assigned to %s %d times (%.0f%% of corrections). " +
                            "Current preference: %d%%. Suggested: %d%%",
                            employee.getFirstName(), employee.getLastName(),
                            location, count, frequency * 100, 
                            currentWeightage, suggestedWeightage
                        ));
                        
                        learnings.add(learning);
                        
                        System.out.println(String.format(
                            "  ✓ LEARNED: %s %s → %s (%d corrections, confidence: %.0f%%)",
                            employee.getFirstName(), employee.getLastName(),
                            location, count, frequency * 100
                        ));
                    }
                }
            }
        }
        
        return learnings;
    }
    
    /**
     * Pattern 2: Day Preferences
     * If employee is consistently assigned on specific days, add to preferredDays list
     * Logic: Detect when frequency > random (14.3% for 7 days) and count >= 3
     */
    private List<Learning> analyzeDayPreferences(List<RotaCorrection> corrections) {
        System.out.println("\n--- Analyzing Day Preferences ---");
        List<Learning> learnings = new ArrayList<>();
        
        Map<Integer, List<RotaCorrection>> byEmployee = corrections.stream()
            .filter(rc -> rc.getCorrectedEmployee() != null)
            .collect(Collectors.groupingBy(rc -> rc.getCorrectedEmployee().getId()));
        
        for (Map.Entry<Integer, List<RotaCorrection>> entry : byEmployee.entrySet()) {
            Integer employeeId = entry.getKey();
            List<RotaCorrection> employeeCorrections = entry.getValue();
            
            if (employeeCorrections.size() < MIN_CORRECTIONS_FOR_PATTERN) {
                continue;
            }
            
            // Count by day of week
            Map<DayOfWeek, Long> dayCounts = employeeCorrections.stream()
                .filter(rc -> rc.getDayOfWeek() != null)
                .collect(Collectors.groupingBy(RotaCorrection::getDayOfWeek, Collectors.counting()));
            
            // ✅ DEBUG: Show day distribution for this employee
            if (dayCounts.isEmpty()) {
                System.out.println("  ⚠️  No dayOfWeek data for employee " + employeeId);
            } else {
                Employee emp = employeeRepository.findById(employeeId).orElse(null);
                if (emp != null) {
                    System.out.println("  Day distribution for " + emp.getFirstName() + " " + emp.getLastName() + ":");
                    dayCounts.forEach((day, count) -> {
                        double freq = (double) count / employeeCorrections.size();
                        System.out.println(String.format("    %s: %d (%.0f%%)", day, count, freq * 100));
                    });
                }
            }
            
            for (Map.Entry<DayOfWeek, Long> dayEntry : dayCounts.entrySet()) {
                DayOfWeek day = dayEntry.getKey();
                Long count = dayEntry.getValue();
                double frequency = (double) count / employeeCorrections.size();
                
                // ✅ IMPROVED: Require both frequency >= 25% AND count >= 3 for statistical significance
                if (frequency >= DAY_PREFERENCE_THRESHOLD && count >= 3) {
                    Employee employee = employeeRepository.findById(employeeId).orElse(null);
                    if (employee == null) continue;
                    
                    // Check if day is already in preferredDays
                    List<DayOfWeek> preferredDays = employee.getPreferredDays();
                    if (preferredDays != null && preferredDays.contains(day)) {
                        continue; // Already preferred
                    }
                    
                    Learning learning = new Learning();
                    learning.setType(LearningType.DAY_PREFERENCE);
                    learning.setEmployeeId(employeeId);
                    learning.setParameter(day.toString());
                    learning.setOldValue("Not preferred");
                    learning.setNewValue("Preferred");
                    learning.setConfidence(frequency);
                    learning.setSupportingEvidence(count.intValue());
                    learning.setDescription(String.format(
                        "%s %s was manually assigned on %s %d times (%.0f%% of corrections, expected random: 14%%). " +
                        "Consider adding %s to preferred days.",
                        employee.getFirstName(), employee.getLastName(),
                        day, count, frequency * 100, day
                    ));
                    
                    learnings.add(learning);
                    
                    System.out.println(String.format(
                        "  ✓ LEARNED: %s %s prefers %s (%d corrections, %.0f%%)",
                        employee.getFirstName(), employee.getLastName(),
                        day, count, frequency * 100
                    ));
                }
            }
        }
        
        if (learnings.isEmpty()) {
            System.out.println("  No day preferences detected (threshold: " + (int)(DAY_PREFERENCE_THRESHOLD * 100) + "%, min count: 3)");
        }
        
        return learnings;
    }
    
    /**
     * Pattern 3: Shift Type Preferences
     * If employee is consistently assigned to specific shift types, add to preferredShifts list
     */
    private List<Learning> analyzeShiftTypePreferences(List<RotaCorrection> corrections) {
        System.out.println("\n--- Analyzing Shift Type Preferences ---");
        List<Learning> learnings = new ArrayList<>();
        
        Map<Integer, List<RotaCorrection>> byEmployee = corrections.stream()
            .filter(rc -> rc.getCorrectedEmployee() != null)
            .collect(Collectors.groupingBy(rc -> rc.getCorrectedEmployee().getId()));
        
        for (Map.Entry<Integer, List<RotaCorrection>> entry : byEmployee.entrySet()) {
            Integer employeeId = entry.getKey();
            List<RotaCorrection> employeeCorrections = entry.getValue();
            
            if (employeeCorrections.size() < MIN_CORRECTIONS_FOR_PATTERN) {
                continue;
            }
            
            // Count by shift type
            Map<ShiftType, Long> shiftTypeCounts = employeeCorrections.stream()
                .filter(rc -> rc.getShiftType() != null)
                .collect(Collectors.groupingBy(RotaCorrection::getShiftType, Collectors.counting()));
            
            for (Map.Entry<ShiftType, Long> shiftEntry : shiftTypeCounts.entrySet()) {
                ShiftType shiftType = shiftEntry.getKey();
                Long count = shiftEntry.getValue();
                double frequency = (double) count / employeeCorrections.size();
                
                if (frequency >= SHIFT_TYPE_THRESHOLD) {
                    Employee employee = employeeRepository.findById(employeeId).orElse(null);
                    if (employee == null) continue;
                    
                    // Check if shift type is already in preferredShifts
                    List<ShiftType> preferredShifts = employee.getPreferredShifts();
                    if (preferredShifts != null && preferredShifts.contains(shiftType)) {
                        continue; // Already preferred
                    }
                    
                    Learning learning = new Learning();
                    learning.setType(LearningType.SHIFT_TYPE_PREFERENCE);
                    learning.setEmployeeId(employeeId);
                    learning.setParameter(shiftType.toString());
                    learning.setOldValue("Not preferred");
                    learning.setNewValue("Preferred");
                    learning.setConfidence(frequency);
                    learning.setSupportingEvidence(count.intValue());
                    learning.setDescription(String.format(
                        "%s %s was manually assigned to %s shifts %d times (%.0f%% of corrections). " +
                        "Consider adding %s to preferred shifts.",
                        employee.getFirstName(), employee.getLastName(),
                        shiftType, count, frequency * 100, shiftType
                    ));
                    
                    learnings.add(learning);
                    
                    System.out.println(String.format(
                        "  ✓ LEARNED: %s %s prefers %s shifts (confidence: %.0f%%)",
                        employee.getFirstName(), employee.getLastName(),
                        shiftType, frequency * 100
                    ));
                }
            }
        }
        
        return learnings;
    }
    
    /**
     * Pattern 4: Unassigned Patterns
     * Identify shifts that OptaPlanner consistently leaves unassigned
     */
    private List<Learning> analyzeUnassignedPatterns(List<RotaCorrection> corrections) {
        System.out.println("\n--- Analyzing Unassigned Patterns ---");
        List<Learning> learnings = new ArrayList<>();
        
        // Get only unassigned corrections (where original employee was null)
        List<RotaCorrection> unassignedCorrections = corrections.stream()
            .filter(RotaCorrection::wasUnassigned)
            .collect(Collectors.toList());
        
        System.out.println("  Total unassigned shifts filled manually: " + unassignedCorrections.size());
        
        if (unassignedCorrections.isEmpty()) {
            System.out.println("  ✓ No unassigned patterns detected (good!)");
            return learnings;
        }
        
        // Group by location
        Map<String, Long> unassignedByLocation = unassignedCorrections.stream()
            .collect(Collectors.groupingBy(RotaCorrection::getLocation, Collectors.counting()));
        
        // Flag locations with high unassigned counts
        for (Map.Entry<String, Long> entry : unassignedByLocation.entrySet()) {
            String location = entry.getKey();
            Long count = entry.getValue();
            
            if (count >= MIN_UNASSIGNED_FOR_FLAG) {
                Learning learning = new Learning();
                learning.setType(LearningType.NEW_PATTERN);
                learning.setParameter("unassigned_" + location);
                learning.setNewValue("Needs attention");
                learning.setConfidence(0.80); // Medium-high confidence
                learning.setSupportingEvidence(count.intValue());
                learning.setDescription(String.format(
                    "Location %s had %d shifts left unassigned by OptaPlanner. " +
                    "Consider: 1) Adding more employees with this location preference, " +
                    "2) Checking if constraints are too restrictive for this location, " +
                    "3) Increasing location preference rewards.",
                    location, count
                ));
                
                learnings.add(learning);
                
                System.out.println(String.format(
                    "  ! %s: %d shifts consistently unassigned",
                    location, count
                ));
            }
        }
        
        return learnings;
    }
}