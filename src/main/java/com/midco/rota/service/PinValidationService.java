package com.midco.rota.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.midco.rota.dto.ConflictError;
import com.midco.rota.dto.ConflictingShiftDTO;
import com.midco.rota.model.Employee;
import com.midco.rota.model.ShiftAssignment;
import com.midco.rota.repository.EmployeeRepository;

@Service
public class PinValidationService {

	private final EmployeeRepository employeeRepository;

	public PinValidationService(EmployeeRepository employeeRepository) {
		this.employeeRepository = employeeRepository;
	}

	/**
	 * Validates that assignments don't violate same-day rules
	 */
	public List<ConflictError> validateAssignments(List<ShiftAssignment> assignments) {
		List<ConflictError> conflicts = new ArrayList<>();

		// Group by employee and date
		Map<Long, Map<LocalDate, List<ShiftAssignment>>> byEmployeeAndDate = new HashMap<>();

		assignments.stream().filter(sa -> sa.getEmployee() != null).forEach(sa -> {
			Long empId = sa.getEmployee().getId().longValue();
			LocalDate date = sa.getShift().getShiftStart();

			byEmployeeAndDate.computeIfAbsent(empId, k -> new HashMap<>()).computeIfAbsent(date, k -> new ArrayList<>())
					.add(sa);
		});

		// Check each employee's assignments per day
		byEmployeeAndDate.forEach((empId, dateMap) -> {
			dateMap.forEach((date, dayAssignments) -> {
				if (!isAllowedDayAssignments(dayAssignments)) {
					Employee emp = employeeRepository.findById(empId.intValue()).orElse(null);
					String empName = emp != null ? emp.getFirstName() + " " + emp.getLastName() : "Unknown";

					List<ConflictingShiftDTO> shiftDTOs = dayAssignments.stream()
							.map(sa -> ConflictingShiftDTO.builder()
									.location(sa.getShift().getShiftTemplate().getLocation())
									.shiftType(sa.getShift().getShiftTemplate().getShiftTypeCode())
									.startTime(sa.getShift().getShiftTemplate().getStartTime())
									.endTime(sa.getShift().getShiftTemplate().getEndTime()).build())
							.toList();

					conflicts.add(ConflictError.builder().employeeId(empId).employeeName(empName).date(date)
							.conflictingShifts(shiftDTOs).build());
				}
			});
		});

		return conflicts;
	}

	/**
	 * Would assigning {@code requester} to {@code requestedShift} break the
	 * same-day rules, given the employee's existing assignments in
	 * {@code rotaAssignments}? Used to flag conflicting requests in the admin
	 * list and to block a conflicting approval.
	 */
	public boolean wouldConflict(Employee requester, ShiftAssignment requestedShift,
			List<ShiftAssignment> rotaAssignments) {
		if (requester == null || requestedShift == null || requestedShift.getShift() == null) {
			return false;
		}
		LocalDate date = requestedShift.getShift().getShiftStart();
		if (date == null) {
			return false;
		}
		List<ShiftAssignment> sameDay = new ArrayList<>();
		if (rotaAssignments != null) {
			for (ShiftAssignment sa : rotaAssignments) {
				if (sa.getEmployee() != null
						&& sa.getEmployee().getId().equals(requester.getId())
						&& sa.getShift() != null
						&& date.equals(sa.getShift().getShiftStart())) {
					sameDay.add(sa);
				}
			}
		}
		// Add the requested shift as if it were assigned to this employee.
		sameDay.add(requestedShift);
		return !isAllowedDayAssignments(sameDay);
	}

	/**
	 * Same-day assignment validation. Data-driven: a carer's shifts on one day are
	 * allowed unless two of them overlap in clock time (overnight-aware). Replaces the
	 * old enum matrix, which had no concept of new types (e.g. SHIFT_LEAD) and wrongly
	 * flagged non-overlapping combinations. Returns true if allowed, false if two shifts
	 * clash in time.
	 */
	private boolean isAllowedDayAssignments(List<ShiftAssignment> dayAssignments) {
		if (dayAssignments == null || dayAssignments.size() < 2) {
			return true;
		}
		for (int i = 0; i < dayAssignments.size(); i++) {
			for (int j = i + 1; j < dayAssignments.size(); j++) {
				if (shiftsOverlap(dayAssignments.get(i), dayAssignments.get(j))) {
					return false;
				}
			}
		}
		return true;
	}

	/** True if the two assignments' time intervals overlap (overnight-aware). */
	private static boolean shiftsOverlap(ShiftAssignment a, ShiftAssignment b) {
		LocalDateTime aStart = startOf(a), aEnd = endOf(a), bStart = startOf(b), bEnd = endOf(b);
		if (aStart == null || aEnd == null || bStart == null || bEnd == null) {
			return false;
		}
		return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
	}

	private static LocalDateTime startOf(ShiftAssignment sa) {
		if (sa.getShift() == null || sa.getShift().getShiftTemplate() == null) {
			return null;
		}
		LocalDate date = sa.getShift().getShiftStart();
		LocalTime start = sa.getShift().getShiftTemplate().getStartTime();
		return (date == null || start == null) ? null : date.atTime(start);
	}

	private static LocalDateTime endOf(ShiftAssignment sa) {
		if (sa.getShift() == null || sa.getShift().getShiftTemplate() == null) {
			return null;
		}
		LocalDate date = sa.getShift().getShiftStart();
		LocalTime start = sa.getShift().getShiftTemplate().getStartTime();
		LocalTime end = sa.getShift().getShiftTemplate().getEndTime();
		if (date == null || start == null || end == null) {
			return null;
		}
		LocalDateTime endDt = date.atTime(end);
		if (!end.isAfter(start)) {
			endDt = endDt.plusDays(1); // overnight shift ends the next day
		}
		return endDt;
	}
}