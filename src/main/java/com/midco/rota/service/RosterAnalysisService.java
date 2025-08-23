package com.midco.rota.service;

import java.util.Comparator;
import java.util.Map;

import org.optaplanner.core.api.score.ScoreExplanation;
import org.optaplanner.core.api.score.ScoreManager;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.constraint.ConstraintMatch;
import org.optaplanner.core.api.score.constraint.Indictment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.midco.rota.model.Rota;

@Service
public class RosterAnalysisService {

	private final ScoreManager<Rota, HardSoftScore> scoreManager;

	public RosterAnalysisService(ScoreManager<Rota, HardSoftScore> scoreManager) {
		this.scoreManager = scoreManager;
	}
	private static final Logger logger = LoggerFactory.getLogger(RosterAnalysisService.class);

	public void printHighImpactViolations(Rota solution) {
		ScoreExplanation<Rota, HardSoftScore> explanation = scoreManager.explain(solution);
		Map<Object, Indictment<HardSoftScore>> indictmentMap = explanation.getIndictmentMap();

		solution.getShiftAssignmentList().forEach(System.out::println);

		logger.info("=== High-Impact Violations Report ===");

		indictmentMap.entrySet().stream().sorted(Comparator.comparing(entry -> entry.getValue().getScore()))
				.filter(entry -> {
					HardSoftScore score = entry.getValue().getScore();
					return score.getHardScore() < 0 || score.getSoftScore() < -10;
				}).forEach(entry -> {
					Object entity = entry.getKey();
					Indictment<HardSoftScore> indictment = entry.getValue();
					HardSoftScore score = indictment.getScore();

					logger.info("🔴 Entity: " + entity);
					logger.info("    Total Impact: " + score);

					indictment.getConstraintMatchSet().stream().sorted(Comparator.comparing(ConstraintMatch::getScore))
							.limit(3).forEach(match -> {
								logger.info("    ⚠️ Constraint: " + match.getConstraintName());
								logger.info("       Impact: " + match.getScore());
								logger.info("       Justification: " + match.getJustificationList());
							});

					System.out.println();
				});
	}
}
