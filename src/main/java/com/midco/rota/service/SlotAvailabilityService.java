package com.midco.rota.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.midco.rota.model.ShiftAssignment;
import com.midco.rota.repository.PublishLogRepository;
import com.midco.rota.repository.ShiftAssignmentRepository;
import com.midco.rota.repository.ShiftRequestRepository;

/**
 * Enforces the response-bound visibility rules for published slots:
 * <ul>
 *   <li>Rule 1: ≥ 5 requests since publish → withhold immediately.</li>
 *   <li>Rule 2: ≥ 24h since publish AND ≥ 1 request → withhold.</li>
 *   <li>else: stay visible.</li>
 * </ul>
 *
 * <p>Requests are counted since the slot's <b>most recent</b> publish, so
 * re-publishing — which also clears the withheld flag in
 * {@code ShiftPublishService} — restarts the window. Withholding sets the flag
 * (so {@code listAvailable} can filter cheaply) and broadcasts a silent
 * {@code SHIFT_UNAVAILABLE} so foreground apps drop the slot at once; sleeping
 * apps pick it up on their next refresh via the flag.
 */
@Service
public class SlotAvailabilityService {

	private static final Logger log = LoggerFactory.getLogger(SlotAvailabilityService.class);

	private static final int REQUEST_THRESHOLD = 5;
	private static final Duration RESPONSE_WINDOW = Duration.ofHours(24);

	private final ShiftAssignmentRepository assignmentRepository;
	private final ShiftRequestRepository requestRepository;
	private final PublishLogRepository publishLogRepository;
	private final FcmPushNotificationService fcm;

	public SlotAvailabilityService(ShiftAssignmentRepository assignmentRepository,
			ShiftRequestRepository requestRepository, PublishLogRepository publishLogRepository,
			FcmPushNotificationService fcm) {
		this.assignmentRepository = assignmentRepository;
		this.requestRepository = requestRepository;
		this.publishLogRepository = publishLogRepository;
		this.fcm = fcm;
	}

	/** Real-time check after a new request — applies Rule 1 (and Rule 2 if already due). */
	@Transactional
	public void onRequestReceived(ShiftAssignment assignment) {
		if (assignment == null || assignment.getEmployee() != null || assignment.isWithheld()) {
			return;
		}
		if (shouldWithhold(assignment)) {
			withhold(assignment);
		}
	}

	/** Hourly sweep for the time-based rule (and a backstop for the count rule). */
	@Scheduled(cron = "0 0 * * * *")
	@Transactional
	public void sweep() {
		List<Long> ids = requestRepository.findDistinctShiftAssignmentIds();
		if (ids.isEmpty()) {
			return;
		}
		int count = 0;
		for (ShiftAssignment a : assignmentRepository.findAllById(ids)) {
			if (a.getEmployee() != null || a.isWithheld()) {
				continue;
			}
			if (shouldWithhold(a)) {
				withhold(a);
				count++;
			}
		}
		if (count > 0) {
			log.info("Availability sweep withheld {} slot(s)", count);
		}
	}

	private boolean shouldWithhold(ShiftAssignment a) {
		LocalDateTime publishedAt = publishLogRepository.findLatestPublishedAt(rotaId(a), service(a));
		if (publishedAt == null) {
			return false; // never published — not subject to the rules
		}
		long requests = requestRepository.countByShiftAssignmentIdAndRequestedAtAfter(a.getId(), publishedAt);
		if (requests >= REQUEST_THRESHOLD) {
			return true; // Rule 1
		}
		boolean past = publishedAt.plus(RESPONSE_WINDOW).isBefore(LocalDateTime.now());
		return past && requests >= 1; // Rule 2
	}

	private void withhold(ShiftAssignment a) {
		a.setWithheld(true);
		assignmentRepository.save(a);

		Map<String, String> data = new HashMap<>();
		data.put("type", "SHIFT_UNAVAILABLE");
		data.put("shiftAssignmentId", String.valueOf(a.getId()));
		if (rotaId(a) != null) {
			data.put("rotaId", String.valueOf(rotaId(a)));
		}
		fcm.broadcastDataToEmployeesTopic(data);
		log.info("Withheld assignment {} from the Available list", a.getId());
	}

	private static Long rotaId(ShiftAssignment a) {
		return a.getRota() != null ? a.getRota().getId() : null;
	}

	private static String service(ShiftAssignment a) {
		if (a.getShift() == null || a.getShift().getShiftTemplate() == null) {
			return null;
		}
		return a.getShift().getShiftTemplate().getLocation();
	}
}
