package staffschedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.midco.rota.dto.LocationOption;
import com.midco.rota.model.Employee;
import com.midco.rota.model.ServiceLocation;
import com.midco.rota.model.ShiftTemplate;
import com.midco.rota.opt.RotaConstraintProvider;

/** A service with flats under it: how dropdowns group them, and who may work which level. */
class ServiceAndFlatsTest {

	private static ServiceLocation row(long id, String location, Long parentId) {
		ServiceLocation r = new ServiceLocation();
		ReflectionTestUtils.setField(r, "id", id);
		ReflectionTestUtils.setField(r, "name", "Name " + location);
		ReflectionTestUtils.setField(r, "staffrotaLocation", location);
		ReflectionTestUtils.setField(r, "parentId", parentId);
		return r;
	}

	private static ShiftTemplate at(String location) {
		ShiftTemplate t = new ShiftTemplate();
		t.setLocation(location);
		return t;
	}

	private static Employee listing(String... services) {
		Employee e = new Employee();
		e.setPreferredService(List.of(services));
		return e;
	}

	@Test
	void singleServicesComeFirstThenFlatsThenTheirWholeService() {
		List<LocationOption> options = LocationOption.from(List.of(
				row(1, "Alexandra Road", null), row(2, "Flat B", 1L), row(3, "Flat A", 1L), row(4, "61 VICTORIA", null)));
		assertEquals(List.of(new LocationOption("61 VICTORIA", null, false),
				new LocationOption("Flat A", "Alexandra Road", false),
				new LocationOption("Flat B", "Alexandra Road", false),
				new LocationOption("Alexandra Road", "Alexandra Road", true)), options);
	}

	@Test
	void aFlatWhoseServiceIsInactiveStandsOnItsOwn() {
		assertEquals(List.of(new LocationOption("Flat A", null, false)),
				LocationOption.from(List.of(row(3, "Flat A", 1L))));
	}

	@Test
	void fallsBackToTheNameWhenNotBridged() {
		ServiceLocation r = row(1, null, null);
		assertEquals("Name null", LocationOption.from(List.of(r)).get(0).location());
	}

	@Test
	void listingTheServiceCoversOnlyWholeServiceShifts() {
		Employee carer = listing("Alexandra Road:60");
		assertFalse(RotaConstraintProvider.isOutsideServices(carer, at("Alexandra Road")));
		assertTrue(RotaConstraintProvider.isOutsideServices(carer, at("Alexandra Flat A - KC")),
				"listing the service must not cover its flats");
	}

	@Test
	void listingAFlatCoversOnlyThatFlat() {
		Employee carer = listing("Alexandra Flat A - KC:60");
		assertFalse(RotaConstraintProvider.isOutsideServices(carer, at("Alexandra Flat A - KC")));
		assertTrue(RotaConstraintProvider.isOutsideServices(carer, at("Alexandra Flat B - LC")));
		assertTrue(RotaConstraintProvider.isOutsideServices(carer, at("Alexandra Road")),
				"listing a flat must not cover whole-service shifts");
	}
}
