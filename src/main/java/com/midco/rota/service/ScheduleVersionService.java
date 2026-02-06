package com.midco.rota.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.midco.rota.PinConflictException;
import com.midco.rota.dto.AssignmentDiffDTO;
import com.midco.rota.dto.AssignmentVersionDTO;
import com.midco.rota.dto.ChangeDTO;
import com.midco.rota.dto.ComparisonStatisticsDTO;
import com.midco.rota.dto.ConflictError;
import com.midco.rota.dto.RollbackRequest;
import com.midco.rota.dto.SaveScheduleRequest;
import com.midco.rota.dto.ShiftAssignmentChangeDTO;
import com.midco.rota.dto.VersionComparisonDTO;
import com.midco.rota.dto.VersionDetailDTO;
import com.midco.rota.dto.VersionHistoryDTO;
import com.midco.rota.dto.VersionStatisticsDTO;
import com.midco.rota.dto.VersionSummaryDTO;
import com.midco.rota.model.Employee;
import com.midco.rota.model.PinnedTemplateAssignment;
import com.midco.rota.model.Rota;
import com.midco.rota.model.ScheduleChange;
import com.midco.rota.model.ScheduleVersion;
import com.midco.rota.model.ScheduleVersionAudit;
import com.midco.rota.model.Shift;
import com.midco.rota.model.ShiftAssignment;
import com.midco.rota.model.ShiftAssignmentVersion;
import com.midco.rota.model.ShiftTemplate;
import com.midco.rota.repository.EmployeeRepository;
import com.midco.rota.repository.PinnedTemplateAssignmentRepository;
import com.midco.rota.repository.RotaRepository;
import com.midco.rota.repository.ScheduleChangeRepository;
import com.midco.rota.repository.ScheduleVersionAuditRepository;
import com.midco.rota.repository.ScheduleVersionRepository;
import com.midco.rota.repository.ShiftAssignmentRepository;
import com.midco.rota.repository.ShiftAssignmentVersionRepository;
import com.midco.rota.repository.ShiftRepository;
import com.midco.rota.repository.ShiftTemplateRepository;

/**
 * Service for managing schedule versions All business logic for versioning is
 * here (not in database) NO LOMBOK VERSION
 */
@Service
public class ScheduleVersionService {

	private static final Logger log = LoggerFactory.getLogger(ScheduleVersionService.class);

	private final ScheduleVersionRepository versionRepository;
	private final ShiftAssignmentVersionRepository assignmentVersionRepository;
	private final ScheduleChangeRepository changeRepository;
	private final ScheduleVersionAuditRepository auditRepository;

	// Your existing repositories - UPDATE THESE NAMES TO MATCH YOUR PROJECT!
	private final ShiftAssignmentRepository rotaShiftAssignmentRepository;
	private final ShiftRepository shiftRepository;
	private final EmployeeRepository employeeRepository;
	private final ShiftTemplateRepository shiftTemplatesRepository;
	private final RotaRepository rotaRepository;

	private final PinValidationService pinValidationService;
	private final PinnedTemplateAssignmentRepository pinnedTemplateAssignmentRepository;

	// Constructor (replaces @RequiredArgsConstructor)
	@Autowired
	public ScheduleVersionService(ScheduleVersionRepository versionRepository,
			ShiftAssignmentVersionRepository assignmentVersionRepository, ScheduleChangeRepository changeRepository,
			ScheduleVersionAuditRepository auditRepository, ShiftAssignmentRepository rotaShiftAssignmentRepository,
			ShiftRepository shiftRepository, EmployeeRepository employeeRepository,
			ShiftTemplateRepository shiftTemplatesRepository, RotaRepository rotaRepository,
			PinValidationService pinValidationService,
			PinnedTemplateAssignmentRepository pinnedTemplateAssignmentRepository) {

		this.versionRepository = versionRepository;
		this.assignmentVersionRepository = assignmentVersionRepository;
		this.changeRepository = changeRepository;
		this.auditRepository = auditRepository;
		this.rotaShiftAssignmentRepository = rotaShiftAssignmentRepository;
		this.shiftRepository = shiftRepository;
		this.employeeRepository = employeeRepository;
		this.shiftTemplatesRepository = shiftTemplatesRepository;
		this.rotaRepository = rotaRepository;
		this.pinnedTemplateAssignmentRepository = pinnedTemplateAssignmentRepository;
		this.pinValidationService = pinValidationService;
	}

	/**
	 * Create a new version from current rota state
	 */

	@Transactional
	public VersionDetailDTO createVersion(SaveScheduleRequest request) {
		log.info("Creating version for rota {} by {}", request.getRotaId(), request.getUsername());

		// ✅ UPDATED: Validate ALL assignments if pinning
		if (request.isPinAllChanges()) {
			log.info("Validating all assignments for pin conflicts");

			List<ShiftAssignment> allAssignments = rotaShiftAssignmentRepository.findByRotaId(request.getRotaId());
			List<ConflictError> conflicts = pinValidationService.validateAssignments(allAssignments);

			if (!conflicts.isEmpty()) {
				log.error("Pin validation failed: {} conflicts found", conflicts.size());
				throw new PinConflictException("Cannot pin assignments with conflicts", conflicts);
			}

			log.info("Pin validation passed - no conflicts in {} assignments", allAssignments.size());
		}

		// Apply changes to database FIRST
		if (request.getChanges() != null && !request.getChanges().isEmpty()) {
			applyChangesToDatabase(request.getRotaId(), request.getChanges());
			log.info("Applied {} changes to database", request.getChanges().size());
		}

		// Get previous current version
		ScheduleVersion previousVersion = versionRepository.findCurrentVersionByRotaId(request.getRotaId())
				.orElse(null);

		// Mark old version as not current
		if (previousVersion != null) {
			previousVersion.setIsCurrent(false);
			versionRepository.save(previousVersion);
			log.debug("Marked version {} as not current", previousVersion.getVersionNumber());
		}

		// Calculate next version number
		Integer nextVersionNumber = versionRepository.findMaxVersionNumber(request.getRotaId()).map(v -> v + 1)
				.orElse(1);

		// Create new version
		ScheduleVersion newVersion = ScheduleVersion.builder().rotaId(request.getRotaId())
				.versionNumber(nextVersionNumber)
				.versionLabel(
						request.getVersionLabel() != null ? request.getVersionLabel() : "Version " + nextVersionNumber)
				.isCurrent(true).createdBy(request.getUsername()).comment(request.getComment()).build();

		newVersion = versionRepository.save(newVersion);
		log.info("Created version {} (id={})", nextVersionNumber, newVersion.getId());

		// Get current assignments from database
		List<ShiftAssignment> currentAssignments = rotaShiftAssignmentRepository.findByRotaId(request.getRotaId());

		// ✅ UPDATED: Pin ALL assignments if requested
		if (request.isPinAllChanges()) {
			createPinsFromAllAssignments(request.getRotaId(), request.getUsername());
		}

		// Create assignment version snapshots
		List<ShiftAssignmentVersion> assignmentVersions = new ArrayList<>();
		for (ShiftAssignment assignment : currentAssignments) {
			Integer employeeId = null;
			if (assignment.getEmployee() != null) {
				employeeId = assignment.getEmployee().getId();
			}

			Long shiftId = null;
			if (assignment.getShift() != null) {
				shiftId = assignment.getShift().getId();
			}

			Long rotaId = null;
			if (assignment.getRota() != null) {
				rotaId = assignment.getRota().getId();
			}

			ShiftAssignmentVersion av = ShiftAssignmentVersion.builder().versionId(newVersion.getId()).shiftId(shiftId)
					.employeeId(employeeId).rotaId(rotaId).build();
			assignmentVersions.add(av);
		}

		assignmentVersionRepository.saveAll(assignmentVersions);
		log.info("Saved {} assignment snapshots", assignmentVersions.size());

		// Track changes if there was a previous version
		int changeCount = 0;
		if (previousVersion != null && request.getChanges() != null) {
			changeCount = trackChanges(previousVersion, newVersion, request);
			log.info("Tracked {} changes", changeCount);
		}

		// Update statistics
		newVersion.setTotalAssignments(assignmentVersions.size());
		newVersion.setChangesFromPrevious(changeCount);
		versionRepository.save(newVersion);

		// Create audit log
		createAuditLog(newVersion.getId(), ScheduleVersionAudit.AuditAction.CREATED, request.getUsername(), null, null);

		// Verify only one current version exists
		long currentCount = versionRepository.countCurrentVersions(request.getRotaId());
		if (currentCount > 1) {
			log.error("INTEGRITY ERROR: Multiple current versions for rota {}", request.getRotaId());
			throw new IllegalStateException("Multiple current versions detected!");
		}

		log.info("Successfully created version {} for rota {}", nextVersionNumber, request.getRotaId());
		return buildVersionDetail(newVersion);
	}

	/**
	 * Get version history for a rota
	 */
	@Transactional(readOnly = true)
	public VersionHistoryDTO getVersionHistory(Long rotaId) {
		List<ScheduleVersion> versions = versionRepository.findByRotaIdOrderByVersionNumberDesc(rotaId);

		VersionSummaryDTO current = versions.stream().filter(ScheduleVersion::getIsCurrent).findFirst()
				.map(this::toSummaryDTO).orElse(null);

		List<VersionSummaryDTO> summaries = versions.stream().map(this::toSummaryDTO).collect(Collectors.toList());

		return VersionHistoryDTO.builder().rotaId(rotaId).versions(summaries).currentVersion(current)
				.totalVersions(versions.size()).build();
	}

	/**
	 * Get detailed information about a specific version
	 */
	@Transactional(readOnly = true)
	public VersionDetailDTO getVersionDetail(Long versionId, boolean includeChanges) {
		ScheduleVersion version = versionRepository.findById(versionId)
				.orElseThrow(() -> new RuntimeException("Version not found: " + versionId));

		return buildVersionDetail(version, includeChanges);
	}

	/**
	 * Rollback to a previous version (creates new version with old state)
	 */
	@Transactional
	public VersionDetailDTO rollbackToVersion(RollbackRequest request) {
		log.info("Rolling back to version {} by {}", request.getTargetVersionId(), request.getUsername());

		ScheduleVersion targetVersion = versionRepository.findById(request.getTargetVersionId())
				.orElseThrow(() -> new RuntimeException("Target version not found"));

		// Get target version's assignments
		List<ShiftAssignmentVersion> targetAssignments = assignmentVersionRepository
				.findByVersionId(request.getTargetVersionId());

		// BUSINESS LOGIC: Mark current version as not current
		ScheduleVersion currentVersion = versionRepository.findCurrentVersionByRotaId(targetVersion.getRotaId())
				.orElse(null);

		if (currentVersion != null) {
			currentVersion.setIsCurrent(false);
			versionRepository.save(currentVersion);
		}

		// BUSINESS LOGIC: Calculate next version number
		Integer nextVersionNumber = versionRepository.findMaxVersionNumber(targetVersion.getRotaId()).map(v -> v + 1)
				.orElse(1);

		// Create new version (rollback creates new version, doesn't modify history)
		ScheduleVersion rollbackVersion = ScheduleVersion.builder().rotaId(targetVersion.getRotaId())
				.versionNumber(nextVersionNumber).versionLabel("Rollback to v" + targetVersion.getVersionNumber())
				.isCurrent(true).createdBy(request.getUsername())
				.comment("Rolled back to version " + targetVersion.getVersionNumber()
						+ (request.getReason() != null ? ": " + request.getReason() : ""))
				.build();

		rollbackVersion = versionRepository.save(rollbackVersion);

		// Copy target version's assignments
		List<ShiftAssignmentVersion> newAssignments = new ArrayList<>();
		for (ShiftAssignmentVersion oldAssignment : targetAssignments) {
			ShiftAssignmentVersion newAssignment = ShiftAssignmentVersion.builder().versionId(rollbackVersion.getId())
					.shiftId(oldAssignment.getShiftId()).employeeId(oldAssignment.getEmployeeId())
					.rotaId(oldAssignment.getRotaId()).build();
			newAssignments.add(newAssignment);
		}

		assignmentVersionRepository.saveAll(newAssignments);

		// BUSINESS LOGIC: Update live rota_shift_assignment table
		updateLiveAssignments(targetVersion.getRotaId(), targetAssignments);

		// Update statistics
		rollbackVersion.setTotalAssignments(newAssignments.size());
		versionRepository.save(rollbackVersion);

		// Create audit logs
		createAuditLog(rollbackVersion.getId(), ScheduleVersionAudit.AuditAction.CREATED, request.getUsername(), null,
				null);
		createAuditLog(targetVersion.getId(), ScheduleVersionAudit.AuditAction.ROLLED_BACK, request.getUsername(), null,
				null);

		log.info("Rolled back to version {} as new version {}", targetVersion.getVersionNumber(),
				rollbackVersion.getVersionNumber());

		return buildVersionDetail(rollbackVersion);
	}

	/**
	 * Compare two versions
	 */
	@Transactional(readOnly = true)
	public VersionComparisonDTO compareVersions(Long versionAId, Long versionBId) {
		ScheduleVersion versionA = versionRepository.findById(versionAId)
				.orElseThrow(() -> new RuntimeException("Version A not found"));
		ScheduleVersion versionB = versionRepository.findById(versionBId)
				.orElseThrow(() -> new RuntimeException("Version B not found"));

		List<ShiftAssignmentVersion> assignmentsA = assignmentVersionRepository.findByVersionId(versionAId);
		List<ShiftAssignmentVersion> assignmentsB = assignmentVersionRepository.findByVersionId(versionBId);

		// Build maps for comparison
		Map<Long, ShiftAssignmentVersion> mapA = assignmentsA.stream()
				.collect(Collectors.toMap(ShiftAssignmentVersion::getShiftId, a -> a));
		Map<Long, ShiftAssignmentVersion> mapB = assignmentsB.stream()
				.collect(Collectors.toMap(ShiftAssignmentVersion::getShiftId, a -> a));

		// Find all unique shift IDs
		Set<Long> allShiftIds = new HashSet<>();
		allShiftIds.addAll(mapA.keySet());
		allShiftIds.addAll(mapB.keySet());

		// Compare
		List<AssignmentDiffDTO> differences = new ArrayList<>();
		int unchangedCount = 0;
		int addedCount = 0;
		int removedCount = 0;
		int reassignedCount = 0;

		for (Long shiftId : allShiftIds) {
			ShiftAssignmentVersion assignmentA = mapA.get(shiftId);
			ShiftAssignmentVersion assignmentB = mapB.get(shiftId);

			String changeType;
			Integer empAId = assignmentA != null ? assignmentA.getEmployeeId() : null;
			Integer empBId = assignmentB != null ? assignmentB.getEmployeeId() : null;

			if (assignmentA == null) {
				changeType = "ADDED";
				addedCount++;
			} else if (assignmentB == null) {
				changeType = "REMOVED";
				removedCount++;
			} else if (!Objects.equals(empAId, empBId)) {
				changeType = "REASSIGNED";
				reassignedCount++;
			} else {
				changeType = "UNCHANGED";
				unchangedCount++;
				continue; // Skip unchanged assignments
			}

			// Get shift details
			Shift shift = shiftRepository.findById(shiftId).orElse(null);
			ShiftTemplate template = shift != null && shift.getShiftTemplate() != null
					? shiftTemplatesRepository.findById(shift.getShiftTemplate().getId().intValue()).orElse(null)
					: null;

			differences.add(AssignmentDiffDTO.builder().shiftId(shiftId)
					.location(template != null ? template.getLocation() : null)
					.shiftType(template != null ? template.getShiftType().name() : null)
					.shiftStart(shift != null && shift.getShiftStart() != null ? shift.getShiftStart().atStartOfDay()
							: null)
					.changeType(changeType).employeeAId(empAId).employeeAName(getEmployeeName(empAId))
					.employeeBId(empBId).employeeBName(getEmployeeName(empBId)).build());
		}

		// Calculate statistics
		int totalShifts = allShiftIds.size();
		int changedShifts = addedCount + removedCount + reassignedCount;
		double changePercentage = totalShifts > 0 ? (changedShifts * 100.0 / totalShifts) : 0.0;

		ComparisonStatisticsDTO statistics = ComparisonStatisticsDTO.builder().totalShifts(totalShifts)
				.unchangedShifts(unchangedCount).changedShifts(changedShifts).shiftsAdded(addedCount)
				.shiftsRemoved(removedCount).shiftsReassigned(reassignedCount)
				.changePercentage(Math.round(changePercentage * 100.0) / 100.0).build();

		return VersionComparisonDTO.builder().versionA(toSummaryDTO(versionA)).versionB(toSummaryDTO(versionB))
				.differences(differences).statistics(statistics).build();
	}

	// ===== PRIVATE HELPER METHODS =====

	private int trackChanges(ScheduleVersion fromVersion, ScheduleVersion toVersion, SaveScheduleRequest request) {

		if (request.getChanges() == null || request.getChanges().isEmpty()) {
			return 0;
		}

		List<ScheduleChange> changes = new ArrayList<>();

		for (ShiftAssignmentChangeDTO changeDTO : request.getChanges()) {
			ScheduleChange.ChangeType changeType = determineChangeType(changeDTO.getOldEmployeeId(),
					changeDTO.getNewEmployeeId());

			ScheduleChange change = ScheduleChange.builder().fromVersionId(fromVersion.getId())
					.toVersionId(toVersion.getId()).shiftId(changeDTO.getShiftId()).changeType(changeType)
					.oldEmployeeId(changeDTO.getOldEmployeeId()).newEmployeeId(changeDTO.getNewEmployeeId())
					.changeReason(parseChangeReason(changeDTO.getChangeReason())).changedBy(request.getUsername())
					.build();

			changes.add(change);
		}

		changeRepository.saveAll(changes);
		return changes.size();
	}

	private ScheduleChange.ChangeType determineChangeType(Integer oldEmpId, Integer newEmpId) {
		if (oldEmpId == null && newEmpId != null) {
			return ScheduleChange.ChangeType.ASSIGNED;
		} else if (oldEmpId != null && newEmpId == null) {
			return ScheduleChange.ChangeType.UNASSIGNED;
		} else if (oldEmpId != null && newEmpId != null && !oldEmpId.equals(newEmpId)) {
			return ScheduleChange.ChangeType.REASSIGNED;
		}
		return ScheduleChange.ChangeType.REASSIGNED;
	}

	private ScheduleChange.ChangeReason parseChangeReason(String reason) {
		try {
			return ScheduleChange.ChangeReason.valueOf(reason);
		} catch (Exception e) {
			return ScheduleChange.ChangeReason.MANUAL_ASSIGN;
		}
	}

	private void updateLiveAssignments(Long rotaId, List<ShiftAssignmentVersion> versionAssignments) {
		// ✅ Don't delete - update existing assignments
		List<ShiftAssignment> existingAssignments = rotaShiftAssignmentRepository.findByRotaId(rotaId);
		Map<Long, ShiftAssignment> existingMap = existingAssignments.stream()
				.collect(Collectors.toMap(sa -> sa.getShift().getId(), sa -> sa));

		for (ShiftAssignmentVersion va : versionAssignments) {
			ShiftAssignment assignment = existingMap.get(va.getShiftId());

			if (assignment == null) {
				assignment = new ShiftAssignment();
				Shift shift = shiftRepository.findById(va.getShiftId()).orElseThrow();
				Rota rota = rotaRepository.findById(va.getRotaId()).orElseThrow();
				assignment.setShift(shift);
				assignment.setRota(rota);
			}

			// ✅ Set or clear employee
			if (va.getEmployeeId() != null) {
				Employee employee = employeeRepository.findById(va.getEmployeeId()).orElseThrow();
				assignment.setEmployee(employee);
			} else {
				assignment.setEmployee(null);
			}

			rotaShiftAssignmentRepository.save(assignment);
		}
	}

	private void createAuditLog(Long versionId, ScheduleVersionAudit.AuditAction action, String username,
			String ipAddress, String userAgent) {
		ScheduleVersionAudit audit = ScheduleVersionAudit.builder().versionId(versionId).action(action)
				.performedBy(username).ipAddress(ipAddress).userAgent(userAgent).build();

		auditRepository.save(audit);
	}

	private VersionDetailDTO buildVersionDetail(ScheduleVersion version) {
		return buildVersionDetail(version, true);
	}

	private VersionDetailDTO buildVersionDetail(ScheduleVersion version, boolean includeChanges) {
		List<ShiftAssignmentVersion> assignments = assignmentVersionRepository.findByVersionId(version.getId());

		// Convert to DTOs with shift details
		List<AssignmentVersionDTO> assignmentDTOs = assignments.stream().map(this::toAssignmentDTO)
				.collect(Collectors.toList());

		// Get changes if requested
		List<ChangeDTO> changeDTOs = new ArrayList<>();
		if (includeChanges) {
			List<ScheduleChange> changes = changeRepository.findByToVersionId(version.getId());
			changeDTOs = changes.stream().map(this::toChangeDTO).collect(Collectors.toList());
		}

		// Calculate statistics
		VersionStatisticsDTO statistics = calculateStatistics(assignments);

		return VersionDetailDTO.builder().version(toSummaryDTO(version)).assignments(assignmentDTOs).changes(changeDTOs)
				.statistics(statistics).build();
	}

	private VersionSummaryDTO toSummaryDTO(ScheduleVersion version) {
		return VersionSummaryDTO.builder().versionId(version.getId()).rotaId(version.getRotaId())
				.versionNumber(version.getVersionNumber()).versionLabel(version.getVersionLabel())
				.isCurrent(version.getIsCurrent()).createdAt(version.getCreatedAt()).createdBy(version.getCreatedBy())
				.comment(version.getComment()).totalAssignments(version.getTotalAssignments())
				.changesFromPrevious(version.getChangesFromPrevious()).build();
	}

	private AssignmentVersionDTO toAssignmentDTO(ShiftAssignmentVersion assignment) {
		// Get shift details
		Shift shift = shiftRepository.findById(assignment.getShiftId()).orElse(null);
		ShiftTemplate template = shift != null && shift.getShiftTemplate() != null
				? shiftTemplatesRepository.findById(shift.getShiftTemplate().getId().intValue()).orElse(null)
				: null;

		// Get employee details
		Employee employee = assignment.getEmployeeId() != null
				? employeeRepository.findById(assignment.getEmployeeId()).orElse(null)
				: null;

		return AssignmentVersionDTO.builder().assignmentId(assignment.getId()).shiftId(assignment.getShiftId())
				.employeeId(assignment.getEmployeeId())
				.employeeFirstName(employee != null ? employee.getFirstName() : null)
				.employeeLastName(employee != null ? employee.getLastName() : null).rotaId(assignment.getRotaId())
				.assignedAt(assignment.getAssignedAt()).location(template != null ? template.getLocation() : null)
				.shiftType(template != null ? template.getShiftType().name() : null)
				.dayOfWeek(template != null ? template.getDayOfWeek().name() : null)
				.startTime(
						template != null ? template.getStartTime() != null ? template.getStartTime().toString() : null
								: null)
				.endTime(template != null ? template.getEndTime() != null ? template.getEndTime().toString() : null
						: null)
				.shiftStart(
						shift != null && shift.getShiftStart() != null ? shift.getShiftStart().atStartOfDay() : null)
				.shiftEnd(shift != null && shift.getShiftEnd() != null ? shift.getShiftEnd().atTime(23, 59) : null)

				.build();
	}

	private ChangeDTO toChangeDTO(ScheduleChange change) {
		Shift shift = shiftRepository.findById(change.getShiftId()).orElse(null);
		ShiftTemplate template = shift != null && shift.getShiftTemplate() != null
				? shiftTemplatesRepository.findById(shift.getShiftTemplate().getId().intValue()).orElse(null)
				: null;

		Employee oldEmployee = change.getOldEmployeeId() != null
				? employeeRepository.findById(change.getOldEmployeeId()).orElse(null)
				: null;
		Employee newEmployee = change.getNewEmployeeId() != null
				? employeeRepository.findById(change.getNewEmployeeId()).orElse(null)
				: null;

		return ChangeDTO.builder().changeId(change.getId()).shiftId(change.getShiftId())
				.changeType(change.getChangeType().name()).oldEmployeeId(change.getOldEmployeeId())
				.oldEmployeeFirstName(oldEmployee != null ? oldEmployee.getFirstName() : null)
				.oldEmployeeLastName(oldEmployee != null ? oldEmployee.getLastName() : null)
				.newEmployeeId(change.getNewEmployeeId())
				.newEmployeeFirstName(newEmployee != null ? newEmployee.getFirstName() : null)
				.newEmployeeLastName(newEmployee != null ? newEmployee.getLastName() : null)
				.changeReason(change.getChangeReason() != null ? change.getChangeReason().name() : null)
				.changedAt(change.getChangedAt()).changedBy(change.getChangedBy())
				.location(template != null ? template.getLocation() : null)
				.shiftType(template != null ? template.getShiftType().name() : null)
				.shiftStart(
						shift != null && shift.getShiftStart() != null ? shift.getShiftStart().atStartOfDay() : null)

				.build();
	}

	private VersionStatisticsDTO calculateStatistics(List<ShiftAssignmentVersion> assignments) {
		int totalShifts = assignments.size();
		int assignedShifts = (int) assignments.stream().filter(a -> a.getEmployeeId() != null).count();
		int unassignedShifts = totalShifts - assignedShifts;
		int uniqueEmployees = (int) assignments.stream().map(ShiftAssignmentVersion::getEmployeeId)
				.filter(Objects::nonNull).distinct().count();
		double allocationRate = totalShifts > 0 ? (assignedShifts * 100.0 / totalShifts) : 0.0;

		return VersionStatisticsDTO.builder().totalShifts(totalShifts).assignedShifts(assignedShifts)
				.unassignedShifts(unassignedShifts).uniqueEmployees(uniqueEmployees)
				.allocationRate(Math.round(allocationRate * 100.0) / 100.0).build();
	}

	private String getEmployeeName(Integer employeeId) {
		if (employeeId == null)
			return null;
		Employee employee = employeeRepository.findById(employeeId).orElse(null);
		if (employee == null)
			return null;
		return employee.getFirstName() + " " + employee.getLastName();
	}

	private void applyChangesToDatabase(Long rotaId, List<ShiftAssignmentChangeDTO> changes) {
		for (ShiftAssignmentChangeDTO change : changes) {
			Long shiftId = change.getShiftId();
			Integer newEmployeeId = change.getNewEmployeeId();

			List<ShiftAssignment> existing = rotaShiftAssignmentRepository.findByRotaIdAndShiftId(rotaId, shiftId);

			if (newEmployeeId == null) {
				// ✅ FIXED: Set employee to NULL instead of deleting row
				if (!existing.isEmpty()) {
					ShiftAssignment assignment = existing.get(0);
					assignment.setEmployee(null);
					rotaShiftAssignmentRepository.save(assignment);
				}
			} else {
				// ASSIGN/REASSIGN
				ShiftAssignment assignment = existing.isEmpty() ? new ShiftAssignment() : existing.get(0);

				Shift shift = shiftRepository.findById(shiftId).orElseThrow();
				Employee employee = employeeRepository.findById(newEmployeeId).orElseThrow();
				Rota rota = rotaRepository.findById(rotaId).orElseThrow();

				assignment.setShift(shift);
				assignment.setEmployee(employee);
				assignment.setRota(rota);

				rotaShiftAssignmentRepository.save(assignment);
			}
		}
	}

	/**
	 * Build ShiftAssignments from changes for validation
	 */
	private List<ShiftAssignment> buildAssignmentsFromChanges(Long rotaId, List<ShiftAssignmentChangeDTO> changes) {
		List<ShiftAssignment> assignments = new ArrayList<>();

		for (ShiftAssignmentChangeDTO change : changes) {
			if (change.getNewEmployeeId() == null) {
				// Skip unassigned shifts in validation
				continue;
			}

			Shift shift = shiftRepository.findById(change.getShiftId()).orElse(null);
			Employee employee = employeeRepository.findById(change.getNewEmployeeId()).orElse(null);
			Rota rota = rotaRepository.findById(rotaId).orElse(null);

			if (shift != null && employee != null && rota != null) {
				ShiftAssignment assignment = new ShiftAssignment();
				assignment.setShift(shift);
				assignment.setEmployee(employee);
				assignment.setRota(rota);
				assignments.add(assignment);
			}
		}

		return assignments;
	}

	/**
	 * Create pinned template assignments from ALL current assignments
	 */
	private void createPinsFromAllAssignments(Long rotaId, String username) {
		// Get ALL current assignments from the rota
		List<ShiftAssignment> allAssignments = rotaShiftAssignmentRepository.findByRotaId(rotaId);

		int created = 0;
		int skipped = 0;

		for (ShiftAssignment assignment : allAssignments) {
			// Skip unassigned shifts
			if (assignment.getEmployee() == null) {
				continue;
			}

			// Get shift template
			Shift shift = assignment.getShift();
			if (shift == null || shift.getShiftTemplate() == null) {
				continue;
			}

			Long templateId = shift.getShiftTemplate().getId().longValue();
			Integer employeeId = assignment.getEmployee().getId();

			// Check if pin already exists
			boolean exists = pinnedTemplateAssignmentRepository.existsByShiftTemplateIdAndEmployeeId(templateId,
					employeeId.longValue());

			if (exists) {
				skipped++;
				continue;
			}

			// Create pin
			PinnedTemplateAssignment pin = PinnedTemplateAssignment.builder().shiftTemplateId(templateId)
					.employeeId(Long.valueOf(employeeId)).pinnedByUserId(null) // Can add user lookup if needed
					.build();

			pinnedTemplateAssignmentRepository.save(pin);
			created++;

			log.debug("Created pin: template={}, employee={}", templateId, employeeId);
		}

		log.info("Pinned all assignments: {} created, {} already existed", created, skipped);
	}

	/**
	 * Create pinned template assignments from changes
	 */
	private void createPinsFromChanges(List<ShiftAssignmentChangeDTO> changes, String username) {
		for (ShiftAssignmentChangeDTO change : changes) {
			Integer newEmployeeId = change.getNewEmployeeId();

			// Skip unassigned shifts
			if (newEmployeeId == null) {
				continue;
			}

			// Get shift to find template
			Shift shift = shiftRepository.findById(change.getShiftId()).orElse(null);
			if (shift == null || shift.getShiftTemplate() == null) {
				continue;
			}

			Long templateId = shift.getShiftTemplate().getId().longValue();

			// Check if pin already exists
			boolean exists = pinnedTemplateAssignmentRepository.existsByShiftTemplateIdAndEmployeeId(templateId,
					newEmployeeId.longValue());

			if (!exists) {
				PinnedTemplateAssignment pin = PinnedTemplateAssignment.builder().shiftTemplateId(templateId)
						.employeeId(Long.valueOf(newEmployeeId)).pinnedByUserId(null) // You can add user ID lookup if
																						// needed
						.build();

				pinnedTemplateAssignmentRepository.save(pin);
				log.debug("Created pin: template={}, employee={}", templateId, newEmployeeId);
			}
		}
	}
}