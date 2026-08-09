package com.midco.rota;

import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.api.solver.SolverManager;
import org.optaplanner.core.config.solver.SolverConfig;
import org.optaplanner.core.config.solver.SolverManagerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.midco.rota.model.Rota;

/**
 * Builds the second, daemon-mode {@link SolverManager} used for continuous /
 * live real-time planning (P2), separate from the batch {@code solverManager}
 * that the OptaPlanner Spring Boot starter auto-configures from
 * {@code solverConfig.xml}.
 *
 * <p>The bean is named {@code liveSolverManager}; inject it with
 * {@code @Qualifier("liveSolverManager")}. Existing batch injections keep
 * resolving to the auto-configured bean (named {@code solverManager}) by
 * parameter name, so this addition does not disturb the batch path.
 */
@Configuration
public class LiveSolverConfiguration {

	/**
	 * Cap on how many live rotas may solve concurrently. Each live session holds
	 * a daemon solver busy on a thread, so this bounds CPU/heap. Idle sessions are
	 * evicted by LiveSolverSessionService to free slots.
	 */
	private static final String PARALLEL_LIVE_SOLVER_COUNT = "4";

	@Bean(name = "liveSolverManager")
	public SolverManager<Rota, Long> liveSolverManager() {
		SolverConfig solverConfig = SolverConfig.createFromXmlResource("liveSolverConfig.xml");
		SolverFactory<Rota> solverFactory = SolverFactory.create(solverConfig);
		SolverManagerConfig managerConfig = new SolverManagerConfig()
				.withParallelSolverCount(PARALLEL_LIVE_SOLVER_COUNT);
		return SolverManager.create(solverFactory, managerConfig);
	}
}
