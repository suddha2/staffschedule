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
import com.midco.rota.opt.RotaConstraintProvider;
import com.midco.rota.opt.ShiftOverlap;
import com.midco.rota.opt.ShiftTypeMeta;
import com.midco.rota.repository.EmployeeRepository;

@Service
public class PinValidationService {

	private final EmployeeRepository employeeRepository;

	public PinValidationService(EmployeeRepository employeeRepository) {
		this.employeeRepository = employeeRepository;
	}

	/**
	 * Save-time rules for a rota (the normal save and the pin-all save): a saved rota
	 * becomes standing pins, so anything that breaks a hard rule here would repeat every
	 * period. Checks, with the same definitions the solver scores:
	 * <ul>
	 * <li>same-day shifts that overlap in time;</li>
	 * <li>wrong gender for the shift / missing a required skill;</li>
	 * <li>compliance 6:1 - a permanent carer's 7th consecutive working day.</li>
	 * </ul>
	 * Each conflict carries a reason and a shift id so the grid can highlight it.
	 */
	public List<ConflictError> validateAssignments(List<ShiftAssignment> assignments) {
		List<ConflictError> conflicts = new ArrayList<>();

		// Group by employee and date
		Map<Long, Map<LocalDate, List<ShiftAssignment>>> byEmployeeAndDate = new HashMap<>();
		Map<Long, Employee> employees = new HashMap<>();

		assignments.stream().filter(sa -> sa.getEmployee() != null && sa.getShift() != null
				&& sa.getShift().getShiftStart() != null).forEach(sa -> {
			Long empId = sa.getEmployee().getId().longValue();
			LocalDate date = sa.getShift().getShiftStart();
			employees.putIfAbsent(empId, sa.getEmployee());
			byEmployeeAndDate.computeIfAbsent(empId, k -> new java.util.TreeMap<>()).computeIfAbsent(date, k -> new ArrayList<>())
					.add(sa);
		});

		byEmployeeAndDate.forEach((empId, dateMap) -> {
			Employee emp = employees.get(empId);
			String empName = emp != null && emp.getFirstName() != null ? emp.getFirstName() + " " + emp.getLastName()
					: employeeRepository.findById(empId.intValue()).map(e -> e.getFirstName() + " " + e.getLastName())
							.orElse("Unknown");

			dateMap.forEach((date, dayAssignments) -> {
				// 1. Same-day shifts that overlap in time
				if (!isAllowedDayAssignments(dayAssignments)) {
					conflicts.add(conflict(empId, empName, date, dayAssignments, "Overlapping shifts on the same day"));
				}
				// 2. Eligibility: gender (worked shifts, as the solver scores it) and required skills
				for (ShiftAssignment sa : dayAssignments) {
					var t = sa.getShift().getShiftTemplate();
					if (t == null) {
						continue;
					}
					if (ShiftTypeMeta.countsAsWork(t.getShiftTypeCode())
							&& RotaConstraintProvider.isGenderMismatch(sa.getEmployee(), t.getGender())) {
						conflicts.add(conflict(empId, empName, date, List.of(sa),
								"Wrong gender for this shift (needs " + t.getGender() + ")"));
					}
					if (RotaConstraintProvider.isMissingRequiredSkill(sa.getEmployee(), t.getRequiredSkills())) {
						conflicts.add(conflict(empId, empName, date, List.of(sa),
								"Missing a required skill (" + String.join(", ", t.getRequiredSkills()) + ")"));
					}
				}
			});

			// 3. Compliance 6:1 (permanent carers): flag every 7th-or-later consecutive day
			if (RotaConstraintProvider.sixOneApplies(emp)) {
				int streak = 0;
				LocalDate prev = null;
				for (Map.Entry<LocalDate, List<ShiftAssignment>> day : dateMap.entrySet()) {
					streak = (prev != null && prev.plusDays(1).equals(day.getKey())) ? streak + 1 : 1;
					if (streak >= 7) {
						conflicts.add(conflict(empId, empName, day.getKey(), day.getValue(),
								"Day " + streak + " in a row - permanent carers need 1 day off in 7 (6:1)"));
					}
					prev = day.getKey();
				}
			}
		});

		return conflicts;
	}

	private static ConflictError conflict(Long empId, String empName, LocalDate date, List<ShiftAssignment> shifts,
			String reason) {
		List<ConflictingShiftDTO> shiftDTOs = shifts.stream()
				.map(sa -> ConflictingShiftDTO.builder()
						.location(sa.getShift().getShiftTemplate().getLocation())
						.shiftType(sa.getShift().getShiftTemplate().getShiftTypeCode())
						.startTime(sa.getShift().getShiftTemplate().getStartTime())
						.endTime(sa.getShift().getShiftTemplate().getEndTime()).build())
				.toList();
		Long shiftId = shifts.isEmpty() || shifts.get(0).getShift().getId() == null ? null
				: shifts.get(0).getShift().getId().longValue();
		return ConflictError.builder().employeeId(empId).employeeName(empName).date(date)
				.conflictingShifts(shiftDTOs).reason(reason).shiftId(shiftId).build();
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