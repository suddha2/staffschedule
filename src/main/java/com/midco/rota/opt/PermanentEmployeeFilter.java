package com.midco.rota.opt;

import org.optaplanner.core.api.score.director.ScoreDirector;
import org.optaplanner.core.impl.heuristic.selector.common.decorator.SelectionFilter;

import com.midco.rota.model.Employee;
import com.midco.rota.model.Rota;
import com.midco.rota.model.ShiftAssignment;
import com.midco.rota.util.ContractType;

public class PermanentEmployeeFilter implements SelectionFilter<Rota, ShiftAssignment> {
    @Override
    public boolean accept(ScoreDirector<Rota> scoreDirector, ShiftAssignment assignment) {
        // Only consider permanent employees in phase 1
        Employee employee = assignment.getEmployee();
        return employee != null && employee.getContractType() == ContractType.PERMANENT;
    }
}
