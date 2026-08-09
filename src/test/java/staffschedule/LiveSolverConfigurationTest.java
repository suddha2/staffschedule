package staffschedule;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.optaplanner.core.api.solver.SolverManager;

import com.midco.rota.LiveSolverConfiguration;
import com.midco.rota.model.Rota;

/**
 * Pure test that the live/daemon solver config (liveSolverConfig.xml) parses and
 * a {@link SolverManager} builds from it — no Spring context, no Docker. Guards
 * against XML typos (bad class names, invalid daemon/termination config) in the
 * separate live config without needing to boot the app.
 */
class LiveSolverConfigurationTest {

	@Test
	void buildsLiveSolverManagerFromXml() {
		SolverManager<Rota, Long> liveSolverManager = new LiveSolverConfiguration().liveSolverManager();
		assertNotNull(liveSolverManager, "live SolverManager should build from liveSolverConfig.xml");
		liveSolverManager.close();
	}
}
