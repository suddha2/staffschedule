package com.midco.rota;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.midco.rota.service.LearningOrchestrator;

/**
 * Scheduled job to run learning cycle automatically every month
 */
@Component
public class LearningScheduler {
    
    @Autowired
    private LearningOrchestrator orchestrator;
    
    /**
     * Run learning cycle on the 1st of every month at 2 AM
     * Cron: "0 0 2 1 * ?" = second minute hour day month dayOfWeek
     * 
     * To enable this scheduler, add @EnableScheduling to your main application class:
     * @SpringBootApplication
     * @EnableScheduling
     * public class RotaApplication { ... }
     */
    @Scheduled(cron = "0 0 2 1 * ?")
    public void runMonthlyLearningCycle() {
        System.out.println("\n🤖 SCHEDULED: Running monthly learning cycle...");
        
        try {
            orchestrator.runMonthlyLearningCycle();
            System.out.println("✅ Scheduled learning cycle completed successfully\n");
            
            // TODO: Send email notification with summary
            // sendEmailNotification();
            
        } catch (Exception e) {
            System.err.println("❌ Error in scheduled learning cycle: " + e.getMessage());
            e.printStackTrace();
            
            // TODO: Send error notification
            // sendErrorNotification(e);
        }
    }
    
    /**
     * Optional: Run weekly analysis (for faster feedback during initial months)
     * Runs every Monday at 3 AM
     * Comment out if not needed
     */
    // @Scheduled(cron = "0 0 3 * * MON")
    public void runWeeklyLearningCycle() {
        System.out.println("\n🤖 SCHEDULED: Running weekly learning cycle...");
        
        try {
            orchestrator.runMonthlyLearningCycle(); // Uses last month's data
            System.out.println("✅ Scheduled weekly learning cycle completed successfully\n");
        } catch (Exception e) {
            System.err.println("❌ Error in weekly learning cycle: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * TODO: Send email notification with learning summary
     */
    private void sendEmailNotification() {
        // Implement email notification
        // Could include:
        // - OptaPlanner allocation rate
        // - Number of patterns discovered
        // - Number of learnings auto-applied
        // - Number of learnings pending review
        // - Link to review pending learnings
    }
    
    /**
     * TODO: Send error notification
     */
    private void sendErrorNotification(Exception e) {
        // Implement error notification
    }
}