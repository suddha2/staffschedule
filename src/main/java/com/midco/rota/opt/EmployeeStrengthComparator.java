package com.midco.rota.opt;

import java.util.Comparator;
import com.midco.rota.model.Employee;
import com.midco.rota.util.ContractType;

public class EmployeeStrengthComparator implements Comparator<Employee> {
    
    @Override
    public int compare(Employee a, Employee b) {
        // 1. CONTRACT TYPE IS PARAMOUNT - compare first, independently
        int contractCompare = Integer.compare(
            contractPriority(b.getContractType()),
            contractPriority(a.getContractType())
        );
        if (contractCompare != 0) {
            return contractCompare; // Stop here if contract types differ
        }
        
        // 2. Within same contract type, prefer fewer restrictions
        int restrictionPenaltyA = restrictionPenalty(a);
        int restrictionPenaltyB = restrictionPenalty(b);
        int restrictionCompare = Integer.compare(restrictionPenaltyA, restrictionPenaltyB);
        if (restrictionCompare != 0) {
            return restrictionCompare; // Fewer restrictions = stronger
        }
        
        // 3. Within same contract type and restrictions, prefer more availability
        int availabilityScoreA = availabilityScore(a);
        int availabilityScoreB = availabilityScore(b);
        int availabilityCompare = Integer.compare(availabilityScoreB, availabilityScoreA);
        if (availabilityCompare != 0) {
            return availabilityCompare;
        }
        
        // 4. Prefer employees with more max hours (more flexible)
        int maxHoursCompare = a.getMaxHrs().compareTo(b.getMaxHrs());
        if (maxHoursCompare != 0) {
            return -maxHoursCompare; // Higher max hours = stronger
        }
        
        // 5. Stable ordering by ID
        return a.getId().compareTo(b.getId());
    }
    
    private int contractPriority(ContractType type) {
        return switch (type) {
            case PERMANENT -> 1000;      // Highest priority

            case ZERO_HOURS -> 10;       // Lowest priority
            default -> 1;
        };
    }
    
    private int availabilityScore(Employee emp) {
        int score = 0;
        if (emp.getPreferredService() != null)
            score += emp.getPreferredService().size() * 5;
        if (emp.getPreferredDay() != null)
            score += emp.getPreferredDay().size() * 3;
        if (emp.getPreferredShift() != null)
            score += emp.getPreferredShift().size() * 2;
        return score;
    }
    
    private int restrictionPenalty(Employee emp) {
        int penalty = 0;
        if (emp.getRestrictedService() != null)
            penalty += emp.getRestrictedService().size() * 5;
        if (emp.getRestrictedDay() != null)
            penalty += emp.getRestrictedDay().size() * 3;
        if (emp.getRestrictedShift() != null)
            penalty += emp.getRestrictedShift().size() * 2;
        return penalty;
    }
}