package com.midco.rota.dto;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.midco.rota.model.ServiceLocation;

/**
 * One choice in a location dropdown. Each flat and the service above it are ordinary locations;
 * {@code service} names the building for a flat or a whole-service choice, and is null for a
 * service with no flats.
 */
public record LocationOption(String location, String service, boolean wholeService) {

	/**
	 * Services with no flats first, then each service with flats: its flats, then the whole
	 * service last, so flat level is the natural pick. A flat whose service is inactive stands
	 * on its own.
	 */
	public static List<LocationOption> from(List<ServiceLocation> rows) {
		Map<Long, ServiceLocation> byId = rows.stream().collect(Collectors.toMap(ServiceLocation::getId, r -> r));
		Map<Long, List<ServiceLocation>> flatsOf = rows.stream()
				.filter(r -> r.getParentId() != null && byId.containsKey(r.getParentId()))
				.collect(Collectors.groupingBy(ServiceLocation::getParentId));
		Set<Long> flatIds = flatsOf.values().stream().flatMap(List::stream).map(ServiceLocation::getId)
				.collect(Collectors.toSet());
		Comparator<ServiceLocation> byLocation = Comparator.comparing(LocationOption::locationOf);

		List<LocationOption> options = new ArrayList<>();
		rows.stream().filter(r -> !flatIds.contains(r.getId()) && !flatsOf.containsKey(r.getId())).sorted(byLocation)
				.forEach(r -> options.add(new LocationOption(locationOf(r), null, false)));
		rows.stream().filter(r -> flatsOf.containsKey(r.getId())).sorted(byLocation).forEach(parent -> {
			String service = locationOf(parent);
			flatsOf.get(parent.getId()).stream().sorted(byLocation)
					.forEach(flat -> options.add(new LocationOption(locationOf(flat), service, false)));
			options.add(new LocationOption(service, service, true));
		});
		return options;
	}

	/** The bridged staffrota string where known, else the canonical name. */
	private static String locationOf(ServiceLocation r) {
		return r.getStaffrotaLocation() != null ? r.getStaffrotaLocation() : r.getName();
	}
}
