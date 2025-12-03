package com.midco.rota;

import com.midco.rota.service.PeriodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

/**
 * Initialize period cache at application startup
 */
@Configuration
public class PeriodInitializer {
    
    @Autowired
    private PeriodService periodService;
    
    @PostConstruct
    public void init() {
        System.out.println("Loading pay cycle periods...");
        periodService.preloadCache();
        System.out.println("✓ Period cache initialized");
    }
}