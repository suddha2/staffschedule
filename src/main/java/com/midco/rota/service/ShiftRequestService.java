package com.midco.rota.service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.midco.rota.dto.DeviceRegistrationDTO;
import com.midco.rota.dto.FitDTO;
import com.midco.rota.dto.HoursImpactDTO;
import com.midco.rota.dto.ShiftRequestDTO;
import com.midco.rota.dto.ShiftRequestNotificationPayload;
import com.midco.rota.dto.ShiftRequestSubmissionDTO;
import com.midco.rota.dto.UnallocatedShiftDTO;
import com.midco.rota.model.Employee;
import com.midco.rota.model.EmployeeDevice;
import com.midco.rota.model.Rota;
import com.midco.rota.model.Shift;
import com.midco.rota.model.ShiftAssignment;
import com.midco.rota.model.ShiftRequest;
import com.midco.rota.model.ShiftTemplate;
import com.midco.rota.util.Gender;
import com.midco.rota.util.ShiftType;
import com.midco.rota.repository.DeferredSolveRequestRepository;
import com.midco.rota.repository.EmployeeDeviceRepository;
import com.midco.rota.repository.EmployeeRepository;
import com.midco.rota.repository.RotaRepository;
import com.midco.rota.repository.ShiftAssignmentRepository;
import com.midco.rota.repository.ShiftRequestRepository;
import com.midco.rota.util.ShiftRequestStatus;

@Service
public class ShiftRequestService {

	private static final Logger log = LoggerFactory.getLogger(ShiftRequestService.class);

	private final EmployeeRepository employeeRepository;
	private final EmployeeDeviceRepository deviceRepository;
	private final ShiftRequestRepository shiftRequestRepository;
	private final ShiftAssignmentRepository shiftAssignmentRepository;
	private final RotaRepository rotaRepository;
	private final DeferredSolveRequestRepository deferredSolveRequestRepository;
	private final FcmPushNotificationService fcm;
	private final RosterUpdateService rosterUpdateService;

	public ShiftRequestService(EmployeeRepository employeeRepository,
			EmployeeDeviceRepository deviceRepository,
			ShiftRequestRepository shiftRequestRepository,
			ShiftAssignmentRepository shiftAssignmentRepository,
			RotaRepository rotaRepository,
			DeferredSolveRequestRepository deferredSolveRequestRepository,
			FcmPushNotificationService fcm,
			RosterUpdateService rosterUpdateService) {
		this.employeeRepository = employeeRepository;
		this.deviceRepository = deviceRepository;
		this.shiftRequestRepository = shiftRequestRepository;
		this.shiftAssignmentRepository = shiftAssignmentRepository;
		this.rotaRepository = rotaRepository;
		this.deferredSolveRequestRepository = deferredSolveRequestRepository;
		this.fcm = fcm;
		this.rosterUpdateService = rosterUpdateService;
	}

	private Employee requireActiveEmployee(String email) {
		return employeeRepository.findByEmail(email)
				.filter(Employee::isActive)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
						"No active employee for the authenticated account"));
	}

	@Transactional
	public void registerDevice(String email, DeviceRegistrationDTO dto) {
		Employee employee = requireActiveEmployee(email);
		EmployeeDevice device = deviceRepository.findByFcmToken(dto.getFcmToken())
				.orElseGet(EmployeeDevice::new);
		device.setEmployee(employee);
		device.setFcmToken(dto.getFcmToken());
		if (dto.getPlatform() != null) {
			device.setPlatform(dto.getPlatform());
		}
		device.setActive(true);
		device.setLastSeenAt(LocalDateTime.now());
		if (device.getId() == null) {
			device.setRegisteredAt(LocalDateTime.now());
		}
		deviceRepository.save(device);
		fcm.subscribeToEmployeesTopic(dto.getFcmToken());
		log.info("Registered FCM device for employee {} (platform={})", employee.getId(), device.getPlatform());
	}

	public List<ShiftRequestDTO> listOwnRequests(String email, ShiftRequestStatus status) {
		Employee employee = requireActiveEmployee(email);
		List<ShiftRequest> requests = (status == null)
				? shiftRequestRepository.findByEmployeeIdOrderByRequestedAtDesc(employee.getId())
				: shiftRequestRepository.findByEmployeeIdAndStatusOrderByRequestedAtDesc(employee.getId(), status);
		// fit is intentionally left null on the mobile-side response — it's an admin-context concept.
		return requests.stream().map(ShiftRequestDTO::fromEntity).toList();
	}

	public List<UnallocatedShiftDTO> listAvailable(Long rotaId, String service) {
		Rota rota = rotaRepository.findById(rotaId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rota not found: " + rotaId));
		if (rota.getShiftAssignmentList() == null) {
			return List.of();
		}
		return rota.getShiftAssignmentList().stream()
				.filter(sa -> sa.getEmployee() == null)
				.filter(sa -> service == null
						|| (sa.getShift() != null
								&& sa.getShift().getShiftTemplate() != null
								&& service.equals(sa.getShift().getShiftTemplate().getLocation())))
				.map(UnallocatedShiftDTO::fromEntity)
				.toList();
	}

	@Transactional
	public List<ShiftRequestDTO> submitRequest(String email, ShiftRequestSubmissionDTO dto) {
		Employee employee = requireActiveEmployee(email);
		Long rotaId = dto.getRotaId();

		List<ShiftRequest> created = new ArrayList<>();
		for (Long saId : dto.getShiftAssignmentIds()) {
			ShiftAssignment sa = shiftAssignmentRepository.findById(saId)
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
							"Shift assignment not found: " + saId));
			if (sa.getRota() == null || !rotaId.equals(sa.getRota().getId())) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Shift assignment " + saId + " does not belong to rota " + rotaId);
			}
			if (sa.getEmployee() != null) {
				throw new ResponseStatusException(HttpStatus.CONFLICT,
						"Shift assignment " + saId + " is already allocated");
			}
			// A previously rejected request does NOT block re-submission — only an in-flight
			// PENDING row does. Fail the entire batch loudly if a duplicate PENDING is seen,
			// so the employee gets a clear 409 instead of a silent no-op.
			if (shiftRequestRepository.existsByShiftAssignmentIdAndEmployeeIdAndStatus(
					saId, employee.getId(), ShiftRequestStatus.PENDING)) {
				throw new ResponseStatusException(HttpStatus.CONFLICT,
						"You already have a pending request for shift assignment " + saId);
			}
			ShiftRequest req = new ShiftRequest();
			req.setShiftAssignment(sa);
			req.setEmployee(employee);
			req.setRotaId(rotaId);
			req.setStatus(ShiftRequestStatus.PENDING);
			req.setRequestedAt(LocalDateTime.now());
			created.add(shiftRequestRepository.save(req));
		}

		if (!created.isEmpty()) {
			ShiftRequestNotificationPayload payload = new ShiftRequestNotificationPayload(
					rotaId,
					employee.getId(),
					((employee.getFirstName() == null ? "" : employee.getFirstName()) + " "
							+ (employee.getLastName() == null ? "" : employee.getLastName())).trim(),
					created.stream().map(r -> r.getShiftAssignment().getId()).toList(),
					created.size(),
					LocalDateTime.now());
			rosterUpdateService.pushShiftRequestNotificationToAdmins(payload);
		}

		log.info("Employee {} submitted {} shift request(s) for rota {}", employee.getId(), created.size(), rotaId);
		return created.stream().map(ShiftRequestDTO::fromEntity).toList();
	}

	public List<ShiftRequestDTO> listRequestsForRota(Long rotaId, ShiftRequestStatus status) {
		List<ShiftRequest> requests;
		if (rotaId != null) {
			requests = (status == null)
					? shiftRequestRepository.findByRotaId(rotaId)
					: shiftRequestRepository.findByRotaIdAndStatus(rotaId, status);
		} else {
			// Cross-rota listing — default to PENDING when no status is given,
			// so we don't accidentally pull historical rows across every rota.
			ShiftRequestStatus effectiveStatus = (status == null) ? ShiftRequestStatus.PENDING : status;
			requests = shiftRequestRepository.findByStatus(effectiveStatus);
		}
		if (requests.isEmpty()) {
			return List.of();
		}
		// Batch-load every distinct rota referenced so fit computation stays N+1-free.
		Set<Long> rotaIds = new HashSet<>();
		for (ShiftRequest r : requests) {
			if (r.getRotaId() != null) {
				rotaIds.add(r.getRotaId());
			}
		}
		Map<Long, Rota> rotaCache = new HashMap<>();
		for (Long id : rotaIds) {
			rotaRepository.findById(id).ifPresent(r -> rotaCache.put(id, r));
		}
		return requests.stream().map(req -> {
			ShiftRequestDTO dto = ShiftRequestDTO.fromEntity(req);
			Rota rotaForFit = req.getRotaId() == null ? null : rotaCache.get(req.getRotaId());
			dto.setFit(computeFit(req.getEmployee(), req.getShiftAssignment(), rotaForFit));
			return dto;
		}).toList();
	}

	// =====================================================================================
	// Fit computation — admin matrix view of (employee, requested shift) suitability.
	// All inputs are already in memory; no extra DB queries.
	// =====================================================================================

	private FitDTO computeFit(Employee employee, ShiftAssignment requestedSA, Rota rota) {
		if (employee == null || requestedSA == null || requestedSA.getShift() == null
				|| requestedSA.getShift().getShiftTemplate() == null) {
			return null;
		}
		Shift shift = requestedSA.getShift();
		ShiftTemplate tpl = shift.getShiftTemplate();

		Map<String, FitDTO.State> criteria = new LinkedHashMap<>();
		List<String> notes = new ArrayList<>();

		criteria.put("service", evalService(employee, tpl.getLocation()));
		criteria.put("day", evalDay(employee, shift.getShiftStart() == null ? null : shift.getShiftStart().getDayOfWeek()));
		criteria.put("shiftType", evalShiftType(employee, tpl.getShiftType()));
		criteria.put("gender", evalGender(employee, tpl.getRequiredGender(), notes));
		criteria.put("skills", evalSkills(employee, tpl.getRequiredSkills(), notes));
		criteria.put("region", evalRegion(employee, tpl.getRegion()));

		HoursImpactDTO hours = computeHoursImpact(employee, requestedSA, rota);

		FitDTO.Summary summary = deriveSummary(criteria.values(), hours == null ? null : hours.getState());
		return new FitDTO(summary, criteria, hours, notes);
	}

	private FitDTO.State evalService(Employee emp, String location) {
		if (location == null) {
			return FitDTO.State.NEUTRAL;
		}
		List<String> restricted = emp.getRestrictedService();
		if (restricted != null && containsServiceName(restricted, location)) {
			return FitDTO.State.MISMATCH;
		}
		Map<String, Integer> preferred = emp.getPreferredServiceWeightsMap();
		if (preferred != null && preferred.keySet().stream().anyMatch(k -> k != null && k.equalsIgnoreCase(location))) {
			return FitDTO.State.MATCH;
		}
		return FitDTO.State.NEUTRAL;
	}

	private FitDTO.State evalDay(Employee emp, DayOfWeek day) {
		if (day == null) {
			return FitDTO.State.NEUTRAL;
		}
		if (emp.getRestrictedDays() != null && emp.getRestrictedDays().contains(day)) {
			return FitDTO.State.MISMATCH;
		}
		if (emp.getPreferredDays() != null && emp.getPreferredDays().contains(day)) {
			return FitDTO.State.MATCH;
		}
		return FitDTO.State.NEUTRAL;
	}

	private FitDTO.State evalShiftType(Employee emp, ShiftType type) {
		if (type == null) {
			return FitDTO.State.NEUTRAL;
		}
		if (emp.getRestrictedShifts() != null && emp.getRestrictedShifts().contains(type)) {
			return FitDTO.State.MISMATCH;
		}
		if (emp.getPreferredShifts() != null && emp.getPreferredShifts().contains(type)) {
			return FitDTO.State.MATCH;
		}
		return FitDTO.State.NEUTRAL;
	}

	private FitDTO.State evalGender(Employee emp, Gender required, List<String> notes) {
		if (required == null) {
			return FitDTO.State.MATCH;
		}
		// Treat ANY/UNSPECIFIED-style values as no requirement; otherwise require equality.
		if ("ANY".equalsIgnoreCase(required.name())) {
			return FitDTO.State.MATCH;
		}
		if (emp.getGender() == null) {
			notes.add("Shift requires gender " + required + " but employee gender is unset");
			return FitDTO.State.MISMATCH;
		}
		if (required == emp.getGender()) {
			return FitDTO.State.MATCH;
		}
		notes.add("Shift requires gender " + required + ", employee is " + emp.getGender());
		return FitDTO.State.MISMATCH;
	}

	private FitDTO.State evalSkills(Employee emp, List<String> required, List<String> notes) {
		if (required == null || required.isEmpty()) {
			return FitDTO.State.MATCH;
		}
		Set<String> empSkills = new HashSet<>();
		if (emp.getSkills() != null) {
			for (String s : emp.getSkills()) {
				if (s != null) empSkills.add(s.trim().toLowerCase());
			}
		}
		List<String> missing = new ArrayList<>();
		for (String r : required) {
			if (r == null) continue;
			if (!empSkills.contains(r.trim().toLowerCase())) {
				missing.add(r);
			}
		}
		if (missing.isEmpty()) {
			return FitDTO.State.MATCH;
		}
		notes.add("Missing required skill(s): " + String.join(", ", missing));
		return FitDTO.State.MISMATCH;
	}

	private FitDTO.State evalRegion(Employee emp, String region) {
		if (emp.getPreferredRegion() == null || emp.getPreferredRegion().isBlank()) {
			return FitDTO.State.NEUTRAL;
		}
		if (region == null) {
			return FitDTO.State.NEUTRAL;
		}
		return emp.getPreferredRegion().equalsIgnoreCase(region) ? FitDTO.State.MATCH : FitDTO.State.MISMATCH;
	}

	private boolean containsServiceName(List<String> list, String location) {
		// List entries may be plain ("Cedar House") or weighted ("Cedar House:60"). Strip the suffix.
		for (String entry : list) {
			if (entry == null) continue;
			String name = entry.contains(":") ? entry.substring(0, entry.indexOf(':')) : entry;
			if (name.trim().equalsIgnoreCase(location)) {
				return true;
			}
		}
		return false;
	}

	private HoursImpactDTO computeHoursImpact(Employee employee, ShiftAssignment requestedSA, Rota rota) {
		Integer week = requestedSA.getShift().getAbsoluteWeek();
		BigDecimal current = BigDecimal.ZERO;
		if (rota != null && rota.getShiftAssignmentList() != null && week != null) {
			for (ShiftAssignment sa : rota.getShiftAssignmentList()) {
				if (sa.getEmployee() == null) continue;
				if (!employee.getId().equals(sa.getEmployee().getId())) continue;
				if (sa.getShift() == null) continue;
				if (!week.equals(sa.getShift().getAbsoluteWeek())) continue;
				BigDecimal d = sa.getShift().getDurationInHours();
				if (d != null) current = current.add(d);
			}
		}
		BigDecimal shiftHrs = requestedSA.getShift().getDurationInHours();
		if (shiftHrs == null) shiftHrs = BigDecimal.ZERO;
		BigDecimal after = current.add(shiftHrs);

		BigDecimal minH = employee.getMinHrs();
		BigDecimal maxH = employee.getMaxHrs();
		FitDTO.State state;
		if (maxH != null && after.compareTo(maxH) > 0) {
			state = FitDTO.State.MISMATCH;
		} else if (minH == null && maxH == null) {
			state = FitDTO.State.NEUTRAL;
		} else if (minH != null && after.compareTo(minH) < 0) {
			state = FitDTO.State.NEUTRAL;
		} else {
			state = FitDTO.State.MATCH;
		}
		return new HoursImpactDTO(week, current, shiftHrs, after, minH, maxH, state);
	}

	private FitDTO.Summary deriveSummary(java.util.Collection<FitDTO.State> criteriaStates, FitDTO.State hoursState) {
		int matches = 0;
		int mismatches = 0;
		for (FitDTO.State s : criteriaStates) {
			if (s == FitDTO.State.MATCH) matches++;
			else if (s == FitDTO.State.MISMATCH) mismatches++;
		}
		if (hoursState == FitDTO.State.MISMATCH) mismatches++;
		else if (hoursState == FitDTO.State.MATCH) matches++;

		if (mismatches > 0) return FitDTO.Summary.WEAK;
		if (matches >= 3) return FitDTO.Summary.STRONG;
		return FitDTO.Summary.OK;
	}

	@Transactional
	public ShiftRequestDTO resolveRequest(Long requestId, String adminUsername, String action) {
		if (!"APPROVE".equals(action) && !"REJECT".equals(action)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "action must be APPROVE or REJECT");
		}

		deferredSolveRequestRepository.findFirstByCompletedFalse().ifPresent(d -> {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"A solver run is in progress; try again once it completes");
		});

		ShiftRequest req = shiftRequestRepository.findById(requestId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"Shift request not found: " + requestId));

		if (req.getStatus() != ShiftRequestStatus.PENDING) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"Shift request is not pending (status=" + req.getStatus() + ")");
		}

		LocalDateTime now = LocalDateTime.now();
		if ("APPROVE".equals(action)) {
			ShiftAssignment assignment = req.getShiftAssignment();
			if (assignment.getEmployee() != null) {
				throw new ResponseStatusException(HttpStatus.CONFLICT,
						"Shift is already allocated");
			}
			assignment.setEmployee(req.getEmployee());
			try {
				// saveAndFlush forces the version-bump to run NOW so a concurrent
				// approval surfaces as 409 *before* we send any FCM pushes.
				shiftAssignmentRepository.saveAndFlush(assignment);
			} catch (OptimisticLockingFailureException e) {
				log.info("Optimistic lock collision approving request {} on assignment {}: {}",
						req.getId(), assignment.getId(), e.getMessage());
				throw new ResponseStatusException(HttpStatus.CONFLICT,
						"Shift was just allocated by another admin; refresh and try again");
			}

			req.setStatus(ShiftRequestStatus.APPROVED);
			req.setResolvedAt(now);
			req.setResolvedBy(adminUsername);
			shiftRequestRepository.save(req);

			Map<String, String> approvedData = new HashMap<>();
			approvedData.put("rotaId", String.valueOf(req.getRotaId()));
			approvedData.put("shiftAssignmentId", String.valueOf(assignment.getId()));
			approvedData.put("type", "SHIFT_APPROVED");
			fcm.sendToEmployee(req.getEmployee().getId(),
					"Shift allocated",
					"Your requested shift has been approved",
					approvedData);

			List<ShiftRequest> others = shiftRequestRepository
					.findByShiftAssignmentIdAndStatus(assignment.getId(), ShiftRequestStatus.PENDING);
			int filledCount = 0;
			for (ShiftRequest other : others) {
				if (other.getId().equals(req.getId())) {
					continue;
				}
				other.setStatus(ShiftRequestStatus.FILLED);
				other.setResolvedAt(now);
				other.setResolvedBy(adminUsername);
				shiftRequestRepository.save(other);
				filledCount++;

				Map<String, String> filledData = new HashMap<>();
				filledData.put("rotaId", String.valueOf(other.getRotaId()));
				filledData.put("shiftAssignmentId", String.valueOf(assignment.getId()));
				filledData.put("type", "SHIFT_FILLED");
				fcm.sendToEmployee(other.getEmployee().getId(),
						"Shift filled",
						"A shift you requested has been allocated to another employee",
						filledData);
			}
			log.info("Approved request {} (employee {} -> assignment {}); marked {} other requests as FILLED",
					req.getId(), req.getEmployee().getId(), assignment.getId(), filledCount);
		} else {
			req.setStatus(ShiftRequestStatus.REJECTED);
			req.setResolvedAt(now);
			req.setResolvedBy(adminUsername);
			shiftRequestRepository.save(req);

			Map<String, String> rejectedData = new HashMap<>();
			rejectedData.put("rotaId", String.valueOf(req.getRotaId()));
			rejectedData.put("shiftAssignmentId", String.valueOf(req.getShiftAssignment().getId()));
			rejectedData.put("type", "SHIFT_REJECTED");
			fcm.sendToEmployee(req.getEmployee().getId(),
					"Shift request rejected",
					"Your shift request was not approved",
					rejectedData);
			log.info("Rejected request {} (employee {})", req.getId(), req.getEmployee().getId());
		}

		return ShiftRequestDTO.fromEntity(req);
	}
}
