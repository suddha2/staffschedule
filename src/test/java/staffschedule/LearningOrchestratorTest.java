package staffschedule;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import com.midco.rota.RotaServiceApplication;
import com.midco.rota.service.LearningOrchestrator;

@SpringBootTest(classes = RotaServiceApplication.class)
@TestPropertySource(locations = "classpath:application-dev.properties")
class LearningOrchestratorTest {

	@Autowired
	private LearningOrchestrator learningOrchestrator;

	@Test
	void testRunLearningCycle() {

		LocalDate stDate = LocalDate.parse("18/08/2025", DateTimeFormatter.ofPattern("dd/MM/yyyy"));
		LocalDate edDate = LocalDate.parse("01/09/2025", DateTimeFormatter.ofPattern("dd/MM/yyyy"));
		learningOrchestrator.runLearningCycle(stDate, edDate);
	}

}
