package com.midco.rota;

import java.util.List;

import org.optaplanner.core.api.score.constraint.ConstraintMatchTotal;

import com.midco.rota.model.Rota;

public class RotaUpdatePayload {
    private Rota rota;
    private List<ConstraintMatchTotal<?>> violations;
    private String status;
    
    public Rota getRota() {
        return rota;
    }

    public void setRota(Rota rota) {
        this.rota = rota;
    }
    
    public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public List<ConstraintMatchTotal<?>> getViolations() {
        return violations;
    }

    public void setViolations(List<ConstraintMatchTotal<?>> violations) {
        this.violations = violations;
    }
}