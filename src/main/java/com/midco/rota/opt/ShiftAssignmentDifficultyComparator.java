package com.midco.rota.opt;

import com.midco.rota.model.ShiftAssignment;
import com.midco.rota.model.ShiftTemplate;
import java.util.Comparator;

/**
 * Determines which ShiftAssignments should be solved first during construction heuristic.
 * Higher difficulty = solved first.
 * Priority 1 locations = highest difficulty = solved first.
 */
public class ShiftAssignmentDifficultyComparator implements Comparator<ShiftAssignment> {
    
    @Override
    public int compare(ShiftAssignment a, ShiftAssignment b) {
        // Primary: Compare by priority (lower priority number = higher difficulty = solve first)
        Integer priorityA = getPriority(a);
        Integer priorityB = getPriority(b);
        
        int priorityComparison = Integer.compare(priorityA, priorityB);
        if (priorityComparison != 0) {
            return priorityComparison;  // Priority 1 < Priority 5, so Priority 1 is solved first
        }
        
        // Secondary: If same priority, solve shifts with gender requirements first
        // (they're more constrained, so harder to assign)
        boolean aHasGender = a.getShift().getShiftTemplate().getRequiredGender() != null;
        boolean bHasGender = b.getShift().getShiftTemplate().getRequiredGender() != null;
        
        if (aHasGender && !bHasGender) return -1;  // a is harder
        if (!aHasGender && bHasGender) return 1;   // b is harder
        
        // Tertiary: Solve earlier dates first (helps with continuity planning)
        int dateComparison = a.getShift().getShiftStart().compareTo(b.getShift().getShiftStart());
        if (dateComparison != 0) {
            return dateComparison;
        }
        
        // Quaternary: Solve by shift ID for consistency
        return Long.compare(a.getShift().getId(), b.getShift().getId());
    }
    
    private Integer getPriority(ShiftAssignment sa) {
        ShiftTemplate st = sa.getShift().getShiftTemplate();
        Integer priority = st.getPriority();
        
        // Default to middle priority if null
        if (priority == null || priority <= 0) {
            return 5;
        }
        
//        // Cap at 10 to keep reasonable range
//        if (priority > 10) {
//            return 10;
//        }
        
        return priority;
    }
}