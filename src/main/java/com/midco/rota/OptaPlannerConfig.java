package com.midco.rota;

import org.optaplanner.core.api.score.ScoreManager;
import org.optaplanner.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.api.solver.SolverManager;
import org.optaplanner.core.config.solver.SolverConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.midco.rota.model.Rota;

/**
 * Explicit OptaPlanner beans for the batch solve path.
 *
 * <p>OptaPlanner 10's Spring Boot starter auto-configuration does not reliably
 * register the {@code SolverConfig} / {@code SolverManager} / {@code ScoreManager}
 * beans in this project (the context failed to start with "No qualifying bean of
 * type SolverConfig/ScoreManager"), and its benchmark auto-config additionally
 * demands a {@code SolverConfig} bean. Both auto-configs are excluded on
 * {@link RotaServiceApplication}; we build the beans here directly from
 * {@code solverConfig.xml} — the same pattern used for the live solver in
 * {@link LiveSolverConfiguration}.
 *
 * <p>The batch manager keeps the bean name {@code solverManager} so existing
 * injections resolve to it; the live one stays {@code liveSolverManager}.
 */
@Configuration
public class OptaPlannerConfig {

	private static SolverFactory<Rota> batchSolverFactory() {
		return SolverFactory.create(SolverConfig.createFromXmlResource("solverConfig.xml"));
	}

	@Bean
	public SolverManager<Rota, Long> solverManager() {
		return SolverManager.create(batchSolverFactory());
	}

	@Bean
	public ScoreManager<Rota, HardSoftLongScore> scoreManager() {
		return ScoreManager.create(batchSolverFactory());
	}
}
