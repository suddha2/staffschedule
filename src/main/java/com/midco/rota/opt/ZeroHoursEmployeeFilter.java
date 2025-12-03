package com.midco.rota.opt;

import org.optaplanner.core.api.score.director.ScoreDirector;
import org.optaplanner.core.impl.heuristic.selector.common.decorator.SelectionFilter;

import com.midco.rota.model.Employee;
import com.midco.rota.model.Rota;
import com.midco.rota.model.ShiftAssignment;
import com.midco.rota.util.ContractType;

public class ZeroHoursEmployeeFilter implements SelectionFilter<Rota, ShiftAssignment> {
    @Override
    public boolean accept(ScoreDirector<Rota> scoreDirector, ShiftAssignment assignment) {
        // Only consider zero-hours employees in phase 2
        Employee employee = assignment.getEmployee();
        return employee != null && employee.getContractType() == ContractType.ZERO_HOURS;
    }
}
