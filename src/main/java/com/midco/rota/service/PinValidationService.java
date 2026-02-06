package com.midco.rota.service;

import java.time.LocalDate;
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
import com.midco.rota.util.ShiftType;

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
									.shiftType(sa.getShift().getShiftTemplate().getShiftType())
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
	 * Same-day assignment validation logic Returns true if allowed, false if
	 * violates rules
	 */
	private boolean isAllowedDayAssignments(List<ShiftAssignment> dayAssignments) {
		if (dayAssignments == null || dayAssignments.isEmpty())
			return true;
		if (dayAssignments.size() == 1)
			return true;

		List<ShiftType> types = dayAssignments.stream().map(sa -> sa.getShift().getShiftTemplate().getShiftType())
				.toList();

		List<String> locations = dayAssignments.stream().map(sa -> sa.getShift().getShiftTemplate().getLocation())
				.toList();

		// Check: All FLOATING
		boolean allFloating = types.stream().allMatch(t -> t == ShiftType.FLOATING);
		if (allFloating) {
			long distinctLocs = locations.stream().distinct().count();
			return distinctLocs == locations.size();
		}

		// Check: No mixing FLOATING with non-FLOATING
		boolean containsFloating = types.stream().anyMatch(t -> t == ShiftType.FLOATING);
		boolean containsNonFloating = types.stream().anyMatch(t -> t == ShiftType.DAY || t == ShiftType.LONG_DAY
				|| t == ShiftType.WAKING_NIGHT || t == ShiftType.SLEEP_IN);

		if (containsFloating && containsNonFloating) {
			return false;
		}

		// Check: Exactly 2 shifts - LONG_DAY + SLEEP_IN at same location
		if (dayAssignments.size() == 2) {
			ShiftType t1 = types.get(0);
			ShiftType t2 = types.get(1);
			boolean ldSiPair = (t1 == ShiftType.LONG_DAY && t2 == ShiftType.SLEEP_IN)
					|| (t1 == ShiftType.SLEEP_IN && t2 == ShiftType.LONG_DAY);
			boolean sameLocation = locations.get(0) != null && locations.get(0).equals(locations.get(1));
			return ldSiPair && sameLocation;
		}

		// Any other case with 2+ non-floating shifts is invalid
		return false;
	}
}