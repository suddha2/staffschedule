package staffschedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.midco.rota.dto.ServiceLocationRequest;
import com.midco.rota.model.Region;
import com.midco.rota.model.ServiceLocation;
import com.midco.rota.repository.RegionRepository;
import com.midco.rota.repository.ServiceLocationRepository;
import com.midco.rota.service.ServiceLocationService;

/** Manage Services: the limits on linking flats to a service and on adding services. */
class ServiceLocationServiceTest {

	private final ServiceLocationRepository repo = mock(ServiceLocationRepository.class);
	private final RegionRepository regions = mock(RegionRepository.class);
	private final ServiceLocationService svc = new ServiceLocationService(repo, regions);
	private final List<ServiceLocation> barnet = new ArrayList<>();
	private ServiceLocation alexandra, flatA, victoria, kentHouse;

	private ServiceLocation row(long id, String location, Long parentId, String region) {
		ServiceLocation r = new ServiceLocation();
		ReflectionTestUtils.setField(r, "id", id);
		r.setName("Name of " + location);
		r.setStaffrotaLocation(location);
		r.setParentId(parentId);
		r.setStaffrotaRegion(region);
		r.setRegionCode("01");
		r.setRegionName("Barnet");
		r.setActive(true);
		return r;
	}

	@BeforeEach
	void setUp() {
		alexandra = row(1, "Alexandra Road", null, "BARNET");
		flatA = row(2, "Flat A", 1L, "BARNET");
		victoria = row(3, "61 VICTORIA", null, "BARNET");
		kentHouse = row(4, "BLACKTHORN A", null, "KENT");
		barnet.addAll(List.of(alexandra, flatA, victoria));
		when(repo.findByStaffrotaRegionOrderByNameAsc("BARNET")).thenReturn(barnet);
		when(repo.findByStaffrotaRegionOrderByNameAsc("KENT")).thenReturn(List.of(kentHouse));
		when(repo.findById(any())).thenAnswer(i -> barnet.stream().filter(r -> r.getId().equals(i.getArgument(0))).findFirst());
		when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
		Region region = new Region();
		region.setCode("01");
		region.setName("Barnet");
		when(regions.findFirstByStaffrotaRegionAndActiveTrueOrderByCodeAsc("BARNET")).thenReturn(Optional.of(region));
	}

	private static ServiceLocationRequest edit(ServiceLocation s, Long parentId, Boolean active) {
		return new ServiceLocationRequest(null, s.getName(), s.getStaffrotaLocation(), parentId, s.getMobizioId(), active);
	}

	private void assertRefused(String message, org.junit.jupiter.api.function.Executable call) {
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, call);
		assertTrue(e.getMessage().contains(message), e.getMessage());
	}

	@Test
	void linkingAndUnlinkingAFlatOnlySetsTheParent() {
		assertEquals(1L, svc.update(3, edit(victoria, 1L, true)).getParentId());
		assertNull(svc.update(3, edit(victoria, null, true)).getParentId());
		assertEquals("61 VICTORIA", victoria.getStaffrotaLocation());
	}

	@Test
	void aFlatCannotHaveFlatsUnderIt() {
		assertRefused("is a flat", () -> svc.update(3, edit(victoria, 2L, true)));
	}

	@Test
	void aServiceWithFlatsCannotSitUnderAnother() {
		assertRefused("has flats", () -> svc.update(1, edit(alexandra, 3L, true)));
	}

	@Test
	void aServiceCannotSitUnderItself() {
		assertRefused("under itself", () -> svc.update(3, edit(victoria, 3L, true)));
	}

	@Test
	void theParentMustBeInTheSameRegion() {
		assertRefused("same region", () -> svc.update(3, edit(victoria, 4L, true)));
	}

	@Test
	void anInactiveServiceCannotTakeFlats() {
		victoria.setActive(false);
		assertRefused("inactive", () -> svc.update(2, edit(flatA, 3L, true)));
	}

	@Test
	void aServiceWithActiveFlatsCannotBeDeactivated() {
		assertRefused("flats first", () -> svc.update(1, edit(alexandra, null, false)));
		flatA.setActive(false);
		assertEquals(false, svc.update(1, edit(alexandra, null, false)).isActive());
	}

	@Test
	void theRotaNameCannotBeChangedOnceSet() {
		assertRefused("can't be changed", () -> svc.update(3,
				new ServiceLocationRequest(null, "New", "61 VICTORIA ROAD", null, null, true)));
	}

	@Test
	void aMissingRotaNameCanBeFilledInOnce() {
		ServiceLocation golderton = row(5, null, null, "BARNET");
		barnet.add(golderton);
		assertEquals("2 GOLDERTON", svc.update(5,
				new ServiceLocationRequest(null, "2 Golderton Flat", "2 GOLDERTON", null, null, true)).getStaffrotaLocation());
	}

	@Test
	void addingAFlatTakesTheRegionFromItsServiceAndMakesItsOwnReference() {
		ServiceLocation flat = svc.create(new ServiceLocationRequest("BARNET", "Flat C", "Alexandra Flat C", 1L, null, null));
		assertEquals(1L, flat.getParentId());
		assertEquals("01", flat.getRegionCode());
		assertEquals("BARNET", flat.getStaffrotaRegion());
		assertTrue(flat.getExternalId().startsWith("LOCAL-"));
		assertTrue(flat.isActive());
	}

	@Test
	void aRotaNameMustBeUniqueInTheRegionAndUsableInServiceLists() {
		assertRefused("already has", () -> svc.create(new ServiceLocationRequest("BARNET", "X", "61 victoria", null, null, true)));
		assertRefused("comma", () -> svc.create(new ServiceLocationRequest("BARNET", "X", "DOM CARE RUNS 1,", null, null, true)));
		assertRefused("comma", () -> svc.create(new ServiceLocationRequest("BARNET", "X", "HOUSE: 1", null, null, true)));
		assertRefused("rota name", () -> svc.create(new ServiceLocationRequest("BARNET", "X", " ", null, null, true)));
	}

	@Test
	void aPeoplePlannerIdCanOnlyBeUsedOnce() {
		flatA.setMobizioId("2-1784");
		assertRefused("already has People Planner ID", () -> svc.update(3,
				new ServiceLocationRequest(null, "61 Victoria Road", "61 VICTORIA", null, "2-1784", true)));
	}
}
