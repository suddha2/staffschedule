package com.midco.rota;

import java.util.List;

import org.optaplanner.core.api.score.ScoreExplanation;
import org.optaplanner.core.api.score.ScoreManager;
import org.optaplanner.core.api.score.constraint.ConstraintMatchTotal;
import org.springframework.stereotype.Service;

import com.midco.rota.model.Rota;

@Service
public class ConstraintExplanationService {

	
	private final ScoreManager<Rota, ?> scoreManager;

    public ConstraintExplanationService(ScoreManager<Rota, ?> scoreManager) {
        this.scoreManager = scoreManager;
    }

    
    public List<ConstraintMatchTotal<?>> getConstraintViolations(Rota rota) {
        ScoreExplanation<Rota, ?> explanation = scoreManager.explainScore(rota);
        return List.copyOf(explanation.getConstraintMatchTotalMap().values());
    }
}
