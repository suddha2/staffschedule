package com.midco.rota.opt;

import org.optaplanner.core.api.score.director.ScoreDirector;
import org.optaplanner.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.midco.rota.model.Rota;
import com.midco.rota.model.ShiftAssignment;
import com.midco.rota.util.ContractType;

/**
 * Filter that only allows changing assignments with ZERO_HOURS employees
 * or unassigned shifts. This PROTECTS permanent employee assignments
 * from being changed during optimization phases.
 * 
 * Used in Phases 3-5 (optimization) to ensure permanent employee
 * assignments from Phase 1 remain locked.
 */
public class ZeroHoursAssignmentFilter implements SelectionFilter<Rota, ShiftAssignment> {

    private static final Logger logger = LoggerFactory.getLogger(ZeroHoursAssignmentFilter.class);
    private static boolean firstCall = true;
    
    @Override
    public boolean accept(ScoreDirector<Rota> scoreDirector, ShiftAssignment assignment) {
        if (firstCall) {
            logger.info("===== ZeroHoursAssignmentFilter ACTIVE - Protecting permanent assignments =====");
            firstCall = false;
        }
        
        // Allow changes to unassigned shifts (employee == null)
        if (assignment.getEmployee() == null) {
            return true;
        }
        
        // Only allow changes to zero-hours assignments
        // This blocks permanent assignments from being changed
        boolean isZeroHours = assignment.getEmployee().getContractType() == ContractType.ZERO_HOURS;
        
        if (!isZeroHours) {
            logger.trace("Blocking change to PERMANENT employee assignment: {} at {}", 
                assignment.getEmployee().getName(),
                assignment.getShift().getShiftTemplate().getLocation());
        }
        
        return isZeroHours;
    }
}