package com.midco.rota.opt;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;

import org.optaplanner.core.impl.heuristic.move.Move;
import org.optaplanner.core.impl.heuristic.selector.move.factory.MoveListFactory;

import com.midco.rota.model.Employee;
import com.midco.rota.model.Rota;
import com.midco.rota.model.ShiftAssignment;
import com.midco.rota.util.ContractType;
import com.midco.rota.util.ShiftType;

public class FallbackSwapMoveFactory implements MoveListFactory<Rota> {

    @Override
    public List<Move<Rota>> createMoveList(Rota rota) {
        List<Move<Rota>> moves = new ArrayList<>();

        for (ShiftAssignment sa : rota.getShiftAssignmentList()) {
            Employee current = sa.getEmployee();
            if (current == null || current.getContractType() != ContractType.ZERO_HOURS) continue;

            for (Employee candidate : rota.getEmployeeList()) {
                if (candidate.getContractType() == ContractType.PERMANENT && isEligible(candidate, sa)) {
                    moves.add(new SwapEmployeeMove(sa, current, candidate));
                }
            }
        }

        return moves;
    }

    private boolean isEligible(Employee emp, ShiftAssignment sa) {
        ShiftType type = sa.getShift().getShiftTemplate().getShiftType();
        String location = sa.getShift().getShiftTemplate().getLocation();
        DayOfWeek day = sa.getShift().getShiftTemplate().getDay();

        return !(emp.getRestrictedShift() != null && emp.getRestrictedShift().contains(type)) &&
               !(emp.getRestrictedService() != null && emp.getRestrictedService().contains(location)) &&
               !(emp.getRestrictedDay() != null && emp.getRestrictedDay().contains(day));
    }
}

