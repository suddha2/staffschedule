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
import com.midco.rota.opt.ShiftOverlap;
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
	 * Same-day assignment validation. Delegates to the shared {@link ShiftOverlap}
	 * rule so the save-time block, the solver and the frontend all use one definition:
	 * a carer's shifts on one day are allowed unless two overlap in time.
	 */
	private boolean isAllowedDayAssignments(List<ShiftAssignment> dayAssignments) {
		return ShiftOverlap.allowedSameDay(dayAssignments);
	}
}