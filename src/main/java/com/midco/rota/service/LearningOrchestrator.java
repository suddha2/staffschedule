package com.midco.rota.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.midco.rota.model.Learning;
import com.midco.rota.model.RotaCorrection;

/**
 * Orchestrates the complete learning cycle:
 * 1. Extract corrections from rota_feeder
 * 2. Analyze patterns
 * 3. Auto-apply high confidence learnings
 * 4. Generate reports
 */
@Service
public class LearningOrchestrator {
    
    @Autowired
    private CorrectionExtractorService correctionExtractor;
    
    @Autowired
    private PatternAnalyzer patternAnalyzer;
    
    @Autowired
    private LearningApplicationService learningApplication;
    
    /**
     * Run the complete monthly learning cycle
     */
    public void runMonthlyLearningCycle() {
        System.out.println("\n╔════════════════════════════════════════════════╗");
        System.out.println("║   MONTHLY LEARNING CYCLE START                 ║");
        System.out.println("╚════════════════════════════════════════════════╝\n");
        
        LocalDate today = LocalDate.now();
        LocalDate oneMonthAgo = today.minusMonths(1);
        
        // Step 1: Extract corrections from rota_feeder
        List<RotaCorrection> corrections = correctionExtractor.extractCorrections(oneMonthAgo, today);
        
        // Get statistics
        Map<String, Object> stats = correctionExtractor.getCorrectionStats(oneMonthAgo, today);
        System.out.println("\n=== STATISTICS ===");
        stats.forEach((key, value) -> System.out.println(key + ": " + value));
        
        // Step 2: Analyze patterns from corrections (✅ FIXED: Pass corrections directly)
        List<Learning> learnings = patternAnalyzer.analyzeCorrections(corrections);
        
        // Step 3: Auto-apply high confidence learnings (>= 80%)
        int appliedCount = learningApplication.autoApplyHighConfidenceLearnings();
        
        // Step 4: Generate report
        generateLearningReport(stats, learnings, appliedCount);
        
        System.out.println("\n╔════════════════════════════════════════════════╗");
        System.out.println("║   MONTHLY LEARNING CYCLE COMPLETE              ║");
        System.out.println("╚════════════════════════════════════════════════╝\n");
    }
    
    /**
     * Run learning cycle for custom date range
     */
    public void runLearningCycle(LocalDate startDate, LocalDate endDate) {
        System.out.println("\n╔════════════════════════════════════════════════╗");
        System.out.println("║   LEARNING CYCLE START                         ║");
        System.out.println("║   Range: " + startDate + " to " + endDate + "      ║");
        System.out.println("╚════════════════════════════════════════════════╝\n");
        
        // Step 1: Extract corrections
        List<RotaCorrection> corrections = correctionExtractor.extractCorrections(startDate, endDate);
        
        // Get statistics
        Map<String, Object> stats = correctionExtractor.getCorrectionStats(startDate, endDate);
        System.out.println("\n=== STATISTICS ===");
        stats.forEach((key, value) -> System.out.println(key + ": " + value));
        
        // Step 2: Analyze patterns (✅ FIXED: Pass corrections directly instead of querying DB)
        List<Learning> learnings = patternAnalyzer.analyzeCorrections(corrections);
        
        // Step 3: Auto-apply high confidence learnings
        int appliedCount = learningApplication.autoApplyHighConfidenceLearnings();
        
        // Step 4: Generate report
        generateLearningReport(stats, learnings, appliedCount);
        
        System.out.println("\n╔════════════════════════════════════════════════╗");
        System.out.println("║   LEARNING CYCLE COMPLETE                      ║");
        System.out.println("╚════════════════════════════════════════════════╝\n");
    }
    
    /**
     * Generate learning report
     */
    private void generateLearningReport(Map<String, Object> stats, List<Learning> learnings, int appliedCount) {
        System.out.println("\n╔════════════════════════════════════════════════╗");
        System.out.println("║   LEARNING REPORT                              ║");
        System.out.println("╚════════════════════════════════════════════════╝");
        
        // OptaPlanner Performance
        System.out.println("\n📊 OptaPlanner Performance:");
        System.out.println("   Allocation rate: " + stats.get("allocationRate") + "%");
        System.out.println("   Auto-assigned: " + stats.get("autoAssigned") + " shifts");
        System.out.println("   Unassigned: " + stats.get("autoUnassigned") + " shifts");
        
        // Manual Corrections
        System.out.println("\n✏️  Manual Corrections:");
        System.out.println("   Manual corrections: " + stats.get("manualCorrections"));
        System.out.println("   Correction rate: " + stats.get("correctionRate") + "%");
        
        // Learnings Discovered
        System.out.println("\n🧠 Learnings Discovered:");
        System.out.println("   Total patterns discovered: " + learnings.size());
        
        // Count by type
        Map<String, Long> byType = new java.util.HashMap<>();
        learnings.forEach(l -> byType.merge(l.getType().toString(), 1L, Long::sum));
        byType.forEach((type, count) -> 
            System.out.println("   - " + type + ": " + count)
        );
        
        // Applied Learnings
        System.out.println("\n✅ Learnings Applied:");
        System.out.println("   High-confidence auto-applied: " + appliedCount);
        
        // Pending Review
        int pending = learnings.size() - appliedCount;
        System.out.println("\n⏳ Pending Review:");
        System.out.println("   Learnings requiring manual review: " + pending);
        
        // Next Steps
        if (pending > 0) {
            System.out.println("\n💡 Next Steps:");
            System.out.println("   • Review pending learnings via GET /api/learning/pending");
            System.out.println("   • Apply specific learnings via POST /api/learning/apply/{id}");
            System.out.println("   • Reject learnings via POST /api/learning/reject/{id}");
        }
        
        System.out.println("\n════════════════════════════════════════════════\n");
    }
}