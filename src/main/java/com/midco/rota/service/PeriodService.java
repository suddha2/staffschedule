package com.midco.rota.service;

import com.midco.rota.model.Period;
import com.midco.rota.repository.PeriodRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing government-mandated 4-week periods
 * 
 * This service:
 * - Loads periods from database
 * - Caches them for fast lookup
 * - Provides week number calculations
 */
@Service
public class PeriodService {
    
    @Autowired
    private PeriodRepository periodRepository;
    
    
    private LocalDate referenceDate;
    
    // Cache: date -> period mapping
    private Map<LocalDate, Period> periodCache = new HashMap<>();
    
    /**
     * Find which period a date falls into
     */
    @Cacheable("periods")
    public Optional<Period> findPeriodForDate(LocalDate date) {
        // Check cache first
        if (periodCache.containsKey(date)) {
            return Optional.of(periodCache.get(date));
        }
        
        // Query database
        Optional<Period> period = periodRepository.findByDate(date);
        
        // Cache result
        period.ifPresent(p -> periodCache.put(date, p));
        
        return period;
    }
    
    
    private LocalDate getReferenceDate() {
        if (referenceDate == null) {
            // ✅ Query database for earliest period start
            referenceDate = periodRepository.findEarliestStartDate()
                .orElseThrow(() -> new IllegalStateException(
                    "No periods found in database. Please add periods first."
                ));
        }
        return referenceDate;
    }
    
    public int getAbsoluteWeekNumber(LocalDate date) {
        LocalDate reference = getReferenceDate();
        
        if (date.isBefore(reference)) {
            // Date before first period (shouldn't happen normally)
            return 1;
        }
        
        // Calculate days since reference
        long daysSinceReference = ChronoUnit.DAYS.between(reference, date);
        
        // Convert to weeks (integer division)
        int weeksSinceReference = (int) (daysSinceReference / 7);
        
        // Return 1-based week number
        return weeksSinceReference + 1;
    }
    
    public int getWeekWithinPeriod(LocalDate date) {
    	Optional<Period> period = periodRepository.findByDate(date);
        
        if (period == null) {
            return 1; // Default to week 1 if period not found
        }
        
        long daysSinceStart = ChronoUnit.DAYS.between(period.get().getStartDate(), date);
        return (int) (daysSinceStart / 7) + 1;
    }
    public LocalDate getWeekStart(LocalDate date) {
        LocalDate reference = getReferenceDate();
        long daysSinceReference = ChronoUnit.DAYS.between(reference, date);
        int weeksComplete = (int) (daysSinceReference / 7);
        return reference.plusWeeks(weeksComplete);
    }	
    
    public Period getPeriod(LocalDate date) {
    	Optional<Period> period = periodRepository.findByDate(date);
    	if(period==null)
    		return null;
        return period.get();
    }
    
    /**
     * Calculate which week (1-4) a date falls into
     * 
     * @param date The date to check
     * @return Week number (1-4), or 0 if date is not in any period
     */
    public int calculateWeekNumber(LocalDate date) {
        Optional<Period> period = findPeriodForDate(date);
        
        if (period.isEmpty()) {
            // Date not in any period - log warning
            System.err.println("WARNING: Date " + date + " is not in any configured period");
            return 0;
        }
        
        return period.get().getWeekNumber(date);
    }
    
    /**
     * Get all active periods (for display/reporting)
     */
    public List<Period> getAllActivePeriods() {
        return periodRepository.findAllActive();
    }
    
    /**
     * Clear cache (call if periods are updated)
     */
    public void clearCache() {
        periodCache.clear();
        this.referenceDate = null;
    }
    
    /**
     * Pre-load all periods into cache (call at startup)
     */
    public void preloadCache() {
        List<Period> periods = periodRepository.findAllActive();
        
        for (Period period : periods) {
            // Cache every date in the period
            LocalDate date = period.getStartDate();
            while (!date.isAfter(period.getEndDate())) {
                periodCache.put(date, period);
                date = date.plusDays(1);
            }
        }
        
        System.out.println("✓ Preloaded " + periods.size() + " periods covering " + 
            periodCache.size() + " days");
    }
}