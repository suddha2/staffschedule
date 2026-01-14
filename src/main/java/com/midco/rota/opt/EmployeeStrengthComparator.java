package com.midco.rota.opt;

import java.math.BigDecimal;
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
        // ✅ FIX: Handle null max hours
        BigDecimal maxA = a.getMaxHrs();
        BigDecimal maxB = b.getMaxHrs();
        
        if (maxA != null && maxB != null) {
            int maxHoursCompare = maxA.compareTo(maxB);
            if (maxHoursCompare != 0) {
                return -maxHoursCompare; // Higher max hours = stronger
            }
        } else if (maxA != null) {
            return 1; // a has max hours, b doesn't - a is stronger
        } else if (maxB != null) {
            return -1; // b has max hours, a doesn't - b is stronger
        }
        // Both null - continue to next tiebreaker

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
        if (emp.getPreferredDays() != null)
            score += emp.getPreferredDays().size() * 3;
        if (emp.getPreferredShifts() != null)
            score += emp.getPreferredShifts().size() * 2;
        return score;
    }
    
    private int restrictionPenalty(Employee emp) {
        int penalty = 0;
        if (emp.getRestrictedService() != null)
            penalty += emp.getRestrictedService().size() * 5;
        if (emp.getRestrictedDays() != null)
            penalty += emp.getRestrictedDays().size() * 3;
        if (emp.getRestrictedShifts() != null)
            penalty += emp.getRestrictedShifts().size() * 2;
        return penalty;
    }
}