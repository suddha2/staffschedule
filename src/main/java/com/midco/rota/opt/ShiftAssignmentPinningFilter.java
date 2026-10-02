package com.midco.rota.opt;

import org.optaplanner.core.api.domain.entity.PinningFilter;

import com.midco.rota.model.Rota;
import com.midco.rota.model.WorkShiftAssignment;

/**
 * Pins a work assignment during the solve when it carries a genuine (persisted)
 * pin or the transient CONTINUITY seed-lock. A pinned entity's employee is fixed —
 * the solver won't change it.
 *
 * <p>Replaces {@code @PlanningPin} on the abstract {@code ShiftAssignment} base,
 * which stopped being honoured once the entity was split into the concrete
 * {@code WorkShiftAssignment}/{@code FollowerShiftAssignment} subclasses — pins
 * (and the seed-lock) were silently ignored. A pinningFilter on the concrete
 * entity is applied reliably. FLOATING is deliberately NOT pinned here (it stays
 * solver-assignable, filled last), unlike the old {@code isPinned()} flag.
 */
public class ShiftAssignmentPinningFilter implements PinningFilter<Rota, WorkShiftAssignment> {

    @Override
    public boolean accept(Rota rota, WorkShiftAssignment assignment) {
        return assignment.isSolverPinned();
    }
}
