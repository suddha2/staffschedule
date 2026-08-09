package staffschedule.support;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.midco.rota.model.Employee;
import com.midco.rota.model.Rota;
import com.midco.rota.model.Shift;
import com.midco.rota.model.ShiftAssignment;
import com.midco.rota.model.ShiftTemplate;
import com.midco.rota.util.ShiftType;

/**
 * Shared, no-Spring, no-DB builders for {@link Rota} planning fixtures. Central
 * place to construct valid Employee / ShiftTemplate / Shift / ShiftAssignment /
 * Rota graphs for unit tests (SLEEP_IN pairing, live ProblemChanges, etc.).
 *
 * <p>Assignments are built the way {@code LiveRotaPersistenceService.loadFullRota}
 * prepares them for the live solver: a real DB id plus a {@code @PlanningId}
 * anchored to that id, so ProblemChange lookups-by-id work in tests.
 */
public final class RotaTestFixtures {

	/** A convenient Monday, so DayOfWeek.MONDAY templates line up. */
	public static final LocalDate MONDAY = LocalDate.of(2026, 1, 5);

	private RotaTestFixtures() {
	}

	public static Employee employee(int id, String first, String last) {
		Employee e = new Employee();
		e.setId(id);
		e.setFirstName(first);
		e.setLastName(last);
		return e;
	}

	public static ShiftTemplate template(String location, ShiftType type) {
		ShiftTemplate t = new ShiftTemplate();
		t.setLocation(location);
		t.setShiftType(type);
		t.setDayOfWeek(DayOfWeek.MONDAY);
		t.setStartTime(LocalTime.of(8, 0));
		t.setEndTime(LocalTime.of(20, 0));
		return t;
	}

	public static ShiftAssignment assignment(long id, String location, ShiftType type) {
		return assignment(id, location, type, MONDAY);
	}

	public static ShiftAssignment assignment(long id, String location, ShiftType type, LocalDate date) {
		ShiftAssignment sa = new ShiftAssignment(new Shift(date, template(location, type), 1));
		sa.setId(id);
		sa.setPlanningId(String.valueOf(id)); // mirror loadFullRota
		return sa;
	}

	public static Rota rota(List<Employee> employees, List<ShiftAssignment> assignments) {
		// Rota's constructor divides by employee count, so never hand it an empty list.
		List<Employee> emps = employees.isEmpty()
				? new ArrayList<>(List.of(employee(1, "Seed", "Employee")))
				: new ArrayList<>(employees);
		Rota rota = new Rota(emps, new ArrayList<>(assignments));
		rota.setPlanningId(1L); // Rota.equals/hashCode key on planningId
		return rota;
	}

	public static Rota rota(List<Employee> employees, ShiftAssignment... assignments) {
		return rota(employees, Arrays.asList(assignments));
	}
}
