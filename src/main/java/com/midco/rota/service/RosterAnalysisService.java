package com.midco.rota.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.optaplanner.core.api.score.ScoreExplanation;
import org.optaplanner.core.api.score.ScoreManager;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.constraint.ConstraintMatchTotal;
import org.optaplanner.core.api.score.constraint.Indictment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.midco.rota.model.Employee;
import com.midco.rota.model.Rota;
import com.midco.rota.model.Shift;
import com.midco.rota.model.ShiftAssignment;
import com.midco.rota.model.ShiftTemplate;
import com.midco.rota.util.ContractType;

@Service
public class RosterAnalysisService {

	private final ScoreManager<Rota, HardSoftScore> scoreManager;

	public RosterAnalysisService(ScoreManager<Rota, HardSoftScore> scoreManager) {
		this.scoreManager = scoreManager;

	}

	private static final Logger logger = LoggerFactory.getLogger(RosterAnalysisService.class);

	public void printHighImpactViolations(Rota solution) {

		long unassignedCount = solution.getShiftAssignmentList().stream().filter(sa -> sa.getEmployee() == null)
				.count();
		logger.debug("Unassigned shifts: {}", unassignedCount);

//		ScoreExplanation<Rota, HardSoftScore> explanation = scoreManager.explain(solution);
//		Map<Object, Indictment<HardSoftScore>> indictmentMap = explanation.getIndictmentMap();

//		logUnassignedShifts(solution);
//
//		logUnassignedEmployees(solution);
//
//		logConstraintViolations(solution, scoreManager);
//		
//		logSkippedPermanentForZeroHours(solution);
	}

	public void logUnassignedShifts(Rota rota) {
		rota.getShiftAssignmentList().stream().filter(sa -> sa.getEmployee() == null).forEach(sa -> {
			Shift shift = sa.getShift();
			ShiftTemplate template = shift.getShiftTemplate();
			logger.info(
					"❌ Unassigned Shift → Location: {},type: {}, Date: {}, Time: {}–{}, Required Skills: {}, Gender Requirement: {}",
					template.getLocation(), template.getShiftType(), shift.getShiftStart(), template.getStartTime(),
					template.getEndTime(), template.getRequiredSkills(), template.getRequiredGender());
		});
	}

	public void logUnassignedEmployees(Rota rota) {
		Map<Employee, Long> assignmentCount = rota.getShiftAssignmentList().stream()
				.filter(sa -> sa.getEmployee() != null)
				.collect(Collectors.groupingBy(ShiftAssignment::getEmployee, Collectors.counting()));

		rota.getEmployeeList().stream().filter(emp -> assignmentCount.getOrDefault(emp, 0L) == 0).forEach(emp -> {
			logger.info(emp.toString());
		});
	}

	public void logConstraintViolations(Rota rota, ScoreManager<Rota, HardSoftScore> scoreMgr) {
		ScoreExplanation<Rota, HardSoftScore> explanation = scoreMgr.explain(rota);

		logger.info("🔍 Constraint Contributions:");
		for (ConstraintMatchTotal<?> match : explanation.getConstraintMatchTotalMap().values()) {
			logger.info("  📌 Constraint: {} | Score: {} | Matches: {}", match.getConstraintName(), match.getScore(),
					match.getConstraintMatchCount());
		}

		logger.info("🧠 Indictments:");
		for (Map.Entry<Object, Indictment<HardSoftScore>> entry : explanation.getIndictmentMap().entrySet()) {
			Object entity = entry.getKey();
			Indictment<HardSoftScore> indictment = entry.getValue();

			if (!indictment.getConstraintMatchSet().isEmpty()) {
				logger.info("  🔸 Entity: " + entity + " | Score impact: " + indictment.getScore() + " | Constraints: "
						+ indictment.getConstraintMatchSet().stream().map(cm -> cm.getConstraintName())
								.collect(Collectors.toSet()));
			}
		}
	}
	public void logSkippedPermanentForZeroHours(Rota rota) {
	    rota.getShiftAssignmentList().stream()
	        .filter(sa -> sa.getEmployee() != null && sa.getEmployee().getContractType() == ContractType.ZERO_HOURS)
	        .forEach(sa -> {
	            Shift shift = sa.getShift();
	            List<Employee> skipped = rota.getEmployeeList().stream()
	                .filter(emp -> emp.getContractType() == ContractType.PERMANENT)
	                .filter(emp -> isEligible(emp, shift))
	                .filter(emp -> rota.getShiftAssignmentList().stream()
	                    .noneMatch(a -> a.getShift().equals(shift) && emp.equals(a.getEmployee())))
	                .collect(Collectors.toList());

	            if (!skipped.isEmpty()) {
	                logger.info("🔍 Shift {} assigned to ZERO_HOURS {} — skipped PERMANENTs: {}",
	                    shift.getId(), sa.getEmployee().getId(),
	                    skipped.stream().map(Employee::getId).collect(Collectors.toList()));
	            }
	        });
	}

	private boolean isEligible(Employee emp, Shift shift) {
	    ShiftTemplate template = shift.getShiftTemplate();
	    return (emp.getRestrictedDay() == null || !emp.getRestrictedDay().contains(template.getDay())) &&
	           (emp.getRestrictedShift() == null || !emp.getRestrictedShift().contains(template.getShiftType())) &&
	           (emp.getRestrictedService() == null || !emp.getRestrictedService().contains(template.getLocation()));
	}


}
