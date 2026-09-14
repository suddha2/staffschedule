package com.midco.rota.util;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.midco.rota.model.EmployeeAvailability;

/**
 * Pre-solve builder for the employee unavailability map.
 *
 * <p>Turns {@link EmployeeAvailability} spans into a flat
 * {@code employeeId -> set of unavailable dates}, clipped to the solve window so
 * only relevant days are held. The solver then answers "is this employee
 * unavailable on this date?" with an O(1) set lookup (see
 * {@code Employee.isUnavailableOn}) rather than joining availability facts in
 * the constraint stream.
 */
public final class AvailabilityCalendar {

	private AvailabilityCalendar() {
	}

	/**
	 * Expand the given spans into a per-employee set of unavailable dates,
	 * restricted to [windowStart, windowEnd] inclusive. Spans outside the window,
	 * or with missing fields, are ignored.
	 */
	public static Map<Integer, Set<LocalDate>> build(List<EmployeeAvailability> spans,
			LocalDate windowStart, LocalDate windowEnd) {
		Map<Integer, Set<LocalDate>> map = new HashMap<>();
		if (spans == null || windowStart == null || windowEnd == null) {
			return map;
		}
		for (EmployeeAvailability span : spans) {
			if (span == null || span.getEmployeeId() == null
					|| span.getStartDate() == null || span.getEndDate() == null) {
				continue;
			}
			// Clip the span to the window.
			LocalDate from = span.getStartDate().isBefore(windowStart) ? windowStart : span.getStartDate();
			LocalDate to = span.getEndDate().isAfter(windowEnd) ? windowEnd : span.getEndDate();
			if (from.isAfter(to)) {
				continue; // span does not overlap the window
			}
			Set<LocalDate> dates = map.computeIfAbsent(span.getEmployeeId(), k -> new HashSet<>());
			for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
				dates.add(d);
			}
		}
		return map;
	}
}
