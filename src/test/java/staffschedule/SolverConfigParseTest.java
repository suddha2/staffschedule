package staffschedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.optaplanner.core.config.solver.EnvironmentMode;
import org.optaplanner.core.config.solver.SolverConfig;

/**
 * Pins down what the solver configs actually do. Both run OptaPlanner's default phases
 * (custom phases were benchmarked on 2026-10-13 and weren't consistently better). If you
 * add custom phases on purpose, update this test after re-running that comparison.
 */
class SolverConfigParseTest {

	@Test
	void batchConfigUsesDefaultPhases() {
		SolverConfig cfg = SolverConfig.createFromXmlResource("solverConfig.xml");
		assertNull(cfg.getPhaseConfigList(), "no custom phases: OptaPlanner defaults");
		assertEquals(EnvironmentMode.REPRODUCIBLE, cfg.getEnvironmentMode());
		assertEquals(120L, cfg.getTerminationConfig().getSecondsSpentLimit());
		assertEquals(60L, cfg.getTerminationConfig().getUnimprovedSecondsSpentLimit());
	}

	@Test
	void liveConfigUsesDefaultPhases() {
		SolverConfig cfg = SolverConfig.createFromXmlResource("liveSolverConfig.xml");
		assertNull(cfg.getPhaseConfigList(), "no custom phases: OptaPlanner defaults");
		assertEquals(Boolean.TRUE, cfg.getDaemon());
	}
}
