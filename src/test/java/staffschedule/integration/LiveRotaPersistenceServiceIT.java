package staffschedule.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.midco.rota.model.Employee;
import com.midco.rota.model.Rota;
import com.midco.rota.model.Shift;
import com.midco.rota.model.ShiftAssignment;
import com.midco.rota.model.ShiftTemplate;
import com.midco.rota.service.LiveRotaPersistenceService;
import com.midco.rota.util.ShiftType;

/**
 * Integration test for {@link LiveRotaPersistenceService} against a real
 * PostgreSQL (Testcontainers). A JPA slice ({@code @DataJpaTest}) is used so the
 * Firebase / PASETO / web layers don't need to boot; the schema is generated from
 * the entities (create-drop) since the app runs ddl-auto=none against a
 * pre-provisioned DB and has no Flyway.
 *
 * <p>Skipped automatically when no Docker daemon is available, so the normal
 * build stays green. To run: start Docker, then
 * {@code mvn -Dtest=LiveRotaPersistenceServiceIT test}.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@Import(LiveRotaPersistenceService.class)
@TestPropertySource(properties = {
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.flyway.enabled=false"
})
class LiveRotaPersistenceServiceIT {

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

	@Autowired
	private TestEntityManager em;

	@Autowired
	private LiveRotaPersistenceService persistenceService;

	private Long rotaId;
	private Long assignmentId;
	private Integer aliceId;
	private Integer bobId;

	@BeforeEach
	void seed() {
		ShiftTemplate template = new ShiftTemplate();
		template.setLocation("9 CLAYDON");
		template.setShiftType(ShiftType.DAY);
		template.setDayOfWeek(DayOfWeek.MONDAY);
		template.setStartTime(LocalTime.of(8, 0));
		template.setEndTime(LocalTime.of(20, 0));
		em.persist(template);

		Employee alice = employee("Alice", "Adams");
		Employee bob = employee("Bob", "Brown");
		em.persist(alice);
		em.persist(bob);

		Shift shift = new Shift(LocalDate.of(2026, 1, 5), template, 1);
		em.persist(shift);

		ShiftAssignment sa = new ShiftAssignment(shift);
		sa.setEmployee(alice);

		Rota rota = new Rota(new ArrayList<>(List.of(alice, bob)), new ArrayList<>(List.of(sa)));
		em.persist(rota);
		em.flush();

		rotaId = rota.getId();
		assignmentId = sa.getId();
		aliceId = alice.getId();
		bobId = bob.getId();
		em.clear();
	}

	@Test
	void loadFullRotaAnchorsPlanningIdAndLoadsGraph() {
		Rota rota = persistenceService.loadFullRota(rotaId);

		assertEquals(2, rota.getEmployeeList().size(), "value range = both employees");
		assertEquals(1, rota.getShiftAssignmentList().size());
		ShiftAssignment sa = rota.getShiftAssignmentList().get(0);
		assertEquals(String.valueOf(assignmentId), sa.getPlanningId(), "planningId anchored to DB id");
		assertEquals(aliceId, sa.getEmployee().getId());
	}

	@Test
	void applySnapshotPersistsChangedEmployeeAndCountsIt() {
		Rota best = bestWith(assignmentId, bobId);

		int changed = persistenceService.applySnapshot(rotaId, best);

		assertEquals(1, changed);
		em.clear();
		Rota reloaded = em.find(Rota.class, rotaId);
		assertEquals(bobId, reloaded.getShiftAssignmentList().get(0).getEmployee().getId());
	}

	@Test
	void applySnapshotIsNoOpWhenEmployeeUnchanged() {
		Rota best = bestWith(assignmentId, aliceId);

		assertEquals(0, persistenceService.applySnapshot(rotaId, best));
	}

	private Employee employee(String first, String last) {
		Employee e = new Employee();
		e.setFirstName(first);
		e.setLastName(last);
		return e;
	}

	/** A minimal "best solution" carrying one assignment (by id) -> employee (by id). */
	private Rota bestWith(Long assignmentDbId, Integer employeeDbId) {
		ShiftAssignment bestSa = new ShiftAssignment();
		bestSa.setId(assignmentDbId);
		Employee ref = new Employee();
		ref.setId(employeeDbId);
		bestSa.setEmployee(ref);
		Rota best = new Rota();
		best.setShiftAssignmentList(List.of(bestSa));
		return best;
	}
}
