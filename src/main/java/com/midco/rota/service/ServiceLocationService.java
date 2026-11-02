package com.midco.rota.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.midco.rota.dto.ServiceLocationRequest;
import com.midco.rota.model.Region;
import com.midco.rota.model.ServiceLocation;
import com.midco.rota.repository.RegionRepository;
import com.midco.rota.repository.ServiceLocationRepository;

/**
 * Adds and edits services, and links a flat to the service above it. The link only groups them
 * in dropdowns (V040), so changing it can't alter a rota, a carer's services or the solver. A
 * rota name is different: shift templates and carers' services refer to it as text, so it is set
 * once and not changed here. Rule breaks throw IllegalArgumentException with a message for the
 * user.
 */
@Service
public class ServiceLocationService {

	private final ServiceLocationRepository repo;
	private final RegionRepository regionRepo;

	public ServiceLocationService(ServiceLocationRepository repo, RegionRepository regionRepo) {
		this.repo = repo;
		this.regionRepo = regionRepo;
	}

	public List<ServiceLocation> list(String region) {
		return repo.findByStaffrotaRegionOrderByNameAsc(region);
	}

	public ServiceLocation create(ServiceLocationRequest req) {
		if (isBlank(req.region())) {
			throw new IllegalArgumentException("Choose a region");
		}
		List<ServiceLocation> rows = list(req.region());
		ServiceLocation s = new ServiceLocation();
		ServiceLocation parent = parentFor(s, req, rows);
		String location = checkedLocation(req.location(), s, rows);
		if (location == null) {
			throw new IllegalArgumentException("Enter a rota name");
		}
		if (parent != null) { // same People Planner region as the building
			s.setRegionCode(parent.getRegionCode());
			s.setRegionName(parent.getRegionName());
		} else {
			Region region = regionRepo.findFirstByStaffrotaRegionAndActiveTrueOrderByCodeAsc(req.region())
					.orElseThrow(() -> new IllegalArgumentException("Unknown region " + req.region()));
			s.setRegionCode(region.getCode());
			s.setRegionName(region.getName());
		}
		String reference = UUID.randomUUID().toString().substring(0, 8); // both ids are required; PP has none yet
		s.setExternalId("LOCAL-" + reference);
		s.setServiceId("L" + reference);
		s.setStaffrotaRegion(req.region());
		s.setStaffrotaLocation(location);
		s.setActive(req.active() == null || req.active());
		applyEditable(s, req, parent, rows);
		return repo.save(s);
	}

	public ServiceLocation update(long id, ServiceLocationRequest req) {
		ServiceLocation s = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Service not found"));
		List<ServiceLocation> rows = list(s.getStaffrotaRegion());
		ServiceLocation parent = parentFor(s, req, rows);

		if (isBlank(s.getStaffrotaLocation())) {
			s.setStaffrotaLocation(checkedLocation(req.location(), s, rows));
		} else if (!isBlank(req.location()) && !req.location().trim().equals(s.getStaffrotaLocation())) {
			throw new IllegalArgumentException("The rota name can't be changed here: shifts and carers' services refer to it");
		}
		if (Boolean.FALSE.equals(req.active()) && hasActiveFlats(s, rows)) {
			throw new IllegalArgumentException("Deactivate or move its flats first");
		}
		if (req.active() != null) {
			s.setActive(req.active());
		}
		applyEditable(s, req, parent, rows);
		return repo.save(s);
	}

	private void applyEditable(ServiceLocation s, ServiceLocationRequest req, ServiceLocation parent, List<ServiceLocation> rows) {
		if (isBlank(req.name())) {
			throw new IllegalArgumentException("Enter a service name");
		}
		String mobizioId = isBlank(req.mobizioId()) ? null : req.mobizioId().trim();
		if (mobizioId != null && rows.stream().anyMatch(r -> r != s && mobizioId.equals(r.getMobizioId()))) {
			throw new IllegalArgumentException("Another service already has People Planner ID " + mobizioId);
		}
		s.setName(req.name().trim());
		s.setMobizioId(mobizioId);
		s.setParentId(parent == null ? null : parent.getId());
	}

	/** The service to sit under, after the one-level, same-region rules; null for top level. */
	private ServiceLocation parentFor(ServiceLocation s, ServiceLocationRequest req, List<ServiceLocation> rows) {
		if (req.parentId() == null) {
			return null;
		}
		if (req.parentId().equals(s.getId())) {
			throw new IllegalArgumentException("A service can't sit under itself");
		}
		ServiceLocation parent = rows.stream().filter(r -> req.parentId().equals(r.getId())).findFirst()
				.orElseThrow(() -> new IllegalArgumentException("The service it sits under must be in the same region"));
		if (parent.getParentId() != null) {
			throw new IllegalArgumentException(nameOf(parent) + " is a flat, so it can't have flats under it");
		}
		if (!parent.isActive()) {
			throw new IllegalArgumentException(nameOf(parent) + " is inactive");
		}
		if (s.getId() != null && rows.stream().anyMatch(r -> s.getId().equals(r.getParentId()))) {
			throw new IllegalArgumentException("This service has flats, so it can't sit under another service");
		}
		return parent;
	}

	/** Trimmed rota name, or null if none given. Commas and colons would break carers' service lists. */
	private static String checkedLocation(String raw, ServiceLocation s, List<ServiceLocation> rows) {
		if (isBlank(raw)) {
			return null;
		}
		String location = raw.trim();
		if (location.contains(",") || location.contains(":")) {
			throw new IllegalArgumentException("A rota name can't contain a comma or a colon");
		}
		if (rows.stream().anyMatch(r -> r != s && location.equalsIgnoreCase(nameOf(r)))) {
			throw new IllegalArgumentException("This region already has a service called " + location);
		}
		return location;
	}

	private static boolean hasActiveFlats(ServiceLocation s, List<ServiceLocation> rows) {
		return rows.stream().anyMatch(r -> s.getId().equals(r.getParentId()) && r.isActive());
	}

	private static String nameOf(ServiceLocation r) {
		return r.getStaffrotaLocation() != null ? r.getStaffrotaLocation() : r.getName();
	}

	private static boolean isBlank(String v) {
		return v == null || v.isBlank();
	}
}
