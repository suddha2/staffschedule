package com.midco.rota;

import org.springframework.stereotype.Component;

import com.midco.rota.model.Employee;
import com.midco.rota.model.Shift;
import com.midco.rota.service.PeriodService;

import jakarta.annotation.PostConstruct;

@Component
public class ModelInitializer {
    
    private final PeriodService periodService;
    
    public ModelInitializer(PeriodService periodService) {
        this.periodService = periodService;
    }
    
    @PostConstruct
    public void init() {
        Shift.setPeriodService(periodService);
        Employee.setPeriodService(periodService);
    }
}
